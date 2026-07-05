package com.sprint.recognizer

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

/**
 * Reconhece escrita caractere por caractere, da esquerda para a direita,
 * com fallback para reconhecimento de bloco inteiro.
 *
 * 3 estratégias em ordem:
 * 1. Se for 1 caractere isolado → reconhece direto
 * 2. Se tiver 2+ grupos → reconhece cada um e ordena por X
 * 3. Se tudo falhar → reconhece o bloco inteiro
 */
class StrokeCharacterRecognizer(
    private val context: android.content.Context,
) : MathRecognizer {

    companion object {
        private const val TAG = "StrokeCharacterRecognizer"
        private const val SEGMENT_THRESHOLD_X = 35f
    }

    private var recognizer: TextRecognizer = TextRecognition.getClient(TextRecognizerOptions.Builder().build())

    override suspend fun recognize(strokes: List<List<Offset>>, expectedAnswer: String?): String? {
        if (strokes.isEmpty()) return null

        // Tenta segmentar em caracteres
        val charGroups = segmentByX(strokes)

        return when {
            // Caso 1: único caractere — reconhece direto
            charGroups.size == 1 -> recognizeSingle(strokes, expectedAnswer)

            // Caso 2: múltiplos caracteres — reconhece cada um e ordena
            charGroups.size >= 2 -> {
                val result = recognizeCharacters(charGroups, expectedAnswer)
                if (result != null) result
                else recognizeSingle(strokes, expectedAnswer) // fallback: bloco inteiro
            }

            // Caso 3: fallback — bloco inteiro
            else -> recognizeSingle(strokes, expectedAnswer)
        }
    }

    /**
     * Reconhece o bloco inteiro de strokes como fallback.
     */
    private suspend fun recognizeSingle(strokes: List<List<Offset>>, expectedAnswer: String?): String? {
        val text = recognizeBitmapBlock(strokes) ?: return null
        Log.d(TAG, "single block: '$text'")
        return text
    }

    /**
     * Reconhece cada grupo de caractere individualmente e ordena por X.
     */
    private suspend fun recognizeCharacters(
        charGroups: List<List<List<Offset>>>,
        expectedAnswer: String?,
    ): String? {
        val chars = mutableListOf<Pair<Float, String>>()

        for (group in charGroups) {
            val text = recognizeBitmapBlock(group)
            if (text != null && text.isNotBlank()) {
                val avgX = group.flatten().map { it.x }.average().toFloat()
                chars.add(avgX to text)
            }
        }

        if (chars.isEmpty()) return null

        // Ordena por X (esquerda para direita)
        val ordered = chars.sortedBy { it.first }

        // Junta todos os caracteres reconhecidos
        val result = ordered.joinToString("") { it.second }
        Log.d(TAG, "chars=${chars.size} ordered='$result'")
        return result
    }

    /**
     * Segmenta strokes em grupos de caractere por posição X.
     */
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

    /**
     * Converte strokes em bitmap e reconhece com ML Kit.
     */
    private suspend fun recognizeBitmapBlock(strokes: List<List<Offset>>): String? {
        if (strokes.isEmpty()) return null

        val bitmap = strokesToBitmap(strokes) ?: return null
        val image = InputImage.fromBitmap(bitmap, 0)

        return suspendCancellableCoroutine { cont ->
            recognizer.process(image)
                .addOnSuccessListener { result ->
                    val text = result.text.trim().takeIf { it.isNotEmpty() }
                    cont.resume(text)
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "ocr failed", e)
                    cont.resume(null)
                }
        }
    }

    private fun strokesToBitmap(strokes: List<List<Offset>>): Bitmap? {
        if (strokes.isEmpty()) return null

        var minX = Float.MAX_VALUE; var minY = Float.MAX_VALUE
        var maxX = Float.MIN_VALUE; var maxY = Float.MIN_VALUE
        for (stroke in strokes) {
            for (pt in stroke) {
                if (pt.x < minX) minX = pt.x; if (pt.y < minY) minY = pt.y
                if (pt.x > maxX) maxX = pt.x; if (pt.y > maxY) maxY = pt.y
            }
        }
        val strokeW = maxX - minX; val strokeH = maxY - minY
        if (strokeW <= 0f || strokeH <= 0f) return null

        val pad = 20f; val scale = 4f
        val bmpW = ((strokeW + pad * 2) * scale).toInt().coerceIn(80, 1024)
        val bmpH = ((strokeH + pad * 2) * scale).toInt().coerceIn(60, 1024)

        val bitmap = Bitmap.createBitmap(bmpW, bmpH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(android.graphics.Color.WHITE)

        val paint = Paint().apply {
            color = android.graphics.Color.BLACK
            strokeWidth = maxOf(6f, bmpW.coerceAtMost(bmpH) / 14f)
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND
            isAntiAlias = true
        }

        for (stroke in strokes) {
            if (stroke.size < 2) continue
            val path = Path()
            path.moveTo((stroke[0].x - minX + pad) * scale, (stroke[0].y - minY + pad) * scale)
            for (i in 1 until stroke.size)
                path.lineTo((stroke[i].x - minX + pad) * scale, (stroke[i].y - minY + pad) * scale)
            canvas.drawPath(path, paint)
        }
        return bitmap
    }

    override fun release() {
        recognizer.close()
    }
}
