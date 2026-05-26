package com.strava_matematica.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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
