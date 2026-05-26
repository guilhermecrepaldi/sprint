package com.strava_matematica.viewmodel

import android.app.Application
import android.content.Context
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.strava_matematica.model.ApiStatus
import com.strava_matematica.model.GestureConfig
import com.strava_matematica.model.CalibrationRequest
import com.strava_matematica.model.CalibrationSample
import com.strava_matematica.model.DrillFlushResult
import com.strava_matematica.model.ReviewSuggestion
import com.strava_matematica.model.SprintHistoryItem
import com.strava_matematica.model.Folha
import com.strava_matematica.model.FolhaField
import com.strava_matematica.model.IdentifyTopicRequest
import com.strava_matematica.model.SessionConfig
import com.strava_matematica.model.SessionStatus
import com.strava_matematica.model.SubmitResult
import com.strava_matematica.model.SessionStartRequest
import com.strava_matematica.model.SubmitRequest
import com.strava_matematica.model.FieldSubmit
import com.strava_matematica.network.ApiClient
import com.strava_matematica.recognizer.MathRecognizer
import com.strava_matematica.recognizer.MlKitRecognizer
import com.strava_matematica.ui.folha.ImageUtils
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import java.util.UUID

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
)

class SessionViewModel(application: Application) : AndroidViewModel(application) {
    private val api = ApiClient.create()
    private val prefs = application.getSharedPreferences("love_class_prefs", Context.MODE_PRIVATE)

    // Active recognizer: ML Kit (free, on-device, ~30 MB model download on first run).
    // Swap → IinkRecognizer(application) after MyScript setup for full math notation support.
    private val recognizer: MathRecognizer = MlKitRecognizer(application)
    private val _uiState = MutableStateFlow(SessionUiState())
    val uiState: StateFlow<SessionUiState> = _uiState

    init {
        // Skip onboarding/dashboard — go straight to the first exercise session.
        // Set DASHBOARD while the API call is in-flight so MainActivity has a valid state to render.
        _uiState.update { it.copy(status = SessionStatus.DASHBOARD) }
        startSessionFromDashboard()
        fetchHistory()
        fetchSkillProgress()
        // Carregar notas persistidas
        val notesRaw = prefs.getString("sprint_notes_v1", "[]") ?: "[]"
        val savedNotes = runCatching {
            NotesJson.decodeFromString<List<SprintNoteJson>>(notesRaw).map { it.toSprintNote() }
        }.getOrDefault(emptyList())
        if (savedNotes.isNotEmpty()) {
            _uiState.update { it.copy(notes = savedNotes) }
        }
    }

    // ── Config ───────────────────────────────────────────────────────────────

    fun updateConfig(config: SessionConfig) {
        _uiState.update { it.copy(config = config) }
    }

    // ── Gesture onboarding ───────────────────────────────────────────────────

    fun onGestureOnboardingComplete() {
        prefs.edit().putBoolean("gesture_done", true).apply()
        _uiState.update { it.copy(status = SessionStatus.DASHBOARD) }
    }

    // ── Calibration ──────────────────────────────────────────────────────────

    fun onCalibrationComplete(skipped: Boolean) {
        markCalibrationDone()
        _uiState.update { it.copy(status = SessionStatus.DASHBOARD) }
    }

    fun submitCalibration(samples: List<CalibrationSample>) {
        _uiState.update { it.copy(apiStatus = ApiStatus.CONNECTING, errorMessage = null) }
        viewModelScope.launch {
            try {
                api.calibrate(
                    studentId = _uiState.value.studentId,
                    body = CalibrationRequest(samples = samples),
                )
                markCalibrationDone()
                _uiState.update { it.copy(status = SessionStatus.DASHBOARD, apiStatus = ApiStatus.OK) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        status = SessionStatus.CALIBRATION,
                        apiStatus = ApiStatus.ERROR,
                        errorMessage = e.message,
                    )
                }
            }
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
        _uiState.update { it.copy(
            densityLevel = level,
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

    fun applySprintScrollSelection(skillTag: String, density: String, exactCurrent: Boolean, field: FolhaField?) {
        val fieldSkill = field?.skillTags?.firstOrNull() ?: skillTag
        val chosenSkill = if (exactCurrent) fieldSkill else skillTag
        val base = _uiState.value.config.copy(
            skillPin = null,
            templatePin = null,
            focusSourceExerciseId = null,
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
            )
        } else {
            densityToConfig(density, base)
        }

        _uiState.update {
            it.copy(
                selectedSkillTag = chosenSkill,
                densityLevel = if (exactCurrent) "exact" else density,
                config = selectedConfig,
                apiStatus = ApiStatus.CONNECTING,
                errorMessage = null,
            )
        }
        startSessionFromDashboard()
    }

    /** Chamado pelo DashboardScreen após o aluno parar de escrever (debounce). */
    fun identifyTopic(strokes: List<List<Offset>>) {
        if (strokes.isEmpty()) return
        viewModelScope.launch {
            try {
                val base64 = ImageUtils.exportBitmap(strokes)
                val res = api.identifyTopic(IdentifyTopicRequest(imageBase64 = base64))
                if (res.confidence >= 0.5f) {
                    _uiState.update {
                        it.copy(
                            identifiedSkillTag = res.skillTag,
                            selectedSkillTag = res.skillTag,
                        )
                    }
                }
            } catch (_: Exception) {
                // identificação silenciosa — nunca travar dashboard
            }
        }
    }

    // ── Session start (a partir do dashboard) ────────────────────────────────

    fun startSessionFromDashboard() {
        val state = _uiState.value
        startSession(skillTag = state.selectedSkillTag, density = state.densityLevel, baseConfig = state.config)
    }

    private fun startSession(skillTag: String, density: String, baseConfig: SessionConfig) {
        val densityConfig = densityToConfig(density, baseConfig)
        _uiState.update { it.copy(apiStatus = ApiStatus.CONNECTING, errorMessage = null) }
        viewModelScope.launch {
            try {
                val req = SessionStartRequest(
                    studentId = _uiState.value.studentId,
                    config = densityConfig.copy(skillPin = skillTag),
                )
                val res = api.startSession(req)
                _uiState.update {
                    it.copy(
                        sessionId = res.sessionId,
                        currentFolha = res.firstFolha,
                        selectedSkillTag = skillTag,
                        status = SessionStatus.ACTIVE,
                        apiStatus = ApiStatus.OK,
                        sessionCorrect = 0,
                        sessionTotal = 0,
                    )
                }
                fetchHistory()
                fetchSkillProgress()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        status = SessionStatus.DASHBOARD,
                        errorMessage = e.message,
                        apiStatus = ApiStatus.ERROR,
                    )
                }
            }
        }
    }

    // ── Submit ───────────────────────────────────────────────────────────────

    fun submitFolha(folhaState: FolhaUiState) {
        val state = _uiState.value
        val folha = state.currentFolha ?: return
        val sessionId = state.sessionId ?: return

        _uiState.update { it.copy(status = SessionStatus.SUBMITTING, apiStatus = ApiStatus.CONNECTING, errorMessage = null) }

        viewModelScope.launch {
            try {
                // Recognize all fields concurrently (ML Kit is fast; Claude OCR is the fallback).
                val recognizedTexts: List<String?> = folha.fields.map { field ->
                    async {
                        val strokes = folhaState.fieldStrokes[field.fieldIndex].orEmpty()
                        recognizer.recognize(strokes)
                    }
                }.awaitAll()

                val fields = folha.fields.mapIndexed { i, field ->
                    val strokes = folhaState.fieldStrokes[field.fieldIndex].orEmpty()
                    val imageBase64 = ImageUtils.exportBitmap(strokes)
                    FieldSubmit(
                        fieldIndex = field.fieldIndex,
                        exerciseId = field.exerciseId,
                        imageBase64 = imageBase64,
                        totalTimeMs = folhaState.fieldTiming[field.fieldIndex]?.totalTimeMs ?: 10000L,
                        timeToFirstStrokeMs = folhaState.fieldTiming[field.fieldIndex]?.firstStrokeAtMs ?: 2000L,
                        penEvents = folhaState.fieldEvents[field.fieldIndex].orEmpty(),
                        recognizedText = recognizedTexts[i],
                        recognitionEngine = if (recognizedTexts[i] != null) "mlkit_digital_ink" else null,
                        recognitionConfidence = if (recognizedTexts[i] != null) 0.9f else null,
                    )
                }
                val req = SubmitRequest(
                    folhaId = folha.folhaId,
                    submittedAtMs = System.currentTimeMillis(),
                    fields = fields,
                )
                val res = api.submitFolha(sessionId, req)
                val finished = res.sessionStatus == "finished"
                val folhaCorrect = res.results.count { it.isCorrect }
                val folhaTotal = res.results.size
                _uiState.update {
                    it.copy(
                        lastResult = res,
                        status = if (finished) SessionStatus.FINISHED else SessionStatus.RESULT,
                        apiStatus = ApiStatus.OK,
                        sessionCorrect = it.sessionCorrect + folhaCorrect,
                        sessionTotal = it.sessionTotal + folhaTotal,
                    )
                }
                fetchSkillProgress()
                fetchHistory()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(errorMessage = e.message, status = SessionStatus.ACTIVE, apiStatus = ApiStatus.ERROR)
                }
            }
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
                val history = api.getSessionHistory(studentId)
                _uiState.update { it.copy(sprintHistory = history) }
            } catch (_: Exception) {
                // History is best-effort — dashboard still renders with stale data
            }
        }
    }

    fun fetchSkillProgress() {
        val studentId = _uiState.value.studentId
        viewModelScope.launch {
            try {
                val progress = api.getSkillProgress(studentId)
                _uiState.update {
                    it.copy(
                        skillStatuses = it.skillStatuses + progress.associate { item -> item.skill to item.status },
                        skillAttempts = progress.associate { item -> item.skill to item.attemptCount },
                        skillAvailable = progress.associate { item -> item.skill to item.availableCount },
                        skillAccuracy = progress.associate { item -> item.skill to item.accuracy },
                    )
                }
            } catch (_: Exception) {
                // Progress is best-effort; Sprint flow must keep running.
            }
        }
    }

    fun updateGesture(action: String, gesture: String) {
        _uiState.update { it.copy(gestureConfig = it.gestureConfig.withMapping(action, gesture)) }
    }

    private fun fetchReviewSuggestions() {
        val studentId = _uiState.value.studentId
        viewModelScope.launch {
            try {
                val suggestions = api.getReviewSuggestions(studentId)
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
        recognizer.release()
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
        "exact" -> base.copy(focusMode = true, difficultyBlockSize = 200, focusTargetCount = 200, exercisesPerPage = 1, fixationDensity = "exata")
        "high" -> base.copy(difficultyStep = 0.8, focusMode = true, difficultyBlockSize = 15, focusTargetCount = 150, exercisesPerPage = 1, fixationDensity = "leve")
        "low"  -> base.copy(difficultyStep = 0.25, focusMode = true, difficultyBlockSize = 60, focusTargetCount = 600, exercisesPerPage = 1, fixationDensity = "densa")
        else   -> base.copy(difficultyStep = 0.5, focusMode = true, difficultyBlockSize = 30, focusTargetCount = 300, exercisesPerPage = 1, fixationDensity = "fixa")
    }
}
