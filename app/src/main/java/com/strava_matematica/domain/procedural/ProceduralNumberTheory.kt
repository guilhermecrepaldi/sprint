package com.strava_matematica.domain.procedural

import java.util.UUID

object ProceduralNumberTheory {

    fun generate(skillTag: String, mmr: Int): ProceduralExercise {
        val diff = (mmr.toDouble() / 100.0).toInt().coerceAtLeast(1)
        
        val (statement, expectedAnswer) = when (skillTag) {
            "fnd_num_nat", "fnd_num_int", "fnd_num_real", "fnd_num_comp" -> {
                val n = ProceduralEngine.randomInstance.nextInt(1, 4)
                if (n == 1) "O número -5 pertence ao conjunto dos Números Naturais (N)? Responda V ou F." to "F"
                else if (n == 2) "O número pi (π) é um número Racional (Q) ou Irracional (I)? Responda R ou I." to "I"
                else "Todo número Real (R) é também um número Complexo (C)? Responda V ou F." to "V"
            }
            "alg_num_div" -> {
                val n = ProceduralEngine.randomInstance.nextInt(2, 9)
                val m = ProceduralEngine.randomInstance.nextInt(3, 10)
                "Se a = $n e b = ${n*m}, podemos afirmar que 'a divide b' (a|b)? Responda V ou F." to "V"
            }
            "alg_num_prim" -> {
                "Qual é o menor número primo que é também um número par?" to "2"
            }
            "alg_num_mod" -> {
                val n = ProceduralEngine.randomInstance.nextInt(10, 50)
                val m = ProceduralEngine.randomInstance.nextInt(3, 9)
                "Qual é o valor de $n mod $m (o resto da divisão de $n por $m)?" to (n % m).toString()
            }
            "alg_num_dio" -> {
                "Na equação diofantina linear ax + by = c, para que exista solução inteira, c deve ser múltiplo de qual propriedade de a e b?\n1) MDC(a,b)\n2) MMC(a,b)\n(Responda 1 ou 2)" to "1"
            }
            else -> "Questão de Teoria dos Números." to "1"
        }

        return ProceduralExercise(
            id = UUID.randomUUID().toString(),
            statement = statement,
            expectedAnswer = expectedAnswer,
            primarySkill = skillTag,
            difficulty = diff.toDouble(),
            templateId = "num_theory_basic",
            canvasMode = "blank",
            validatorType = "exact",
            answerType = "text"
        )
    }
}
