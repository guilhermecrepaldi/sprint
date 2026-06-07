package com.strava_matematica.recognizer

import android.content.Context
import android.util.Log
import androidx.compose.ui.geometry.Offset
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.vision.digitalink.DigitalInkRecognition
import com.google.mlkit.vision.digitalink.DigitalInkRecognitionModel
import com.google.mlkit.vision.digitalink.DigitalInkRecognitionModelIdentifier
import com.google.mlkit.vision.digitalink.DigitalInkRecognizerOptions
import com.google.mlkit.vision.digitalink.Ink
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class MlKitRecognizer(private val context: Context) : MathRecognizer {

    companion object {
        private const val TAG = "MlKitRecognizer"
    }

    private val mathModelIdentifier = DigitalInkRecognitionModelIdentifier.fromLanguageTag("zxx-Zsym-x-math")
    private val mathModel = mathModelIdentifier?.let {
        DigitalInkRecognitionModel.builder(it).build()
    }

    private val defaultModelIdentifier = DigitalInkRecognitionModelIdentifier.fromLanguageTag("default")
    private val defaultModel = defaultModelIdentifier?.let {
        DigitalInkRecognitionModel.builder(it).build()
    }

    override suspend fun recognize(strokes: List<List<Offset>>, expectedAnswer: String?): String? {
        if (strokes.isEmpty()) return null

        // Try math model first
        val mathResult = recognizeWithModel(mathModel, strokes, expectedAnswer)
        if (mathResult != null) {
            val cleaned = postProcess(mathResult, expectedAnswer)
            if (cleaned != null) {
                Log.d(TAG, "math ok: '$mathResult' -> '$cleaned'")
                return cleaned
            }
        }

        // Fallback: default writing model (better for digits)
        val defaultResult = recognizeWithModel(defaultModel, strokes, expectedAnswer)
        if (defaultResult != null) {
            val cleaned = postProcess(defaultResult, expectedAnswer)
            if (cleaned != null) {
                Log.d(TAG, "default ok: '$defaultResult' -> '$cleaned'")
                return cleaned
            }
        }

        Log.d(TAG, "both failed: math='$mathResult', default='$defaultResult'")
        return mathResult ?: defaultResult
    }

    private suspend fun recognizeWithModel(
        model: DigitalInkRecognitionModel?,
        strokes: List<List<Offset>>,
        expectedAnswer: String?
    ): String? {
        if (model == null) return null

        val modelManager = RemoteModelManager.getInstance()
        if (!ensureModelDownloaded(modelManager, model)) return null

        return suspendCancellableCoroutine { cont ->
            val inkBuilder = Ink.builder()
            var time = 0L
            for (stroke in strokes) {
                val sb = Ink.Stroke.builder()
                for (pt in stroke) {
                    sb.addPoint(Ink.Point.create(pt.x, pt.y, time))
                    time += 10L
                }
                inkBuilder.addStroke(sb.build())
            }
            val ink = inkBuilder.build()
            val recognizer = DigitalInkRecognition.getClient(
                DigitalInkRecognizerOptions.builder(model).build()
            )
            recognizer.recognize(ink)
                .addOnSuccessListener { result ->
                    var bestText = result.candidates.firstOrNull()?.text
                    if (expectedAnswer != null && bestText != null) {
                        val expectedClean = expectedAnswer.replace("\\s".toRegex(), "")
                        for (candidate in result.candidates) {
                            val candidateClean = candidate.text.replace("\\s".toRegex(), "")
                            if (candidateClean == expectedClean) {
                                bestText = candidate.text
                                cont.resume(bestText)
                                return@addOnSuccessListener
                            }
                        }
                    }
                    if (bestText != null) {
                        val mathRegex = Regex("""^[-+*/=0-9().,\s]+$""")
                        if (!bestText!!.matches(mathRegex)) {
                            for (candidate in result.candidates) {
                                if (candidate.text.matches(mathRegex)) {
                                    bestText = candidate.text
                                    break
                                }
                            }
                        }
                    }
                    cont.resume(bestText)
                }
                .addOnFailureListener { cont.resume(null) }
        }
    }

    private fun postProcess(text: String, expectedAnswer: String?): String? {
        if (text.isBlank()) return null
        val trimmed = text.trim()
        val isNum = expectedAnswer?.matches(Regex("""^-?\d+([.,]\d+)?$""")) == true
        if (isNum) {
            val numMatch = Regex("""-?\d+([.,]\d+)?""").find(trimmed)
            if (numMatch != null && numMatch.value.length in 1..8) return numMatch.value
            if (trimmed.matches(Regex("""^[-+*/=0-9().,\s]+$"""))) return trimmed
            val digits = trimmed.replace(Regex("""[^\d]"""), "")
            if (digits.length in 1..8 && expectedAnswer != null) {
                val expDigits = expectedAnswer.replace(Regex("""[^\d]"""), "")
                if (digits.length == expDigits.length) return digits
            }
            if (trimmed.length <= 10 && trimmed.count { it.isLetter() } == 0) return trimmed
            return trimmed
        }
        return trimmed
    }

    private suspend fun ensureModelDownloaded(mgr: RemoteModelManager, model: DigitalInkRecognitionModel): Boolean =
        suspendCancellableCoroutine { cont ->
            mgr.isModelDownloaded(model)
                .addOnSuccessListener { ok ->
                    if (ok) cont.resume(true)
                    else mgr.download(model, DownloadConditions.Builder().build())
                        .addOnSuccessListener { cont.resume(true) }
                        .addOnFailureListener { cont.resume(false) }
                }
                .addOnFailureListener { cont.resume(false) }
        }
}
