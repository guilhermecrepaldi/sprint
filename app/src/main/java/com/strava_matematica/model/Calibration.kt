package com.strava_matematica.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CalibrationSample(
    @SerialName("expected_char") val expectedChar: String,
    @SerialName("image_base64") val imageBase64: String,
)

@Serializable
data class CalibrationRequest(
    val samples: List<CalibrationSample>,
)

@Serializable
data class CalibrationResponse(
    @SerialName("weak_chars") val weakChars: List<String>,
    @SerialName("overall_score") val overallScore: Float,
)
