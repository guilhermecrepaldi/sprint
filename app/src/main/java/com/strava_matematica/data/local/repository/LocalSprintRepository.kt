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
            updateSkillMemory(studentId, field.skillTags.firstOrNull() ?: skill, correct, folha.difficulty)

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
        val exercise = selectExercise(studentId, skillTag, config)
        return Folha(
            folhaId = "$sessionId:$pageIndex:${exercise.id}",
            pageIndex = pageIndex,
            difficulty = exercise.difficulty,
            fields = listOf(exercise.toFolhaField()),
        )
    }

    private suspend fun selectExercise(
        studentId: String,
        skillTag: String,
        config: SessionConfig,
    ): ExerciseEntity {
        val attempts = runtime.skillMemoryDao().get(studentId, skillTag)?.totalAttempts ?: 0
        val template = config.templatePin
        val id = if (template != null) {
            val count = catalog.countByTemplate(template)
            catalog.exerciseIdByTemplateOffset(template, attempts % count.coerceAtLeast(1))
        } else {
            val difficultyFloor = config.difficultyStart +
                (attempts / config.difficultyBlockSize.coerceAtLeast(1)) * config.difficultyStep
            val count = catalog.countBySkill(skillTag).coerceAtLeast(1)
            catalog.exerciseIdBySkillDifficultyOffset(skillTag, difficultyFloor.coerceIn(1.0, 10.0), attempts % count)
                ?: catalog.exerciseIdBySkillOffset(skillTag, attempts % count)
        }
        return catalog.getById(id ?: "")
            ?: catalog.getById(catalog.exerciseIdBySkillOffset("soma_subtracao", 0).orEmpty())
            ?: error("Catalogo local vazio ou corrompido")
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

    private suspend fun updateSkillMemory(studentId: String, skill: String, correct: Boolean, difficulty: Double) {
        val current = runtime.skillMemoryDao().get(studentId, skill)
        val score = ((current?.masterScore ?: 0.0) + adaptiveDelta(correct, difficulty)).coerceIn(0.0, 1.0)
        runtime.skillMemoryDao().upsert(
            StudentSkillMemoryEntity(
                studentId = studentId,
                skill = skill,
                masterScore = score,
                totalAttempts = (current?.totalAttempts ?: 0) + 1,
                correctAttempts = (current?.correctAttempts ?: 0) + if (correct) 1 else 0,
                lastUpdated = System.currentTimeMillis(),
            ),
        )
    }

    private fun adaptiveDelta(correct: Boolean, difficulty: Double): Double {
        return if (correct) 0.018 + (difficulty / 10.0) * 0.018 else -(0.014 + (difficulty / 10.0) * 0.030)
    }

    private fun ExerciseEntity.toFolhaField(): FolhaField {
        return FolhaField(
            fieldIndex = 0,
            exerciseId = id,
            subject = subject,
            canvasMode = canvasMode,
            statement = statement,
            skillTags = listOf(primarySkill),
            estimatedTimeMs = estimatedTimeMs,
            templateId = templateId,
            nodeId = nodeId,
            methodTags = parseStringList(methodTagsJson),
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
