package com.strava_matematica.recognizer

import android.content.Context
import androidx.compose.ui.geometry.Offset
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.vision.digitalink.DigitalInkRecognition
import com.google.mlkit.vision.digitalink.DigitalInkRecognizerOptions
import com.google.mlkit.vision.digitalink.DigitalInkRecognitionModel
import com.google.mlkit.vision.digitalink.DigitalInkRecognitionModelIdentifier
import com.google.mlkit.vision.digitalink.Ink
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

class MlKitRecognizer(@Suppress("UNUSED_PARAMETER") context: Context) : MathRecognizer {

    private val modelIdentifier = DigitalInkRecognitionModelIdentifier.fromLanguageTag("zxx-Zsym-x-math")
    private val model = modelIdentifier?.let {
        DigitalInkRecognitionModel.builder(it).build()
    }

    override suspend fun recognize(strokes: List<List<Offset>>, expectedAnswer: String?): String? {
        val model = this.model ?: return null
        if (strokes.isEmpty()) return null

        val modelManager = RemoteModelManager.getInstance()
        val isDownloaded = ensureModelDownloaded(modelManager, model)
        if (!isDownloaded) return null

        return suspendCancellableCoroutine { continuation ->
            val inkBuilder = Ink.builder()
            var time = 0L
            for (stroke in strokes) {
                val strokeBuilder = Ink.Stroke.builder()
                for (point in stroke) {
                    strokeBuilder.addPoint(Ink.Point.create(point.x, point.y, time))
                    time += 10L
                }
                inkBuilder.addStroke(strokeBuilder.build())
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
                                continuation.resume(bestText)
                                return@addOnSuccessListener
                            }
                        }
                    }

                    // Se não achamos a resposta exata, tentar mitigar alucinações de letras
                    if (bestText != null) {
                        val isExpectedNumeric = expectedAnswer?.matches(Regex("""^-?\d+([.,]\d+)?$""")) == true
                        val isMathExpected = true // Na matemática, preferimos sempre números e sinais

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

                    continuation.resume(bestText)
                }
                .addOnFailureListener {
                    continuation.resume(null)
                }
        }
    }

    private suspend fun ensureModelDownloaded(
        modelManager: RemoteModelManager,
        model: DigitalInkRecognitionModel
    ): Boolean = suspendCancellableCoroutine { continuation ->
        modelManager.isModelDownloaded(model)
            .addOnSuccessListener { downloaded ->
                if (downloaded) {
                    continuation.resume(true)
                } else {
                    val conditions = DownloadConditions.Builder().build()
                    modelManager.download(model, conditions)
                        .addOnSuccessListener {
                            continuation.resume(true)
                        }
                        .addOnFailureListener {
                            continuation.resume(false)
                        }
                }
            }
            .addOnFailureListener {
                continuation.resume(false)
            }
    }
}
