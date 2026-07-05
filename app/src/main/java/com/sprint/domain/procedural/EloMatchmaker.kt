package com.sprint.domain.procedural

import kotlin.math.roundToInt
import kotlin.math.pow

object EloMatchmaker {

    /**
     * Calcula o novo MMR baseado no tempo gasto (APM) e correção.
     * @param currentMmr O MMR atual do usuário nesta skill.
     * @param isCorrect Se a resposta final foi correta.
     * @param timeSpentMs Tempo total gasto no exercício.
     * @param targetTimeMs Tempo alvo para a dificuldade atual (Flow Zone).
     * @return O novo MMR ajustado.
     */
    fun calculateNewMmr(
        currentMmr: Int,
        isCorrect: Boolean,
        timeSpentMs: Long,
        targetTimeMs: Long = 15_000L, // 15 segundos target
        isUnderInefficacy: Boolean = false,
        consecutiveFailsInInefficacy: Int = 0
    ): Int {
        var delta = 0.0

        if (isCorrect) {
            // Ganho base de 15 MMR por acerto
            delta = 15.0
            
            // Bônus de velocidade (APM alto)
            if (timeSpentMs < targetTimeMs) {
                val speedBonus = ((targetTimeMs - timeSpentMs).toDouble() / targetTimeMs) * 10.0
                delta += speedBonus
            } else {
                // Penalidade por hesitação (demorou muito, fluxo caindo)
                val hesitationPenalty = ((timeSpentMs - targetTimeMs).toDouble() / targetTimeMs) * 5.0
                delta -= hesitationPenalty.coerceAtMost(10.0) // Nunca perde mais que 10 no bônus
            }
        } else {
            if (isUnderInefficacy) {
                // Se isCorrect for false e isUnderInefficacy for true, delta do MMR é penalizado exponencialmente
                delta = -(25.0 * 1.8.pow(consecutiveFailsInInefficacy.toDouble()))
                delta = delta.coerceAtLeast(-250.0)
            } else {
                // Perda base por erro
                delta = -20.0
                
                // Se errou muito rápido (chute/tilt), penalidade maior
                if (timeSpentMs < 5000L) {
                    delta -= 10.0
                }
            }
        }

        val newMmr = (currentMmr + delta).roundToInt()
        return newMmr.coerceAtLeast(800) // MMR Mínimo (Piso)
    }

    /**
     * Converte o Master Score antigo (0.0 a 1.0) para um MMR base inicial.
     */
    fun masterScoreToMmr(masterScore: Double): Int {
        return 800 + (masterScore * 1200).toInt() // Mapeia 0->800, 1.0->2000
    }

    /**
     * Converte o MMR de volta para Master Score para a UI atual.
     */
    fun mmrToMasterScore(mmr: Int): Double {
        return ((mmr - 800).toDouble() / 1200.0).coerceIn(0.0, 1.0)
    }
}
