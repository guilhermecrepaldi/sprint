package com.sprint.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SessionSync(
    val id: String,
    @SerialName("student_id") val studentId: String,
    @SerialName("started_at") val startedAt: Long,
    @SerialName("ended_at") val endedAt: Long? = null,
    @SerialName("skill_pin") val skillPin: String,
    val density: String,
    @SerialName("template_pin") val templatePin: String? = null,
    @SerialName("config_json") val configJson: String,
)

@Serializable
data class AttemptSync(
    val id: Long,
    @SerialName("session_id") val sessionId: String,
    @SerialName("student_id") val studentId: String,
    @SerialName("exercise_id") val exerciseId: String,
    val skill: String,
    @SerialName("is_correct") val isCorrect: Boolean,
    @SerialName("user_response") val userResponse: String,
    @SerialName("expected_answer") val expectedAnswer: String,
    @SerialName("validator_type") val validatorType: String,
    @SerialName("attempt_timestamp") val attemptTimestamp: Long,
    @SerialName("duration_seconds") val durationSeconds: Int,
)

@Serializable
data class SyncPayload(
    val sessions: List<SessionSync>,
    val attempts: List<AttemptSync>,
)

@Serializable
data class SyncResponse(
    val status: String,
    val message: String? = null,
)
