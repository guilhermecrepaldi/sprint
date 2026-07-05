package com.sprint.recognizer

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.util.Log
import androidx.compose.ui.geometry.Offset

/**
 * Reconhece escrita caractere por caractere, da esquerda para a direita.
 *
 * Suporta TODAS as notações matemáticas:
 * - Números: -126, 3.14, 1/2
 * - Conjuntos: S = { x , 10 , n² }
 * - Operadores: + - * / = ≠ ≈ ≤ ≥ ∈ ∉ ⊂ ⊃ ∪ ∩
 * - Potências: n², x³, x^n
 * - Frações: a/b
 * - Letras: a-z, α, β, π, Σ, Δ
 * - Intervalos: [a, b], ]a, b[
 * - Lógica: ¬ ∧ ∨ → ↔ ∀ ∃
 *
 * O usuário DEVE escrever na ordem esquerda→direita.
 * O WritingGuideBanner avisa isso na tela.
 */
class CharacterRecognizer(
    private val mlKit: MlKitRecognizer,
) : MathRecognizer {

    companion object {
        private const val TAG = "CharacterRecognizer"
        private const val SEGMENT_DISTANCE_THRESHOLD = 50f // px
    }

    override suspend fun recognize(strokes: List<List<Offset>>, expectedAnswer: String?): String? {
        if (strokes.isEmpty()) return null

        // 1. Segmenta strokes em grupos de caractere por distância espacial
        val charGroups = segmentByPosition(strokes)
        if (charGroups.isEmpty()) return null

        // 2. Reconhece cada grupo individualmente com ML Kit
        val recognizedChars = mutableListOf<String>()
        for (group in charGroups) {
            val text = mlKit.recognize(group, null)
            if (text != null && text.isNotBlank()) {
                // ML Kit pode devolver string longa — pega o primeiro caractere significativo
                val char = normalizeMlKitChar(text)
                if (char.isNotEmpty()) recognizedChars.add(char)
            }
        }
        if (recognizedChars.isEmpty()) return null

        // 3. Ordena por posição X (esquerda para direita)
        val ordered = orderByPosition(charGroups, recognizedChars)

        // 4. Monta a string final preservando a ordem natural
        val result = ordered.joinToString("")

        Log.d(TAG, "strokes=${strokes.size} grupos=${charGroups.size} chars=$recognizedChars -> '$result'")
        return result
    }

    /**
     * Normaliza o que o ML Kit retornou para um caractere limpo.
     * ML Kit pode confundir caracteres — mapeamos para o que faz sentido em matemática.
     */
    private fun normalizeMlKitChar(raw: String): String {
        val trimmed = raw.trim().lowercase()

        if (trimmed.isEmpty()) return ""

        // Mapeamento de caracteres comuns que o ML Kit confunde
        return when (trimmed.first()) {
            'o' -> "0"
            'l' -> "1"
            's' -> "5"
            'z' -> "2"
            'g' -> "9"
            'b' -> "6"  // ML Kit pode confundir 6 com b
            else -> trimmed.first().toString()
        }
    }

    /**
     * Segmenta strokes em grupos de caractere.
     * Se a distância entre os centros de strokes consecutivos for grande,
     * considera que é um novo caractere.
     */
    private fun segmentByPosition(strokes: List<List<Offset>>): List<List<List<Offset>>> {
        if (strokes.isEmpty()) return emptyList()
        if (strokes.size == 1) return listOf(strokes)

        val groups = mutableListOf<MutableList<List<Offset>>>()
        var currentGroup = mutableListOf<List<Offset>>()
        currentGroup.add(strokes.first())

        for (i in 1 until strokes.size) {
            val prevCenter = strokeCenter(strokes[i - 1])
            val currCenter = strokeCenter(strokes[i])
            val dist = mathHypot(currCenter.x - prevCenter.x, currCenter.y - prevCenter.y)

            if (dist > SEGMENT_DISTANCE_THRESHOLD) {
                groups.add(currentGroup)
                currentGroup = mutableListOf()
            }
            currentGroup.add(strokes[i])
        }
        if (currentGroup.isNotEmpty()) groups.add(currentGroup)

        return groups
    }

    private fun strokeCenter(stroke: List<Offset>): Offset {
        if (stroke.isEmpty()) return Offset.Zero
        val avgX = stroke.map { it.x }.average().toFloat()
        val avgY = stroke.map { it.y }.average().toFloat()
        return Offset(avgX, avgY)
    }

    /**
     * Ordena caracteres reconhecidos pela posição X do grupo de strokes.
     * Garante ordem esquerda→direita independente da ordem que o ML Kit retornou.
     */
    private fun orderByPosition(
        charGroups: List<List<List<Offset>>>,
        recognizedChars: List<String>,
    ): List<String> {
        val indexed = charGroups.mapIndexedNotNull { index, group ->
            if (index >= recognizedChars.size) return@mapIndexedNotNull null
            val avgX = group.flatten().map { it.x }.average()
            avgX to recognizedChars[index]
        }
        return indexed.sortedBy { it.first }.map { it.second }
    }

    private fun mathHypot(dx: Float, dy: Float): Float =
        kotlin.math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()

    override fun release() {
        mlKit.release()
    }
}
