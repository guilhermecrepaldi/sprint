package com.strava_matematica.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DrillItem(
    @SerialName("item_id") val itemId: String,
    val statement: String,
    @SerialName("expected_answer") val expectedAnswer: String,
    @SerialName("skill_tag") val skillTag: String,
    val difficulty: Float,
    // Android auto-avança quando o campo atinge exatamente esta qtd de chars.
    @SerialName("auto_submit_chars") val autoSubmitChars: Int,
)

@Serializable
data class DrillBatch(
    @SerialName("batch_id") val batchId: String,
    val level: String,
    val count: Int,
    val items: List<DrillItem>,
    @SerialName("generated_at") val generatedAt: String,
)

@Serializable
data class DrillItemResult(
    @SerialName("item_id") val itemId: String,
    @SerialName("written_answer") val writtenAnswer: String,
    @SerialName("is_correct") val isCorrect: Boolean,
    @SerialName("time_ms") val timeMs: Int,
)

@Serializable
data class DrillFlushRequest(
    @SerialName("student_id") val studentId: String,
    @SerialName("batch_id") val batchId: String,
    val level: String,
    @SerialName("started_at_ms") val startedAtMs: Long,
    val results: List<DrillItemResult>,
)

@Serializable
data class DrillFlushResult(
    @SerialName("session_id") val sessionId: String,
    val total: Int,
    val correct: Int,
    val accuracy: Float,
    @SerialName("total_time_ms") val totalTimeMs: Int,
    @SerialName("avg_time_ms") val avgTimeMs: Int,
    @SerialName("xp_earned") val xpEarned: Int,
)
