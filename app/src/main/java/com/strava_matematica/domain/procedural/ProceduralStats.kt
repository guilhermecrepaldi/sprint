package com.strava_matematica.domain.procedural

import java.util.UUID
import kotlin.random.Random

object ProceduralStats {

    fun generate(skillTag: String, mmr: Int, random: Random = Random): ProceduralExercise {
        val diffLvl = mmr / 500
        return when (skillTag) {
            "stat_comb_perm", "stat_comb_fund" -> generatePermutacao(diffLvl, random)
            "stat_prob", "stat_prob_cond" -> generateProbabilidade(diffLvl, random)
            else -> generatePermutacao(diffLvl, random)
        }
    }

    private fun factorial(n: Int): Long {
        if (n <= 1) return 1
        var result = 1L
        for (i in 2..n) result *= i
        return result
    }

    /**
     * RPG: Permutações Simples ou Arranjos
     */
    private fun generatePermutacao(diffLvl: Int, random: Random): ProceduralExercise {
        val type = random.nextInt(if (diffLvl < 2) 1 else 2) // 0 = Fatorial/Permutacao simples, 1 = Arranjo
        
        return if (type == 0) {
            // Permutação simples
            val n = random.nextInt(3, 7) // 3! to 6!
            val answer = factorial(n)
            
            val statement = "Calcule o valor de \\($n!\\) (Permutação de $n elementos)."
            ProceduralExercise(
                id = UUID.randomUUID().toString(),
                statement = statement,
                expectedAnswer = answer.toString(),
                primarySkill = "stat_comb_perm",
                difficulty = diffLvl.toDouble(),
                templateId = "stat_perm_01",
                canvasMode = "blank",
                validatorType = "exact",
                answerType = "numeric"
            )
        } else {
            // Arranjo simples: A(n, p) = n! / (n-p)!
            val n = random.nextInt(4, 8)
            val p = random.nextInt(2, minOf(n, 4))
            val answer = factorial(n) / factorial(n - p)
            
            val statement = "Calcule o arranjo simples \\(A_{$n,$p}\\)."
            ProceduralExercise(
                id = UUID.randomUUID().toString(),
                statement = statement,
                expectedAnswer = answer.toString(),
                primarySkill = "stat_comb_perm",
                difficulty = diffLvl.toDouble() + 1.0,
                templateId = "stat_arr_01",
                canvasMode = "blank",
                validatorType = "exact",
                answerType = "numeric"
            )
        }
    }

    /**
     * RPG: Probabilidade Simples
     */
    private fun generateProbabilidade(diffLvl: Int, random: Random): ProceduralExercise {
        // Lançamento de dados ou moedas
        val eventType = random.nextInt(2) // 0 = Dado, 1 = Bolas na urna
        
        return if (eventType == 0) {
            // Dado
            val diceType = if (diffLvl < 2) 6 else arrayOf(6, 8, 12, 20).random(random)
            val numType = random.nextInt(2) // 0 = par/impar, 1 = maior que X
            
            if (numType == 0) {
                val statement = "Ao lançar um dado não viciado de $diceType faces, qual a probabilidade de cair um número par? (Em porcentagem %)"
                val expectedAnswer = "50" // Sempre 50% para par/impar num dado normal par
                ProceduralExercise(
                    id = UUID.randomUUID().toString(),
                    statement = statement,
                    expectedAnswer = expectedAnswer,
                    primarySkill = "stat_prob",
                    difficulty = diffLvl.toDouble(),
                    templateId = "stat_prob_01",
                    canvasMode = "blank",
                    validatorType = "exact",
                    answerType = "numeric"
                )
            } else {
                val x = random.nextInt(2, diceType - 1)
                val favoraveis = diceType - x
                val prob = (favoraveis.toDouble() / diceType.toDouble()) * 100
                // Format decimal softly
                val probStr = "%.1f".format(java.util.Locale.US, prob).replace(".0", "")
                
                val statement = "Ao lançar um dado não viciado de $diceType faces, qual a probabilidade de cair um número estritamente MAIOR que $x? (Em porcentagem %, arredonde uma casa decimal se precisar)"
                ProceduralExercise(
                    id = UUID.randomUUID().toString(),
                    statement = statement,
                    expectedAnswer = probStr,
                    primarySkill = "stat_prob",
                    difficulty = diffLvl.toDouble() + 1.0,
                    templateId = "stat_prob_02",
                    canvasMode = "blank",
                    validatorType = "exact",
                    answerType = "numeric"
                )
            }
        } else {
            // Urna
            val bolasA = random.nextInt(2, 6) * 2 // pares for cleaner percentage
            val bolasB = random.nextInt(2, 6) * 2
            val total = bolasA + bolasB
            val probA = (bolasA.toDouble() / total.toDouble()) * 100
            val probStr = "%.0f".format(java.util.Locale.US, probA)

            val statement = "Uma urna contém $bolasA bolas vermelhas e $bolasB bolas azuis. Qual a probabilidade de sacar uma bola vermelha? (Em porcentagem %)"
            ProceduralExercise(
                id = UUID.randomUUID().toString(),
                statement = statement,
                expectedAnswer = probStr,
                primarySkill = "stat_prob",
                difficulty = diffLvl.toDouble(),
                templateId = "stat_prob_03",
                canvasMode = "blank",
                validatorType = "exact",
                answerType = "numeric"
            )
        }
    }
}
