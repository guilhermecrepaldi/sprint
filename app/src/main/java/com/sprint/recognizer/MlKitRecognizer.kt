package com.sprint.recognizer

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.util.Log
import androidx.compose.ui.geometry.Offset
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class MlKitRecognizer(private val context: Context) : MathRecognizer {

    companion object {
        private const val TAG = "MlKitRecognizer"
    }

    private var recognizer: TextRecognizer = createRecognizer()

    private fun createRecognizer(): TextRecognizer =
        TextRecognition.getClient(TextRecognizerOptions.Builder().build())

    override suspend fun recognize(strokes: List<List<Offset>>, expectedAnswer: String?): String? {
        if (strokes.isEmpty()) return null

        val strokeCount = strokes.sumOf { it.size }
        if (strokeCount < 3) return null

        val bitmap = try {
            strokesToBitmap(strokes)
        } catch (e: Exception) {
            Log.e(TAG, "bitmap render failed", e)
            null
        }

        if (bitmap == null) return null

        val rawText = try {
            recognizeBitmap(bitmap)
        } catch (e: Exception) {
            Log.e(TAG, "ocr failed, recreating recognizer", e)
            recognizer.close()
            recognizer = createRecognizer()
            null
        }

        if (rawText != null) {
            val cleaned = postProcess(rawText, expectedAnswer)
            Log.d(TAG, "ocr='$rawText' -> '$cleaned'")
            return cleaned
        }

        Log.d(TAG, "ocr returned null")
        return null
    }

    private fun strokesToBitmap(strokes: List<List<Offset>>): Bitmap? {
        if (strokes.isEmpty()) return null

        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxX = Float.MIN_VALUE
        var maxY = Float.MIN_VALUE
        for (stroke in strokes) {
            for (pt in stroke) {
                if (pt.x < minX) minX = pt.x
                if (pt.y < minY) minY = pt.y
                if (pt.x > maxX) maxX = pt.x
                if (pt.y > maxY) maxY = pt.y
            }
        }
        val strokeW = maxX - minX
        val strokeH = maxY - minY
        if (strokeW <= 0f || strokeH <= 0f) return null

        val pad = 24f
        val scale = 4f
        val bmpW = ((strokeW + pad * 2) * scale).toInt().coerceIn(120, 2048)
        val bmpH = ((strokeH + pad * 2) * scale).toInt().coerceIn(60, 1024)

        val bitmap = Bitmap.createBitmap(bmpW, bmpH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(android.graphics.Color.WHITE)

        val paint = Paint().apply {
            color = android.graphics.Color.BLACK
            strokeWidth = maxOf(8f, bmpW.coerceAtMost(bmpH) / 12f)
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            isAntiAlias = true
        }

        for (stroke in strokes) {
            if (stroke.size < 2) continue
            val path = Path()
            path.moveTo((stroke[0].x - minX + pad) * scale, (stroke[0].y - minY + pad) * scale)
            for (i in 1 until stroke.size) {
                path.lineTo((stroke[i].x - minX + pad) * scale, (stroke[i].y - minY + pad) * scale)
            }
            canvas.drawPath(path, paint)
        }

        return bitmap
    }

    private suspend fun recognizeBitmap(bitmap: Bitmap): String? {
        val image = InputImage.fromBitmap(bitmap, 0)
        return suspendCancellableCoroutine { cont ->
            recognizer.process(image)
                .addOnSuccessListener { result ->
                    val text = result.text.trim().takeIf { it.isNotEmpty() }
                    cont.resume(text)
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "ocr task failed", e)
                    cont.resume(null)
                }
        }
    }

    private fun postProcess(text: String, expectedAnswer: String?): String? {
        val normalized = normalizeMath(text)
        if (normalized.isBlank()) return null

        // Aceita se tiver pelo menos 1 digito — deixa passar letras, simbolos, operadores
        // O DeterministicValidator fara a validacao final contra o gabarito
        if (!normalized.any { it.isDigit() }) return null

        return normalized
    }

    private fun normalizeMath(s: String): String {
        var result = s.trim()
            .replace(Regex("[−–—]"), "-")
            .replace(Regex("[×·∗]"), "*")
            .replace(Regex("[÷⁄]"), "/")
            .replace(" ", "")
            .lowercase()

        // Normaliza apenas palavras de números conhecidas. Sem substituições perigosas de caractere único.
        result = result
            .replace("zero", "0")
            .replace("um", "1")
            .replace("dois", "2")
            .replace("tres", "3")
            .replace("quatro", "4")
            .replace("cinco", "5")
            .replace("seis", "6")
            .replace("sete", "7")
            .replace("oito", "8")
            .replace("nove", "9")
            .replace("dez", "10")

        return result
    }
}
