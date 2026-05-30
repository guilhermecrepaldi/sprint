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
            else -> BktParams(l0 = 0.2, t = 0.15, g = 0.05, s = 0.1)
        }
    }

    fun getInitialMastery(skill: String): Double {
        return getParams(skill).l0
    }

    fun updateMastery(skill: String, currentMastery: Double, isCorrect: Boolean): Double {
        val params = getParams(skill)
        val prevL = if (currentMastery <= 0.0) params.l0 else currentMastery

        val pLGivenObs = if (isCorrect) {
            val numerator = prevL * (1.0 - params.s)
            val denominator = numerator + (1.0 - prevL) * params.g
            if (denominator == 0.0) prevL else numerator / denominator
        } else {
            val numerator = prevL * params.s
            val denominator = numerator + (1.0 - prevL) * (1.0 - params.g)
            if (denominator == 0.0) prevL else numerator / denominator
        }

        val nextL = pLGivenObs + (1.0 - pLGivenObs) * params.t
        return nextL.coerceIn(0.0, 1.0)
    }
}
