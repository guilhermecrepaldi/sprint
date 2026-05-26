package com.strava_matematica.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

enum class BackgroundMode { WHITE, PARCHMENT, SLATE, DARK }
enum class DurationMode { UNLIMITED, TIMED, PAGES }
enum class DifficultyProgression { ARITHMETIC, GEOMETRIC }
enum class SessionStatus { GESTURE_ONBOARDING, CONFIG, CALIBRATION, DASHBOARD, ACTIVE, SUBMITTING, RESULT, FINISHED, DRILL, DRILL_RESULT, RANKING, ERROR }
enum class ApiStatus { CONNECTING, OK, OFFLINE, ERROR }

@Serializable
data class SessionConfig(
    val subject: String = "math",
    @SerialName("show_thermometer") val showThermometer: Boolean = true,
    val background: String = "white",
    @SerialName("pen_color") val penColor: String = "#1a1a1a",
    @SerialName("duration_mode") val durationMode: String = "unlimited",
    @SerialName("duration_limit_ms") val durationLimitMs: Int? = null,
    @SerialName("pages_limit") val pagesLimit: Int? = null,
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
    @SerialName("skill_pin") val skillPin: String? = null,
    @SerialName("template_pin") val templatePin: String? = null,
    @SerialName("focus_source_exercise_id") val focusSourceExerciseId: String? = null,
    @SerialName("focus_mode") val focusMode: Boolean = false,
    @SerialName("difficulty_block_size") val difficultyBlockSize: Int = 30,
    @SerialName("focus_target_count") val focusTargetCount: Int = 300,
    @SerialName("fixation_density") val fixationDensity: String = "fixa",
    // Local UI preferences — not sent to backend
    @SerialName("guide_mode") val guideMode: String = "nenhuma",  // nenhuma | horizontal | dots | grade
    @SerialName("pen_width") val penWidth: Float = 2.2f,
    @SerialName("show_eraser_button") val showEraserButton: Boolean = false,
) {
    val backgroundMode: BackgroundMode
        get() = when (background) {
            "dark"      -> BackgroundMode.DARK
            "parchment" -> BackgroundMode.PARCHMENT
            "slate"     -> BackgroundMode.SLATE
            else        -> BackgroundMode.WHITE
        }
}
