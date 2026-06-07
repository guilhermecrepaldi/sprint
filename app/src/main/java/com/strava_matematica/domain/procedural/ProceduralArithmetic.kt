package com.strava_matematica.domain.procedural

import java.util.UUID
import kotlin.random.Random

object ProceduralArithmetic {

    fun generate(skillTag: String, mmr: Int): ProceduralExercise {
        val diff = (mmr.toDouble() / 100.0).toInt().coerceAtLeast(1)
        return when (skillTag) {
            "mmc_mdc" -> generateMmcMdc(diff)
            "divisibility_primes" -> generatePrimes(diff)
            "dizimas" -> generateDizimas(diff)
            "scientific_notation" -> generateScientificNotation(diff)
            else -> generateMmcMdc(diff)
        }
    }

    private fun generateMmcMdc(difficulty: Int): ProceduralExercise {
        val a = ProceduralEngine.randomInstance.nextInt(4, 20 + difficulty * 5)
        val b = ProceduralEngine.randomInstance.nextInt(4, 20 + difficulty * 5)
        
        val isMmc = ProceduralEngine.randomInstance.nextBoolean()
        
        fun gcd(a: Int, b: Int): Int = if (b == 0) a else gcd(b, a % b)
        fun lcm(a: Int, b: Int): Int = (a * b) / gcd(a, b)
        
        val answer = if (isMmc) lcm(a, b) else gcd(a, b)
        val operation = if (isMmc) "MMC" else "MDC"
        
        return ProceduralExercise(
            id = UUID.randomUUID().toString(),
            statement = "Calcule o $operation entre $a e $b.",
            expectedAnswer = answer.toString(),
            primarySkill = "mmc_mdc",
            difficulty = difficulty.toDouble(),
            templateId = "arithmetic_01",
            canvasMode = "blank",
            validatorType = "exact",
            answerType = "numeric"
        )
    }

    private fun generatePrimes(difficulty: Int): ProceduralExercise {
        val rng = ProceduralEngine.randomInstance
        val primes = listOf(2, 3, 5, 7, 11, 13, 17, 19, 23, 29, 31, 37, 41, 43, 47)
        val composites = listOf(4, 6, 8, 9, 10, 12, 14, 15, 16, 18, 20, 21, 22, 24, 25, 26, 27, 28, 30, 32, 33, 34, 35, 36, 38, 39, 40, 42, 44, 45, 46, 48, 49, 50)
        val usePrime = rng.nextBoolean()
        val n = if (usePrime) primes.random(rng) else composites.random(rng)
        val answer = if (usePrime) "Sim" else "Não"
        return ProceduralExercise(
            id = UUID.randomUUID().toString(),
            statement = "O número $n é primo? Responda com 'Sim' ou 'Não'.",
            expectedAnswer = answer,
            primarySkill = "divisibility_primes",
            difficulty = difficulty.toDouble(),
            templateId = "arithmetic_02",
            canvasMode = "blank",
            validatorType = "exact",
            answerType = "text"
        )
    }

    private fun generateDizimas(difficulty: Int): ProceduralExercise {
        val rng = ProceduralEngine.randomInstance
        val isTwoDigit = rng.nextBoolean()
        fun gcd(a: Int, b: Int): Int = if (b == 0) a else gcd(b, a % b)

        if (isTwoDigit) {
            // Dízima periódica composta: 0.ababab... = ab/99
            val period = rng.nextInt(11, 99)
            val g = gcd(period, 99)
            val num = period / g
            val den = 99 / g
            val display = "$period"
            return ProceduralExercise(
                id = UUID.randomUUID().toString(),
                statement = "Qual a fração geratriz da dízima periódica 0,${display}${display}${display}...? (Simplifique)",
                expectedAnswer = "$num/$den",
                primarySkill = "dizimas",
                difficulty = difficulty.toDouble(),
                templateId = "arithmetic_03",
                canvasMode = "blank",
                validatorType = "exact",
                answerType = "fraction"
            )
        } else {
            // Dízima periódica simples: 0.NNN... = N/9
            val period = rng.nextInt(1, 9)
            val g = gcd(period, 9)
            return ProceduralExercise(
                id = UUID.randomUUID().toString(),
                statement = "Qual a fração geratriz da dízima periódica 0,$period$period$period...? (Simplifique)",
                expectedAnswer = "${period / g}/${9 / g}",
                primarySkill = "dizimas",
                difficulty = difficulty.toDouble(),
                templateId = "arithmetic_03",
                canvasMode = "blank",
                validatorType = "exact",
                answerType = "fraction"
            )
        }
    }

    private fun generateScientificNotation(difficulty: Int): ProceduralExercise {
        val coef = ProceduralEngine.randomInstance.nextInt(1, 9)
        val exp = ProceduralEngine.randomInstance.nextInt(2, 6)
        val num = coef * Math.pow(10.0, exp.toDouble()).toInt()
        
        return ProceduralExercise(
            id = UUID.randomUUID().toString(),
            statement = "Escreva o número $num em notação científica no formato a*10^b. Qual é o valor do expoente b?",
            expectedAnswer = exp.toString(),
            primarySkill = "scientific_notation",
            difficulty = difficulty.toDouble(),
            templateId = "arithmetic_04",
            canvasMode = "blank",
            validatorType = "exact",
            answerType = "numeric"
        )
    }
}
