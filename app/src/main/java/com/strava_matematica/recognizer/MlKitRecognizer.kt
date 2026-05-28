package com.strava_matematica.recognizer

import android.content.Context
import androidx.compose.ui.geometry.Offset

/**
 * Compatibilidade para builds antigos.
 *
 * O SPRINT deterministico nao usa ML Kit nem reconhecimento de escrita.
 * A caneta fica como rascunho/telemetria; a correcao local usa resposta
 * estruturada digitada pelo aluno.
 */
class MlKitRecognizer(@Suppress("UNUSED_PARAMETER") context: Context) : MathRecognizer {
    override suspend fun recognize(strokes: List<List<Offset>>): String? = null
}
