package com.strava_matematica.data.local.repository

import android.content.Context
import com.strava_matematica.data.local.catalog.ExerciseEntity
import com.strava_matematica.data.local.catalog.SprintCatalogDatabase
import com.strava_matematica.data.local.runtime.ExerciseAttemptEntity
import com.strava_matematica.data.local.runtime.PenEventEntity
import com.strava_matematica.data.local.runtime.SessionEntity
import com.strava_matematica.data.local.runtime.SprintRuntimeDatabase
import com.strava_matematica.data.local.runtime.StudentEntity
import com.strava_matematica.data.local.runtime.StudentSkillMemoryEntity
import com.strava_matematica.model.FieldResult
import com.strava_matematica.model.Folha
import com.strava_matematica.model.FolhaField
import com.strava_matematica.domain.procedural.ProceduralEngine
import com.strava_matematica.domain.procedural.ProceduralExercise
import com.strava_matematica.domain.procedural.EloMatchmaker
import com.strava_matematica.domain.procedural.MathBktEngine
import com.strava_matematica.model.HeatmapDay
import com.strava_matematica.model.PenEvent
import com.strava_matematica.model.SessionConfig
import com.strava_matematica.model.SessionStartResponse
import com.strava_matematica.model.SkillProgressItem
import com.strava_matematica.model.SprintHistoryItem
import com.strava_matematica.model.SubmitResult
import com.strava_matematica.model.Thermometer
import com.strava_matematica.viewmodel.FolhaUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.math.max
import kotlin.math.roundToInt

class LocalSprintRepository private constructor(context: Context) {
    private val catalog = SprintCatalogDatabase.getInstance(context).exerciseDao()
    private val runtime = SprintRuntimeDatabase.getInstance(context)
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun startSession(
        studentId: String,
        skillTag: String,
        density: String,
        config: SessionConfig,
    ): SessionStartResponse = withContext(Dispatchers.IO) {
        ensureStudent(studentId)
        val sessionId = UUID.randomUUID().toString()
        runtime.sessionDao().upsert(
            SessionEntity(
                id = sessionId,
                studentId = studentId,
                startedAt = System.currentTimeMillis(),
                endedAt = null,
                skillPin = skillTag,
                density = density,
                templatePin = config.templatePin,
                configJson = json.encodeToString(config),
            ),
        )
        SessionStartResponse(
            sessionId = sessionId,
            configId = "local",
            firstFolha = nextFolha(studentId, sessionId, skillTag, config, pageIndex = 0),
        )
    }

    suspend fun submitFolha(
        studentId: String,
        sessionId: String,
        folha: Folha,
        folhaState: FolhaUiState,
        config: SessionConfig,
    ): SubmitResult = withContext(Dispatchers.IO) {
        val session = runtime.sessionDao().getById(sessionId)
        val skill = session?.skillPin ?: config.skillPin ?: folha.fields.firstOrNull()?.skillTags?.firstOrNull() ?: "soma_subtracao"
        val now = System.currentTimeMillis()

        val results = folha.fields.map { field ->
            val typedAnswer = folhaState.fieldTypedAnswers[field.fieldIndex].orEmpty()
            val expected = field.expectedAnswer.orEmpty()
            val validator = field.validatorType ?: "exact"
            val correct = DeterministicValidator.evaluate(typedAnswer, expected, validator)
            val durationMs = folhaState.fieldTiming[field.fieldIndex]?.totalTimeMs ?: 0L
            val attemptId = runtime.attemptDao().insert(
                ExerciseAttemptEntity(
                    sessionId = sessionId,
                    studentId = studentId,
                    exerciseId = field.exerciseId,
                    skill = field.skillTags.firstOrNull() ?: skill,
                    isCorrect = correct,
                    userResponse = typedAnswer,
                    expectedAnswer = expected,
                    validatorType = validator,
                    attemptTimestamp = now,
                    durationSeconds = max(1, (durationMs / 1000L).toInt()),
                ),
            )
            persistPenEvents(attemptId, folhaState.fieldEvents[field.fieldIndex].orEmpty(), now)
            updateSkillMemory(studentId, field.skillTags.firstOrNull() ?: skill, correct, durationMs)

            FieldResult(
                fieldIndex = field.fieldIndex,
                recognizedAnswer = typedAnswer.ifBlank { null },
                expectedAnswer = expected,
                isCorrect = correct,
                score = if (correct) 100 else 0,
                errorType = if (correct) null else "deterministic_mismatch",
                recognitionEngine = "structured_local",
                recognitionConfidence = 1.0f,
                analysisReliable = true,
            )
        }

        val correct = results.count { it.isCorrect }
        val pageScore = if (results.isEmpty()) 0 else ((correct.toDouble() / results.size) * 100).roundToInt()
        SubmitResult(
            results = results,
            pageScore = pageScore,
            thermometer = Thermometer(value = pageScore.toDouble(), trend = if (pageScore >= 70) "up" else "down"),
            restartTriggered = false,
            sessionStatus = "active",
            nextFolha = nextFolha(
                studentId = studentId,
                sessionId = sessionId,
                skillTag = skill,
                config = config.copy(skillPin = skill),
                pageIndex = folha.pageIndex + 1,
            ),
        )
    }

    suspend fun history(studentId: String): List<SprintHistoryItem> = withContext(Dispatchers.IO) {
        runtime.sessionDao().history(studentId).map { item ->
            SprintHistoryItem(
                sessionId = item.sessionId,
                skill = item.skill,
                density = item.density,
                template = item.template,
                exercisesDone = item.exercisesDone,
                accuracy = item.accuracy,
                durationMin = item.durationMin,
                startedAt = dayFormat.format(Date(item.startedAtMs)),
                isActive = item.isActive,
            )
        }
    }

    suspend fun skillProgress(studentId: String): List<SkillProgressItem> = withContext(Dispatchers.IO) {
        val available = catalog.availableBySkill().associate { it.skill to it.available }
        val memory = runtime.skillMemoryDao().all(studentId).associateBy { it.skill }
        available.map { (skill, count) ->
            val item = memory[skill]
            val attempts = item?.totalAttempts ?: 0
            val accuracy = if (attempts > 0) (item?.correctAttempts ?: 0).toFloat() / attempts else 0f
            SkillProgressItem(
                skill = skill,
                status = when {
                    attempts == 0 -> "novo"
                    accuracy >= 0.85f -> "forte"
                    accuracy < 0.60f -> "treinar"
                    else -> "em_progresso"
                },
                accuracy = accuracy,
                fluency = (item?.masterScore ?: 0.0).toFloat(),
                retention = (item?.masterScore ?: 0.0).toFloat(),
                velocity = 0f,
                stability = accuracy,
                fixation = attempts.coerceAtMost(300) / 300f,
                attemptCount = attempts,
                availableCount = count,
                needsTraining = if (attempts > 0 && accuracy < 0.60f) "fixar" else null,
            )
        }
    }

    suspend fun activity(studentId: String, days: Int): List<HeatmapDay> = withContext(Dispatchers.IO) {
        val from = System.currentTimeMillis() - days * 86_400_000L
        val byDate = runtime.attemptDao().activity(studentId, from).associate { it.date to it.count }
        val today = System.currentTimeMillis()
        (days - 1 downTo 0).map { offset ->
            val date = dayFormat.format(Date(today - offset * 86_400_000L))
            HeatmapDay(date = date, count = byDate[date] ?: 0)
        }
    }

    private suspend fun nextFolha(
        studentId: String,
        sessionId: String,
        skillTag: String,
        config: SessionConfig,
        pageIndex: Int,
    ): Folha {
        val fields = mutableListOf<FolhaField>()
        var difficultySum = 0.0

        if (!config.simuladoRulesJson.isNullOrEmpty()) {
            try {
                val rules = json.decodeFromString<List<com.strava_matematica.ui.folha.SimuladoSequenceRule>>(config.simuladoRulesJson)
                var globalIndex = 0
                for (rule in rules) {
                    val ruleConfig = config.copy(
                        digitsCount = rule.digits.toIntOrNull() ?: config.digitsCount,
                        valuesCount = rule.terms.toIntOrNull() ?: config.valuesCount,
                        numberSet = rule.numberSet
                    )
                    for (i in 0 until rule.quantity) {
                        val exercise = selectExercise(studentId, rule.skill, ruleConfig)
                        fields.add(exercise.toFolhaField(fieldIndex = globalIndex))
                        difficultySum += exercise.difficulty
                        globalIndex++
                    }
                }
            } catch (e: Exception) {
                // Se der erro ao ler o json do simulado, cai para o modo padrão
                e.printStackTrace()
            }
        }

        // Modo Padrão (Fallback)
        if (fields.isEmpty()) {
            val count = config.exercisesPerPage.coerceAtLeast(1)
            for (i in 0 until count) {
                val exercise = selectExercise(studentId, skillTag, config)
                fields.add(exercise.toFolhaField(fieldIndex = i))
                difficultySum += exercise.difficulty
            }
        }
        
        val count = fields.size
        val avgDifficulty = if (count > 0) difficultySum / count else 2.0
        val firstExerciseId = fields.firstOrNull()?.exerciseId ?: "default"
        
        return Folha(
            folhaId = "$sessionId:$pageIndex:$firstExerciseId",
            pageIndex = pageIndex,
            difficulty = avgDifficulty,
            fields = fields,
        )
    }

    private suspend fun selectExercise(
        studentId: String,
        skillTag: String,
        config: SessionConfig,
    ): ProceduralExercise {
        val currentMemory = runtime.skillMemoryDao().get(studentId, skillTag)
        val mmr = currentMemory?.masterScore?.let { EloMatchmaker.masterScoreToMmr(it) } ?: 1000
        
        val useProcedural = skillTag == "soma_subtracao" || skillTag == "multiplicacao_divisao"
        if (!useProcedural) {
            // 1. Tentar buscar do catálogo SQLite offline real de 16k exercícios para skills avançadas
            val count = catalog.countBySkill(skillTag)
            if (count > 0) {
                val targetDifficulty = mmr.toDouble() / 100.0
                val offset = kotlin.random.Random.nextInt(count)
                val exerciseId = catalog.exerciseIdBySkillDifficultyOffset(skillTag, targetDifficulty, 0)
                    ?: catalog.exerciseIdBySkillOffset(skillTag, offset)
                    ?: catalog.exerciseIdBySkillOffset(skillTag, 0)

                if (exerciseId != null) {
                    val entity = catalog.getById(exerciseId)
                    if (entity != null) {
                        return ProceduralExercise(
                            id = entity.id,
                            statement = entity.statement,
                            expectedAnswer = entity.expectedAnswer,
                            primarySkill = entity.primarySkill,
                            difficulty = entity.difficulty,
                            templateId = entity.templateId ?: "default",
                            canvasMode = entity.canvasMode,
                            validatorType = entity.validatorType,
                            answerType = entity.answerType,
                        )
                    }
                }
            }
        }
        
        // Fallback síncrono ou geração didática para a engine procedural
        return ProceduralEngine.generate(skillTag, mmr, config)
    }

    private suspend fun ensureStudent(studentId: String) {
        runtime.studentDao().insert(
            StudentEntity(
                id = studentId,
                name = "Aluno SPRINT",
                createdAt = System.currentTimeMillis(),
            ),
        )
    }

    private suspend fun persistPenEvents(attemptId: Long, events: List<PenEvent>, now: Long) {
        if (events.isEmpty()) return
        runtime.attemptDao().insertPenEvents(
            events.map { event ->
                PenEventEntity(
                    attemptId = attemptId,
                    x = event.x,
                    y = event.y,
                    pressure = event.pressure,
                    eventType = event.eventType,
                    timestamp = now + event.ts,
                )
            },
        )
    }

    private suspend fun updateSkillMemory(studentId: String, skill: String, correct: Boolean, durationMs: Long) {
        val current = runtime.skillMemoryDao().get(studentId, skill)
        
        // 1. Obter anterior mastery do DB (se nulo, MathBktEngine.getInitialMastery)
        val prevMastery = current?.masterScore ?: MathBktEngine.getInitialMastery(skill)
        
        // 2. Calcular se skill está sob ineficácia/quarentena pedagógica
        val newTotalAttempts = (current?.totalAttempts ?: 0) + 1
        val newCorrectAttempts = (current?.correctAttempts ?: 0) + if (correct) 1 else 0
        val totalAccuracy = if (newTotalAttempts > 0) newCorrectAttempts.toDouble() / newTotalAttempts else 0.0
        val isUnderInefficacy = (newTotalAttempts >= 5 && totalAccuracy < 0.4) || (prevMastery < 0.15)
        
        // 3. Calcular consecutiveFails
        val consecutiveFails = if (!correct) {
            val recentAttempts = runtime.attemptDao().getRecentAttemptsForSkill(studentId, skill, 10)
            val previousAttempts = if (recentAttempts.isNotEmpty()) recentAttempts.drop(1) else emptyList()
            var prevFails = 0
            for (attempt in previousAttempts) {
                if (!attempt.isCorrect) {
                    prevFails++
                } else {
                    break
                }
            }
            prevFails + 1
        } else {
            0
        }
        
        // 4. Atualizar bktMastery via MathBktEngine.updateMastery
        val bktMastery = MathBktEngine.updateMastery(skill, prevMastery, correct)
        
        // 5. Calcular newMmr chamando EloMatchmaker.calculateNewMmr
        val currentMmr = current?.masterScore?.let { EloMatchmaker.masterScoreToMmr(it) } ?: 1000
        val newMmr = EloMatchmaker.calculateNewMmr(
            currentMmr = currentMmr,
            isCorrect = correct,
            timeSpentMs = durationMs,
            isUnderInefficacy = isUnderInefficacy,
            consecutiveFailsInInefficacy = consecutiveFails
        )
        
        // 6. Converter newMmr de volta para double
        val newScore = EloMatchmaker.mmrToMasterScore(newMmr)
        
        // 7. Persistir StudentSkillMemoryEntity atualizando masterScore = newScore
        runtime.skillMemoryDao().upsert(
            StudentSkillMemoryEntity(
                studentId = studentId,
                skill = skill,
                masterScore = newScore,
                totalAttempts = newTotalAttempts,
                correctAttempts = newCorrectAttempts,
                lastUpdated = System.currentTimeMillis(),
            ),
        )
    }

    private fun ProceduralExercise.toFolhaField(fieldIndex: Int): FolhaField {
        return FolhaField(
            fieldIndex = fieldIndex,
            exerciseId = id,
            subject = "matematica",
            canvasMode = canvasMode,
            statement = statement,
            skillTags = listOf(primarySkill),
            estimatedTimeMs = 15000,
            templateId = templateId,
            nodeId = null,
            methodTags = null,
            expectedAnswer = expectedAnswer,
            validatorType = validatorType,
            answerType = answerType,
        )
    }

    private fun parseStringList(raw: String?): List<String>? {
        if (raw.isNullOrBlank()) return null
        return runCatching { json.decodeFromString<List<String>>(raw) }.getOrNull()
    }

    companion object {
        private val dayFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

        @Volatile private var instance: LocalSprintRepository? = null

        fun getInstance(context: Context): LocalSprintRepository =
            instance ?: synchronized(this) {
                instance ?: LocalSprintRepository(context.applicationContext).also { instance = it }
            }
    }
}
