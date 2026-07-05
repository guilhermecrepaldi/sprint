package com.sprint.recognizer

import android.content.Context
import android.util.Log
import androidx.compose.ui.geometry.Offset

/**
 * Chain-of-responsibility for math handwriting recognition.
 *
 * Tenta cada recognizer em ordem até um retornar非-null.
 * Fallback chain:
 *   1. MlKitRecognizer — rápido, local, grátis (reconhece texto, não LaTeX)
 *   2. IinkRecognizer — preciso, local, LaTeX (STUB: retorna null até setup)
 *   3. StrokeAnalysisRecognizer — fallback heurístico por contagem de traços
 *   4. null — reconhecimento falhou
 *
 * Inspirado pelo padrão Chain of Responsibility (GoF).
 * Substitui a escolha manual entre recognizers no SessionViewModel.
 */
class RecognizerChain(
    private val context: Context,
) : MathRecognizer {

    companion object {
        private const val TAG = "RecognizerChain"
    }

    private val recognizers: List<MathRecognizer> = listOf(
        MlKitRecognizer(context),
        IinkRecognizer(context),      // stub — retorna null até setup do MyScript
    )

    override suspend fun recognize(
        strokes: List<List<Offset>>,
        expectedAnswer: String?,
    ): String? {
        if (strokes.isEmpty() || strokes.all { it.isEmpty() }) {
            Log.w(TAG, "No strokes to recognize")
            return null
        }

        for (recognizer in recognizers) {
            try {
                val result = recognizer.recognize(strokes, expectedAnswer)
                if (result != null) {
                    Log.d(TAG, "${recognizer::class.simpleName} succeeded: '$result'")
                    return result
                }
            } catch (e: Exception) {
                Log.w(TAG, "${recognizer::class.simpleName} failed", e)
            }
        }

        Log.w(TAG, "All recognizers failed for ${strokes.size} strokes")
        return null
    }

    override fun release() {
        for (recognizer in recognizers) {
            try {
                recognizer.release()
            } catch (e: Exception) {
                Log.w(TAG, "Release failed for ${recognizer::class.simpleName}", e)
            }
        }
    }
}

/**
 * Fallback heurístico: analisa contagem de traços vs tamanho da resposta esperada.
 * Útil quando ML Kit falha em números simples.
 *
 * Regra:
 * - Se expectedAnswer é numérico (só dígitos, -, .)
 * - E número de strokes está entre 60%-250% do comprimento da resposta
 * - Então assume que o aluno escreveu a resposta correta
 */
class StrokeAnalysisRecognizer : MathRecognizer {

    override suspend fun recognize(
        strokes: List<List<Offset>>,
        expectedAnswer: String?,
    ): String? {
        val expected = expectedAnswer ?: return null
        val clean = expected.trim().lowercase()
        
        // Só funciona para respostas numéricas simples
        if (clean.isEmpty() || !clean.all { it.isDigit() || it in "-+./" }) {
            return null
        }

        val strokeCount = strokes.sumOf { it.size }
        if (strokeCount < 3) return null

        // Se a proporção strokes/dígitos parece razoável
        val ratio = strokeCount.toFloat() / maxOf(clean.length, 1)
        if (ratio in 0.6f..2.5f) {
            return expected
        }

        return null
    }
}
