package com.strava_matematica.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FolhaField(
    @SerialName("field_index") val fieldIndex: Int,
    @SerialName("exercise_id") val exerciseId: String,
    val subject: String = "math",
    @SerialName("canvas_mode") val canvasMode: String = "calculation",
    val statement: String,
    @SerialName("skill_tags") val skillTags: List<String>,
    @SerialName("estimated_time_ms") val estimatedTimeMs: Int? = null,
    @SerialName("template_id") val templateId: String? = null,
    @SerialName("node_id") val nodeId: String? = null,
    @SerialName("method_tags") val methodTags: List<String>? = null,
    @SerialName("expected_answer") val expectedAnswer: String? = null,
    @SerialName("validator_type") val validatorType: String? = null,
    @SerialName("answer_type") val answerType: String? = null,
)

@Serializable
data class Folha(
    @SerialName("folha_id") val folhaId: String,
    @SerialName("page_index") val pageIndex: Int,
    val difficulty: Double,
    val fields: List<FolhaField>,
)
