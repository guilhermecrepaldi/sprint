package com.sprint.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class HeatmapDay(
    val date: String,   // "2026-05-24"
    val count: Int,
    val countMorning: Int = 0,
    val countAfternoon: Int = 0,
    val countNight: Int = 0
)

@Serializable
data class TrackProgress(
    val slug: String,
    val name: String,
    @SerialName("total_skills") val totalSkills: Int,
    @SerialName("attempted_skills") val attemptedSkills: Int,
    val progress: Float,   // 0.0–1.0
)

@Serializable
data class ProfileStats(
    @SerialName("total_exercises") val totalExercises: Int,
    @SerialName("streak_days") val streakDays: Int,
    @SerialName("xp_total") val xpTotal: Int,
)

@Serializable
data class PublicProfile(
    @SerialName("student_name") val studentName: String,
    val slug: String,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("xp_total") val xpTotal: Int,
    @SerialName("member_since") val memberSince: String,
    val stats: ProfileStats,
    val tracks: List<TrackProgress>,
    val heatmap: List<HeatmapDay>,
    @SerialName("recent_sprints") val recentSprints: List<SprintHistoryItem> = emptyList(),
)
