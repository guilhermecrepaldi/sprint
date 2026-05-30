package com.strava_matematica.domain.procedural

import com.strava_matematica.model.SessionConfig
import java.util.UUID
import kotlin.random.Random

data class ProceduralExercise(
    val id: String,
    val statement: String,
    val expectedAnswer: String,
    val primarySkill: String,
    val difficulty: Double,
    val templateId: String,
    val canvasMode: String = "blank",
    val validatorType: String = "exact",
    val answerType: String = "numeric",
)

object ProceduralEngine {

    fun generate(skillTag: String, mmr: Int, config: SessionConfig = SessionConfig()): ProceduralExercise {
        return when (skillTag) {
            "soma_subtracao" -> generateSomaSubtracao(mmr, config.digitsCount, config.valuesCount)
            "multiplicacao_divisao" -> generateMultiplicacaoDivisao(mmr, config.digitsCount)
            else -> generateSomaSubtracao(mmr, config.digitsCount, config.valuesCount) // Fallback
        }
    }

    private fun generateSomaSubtracao(mmr: Int, digitsCount: Int, valuesCount: Int): ProceduralExercise {
        // Obter faixa de sorteio baseada na ordem de grandeza (digitsCount)
        val minBound = if (digitsCount <= 1) 1 else Math.pow(10.0, (digitsCount - 1).toDouble()).toInt()
        val maxBound = Math.pow(10.0, digitsCount.toDouble()).toInt() - 1
        
        val count = valuesCount.coerceIn(2, 6) // Entre 2 e 6 valores no desafio
        val terms = List(count) { Random.nextInt(minBound, maxBound + 1) }
        val ops = List(count - 1) { if (Random.nextBoolean()) "+" else "-" }
        
        var ans = terms[0]
        val statementBuilder = StringBuilder().append(terms[0])
        for (i in 0 until ops.size) {
            val op = ops[i]
            val term = terms[i + 1]
            statementBuilder.append(" ").append(op).append(" ").append(term)
            if (op == "+") {
                ans += term
            } else {
                ans -= term
            }
        }
        statementBuilder.append(" = ?")
        
        val difficulty = (mmr.toDouble() / 100.0) + (digitsCount * 0.5) + (count * 0.8)

        return ProceduralExercise(
            id = UUID.randomUUID().toString(),
            statement = statementBuilder.toString(),
            expectedAnswer = ans.toString(),
            primarySkill = "soma_subtracao",
            difficulty = difficulty,
            templateId = "basic_addition_subtraction",
            canvasMode = "blank",
            validatorType = "exact",
            answerType = "numeric"
        )
    }

    private fun generateMultiplicacaoDivisao(mmr: Int, digitsCount: Int): ProceduralExercise {
        val minBound = if (digitsCount <= 1) 2 else Math.pow(10.0, (digitsCount - 1).toDouble()).toInt()
        val maxBound = Math.pow(10.0, digitsCount.toDouble()).toInt() - 1

        val a = Random.nextInt(minBound, maxBound + 1)
        val b = Random.nextInt(2, if (digitsCount <= 1) 9 else 12) // Multiplicador de tabuada simples
        val isMult = Random.nextBoolean()

        val difficulty = (mmr.toDouble() / 100.0) + (digitsCount * 0.8)

        return if (isMult) {
            ProceduralExercise(
                id = UUID.randomUUID().toString(),
                statement = "$a \\times $b = ?",
                expectedAnswer = (a * b).toString(),
                primarySkill = "multiplicacao_divisao",
                difficulty = difficulty,
                templateId = "basic_multiplication",
                canvasMode = "blank",
                validatorType = "exact",
                answerType = "numeric"
            )
        } else {
            val product = a * b
            ProceduralExercise(
                id = UUID.randomUUID().toString(),
                statement = "$product \\div $b = ?",
                expectedAnswer = a.toString(),
                primarySkill = "multiplicacao_divisao",
                difficulty = difficulty,
                templateId = "basic_division",
                canvasMode = "blank",
                validatorType = "exact",
                answerType = "numeric"
            )
        }
    }
}
