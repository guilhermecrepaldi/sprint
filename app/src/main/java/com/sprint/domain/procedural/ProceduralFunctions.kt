package com.sprint.domain.procedural

import java.util.UUID
import kotlin.random.Random
import kotlin.math.pow

object ProceduralFunctions {

    fun generate(skillTag: String, mmr: Int): ProceduralExercise {
        val diff = (mmr.toDouble() / 100.0).toInt().coerceAtLeast(1)
        return when (skillTag) {
            "function_eval" -> generateFunctionEval(diff)
            "function_domain" -> generateDomain(diff)
            "function_composition" -> generateComposition(diff)
            "function_exp_log" -> generateExpLog(diff)
            else -> generateFunctionEval(diff)
        }
    }

    private fun generateFunctionEval(difficulty: Int): ProceduralExercise {
        val a = ProceduralEngine.randomInstance.nextInt(1, 5)
        val b = ProceduralEngine.randomInstance.nextInt(1, 10)
        val x = ProceduralEngine.randomInstance.nextInt(-5, 5)
        val answer = a * x + b
        
        return ProceduralExercise(
            id = UUID.randomUUID().toString(),
            statement = "Dada a função f(x) = ${a}x + $b, calcule o valor de f($x).\n\n[fig:math_line, a=$a, b=$b]",
            expectedAnswer = answer.toString(),
            primarySkill = "function_eval",
            difficulty = difficulty.toDouble(),
            templateId = "func_01",
            canvasMode = "blank",
            validatorType = "exact",
            answerType = "numeric"
        )
    }

    private fun generateDomain(difficulty: Int): ProceduralExercise {
        val a = ProceduralEngine.randomInstance.nextInt(1, 10)
        
        return ProceduralExercise(
            id = UUID.randomUUID().toString(),
            statement = "Qual é o valor que NÃO pertence ao domínio da função f(x) = 1 / (x - $a)?",
            expectedAnswer = a.toString(),
            primarySkill = "function_domain",
            difficulty = difficulty.toDouble(),
            templateId = "func_02",
            canvasMode = "blank",
            validatorType = "exact",
            answerType = "numeric"
        )
    }

    private fun generateComposition(difficulty: Int): ProceduralExercise {
        val x = ProceduralEngine.randomInstance.nextInt(1, 5)
        val a = ProceduralEngine.randomInstance.nextInt(2, 4)
        val b = ProceduralEngine.randomInstance.nextInt(1, 5)
        // f(x) = ax, g(x) = x + b
        // f(g(x)) = a(x + b)
        val gx = x + b
        val answer = a * gx
        
        return ProceduralExercise(
            id = UUID.randomUUID().toString(),
            statement = "Dadas f(x) = ${a}x e g(x) = x + $b, calcule f(g($x)).",
            expectedAnswer = answer.toString(),
            primarySkill = "function_composition",
            difficulty = difficulty.toDouble(),
            templateId = "func_03",
            canvasMode = "blank",
            validatorType = "exact",
            answerType = "numeric"
        )
    }

    private fun generateExpLog(difficulty: Int): ProceduralExercise {
        val exp = ProceduralEngine.randomInstance.nextInt(2, 6)
        val ans = 2.0.pow(exp.toDouble()).toInt()
        
        return ProceduralExercise(
            id = UUID.randomUUID().toString(),
            statement = "Resolva a equação exponencial: 2^x = $ans.\n\n[fig:math_exponential, base=2]",
            expectedAnswer = exp.toString(),
            primarySkill = "function_exp_log",
            difficulty = difficulty.toDouble(),
            templateId = "func_04",
            canvasMode = "blank",
            validatorType = "exact",
            answerType = "numeric"
        )
    }
}
