package com.sprint.recognizer

import androidx.compose.ui.geometry.Offset

/**
 * Abstraction over on-device math handwriting recognition.
 *
 * Legacy abstraction kept so older code still compiles.
 *
 * The deterministic offline Sprint does not use handwriting recognition.
 * Pen strokes are stored as scratch/telemetry; correction reads a structured
 * local answer string.
 *
 * Implementations:
 *   - [NoOpRecognizer] — always returns null.
 */
interface MathRecognizer {

    /**
     * Recognize handwritten math from ink strokes.
     *
     * @param strokes List of strokes; each stroke is an ordered list of
     *                screen-pixel points captured by InkCanvas.
     * @return LaTeX string (e.g. `"x = 5"`, `"\frac{1}{2}"`), or null if
     *         recognition is unavailable or failed.
     */
    suspend fun recognize(strokes: List<List<Offset>>, expectedAnswer: String? = null): String?

    /** Release any native resources (call from ViewModel.onCleared). */
    fun release() { /* default: no-op */ }
}

/** Always returns null — uses Claude OCR on the backend. Zero setup needed. */
object NoOpRecognizer : MathRecognizer {
    override suspend fun recognize(strokes: List<List<Offset>>, expectedAnswer: String?): String? = null
}
