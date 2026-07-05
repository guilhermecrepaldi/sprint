package com.strava_matematica.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PenEvent(
    val ts: Long,
    val x: Float,
    val y: Float,
    val pressure: Float? = null,
    val tilt: Float? = null,
    val velocity: Float? = null,
    @SerialName("event_type") val eventType: String,
)
