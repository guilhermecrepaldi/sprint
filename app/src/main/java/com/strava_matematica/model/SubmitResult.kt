package com.strava_matematica.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SessionStartRequest(
    @SerialName("student_id") val studentId: String,
    val config: SessionConfig,
)

@Serializable
data class SessionStartResponse(
    @SerialName("session_id") val sessionId: String,
    @SerialName("config_id") val configId: String,
    @SerialName("first_folha") val firstFolha: Folha,
)

@Serializable
data class FieldSubmit(
    @SerialName("field_index") val fieldIndex: Int,
    @SerialName("exercise_id") val exerciseId: String,
    @SerialName("image_base64") val imageBase64: String,
    @SerialName("total_time_ms") val totalTimeMs: Long,
    @SerialName("time_to_first_stroke_ms") val timeToFirstStrokeMs: Long,
    @SerialName("pen_events") val penEvents: List<PenEvent>,
)

@Serializable
data class SubmitRequest(
    @SerialName("folha_id") val folhaId: String,
    @SerialName("submitted_at_ms") val submittedAtMs: Long,
    val fields: List<FieldSubmit>,
)

@Serializable
data class FieldResult(
    @SerialName("field_index") val fieldIndex: Int,
    @SerialName("recognized_answer") val recognizedAnswer: String?,
    @SerialName("expected_answer") val expectedAnswer: String,
    @SerialName("is_correct") val isCorrect: Boolean,
    val score: Int,
    @SerialName("error_type") val errorType: String? = null,
)

@Serializable
data class Thermometer(
    val value: Double,
    val trend: String,
)

@Serializable
data class SubmitResult(
    val results: List<FieldResult>,
    @SerialName("page_score") val pageScore: Int,
    val thermometer: Thermometer,
    @SerialName("restart_triggered") val restartTriggered: Boolean,
    @SerialName("session_status") val sessionStatus: String,
    @SerialName("next_folha") val nextFolha: Folha? = null,
)
