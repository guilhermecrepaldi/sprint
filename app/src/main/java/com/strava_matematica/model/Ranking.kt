package com.strava_matematica.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RankingEntry(
    val rank: Int,
    @SerialName("student_name") val studentName: String,
    val slug: String? = null,
    @SerialName("xp_week") val xpWeek: Int,
    @SerialName("xp_total") val xpTotal: Int,
)

@Serializable
data class WeeklyRanking(
    val entries: List<RankingEntry>,
    @SerialName("week_start") val weekStart: String,
)
