package com.strava_matematica.domain.procedural

object MathBktEngine {

    private data class BktParams(
        val l0: Double,
        val t: Double,
        val g: Double,
        val s: Double
    )

    private fun getParams(skill: String): BktParams {
        return when (skill) {
            "soma_subtracao",
            "multiplicacao_divisao",
            "fracoes_decimais",
            "porcentagem_razao",
            "potenciacao_radiciacao" -> BktParams(l0 = 0.4, t = 0.25, g = 0.1, s = 0.05)
            "equacao_2_grau",
            "polinomios",
            "matrizes",
            "trigonometria" -> BktParams(l0 = 0.3, t = 0.2, g = 0.15, s = 0.1)
            else -> BktParams(l0 = 0.2, t = 0.15, g = 0.05, s = 0.1)
        }
    }

    fun getInitialMastery(skill: String): Double {
        return getParams(skill).l0
    }

    /**
     * Calcula o decaimento temporal do BKT baseado no tempo desde o último treino.
     * @param currentMastery O nível atual de mastery [0.0, 1.0]
     * @param daysSinceLastPractice Dias corridos desde a última atualização da skill
     * @return O mastery com o "esquecimento" aplicado (nunca menor que l0)
     */
    fun applyTemporalDecay(skill: String, currentMastery: Double, daysSinceLastPractice: Int): Double {
        if (daysSinceLastPractice <= 7) return currentMastery // Sem penalidade na primeira semana
        
        val params = getParams(skill)
        val weeksMissed = (daysSinceLastPractice - 7) / 7.0
        val decayRate = 0.05 // Perde 5% de certeza por semana ociosa
        
        val decayed = currentMastery - (weeksMissed * decayRate)
        return decayed.coerceAtLeast(params.l0)
    }

    fun updateMastery(skill: String, currentMastery: Double, isCorrect: Boolean, daysSinceLastPractice: Int = 0): Double {
        val params = getParams(skill)
        val decayedL = if (currentMastery <= 0.0) params.l0 else applyTemporalDecay(skill, currentMastery, daysSinceLastPractice)

        val pLGivenObs = if (isCorrect) {
            val numerator = decayedL * (1.0 - params.s)
            val denominator = numerator + (1.0 - decayedL) * params.g
            if (denominator == 0.0) decayedL else numerator / denominator
        } else {
            val numerator = decayedL * params.s
            val denominator = numerator + (1.0 - decayedL) * (1.0 - params.g)
            if (denominator == 0.0) decayedL else numerator / denominator
        }

        val nextL = pLGivenObs + (1.0 - pLGivenObs) * params.t
        return nextL.coerceIn(0.0, 1.0)
    }
}
