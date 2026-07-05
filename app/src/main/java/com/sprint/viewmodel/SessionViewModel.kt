package com.sprint.viewmodel

import android.app.Application
import android.content.Context
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sprint.data.local.repository.LocalSprintRepository
import com.sprint.model.ApiStatus
import com.sprint.model.GestureConfig
import com.sprint.model.CalibrationSample
import com.sprint.model.DrillFlushResult
import com.sprint.model.HeatmapDay
import com.sprint.model.ReviewSuggestion
import com.sprint.model.SprintHistoryItem
import com.sprint.model.SkillErrorAnalysis
import com.sprint.model.FragileSkill
import com.sprint.model.ReviewPlanItem
import com.sprint.model.SessionSummary
import com.sprint.model.Folha
import com.sprint.model.FolhaField
import com.sprint.model.SessionConfig
import com.sprint.model.SessionStatus
import com.sprint.model.SubmitResult
import com.sprint.util.SessionLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import java.util.UUID

/** Resultado visual de cada tentativa — alimenta o histórico scrollável na Sprint. */
enum class ResultMark { CORRECT, WRONG, EMPTY }

data class SessionUiState(
    val studentId: String = UUID.randomUUID().toString(),
    val sessionId: String? = null,
    val config: SessionConfig = SessionConfig(),
    val currentFolha: Folha? = null,
    val lastResult: SubmitResult? = null,
    val status: SessionStatus = SessionStatus.GESTURE_ONBOARDING,
    val apiStatus: ApiStatus = ApiStatus.OK,
    val errorMessage: String? = null,
    // Dashboard state
    val selectedSkillTag: String = "soma_subtracao",
    val densityLevel: String = "medium",   // "high" | "medium" | "low": sprint | fixacao | atencao
    val identifiedSkillTag: String? = null, // resultado do OCR do dashboard canvas
    // Drill state
    val drillResult: DrillFlushResult? = null,
    // Canvas state
    val isPaused: Boolean = false,
    val reviewSkills: List<String> = emptyList(),
    val skillStatuses: Map<String, String> = emptyMap(),
    val skillAttempts: Map<String, Int> = emptyMap(),
    val skillAvailable: Map<String, Int> = emptyMap(),
    val skillAccuracy: Map<String, Float> = emptyMap(),
    // Session stats (cumulative across folhas in current session)
    val sessionCorrect: Int = 0,
    val sessionTotal: Int = 0,
    // Mid-sprint notes — cardeadas (tagged to session + exercise)
    val notes: List<SprintNote> = emptyList(),
    // Gesture configuration — user-overridable
    val gestureConfig: GestureConfig = GestureConfig(),
    // Sprint history — loaded from API
    val sprintHistory: List<SprintHistoryItem> = emptyList(),
    val activityDays: List<HeatmapDay> = emptyList(),
    // Histórico visual dos últimos 7 resultados (newest = último da lista)
    val recentResults: List<ResultMark> = emptyList(),
    // Flow adaptativo: engine sinaliza, usuario decide.
    val consecutiveFails: Int = 0,
    val scoreRiskVisible: Boolean = false,
    val scoreRiskDismissedAt: Int? = null,
    val masteryDetected: Boolean = false,    // 5 corretos seguidos -> sugerir próximo tema
    val suggestedNextSkill: String? = null,  // skill sugerida (linear na árvore)
    // Offline-First Anti-Cheat
    val anomaliesLog: List<String> = emptyList(),
    val errorAnalysis: List<SkillErrorAnalysis> = emptyList(),
    val fragileSkills: List<FragileSkill> = emptyList(),
    val reviewPlan: List<ReviewPlanItem> = emptyList(),
    val sessionSummary: SessionSummary? = null,
)

class SessionViewModel(application: Application) : AndroidViewModel(application) {
    private val localRepository = LocalSprintRepository.getInstance(application)
    private val isRemoteMode = false
    private val prefs = application.getSharedPreferences("sprint_prefs", Context.MODE_PRIVATE)
    private val stableStudentId = prefs.getString("sprint_student_id_v1", null)
        ?: UUID.randomUUID().toString().also { generated ->
            prefs.edit().putString("sprint_student_id_v1", generated).apply()
        }

    private val _uiState = MutableStateFlow(SessionUiState(studentId = stableStudentId))
    val uiState: StateFlow<SessionUiState> = _uiState

    init {
        // Skip onboarding/dashboard — go straight to the first exercise session.
        // Set DASHBOARD while the API call is in-flight so MainActivity has a valid state to render.
        _uiState.update { it.copy(status = SessionStatus.DASHBOARD) }
        startSessionFromDashboard()
        fetchHistory()
        fetchSkillProgress()
        fetchActivity()
        // Carregar notas persistidas
        val notesRaw = prefs.getString("sprint_notes_v1", "[]") ?: "[]"
        val savedNotes = runCatching {
            NotesJson.decodeFromString<List<SprintNoteJson>>(notesRaw).map { it.toSprintNote() }
        }.getOrDefault(emptyList())
        if (savedNotes.isNotEmpty()) {
            _uiState.update { it.copy(notes = savedNotes) }
        }

        // Carregar config persistida
        val configRaw = prefs.getString("sprint_config_v1", null)
        if (configRaw != null) {
            runCatching {
                val savedConfig = kotlinx.serialization.json.Json.decodeFromString<SessionConfig>(configRaw)
                _uiState.update { it.copy(config = savedConfig) }
            }
        }
    }

    // ── Config ───────────────────────────────────────────────────────────────

    fun updateConfig(config: SessionConfig) {
        _uiState.update { it.copy(config = config) }
        prefs.edit().putString("sprint_config_v1", kotlinx.serialization.json.Json.encodeToString(config)).apply()
    }

    // ── Gesture onboarding ───────────────────────────────────────────────────

    fun onGestureOnboardingComplete() {
        prefs.edit().putBoolean("sprint_gesture_done", true).apply()
        _uiState.update { it.copy(status = SessionStatus.DASHBOARD) }
    }

    // ── Calibration ──────────────────────────────────────────────────────────

    fun onCalibrationComplete(skipped: Boolean) {
        markCalibrationDone()
        _uiState.update { it.copy(status = SessionStatus.DASHBOARD) }
    }

    fun submitCalibration(samples: List<CalibrationSample>) {
        _uiState.update {
            it.copy(
                currentFolha = null,
                lastResult = null,
                apiStatus = ApiStatus.CONNECTING,
                errorMessage = null,
            )
        }
        viewModelScope.launch {
            markCalibrationDone()
            _uiState.update { it.copy(status = SessionStatus.DASHBOARD, apiStatus = ApiStatus.OK) }
        }
    }

    // ── Dashboard ────────────────────────────────────────────────────────────

    fun selectSkill(tag: String) {
        val cleanedConfig = _uiState.value.config.copy(
            skillPin = null,
            templatePin = null,
            focusSourceExerciseId = null,
        )
        _uiState.update {
            it.copy(
                selectedSkillTag = tag,
                identifiedSkillTag = null,
                densityLevel = if (it.densityLevel == "exact") "medium" else it.densityLevel,
                config = cleanedConfig,
            )
        }
        startSession(skillTag = tag, density = if (_uiState.value.densityLevel == "exact") "medium" else _uiState.value.densityLevel, baseConfig = cleanedConfig)
    }

    fun selectDensity(level: String) {
        val normalized = when (level) {
            "simulado" -> "high"
            "fixação", "fixacao" -> "medium"
            "maratona" -> "low"
            else -> level
        }
        _uiState.update { it.copy(
            densityLevel = normalized,
            config = it.config.copy(exercisesPerPage = 1),
        ) }
    }

    fun boostCurrentExerciseDensity(fieldIndex: Int = 0) {
        val state = _uiState.value
        val field = state.currentFolha?.fields?.firstOrNull { it.fieldIndex == fieldIndex }
            ?: state.currentFolha?.fields?.firstOrNull()
            ?: return
        val templateId = field.templateId ?: return

        val exactFocusConfig = state.config.copy(
            skillPin = field.skillTags.firstOrNull() ?: state.selectedSkillTag,
            templatePin = templateId,
            focusSourceExerciseId = field.exerciseId,
            focusMode = true,
            difficultyBlockSize = 200,
            focusTargetCount = 200,
            difficultyStep = 0.1,
            exercisesPerPage = 1,
            fixationDensity = "exata",
        )
        _uiState.update {
            it.copy(
                selectedSkillTag = field.skillTags.firstOrNull() ?: it.selectedSkillTag,
                densityLevel = "exact",
                config = exactFocusConfig,
            )
        }
        startSessionFromDashboard()
    }

    fun startSimulado(rules: List<com.sprint.ui.folha.SimuladoSequenceRule>, targetTime: String, difficulty: String) {
        val rulesJson = kotlinx.serialization.json.Json.encodeToString(rules)
        val selectedConfig = _uiState.value.config.copy(
            simuladoRulesJson = rulesJson,
            difficultyStart = if (difficulty == "auto") 2.0 else difficulty.toDoubleOrNull() ?: 2.0
        )

        SessionLogger.clear()
        SessionLogger.logSessionStart("SIMULADO", rules.map { it.skill }, difficulty)

        _uiState.update {
            it.copy(
                selectedSkillTag = "simulado",
                densityLevel = "simulado",
                config = selectedConfig,
                apiStatus = ApiStatus.CONNECTING,
                errorMessage = null,
                scoreRiskVisible = false,
                scoreRiskDismissedAt = null,
                masteryDetected = false,
                suggestedNextSkill = null,
            )
        }
        startSessionFromDashboard()
    }

    fun applySprintScrollSelection(
        skillTag: String,
        density: String,
        exactCurrent: Boolean,
        difficultyStart: Double?,
        digitsCount: Int,
        valuesCount: Int,
        numberSet: String,
        field: FolhaField?,
    ) {
        val chosenSkill = skillTag
        val base = _uiState.value.config.copy(
            skillPin = null,
            templatePin = null,
            focusSourceExerciseId = null,
            difficultyStart = SessionConfig().difficultyStart,
        )
        val selectedConfig = if (exactCurrent && field?.templateId != null) {
            base.copy(
                skillPin = chosenSkill,
                templatePin = field.templateId,
                focusSourceExerciseId = field.exerciseId,
                focusMode = true,
                difficultyBlockSize = 200,
                focusTargetCount = 200,
                difficultyStep = 0.1,
                exercisesPerPage = 1,
                fixationDensity = "exata",
                digitsCount = digitsCount,
                valuesCount = valuesCount,
                numberSet = numberSet,
            )
        } else {
            val densityConfig = densityToConfig(density, base).copy(
                digitsCount = digitsCount,
                valuesCount = valuesCount,
                numberSet = numberSet,
            )
            if (difficultyStart != null) densityConfig.copy(difficultyStart = difficultyStart)
            else densityConfig
        }

        _uiState.update {
            it.copy(
                selectedSkillTag = chosenSkill,
                densityLevel = density,
                config = selectedConfig,
                apiStatus = ApiStatus.CONNECTING,
                errorMessage = null,
                scoreRiskVisible = false,
                scoreRiskDismissedAt = null,
                masteryDetected = false,
                suggestedNextSkill = null,
            )
        }
        startSessionFromDashboard()
    }

    /** Chamado pelo DashboardScreen após o aluno parar de escrever (debounce). */
    fun identifyTopic(strokes: List<List<Offset>>) {
        if (strokes.isEmpty()) return
        // Offline deterministico: sem OCR/IA para identificar tema por escrita livre.
        // A escolha de tema fica nos scrolls e na arvore.
    }

    // ── Session start (a partir do dashboard) ────────────────────────────────
    // 🚀 Zoom Out (Spatial Metaphor) 🚀
    fun zoomOutToTree(nodeId: String?) {
        _uiState.update { 
            it.copy(
                status = SessionStatus.DASHBOARD,
                selectedSkillTag = nodeId ?: "soma_subtracao"
            ) 
        }
    }

    fun startSessionFromDashboard() {
        val state = _uiState.value
        startSession(skillTag = state.selectedSkillTag, density = state.densityLevel, baseConfig = state.config)
    }

    fun _setSimuladoState(folha: Folha, simConfig: SessionConfig, onReady: () -> Unit = {}) {
        val sessionId = java.util.UUID.randomUUID().toString()
        val studentId = _uiState.value.studentId

        // Seta estado imediatamente (sincrono)
        _uiState.update {
            it.copy(
                currentFolha = folha,
                config = simConfig,
                selectedSkillTag = "soma_subtracao",
                status = SessionStatus.ACTIVE,
                sessionId = sessionId,
                recentResults = emptyList(),
                consecutiveFails = 0,
                masteryDetected = false,
                suggestedNextSkill = null,
                scoreRiskVisible = false,
                sessionSummary = null,
                lastResult = null,
            )
        }

        // Cria sessao no banco ANTES de chamar onReady
        viewModelScope.launch {
            try {
                localRepository.createSession(
                    sessionId = sessionId,
                    studentId = studentId,
                    config = simConfig,
                )
                // So chama callback apos sessao estar na DB
                onReady()
            } catch (e: Exception) {
                android.util.Log.e("SessionVM", "createSession failed", e)
                // Mesmo com erro, chama onReady para nao travar o app
                onReady()
            }
        }
    }

    private fun startSession(skillTag: String, density: String, baseConfig: SessionConfig) {
        val densityConfig = densityToConfig(density, baseConfig)
        
        SessionLogger.clear()
        SessionLogger.logSessionStart("SPRINT", listOf(skillTag), densityConfig.difficultyStart.toString())

        _uiState.update { it.copy(apiStatus = ApiStatus.CONNECTING, errorMessage = null) }
        viewModelScope.launch {
            try {
                val res = localRepository.startSession(
                    studentId = _uiState.value.studentId,
                    skillTag = skillTag,
                    density = density,
                    config = densityConfig.copy(skillPin = skillTag),
                )
                _uiState.update {
                    it.copy(
                        sessionId = res.sessionId,
                        currentFolha = res.firstFolha,
                        selectedSkillTag = skillTag,
                        status = SessionStatus.ACTIVE,
                        apiStatus = ApiStatus.OK,
                        recentResults = emptyList(),
                        consecutiveFails = 0,
                        scoreRiskVisible = false,
                        scoreRiskDismissedAt = null,
                        masteryDetected = false,
                        suggestedNextSkill = null,
                    )
                }
                fetchHistory()
                fetchSkillProgress()
                fetchActivity()
            } catch (localEx: Exception) {
                _uiState.update {
                    it.copy(
                        status = SessionStatus.DASHBOARD,
                        errorMessage = localEx.message,
                        apiStatus = ApiStatus.ERROR,
                    )
                }
            }
        }
    }

    // ── Session Summary ───────────────────────────────────────────────────────

    private fun computeSessionSummary(state: SessionUiState, folhaState: FolhaUiState): SessionSummary {
        val attempts = state.lastResult?.results ?: emptyList()
        val total = attempts.size
        val correct = attempts.count { it.isCorrect }
        val errorsByType = attempts
            .filter { !it.isCorrect }
            .groupBy { it.errorType ?: "desconhecido" }
            .mapValues { it.value.size }
        val dominantError = errorsByType.maxByOrNull { it.value }?.key
        val recommendedSkill = state.fragileSkills
            .filter { it.priority == "alta" }
            .minByOrNull { it.effectiveMastery }?.skill
            ?: state.fragileSkills.firstOrNull()?.skill
        val totalTimeMs = folhaState.fieldTiming.values.sumOf { it.totalTimeMs }
        return SessionSummary(
            totalExercises = total,
            correctCount = correct,
            accuracy = if (total > 0) correct.toFloat() / total else 0f,
            avgTimeMs = if (total > 0) totalTimeMs / total else 0L,
            totalTimeMs = totalTimeMs,
            dominantErrorType = dominantError,
            dominantErrorSkill = null,
            recommendedSkill = recommendedSkill,
            fragileSkills = state.fragileSkills.take(3),
        )
    }

    // ── Submit ───────────────────────────────────────────────────────────────

    fun submitFolha(folhaState: FolhaUiState) {
        val state = _uiState.value
        val folha = state.currentFolha
        if (folha == null) { android.util.Log.d("SUBMIT_DEBUG", "EARLY RETURN: currentFolha is null"); return }
        val sessionId = state.sessionId
        if (sessionId == null) { android.util.Log.d("SUBMIT_DEBUG", "EARLY RETURN: sessionId is null"); return }

        _uiState.update { it.copy(status = SessionStatus.SUBMITTING, apiStatus = ApiStatus.CONNECTING, errorMessage = null) }

        viewModelScope.launch {
            try {
                val res = localRepository.submitFolha(
                    studentId = state.studentId,
                    sessionId = sessionId,
                    folha = folha,
                    folhaState = folhaState,
                    config = state.config,
                )
                val finished = res.sessionStatus == "finished"
                val folhaCorrect = res.results.count { it.isCorrect }
                val folhaTotal = res.results.size

                val statement = folha.fields.firstOrNull()?.statement ?: ""
                val ans = res.results.firstOrNull()?.recognizedAnswer ?: ""
                val isCorr = res.results.firstOrNull()?.isCorrect == true
                SessionLogger.logExerciseAttempt(statement, ans, isCorr)

                val mark = when {
                    res.results.isEmpty() -> ResultMark.EMPTY
                    res.results.first().recognizedAnswer.isNullOrBlank() -> ResultMark.EMPTY
                    res.results.first().isCorrect -> ResultMark.CORRECT
                    else -> ResultMark.WRONG
                }
                android.util.Log.d("SUBMIT_DEBUG", "fields=" + folha.fields.size + " results=" + res.results.size + " allCorrect=" + res.results.count { it.isCorrect } + "/" + res.results.size + " requireCorrect=" + state.config.requireCorrectToAdvance)
                _uiState.update {
                    val updatedRecent = (it.recentResults + mark).takeLast(7)
                    val fails = updatedRecent.reversed().takeWhile { m -> m == ResultMark.WRONG }.size
                    val corrects = updatedRecent.reversed().takeWhile { m -> m == ResultMark.CORRECT }.size
                    val mastery = corrects >= 5
                    val nextSkill = if (mastery && !it.masteryDetected) nextSkillInTree(it.selectedSkillTag) else it.suggestedNextSkill
                    val dismissedAt = if (fails == 0) null else it.scoreRiskDismissedAt
                    val showScoreRisk = fails >= 5 && dismissedAt == null
                    val summary = if (finished) computeSessionSummary(it, folhaState) else null
                    val allCorrect = res.results.all { it.isCorrect }
                    val retryMode = state.config.requireCorrectToAdvance && !allCorrect
                    val nextFolha = if (!finished && !retryMode) res.nextFolha else null
                    it.copy(
                        lastResult = res,
                        currentFolha = nextFolha ?: it.currentFolha,
                        status = if (finished) SessionStatus.SESSION_SUMMARY
                                 else if (nextFolha != null) SessionStatus.ACTIVE
                                 else SessionStatus.RESULT,
                        apiStatus = ApiStatus.OK,
                        sessionCorrect = it.sessionCorrect + folhaCorrect,
                        sessionTotal = it.sessionTotal + folhaTotal,
                        recentResults = updatedRecent,
                        consecutiveFails = fails,
                        scoreRiskVisible = showScoreRisk,
                        scoreRiskDismissedAt = dismissedAt,
                        masteryDetected = mastery,
                        suggestedNextSkill = nextSkill,
                        sessionSummary = summary,
                    )
                }
                fetchSkillProgress()
                fetchHistory()
                fetchActivity()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(errorMessage = e.message, status = SessionStatus.ACTIVE, apiStatus = ApiStatus.ERROR)
                }
            }
        }
    }

    fun goToSimuladoReview() {
        // Para simulado com blindMode: vai para SESSION_SUMMARY
        // e impede que startSessionFromDashboard seja chamado
        _uiState.update {
            it.copy(
                status = SessionStatus.SESSION_SUMMARY,
                sessionSummary = computeSessionSummary(it, FolhaUiState(folhaId = it.currentFolha?.folhaId ?: "")),
            )
        }
    }

    fun goToNextFolha() {
        val result = _uiState.value.lastResult ?: return
        if (result.nextFolha != null) {
            _uiState.update { it.copy(currentFolha = result.nextFolha, status = SessionStatus.ACTIVE) }
        } else {
            // Session finished — start a fresh one on the same skill automatically.
            startSessionFromDashboard()
        }
    }

    fun resetToActive() {
        _uiState.update { it.copy(status = SessionStatus.ACTIVE) }
    }

    fun dismissMasterySuggestion() {
        _uiState.update { it.copy(masteryDetected = false, suggestedNextSkill = null) }
    }

    fun stayInCurrentSprintAfterScoreWarning() {
        _uiState.update {
            it.copy(
                scoreRiskVisible = false,
                scoreRiskDismissedAt = it.consecutiveFails,
            )
        }
    }

    fun adjustSprintAfterScoreWarning() {
        _uiState.update {
            it.copy(
                scoreRiskVisible = false,
                scoreRiskDismissedAt = it.consecutiveFails,
            )
        }
    }

    fun advanceToNextSkill() {
        val next = _uiState.value.suggestedNextSkill ?: return
        dismissMasterySuggestion()
        selectSkill(next)
    }

    private fun nextSkillInTree(current: String): String? {
        val idx = SKILL_SEQUENCE.indexOf(current)
        return if (idx in 0 until SKILL_SEQUENCE.lastIndex) SKILL_SEQUENCE[idx + 1] else null
    }

    fun resetToConfig() {
        _uiState.update { it.copy(status = SessionStatus.DASHBOARD, errorMessage = null, apiStatus = ApiStatus.OK) }
    }

    // ── Ranking ──────────────────────────────────────────────────────────────

    fun showRanking() {
        _uiState.update { it.copy(status = SessionStatus.RANKING) }
    }

    // ── Drill ────────────────────────────────────────────────────────────────

    fun startDrill() {
        _uiState.update { it.copy(status = SessionStatus.DRILL, drillResult = null) }
    }

    fun onDrillDone(result: DrillFlushResult) {
        _uiState.update { it.copy(status = SessionStatus.DRILL_RESULT, drillResult = result) }
    }

    // ── Canvas pause / resume ────────────────────────────────────────────────

    fun pauseSession() {
        _uiState.update { it.copy(isPaused = true) }
        fetchReviewSuggestions()
    }

    fun resumeSession() {
        _uiState.update { it.copy(isPaused = false) }
    }

    fun logAnomaly(event: String) {
        val timestamp = System.currentTimeMillis()
        val entry = "{\"event\":\"$event\",\"timestamp\":$timestamp}"
        _uiState.update { it.copy(anomaliesLog = it.anomaliesLog + entry) }
    }

    fun addNote(note: SprintNote) {
        val updated = _uiState.value.notes + note
        _uiState.update { it.copy(notes = updated) }
        // Persistir no disco
        val json = runCatching {
            NotesJson.encodeToString(updated.map { it.toJson() })
        }.getOrDefault("[]")
        prefs.edit().putString("sprint_notes_v1", json).apply()
    }

    fun fetchHistory() {
        val studentId = _uiState.value.studentId
        viewModelScope.launch {
            try {
                val history = localRepository.history(studentId)
                _uiState.update { it.copy(sprintHistory = history) }
            } catch (_: Exception) { /* analytics best-effort */ }
        }
    }

    fun fetchSkillProgress() {
        val studentId = _uiState.value.studentId
        viewModelScope.launch {
            try {
                val progress = localRepository.skillProgress(studentId)
                _uiState.update {
                    it.copy(
                        skillStatuses = it.skillStatuses + progress.associate { item -> item.skill to item.status },
                        skillAttempts = progress.associate { item -> item.skill to item.attemptCount },
                        skillAvailable = progress.associate { item -> item.skill to item.availableCount },
                        skillAccuracy = progress.associate { item -> item.skill to item.accuracy },
                    )
                }
            } catch (_: Exception) { /* analytics best-effort */ }
        }
    }

    fun fetchActivity() {
        val studentId = _uiState.value.studentId
        viewModelScope.launch {
            try {
                val activity = localRepository.activity(studentId, days = 35)
                _uiState.update { it.copy(activityDays = activity) }
            } catch (_: Exception) { /* analytics best-effort */ }
        }
    }

    fun updateGesture(action: String, gesture: String) {
        _uiState.update { it.copy(gestureConfig = it.gestureConfig.withMapping(action, gesture)) }
    }

    private fun fetchReviewSuggestions() {
        val studentId = _uiState.value.studentId
        viewModelScope.launch {
            try {
                val suggestions = localRepository.skillProgress(studentId)
                    .filter { it.attemptCount > 0 && it.accuracy < 0.70f }
                    .map {
                        ReviewSuggestion(
                            skill = it.skill,
                            status = it.status,
                            accuracy = it.accuracy,
                            daysIdle = 0,
                            attemptCount = it.attemptCount,
                        )
                    }
                val statuses = suggestions.associate { it.skill to it.status }
                _uiState.update {
                    it.copy(
                        reviewSkills = suggestions.map { s -> s.skill },
                        skillStatuses = it.skillStatuses + statuses,
                        skillAttempts = it.skillAttempts + suggestions.associate { s -> s.skill to s.attemptCount },
                        skillAccuracy = it.skillAccuracy + suggestions.associate { s -> s.skill to s.accuracy },
                    )
                }
            } catch (_: Exception) {
                // Map still renders — review suggestions are best-effort
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private fun needsGestureOnboarding() = !prefs.getBoolean("gesture_done", false)
    private fun needsCalibration() = !prefs.getBoolean("calibration_done", false)
    private fun markCalibrationDone() = prefs.edit().putBoolean("calibration_done", true).apply()

    // Densidade controla repeticao e ritmo:
    // high/Simulado: menos fixacao, sobe rapido.
    // medium/Fixacao: padrao 30/300.
    // low/Atencao: mais fixacao, sobe devagar.
    private fun densityToConfig(density: String, base: SessionConfig): SessionConfig = when (density) {
        "kplus" -> base.copy(difficultyStep = 0.5, focusMode = true, difficultyBlockSize = 20, focusTargetCount = 200, exercisesPerPage = base.exercisesPerPage.coerceAtLeast(2), fixationDensity = "kplus")
        "exact" -> base.copy(focusMode = true, difficultyBlockSize = 200, focusTargetCount = 200, exercisesPerPage = 1, fixationDensity = "exata")
        "high" -> base.copy(difficultyStep = 0.8, focusMode = true, difficultyBlockSize = 15, focusTargetCount = 150, exercisesPerPage = base.exercisesPerPage, fixationDensity = "leve")
        "low"  -> base.copy(difficultyStep = 0.25, focusMode = true, difficultyBlockSize = 60, focusTargetCount = 600, exercisesPerPage = base.exercisesPerPage, fixationDensity = "densa")
        else   -> base.copy(difficultyStep = 0.5, focusMode = true, difficultyBlockSize = 30, focusTargetCount = 300, exercisesPerPage = base.exercisesPerPage, fixationDensity = "fixa")
    }


    fun fetchAnalytics() {
        val studentId = _uiState.value.studentId
        viewModelScope.launch {
            try {
                val memory = localRepository.skillProgress(studentId)
                val attempted = memory.filter { it.attemptCount > 0 }
                
                val errorList: List<SkillErrorAnalysis> = attempted.mapNotNull { item ->
                    val errors = (item.attemptCount * (1f - item.accuracy)).toInt()
                    SkillErrorAnalysis(
                        skill = item.skill,
                        totalAttempts = item.attemptCount,
                        errorCount = errors,
                        accuracy = item.accuracy,
                        dominantError = if (item.accuracy < 0.7f) "dificuldade" else null,
                        dominantErrorPct = if (errors > 0) errors.toFloat() / item.attemptCount else null,
                    )
                }.filter { it.errorCount > 0 }.sortedByDescending { it.errorCount }
                
                val fragile: List<FragileSkill> = attempted.filter { it.accuracy < 0.7f || it.attemptCount < 10 }.map { item ->
                    val priority = when {
                        item.accuracy < 0.5f -> "alta"
                        item.accuracy < 0.7f -> "media"
                        else -> "baixa"
                    }
                    FragileSkill(
                        skill = item.skill,
                        accuracy = item.accuracy,
                        attemptCount = item.attemptCount,
                        daysIdle = 0,
                        effectiveMastery = item.accuracy,
                        errorTrend = "stable",
                        priority = priority,
                        suggestion = when (priority) {
                            "alta" -> "Dominio baixo (${(item.accuracy * 100).toInt()}%). Revisar."
                            "media" -> "Em desenvolvimento (${(item.accuracy * 100).toInt()}%). Praticar mais."
                            else -> "Evoluindo. Manter ritmo."
                        },
                    )
                }.sortedBy { frag: FragileSkill -> mapOf("alta" to 0, "media" to 1, "baixa" to 2).getOrDefault(frag.priority, 3) }
                
                val plan: List<ReviewPlanItem> = fragile.take(5).mapIndexed { i, f ->
                    ReviewPlanItem(
                        skill = f.skill,
                        priority = i + 1,
                        reason = f.suggestion,
                        suggestedExercises = if (f.priority == "alta") 20 else 10,
                    )
                }
                
                _uiState.update { it.copy(
                    errorAnalysis = errorList,
                    fragileSkills = fragile,
                    reviewPlan = plan,
                )}
            } catch (_: Exception) { /* analytics best-effort */ }
        }
    }

    companion object {
        /** Ordem linear da árvore — usada para sugerir próximo tema no mastery. */
        val SKILL_SEQUENCE: List<String> by lazy {
            com.sprint.model.MathCurriculum.getFlatNodes()
                .filter { it.children.isEmpty() }
                .map { it.proceduralTag ?: it.id }
        }
    }
}
