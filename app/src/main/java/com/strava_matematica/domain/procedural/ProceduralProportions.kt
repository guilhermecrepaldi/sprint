package com.strava_matematica.domain.procedural

import java.util.UUID

object ProceduralProportions {

    fun generate(skillTag: String, mmr: Int): ProceduralExercise {
        val diff = (mmr.toDouble() / 100.0).toInt().coerceAtLeast(1)
        return when (skillTag) {
            "rule_of_3" -> generateRuleOf3(diff)
            "percentage" -> generatePercentage(diff)
            "interest" -> generateInterest(diff)
            else -> generateRuleOf3(diff)
        }
    }

    private fun generateRuleOf3(difficulty: Int): ProceduralExercise {
        val a = ProceduralEngine.randomInstance.nextInt(2, 10)
        val b = ProceduralEngine.randomInstance.nextInt(10, 50)
        val c = a * ProceduralEngine.randomInstance.nextInt(2, 5)
        val answer = (b * c) / a
        
        return ProceduralExercise(
            id = UUID.randomUUID().toString(),
            statement = "Se $a operários constroem um muro em $b dias, quantos dias $c operários levariam para construir o mesmo muro, assumindo grandezas diretamente proporcionais? (Exemplo didático)",
            expectedAnswer = answer.toString(),
            primarySkill = "rule_of_3",
            difficulty = difficulty.toDouble(),
            templateId = "prop_01",
            canvasMode = "blank",
            validatorType = "exact",
            answerType = "numeric"
        )
    }

    private fun generatePercentage(difficulty: Int): ProceduralExercise {
        val total = ProceduralEngine.randomInstance.nextInt(1, 10) * 100
        val percent = ProceduralEngine.randomInstance.nextInt(1, 9) * 10
        val answer = (total * percent) / 100
        
        return ProceduralExercise(
            id = UUID.randomUUID().toString(),
            statement = "Calcule $percent% de R$ $total.",
            expectedAnswer = answer.toString(),
            primarySkill = "percentage",
            difficulty = difficulty.toDouble(),
            templateId = "prop_02",
            canvasMode = "blank",
            validatorType = "exact",
            answerType = "numeric"
        )
    }

    private fun generateInterest(difficulty: Int): ProceduralExercise {
        val capital = ProceduralEngine.randomInstance.nextInt(1, 5) * 1000
        val rate = ProceduralEngine.randomInstance.nextInt(1, 5)
        val months = ProceduralEngine.randomInstance.nextInt(2, 12)
        val interest = (capital * rate * months) / 100
        
        return ProceduralExercise(
            id = UUID.randomUUID().toString(),
            statement = "Qual o juro simples gerado por um capital de R$ $capital aplicado a uma taxa de $rate% ao mês durante $months meses?",
            expectedAnswer = interest.toString(),
            primarySkill = "interest",
            difficulty = difficulty.toDouble(),
            templateId = "prop_03",
            canvasMode = "blank",
            validatorType = "exact",
            answerType = "numeric"
        )
    }
}
