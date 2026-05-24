package com.strava_matematica.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

enum class BackgroundMode { WHITE, DARK }
enum class DurationMode { UNLIMITED, TIMED, PAGES }
enum class DifficultyProgression { ARITHMETIC, GEOMETRIC }
enum class SessionStatus { CONFIG, ACTIVE, SUBMITTING, RESULT, FINISHED, ERROR }
enum class ApiStatus { CONNECTING, OK, OFFLINE, ERROR }

@Serializable
data class SessionConfig(
    @SerialName("show_thermometer") val showThermometer: Boolean = true,
    val background: String = "white",
    @SerialName("pen_color") val penColor: String = "#1a1a1a",
    @SerialName("duration_mode") val durationMode: String = "pages",
    @SerialName("duration_limit_ms") val durationLimitMs: Int? = null,
    @SerialName("pages_limit") val pagesLimit: Int? = 10,
    @SerialName("difficulty_progression") val difficultyProgression: String = "arithmetic",
    @SerialName("difficulty_start") val difficultyStart: Double = 2.0,
    @SerialName("difficulty_step") val difficultyStep: Double = 0.5,
    @SerialName("difficulty_ratio") val difficultyRatio: Double = 1.15,
    @SerialName("restart_on_avg") val restartOnAvg: Double? = 7.0,
    @SerialName("restart_window") val restartWindow: Int = 10,
    @SerialName("exercises_per_page") val exercisesPerPage: Int = 5,
    @SerialName("show_correct_count") val showCorrectCount: Boolean = true,
    @SerialName("show_percentage") val showPercentage: Boolean = true,
    @SerialName("blind_mode") val blindMode: Boolean = false,
) {
    val backgroundMode: BackgroundMode
        get() = if (background == "dark") BackgroundMode.DARK else BackgroundMode.WHITE
}
