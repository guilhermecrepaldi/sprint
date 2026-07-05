package com.sprint.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ActivityResponse(
    val days: List<HeatmapDay>,
)

@Serializable
data class ReviewSuggestion(
    val skill: String,
    val status: String,
    val accuracy: Float,
    @SerialName("days_idle") val daysIdle: Int,
    @SerialName("attempt_count") val attemptCount: Int,
)

@Serializable
data class SkillProgressItem(
    val skill: String,
    val status: String,
    val accuracy: Float,
    val fluency: Float = 0f,
    val retention: Float = 0f,
    val velocity: Float = 0f,
    val stability: Float = 0f,
    val fixation: Float = 0f,
    @SerialName("attempt_count") val attemptCount: Int,
    @SerialName("available_count") val availableCount: Int,
    @SerialName("needs_training") val needsTraining: String? = null,
)

@Serializable
data class SprintHistoryItem(
    @SerialName("session_id") val sessionId: String,
    val skill: String,
    val density: String = "fixa",
    val template: String? = null,
    @SerialName("exercises_done") val exercisesDone: Int,
    val accuracy: Int,
    @SerialName("duration_min") val durationMin: Int,
    @SerialName("started_at") val startedAt: String,
    @SerialName("is_active") val isActive: Boolean,
)

// ── Student Analytics Models ─────────────────────────────────────────────

data class ErrorStat(
    val errorType: String,
    val count: Int,
    val pctOfTotal: Float,
)

data class SkillErrorAnalysis(
    val skill: String,
    val totalAttempts: Int,
    val errorCount: Int,
    val accuracy: Float,
    val dominantError: String? = null,
    val dominantErrorPct: Float? = null,
)

data class FragileSkill(
    val skill: String,
    val accuracy: Float,
    val attemptCount: Int,
    val daysIdle: Int,
    val effectiveMastery: Float,
    val errorTrend: String,
    val priority: String,
    val suggestion: String,
)

data class ReviewPlanItem(
    val skill: String,
    val priority: Int,
    val reason: String,
    val suggestedExercises: Int,
)

data class SessionSummary(
    val totalExercises: Int,
    val correctCount: Int,
    val accuracy: Float,
    val avgTimeMs: Long,
    val totalTimeMs: Long,
    val dominantErrorType: String? = null,
    val dominantErrorSkill: String? = null,
    val recommendedSkill: String? = null,
    val mmrDelta: Int = 0,
    val fragileSkills: List<FragileSkill> = emptyList(),
)
