package com.strava_matematica.domain.procedural

import java.util.UUID

object ProceduralCompMath {

    fun generate(skillTag: String, mmr: Int): ProceduralExercise {
        val diff = (mmr.toDouble() / 100.0).toInt().coerceAtLeast(1)
        
        val (statement, expectedAnswer) = when (skillTag) {
            "comp_num_err", "comp_num_int", "comp_num_diff", "comp_num_sys" -> {
                "Quantos bits são padronizados para o expoente na representação de ponto flutuante de precisão simples (Float - IEEE 754)?" to "8"
            }
            "comp_opt_lin", "comp_opt_non", "comp_opt_int" -> {
                "Quantos bits são padronizados para o expoente na representação de ponto flutuante de dupla precisão (Double - IEEE 754)?" to "11"
            }
            "comp_dyn_sys", "comp_dyn_attr", "comp_dyn_chaos" -> {
                "Qual é o valor numérico em base decimal do número binário 1010?" to "10"
            }
            "comp_th_aut", "comp_th_comp", "comp_th_comp_ana" -> {
                "Qual é o valor numérico em base decimal do número binário 1111?" to "15"
            }
            else -> "Converta o binário 100 para decimal." to "4"
        }

        return ProceduralExercise(
            id = UUID.randomUUID().toString(),
            statement = statement,
            expectedAnswer = expectedAnswer,
            primarySkill = skillTag,
            difficulty = diff.toDouble(),
            templateId = "comp_math_basic",
            canvasMode = "blank",
            validatorType = "exact",
            answerType = "numeric"
        )
    }
}
