package com.strava_matematica.recognizer

import androidx.compose.ui.geometry.Offset

/**
 * Abstraction over on-device math handwriting recognition.
 *
 * At submit time, the ViewModel asks the active recognizer for a LaTeX
 * string from the answer-box strokes. If a string is returned, the backend
 * can validate it with sympy (instant, no Claude call). If null, the
 * backend falls back to Claude OCR on the bitmap.
 *
 * Implementations:
 *   - [NoOpRecognizer]   — always returns null (Claude OCR path, default)
 *   - [IinkRecognizer]   — MyScript iink SDK (local, real-time, offline)
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
    suspend fun recognize(strokes: List<List<Offset>>): String?

    /** Release any native resources (call from ViewModel.onCleared). */
    fun release() { /* default: no-op */ }
}

/** Always returns null — uses Claude OCR on the backend. Zero setup needed. */
object NoOpRecognizer : MathRecognizer {
    override suspend fun recognize(strokes: List<List<Offset>>): String? = null
}
