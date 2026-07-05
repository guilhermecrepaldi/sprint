package com.sprint.model

/**
 * Maps each app action to a gesture the user chose.
 * Defaults match the original design; user can reassign freely.
 */
data class GestureConfig(
    val mappings: Map<String, String> = Defaults,
) {
    companion object {
        // action_id → gesture_id
        val Defaults = mapOf(
            ACTION_ADVANCE       to GESTURE_TWO_FINGER_TAP,
            ACTION_GO_SPRINT     to GESTURE_TRIPLE_TAP,
            ACTION_TOGGLE_ERASER to GESTURE_LONG_PRESS,
            ACTION_OPEN_NOTE     to GESTURE_DOUBLE_TAP,
        )

        // Actions
        const val ACTION_ADVANCE       = "advance"
        const val ACTION_GO_SPRINT     = "go_sprint"
        const val ACTION_TOGGLE_ERASER = "toggle_eraser"
        const val ACTION_OPEN_NOTE     = "open_note"

        // Gestures
        const val GESTURE_DOUBLE_TAP      = "double_tap"       // 2 toques
        const val GESTURE_TRIPLE_TAP      = "triple_tap"       // 3 toques
        const val GESTURE_QUAD_TAP        = "quad_tap"         // 4 toques
        const val GESTURE_TWO_FINGER_TAP  = "two_finger_tap"   // 2 dedos
        const val GESTURE_LONG_PRESS      = "long_press"       // segurar

        val GestureLabels = mapOf(
            GESTURE_DOUBLE_TAP     to "2 toques",
            GESTURE_TRIPLE_TAP     to "3 toques",
            GESTURE_QUAD_TAP       to "4 toques",
            GESTURE_TWO_FINGER_TAP to "2 dedos",
            GESTURE_LONG_PRESS     to "segurar",
        )

        val ActionLabels = mapOf(
            ACTION_ADVANCE       to "Avançar exercício",
            ACTION_GO_SPRINT     to "Ir para Sprint",
            ACTION_TOGGLE_ERASER to "Ativar borracha",
            ACTION_OPEN_NOTE     to "Abrir nota",
        )

        val AllGestures = listOf(
            GESTURE_DOUBLE_TAP,
            GESTURE_TRIPLE_TAP,
            GESTURE_QUAD_TAP,
            GESTURE_TWO_FINGER_TAP,
            GESTURE_LONG_PRESS,
        )
    }

    fun gestureFor(action: String): String =
        mappings[action] ?: Defaults[action] ?: GESTURE_DOUBLE_TAP

    fun withMapping(action: String, gesture: String): GestureConfig =
        copy(mappings = mappings + (action to gesture))
}
