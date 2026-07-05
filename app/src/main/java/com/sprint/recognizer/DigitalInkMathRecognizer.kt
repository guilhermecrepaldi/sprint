package com.sprint.recognizer

import android.content.Context
import android.util.Log
import androidx.compose.ui.geometry.Offset
import com.google.mlkit.vision.digitalink.DigitalInkRecognition
import com.google.mlkit.vision.digitalink.DigitalInkRecognitionModel
import com.google.mlkit.vision.digitalink.DigitalInkRecognitionModelIdentifier
import com.google.mlkit.vision.digitalink.DigitalInkRecognizer
import com.google.mlkit.vision.digitalink.DigitalInkRecognizerOptions
import com.google.mlkit.vision.digitalink.Ink
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Reconhece escrita matemática caractere por caractere.
 *
 * 3 estratégias em cascata:
 * 1. ML Kit Digital Ink (modelo de símbolos) — melhor para operadores e símbolos
 * 2. ML Kit Text Recognition (OCR) — fallback confiável para números
 * 3. Bloco inteiro — se segmentação falhar, reconhece tudo de uma vez
 */
class DigitalInkMathRecognizer(
    private val context: Context,
) : MathRecognizer {

    companion object {
        private const val TAG = "DigitalInkMath"
        private const val SEGMENT_THRESHOLD_X = 40f
        private const val SYMBOL_MODEL = "zxx-Zsym-x-symbols"
        private const val TEXT_MODEL = "en"
    }

    private var symbolRecognizer: DigitalInkRecognizer? = null
    private var mlKitTextRecognizer: TextRecognizer? = null

    private fun getSymbolRecognizer(): DigitalInkRecognizer? {
        if (symbolRecognizer == null) {
            try {
                val modelId = DigitalInkRecognitionModelIdentifier.fromLanguageTag(SYMBOL_MODEL)
                if (modelId != null) {
                    symbolRecognizer = DigitalInkRecognition.getClient(
                        DigitalInkRecognizerOptions.builder(
                            DigitalInkRecognitionModel.builder(modelId).build()
                        ).build()
                    )
                    Log.i(TAG, "Symbol recognizer OK")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Symbol recognizer unavailable", e)
            }
        }
        return symbolRecognizer
    }

    private fun getTextRecognizer(): TextRecognizer {
        if (mlKitTextRecognizer == null) {
            mlKitTextRecognizer = TextRecognition.getClient(TextRecognizerOptions.Builder().build())
        }
        return mlKitTextRecognizer!!
    }

    override suspend fun recognize(strokes: List<List<Offset>>, expectedAnswer: String?): String? {
        if (strokes.isEmpty()) return null

        // Tenta segmentar em caracteres
        val charGroups = segmentByX(strokes)

        if (charGroups.size <= 1) {
            // Fallback: reconhece bloco inteiro com TextRecognition (mais confiável)
            return recognizeTextBlock(strokes)
        }

        // Reconhece cada caractere individualmente
        val result = StringBuilder()
        for (group in charGroups) {
            val text = recognizeSingle(group)
            if (text != null && text.isNotBlank()) {
                result.append(text.first().toString())
            }
        }

        if (result.isNotEmpty()) return result.toString()

        // Fallback final: bloco inteiro
        return recognizeTextBlock(strokes)
    }

    /**
     * Reconhece um grupo de strokes como caractere individual.
     * Tenta Digital Ink primeiro, depois Text Recognition.
     */
    private suspend fun recognizeSingle(group: List<List<Offset>>): String? {
        // Tenta Digital Ink (símbolos)
        val symRec = getSymbolRecognizer()
        if (symRec != null) {
            try {
                val ink = strokesToInk(group) ?: return null
                val text = suspendCancellableCoroutine<String?> { cont ->
                    symRec.recognize(ink)
                        .addOnSuccessListener { result ->
                            val t = if (result != null && result.candidates.isNotEmpty())
                                result.candidates.first().text?.trim() else null
                            cont.resume(t)
                        }
                        .addOnFailureListener { cont.resume(null) }
                }
                if (text != null) return text
            } catch (e: Exception) {
                Log.w(TAG, "Digital Ink failed", e)
            }
        }

        // Fallback: Text Recognition via OCR em bitmap
        val bitmap = strokesToBitmap(group) ?: return null
        return suspendCancellableCoroutine { cont ->
            getTextRecognizer().process(
                com.google.mlkit.vision.common.InputImage.fromBitmap(bitmap, 0)
            ).addOnSuccessListener { result ->
                val text = result.text.trim().takeIf { it.isNotEmpty() }
                cont.resume(text)
            }.addOnFailureListener { cont.resume(null) }
        }
    }

    /**
     * Reconhece bloco inteiro com TextRecognition (OCR).
     */
    private suspend fun recognizeTextBlock(strokes: List<List<Offset>>): String? {
        val bitmap = strokesToBitmap(strokes) ?: return null
        return suspendCancellableCoroutine { cont ->
            getTextRecognizer().process(
                com.google.mlkit.vision.common.InputImage.fromBitmap(bitmap, 0)
            ).addOnSuccessListener { result ->
                val text = result.text.trim().takeIf { it.isNotEmpty() }
                cont.resume(text)
            }.addOnFailureListener { cont.resume(null) }
        }
    }

    private fun segmentByX(strokes: List<List<Offset>>): List<List<List<Offset>>> {
        if (strokes.isEmpty()) return emptyList()
        if (strokes.size == 1) return listOf(strokes)

        val groups = mutableListOf<MutableList<List<Offset>>>()
        var currentGroup = mutableListOf<List<Offset>>()
        currentGroup.add(strokes.first())

        for (i in 1 until strokes.size) {
            val prevCenterX = strokes[i - 1].map { it.x }.average()
            val currCenterX = strokes[i].map { it.x }.average()
            val dx = kotlin.math.abs(currCenterX - prevCenterX)

            if (dx > SEGMENT_THRESHOLD_X) {
                groups.add(currentGroup)
                currentGroup = mutableListOf()
            }
            currentGroup.add(strokes[i])
        }
        if (currentGroup.isNotEmpty()) groups.add(currentGroup)

        return groups
    }

    private fun strokesToInk(strokes: List<List<Offset>>): Ink? {
        if (strokes.isEmpty()) return null
        val inkBuilder = Ink.builder()
        for (stroke in strokes) {
            if (stroke.size < 2) continue
            val strokeBuilder = Ink.Stroke.builder()
            for (point in stroke) {
                strokeBuilder.addPoint(Ink.Point.create(point.x, point.y, 0L))
            }
            inkBuilder.addStroke(strokeBuilder.build())
        }
        return inkBuilder.build()
    }

    private fun strokesToBitmap(strokes: List<List<Offset>>): android.graphics.Bitmap? {
        if (strokes.isEmpty()) return null
        var minX = Float.MAX_VALUE; var minY = Float.MAX_VALUE
        var maxX = Float.MIN_VALUE; var maxY = Float.MIN_VALUE
        for (stroke in strokes) for (pt in stroke) {
            if (pt.x < minX) minX = pt.x; if (pt.y < minY) minY = pt.y
            if (pt.x > maxX) maxX = pt.x; if (pt.y > maxY) maxY = pt.y
        }
        val w = maxX - minX; val h = maxY - minY
        if (w <= 0f || h <= 0f) return null
        val pad = 24f; val scale = 4f
        val bmpW = ((w + pad * 2) * scale).toInt().coerceIn(80, 1024)
        val bmpH = ((h + pad * 2) * scale).toInt().coerceIn(60, 1024)
        val bitmap = android.graphics.Bitmap.createBitmap(bmpW, bmpH, android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        canvas.drawColor(android.graphics.Color.WHITE)
        val paint = android.graphics.Paint().apply {
            color = android.graphics.Color.BLACK
            strokeWidth = maxOf(6f, bmpW.coerceAtMost(bmpH) / 14f)
            style = android.graphics.Paint.Style.STROKE
            strokeCap = android.graphics.Paint.Cap.ROUND
            strokeJoin = android.graphics.Paint.Join.ROUND
            isAntiAlias = true
        }
        for (stroke in strokes) {
            if (stroke.size < 2) continue
            val path = android.graphics.Path()
            path.moveTo((stroke[0].x - minX + pad) * scale, (stroke[0].y - minY + pad) * scale)
            for (i in 1 until stroke.size)
                path.lineTo((stroke[i].x - minX + pad) * scale, (stroke[i].y - minY + pad) * scale)
            canvas.drawPath(path, paint)
        }
        return bitmap
    }

    override fun release() {
        try { symbolRecognizer?.close() } catch (_: Exception) {}
        try { mlKitTextRecognizer?.close() } catch (_: Exception) {}
    }
}
