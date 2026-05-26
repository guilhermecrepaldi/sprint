package com.strava_matematica.recognizer

import android.content.Context
import androidx.compose.ui.geometry.Offset
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.vision.digitalink.DigitalInkRecognition
import com.google.mlkit.vision.digitalink.DigitalInkRecognitionModel
import com.google.mlkit.vision.digitalink.DigitalInkRecognitionModelIdentifier
import com.google.mlkit.vision.digitalink.DigitalInkRecognizer
import com.google.mlkit.vision.digitalink.DigitalInkRecognizerOptions
import com.google.mlkit.vision.digitalink.Ink
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * On-device handwriting recognizer using Google ML Kit Digital Ink Recognition.
 *
 * Works offline once the model is downloaded (~30 MB on first launch).
 * Returns plain text — sufficient for high school math answers like "x = 5",
 * "42", "1/2", "-3", "2x + 1". Falls back to null for complex notation
 * (fractions drawn as fractions, superscripts, integral signs), which
 * triggers the Claude OCR path in the backend.
 *
 * Free, no API key required.
 */
class MlKitRecognizer(context: Context) : MathRecognizer {

    private val model: DigitalInkRecognitionModel
    private var recognizer: DigitalInkRecognizer? = null

    init {
        // "en-US" handles alphanumeric + math symbols (=, +, -, /, %, x, y, …).
        // If a math-specific tag is available in a future ML Kit release, swap here.
        val identifier = requireNotNull(
            DigitalInkRecognitionModelIdentifier.fromLanguageTag("en-US")
        ) { "ML Kit: en-US model identifier not found" }

        model = DigitalInkRecognitionModel.builder(identifier).build()

        RemoteModelManager.getInstance().isModelDownloaded(model)
            .addOnSuccessListener { downloaded ->
                if (downloaded) {
                    activate()
                } else {
                    // Download in background; recognize() returns null until ready.
                    RemoteModelManager.getInstance()
                        .download(model, DownloadConditions.Builder().build())
                        .addOnSuccessListener { activate() }
                        // Silently ignore download failures — Claude OCR is the fallback.
                }
            }
    }

    private fun activate() {
        recognizer = DigitalInkRecognition.getClient(
            DigitalInkRecognizerOptions.builder(model).build()
        )
    }

    /**
     * Returns the top recognition candidate or null.
     *
     * Null means: model not ready, no strokes, or low-confidence result.
     * The backend falls back to Claude OCR in all null cases.
     */
    override suspend fun recognize(strokes: List<List<Offset>>): String? {
        val rec = recognizer ?: return null
        val nonEmpty = strokes.filter { it.isNotEmpty() }
        if (nonEmpty.isEmpty()) return null

        val ink = buildInk(nonEmpty)

        return suspendCancellableCoroutine { cont ->
            rec.recognize(ink)
                .addOnSuccessListener { result ->
                    val top = result.candidates.firstOrNull()
                    // ML Kit score: negative log-likelihood. null = model didn't score it.
                    // Reject if score is very high (low confidence) — threshold ~50f is loose
                    // enough for simple math answers and strict enough to skip garbage.
                    val text = if (top != null && (top.score == null || top.score!! < 50f)) {
                        top.text.trim().takeIf { it.isNotEmpty() }
                    } else {
                        null
                    }
                    cont.resume(text)
                }
                .addOnFailureListener { cont.resume(null) }
        }
    }

    override fun release() {
        recognizer?.close()
        recognizer = null
    }

    private fun buildInk(strokes: List<List<Offset>>): Ink {
        val inkBuilder = Ink.builder()
        var t = 0L
        for (stroke in strokes) {
            val strokeBuilder = Ink.Stroke.builder()
            for (point in stroke) {
                strokeBuilder.addPoint(Ink.Point.create(point.x, point.y, t))
                t += 10L
            }
            inkBuilder.addStroke(strokeBuilder.build())
            t += 100L // gap between strokes
        }
        return inkBuilder.build()
    }
}
