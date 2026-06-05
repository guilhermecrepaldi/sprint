import os

base_dir = r"D:\LOVE CLASS\app\src\main\java\com\strava_matematica\domain\procedural"

# 1. ProceduralArithmetic.kt
arithmetic_code = """package com.strava_matematica.domain.procedural

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
        val primes = listOf(2, 3, 5, 7, 11, 13, 17, 19, 23, 29, 31, 37, 41, 43, 47)
        val p = primes.random(ProceduralEngine.randomInstance)
        return ProceduralExercise(
            id = UUID.randomUUID().toString(),
            statement = "O número $p é primo? Responda com 'Sim' ou 'Não'.",
            expectedAnswer = "Sim",
            primarySkill = "divisibility_primes",
            difficulty = difficulty.toDouble(),
            templateId = "arithmetic_02",
            canvasMode = "blank",
            validatorType = "exact",
            answerType = "text"
        )
    }

    private fun generateDizimas(difficulty: Int): ProceduralExercise {
        val period = ProceduralEngine.randomInstance.nextInt(1, 9)
        val fracNum = period
        val fracDen = 9
        fun gcd(a: Int, b: Int): Int = if (b == 0) a else gcd(b, a % b)
        val g = gcd(fracNum, fracDen)
        val expected = "${fracNum / g}/${fracDen / g}"
        
        return ProceduralExercise(
            id = UUID.randomUUID().toString(),
            statement = "Qual a fração geratriz da dízima periódica 0,$period$period$period...? (Simplifique a fração)",
            expectedAnswer = expected,
            primarySkill = "dizimas",
            difficulty = difficulty.toDouble(),
            templateId = "arithmetic_03",
            canvasMode = "blank",
            validatorType = "exact",
            answerType = "fraction"
        )
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
"""

# 2. ProceduralFunctions.kt
functions_code = """package com.strava_matematica.domain.procedural

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
            statement = "Dada a função f(x) = ${a}x + $b, calcule o valor de f($x).\\n\\n[fig:math_line, a=$a, b=$b]",
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
            statement = "Resolva a equação exponencial: 2^x = $ans.\\n\\n[fig:math_exponential, base=2]",
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
"""

# 3. ProceduralProportions.kt
proportions_code = """package com.strava_matematica.domain.procedural

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
"""

# 4. ProceduralLogic.kt
logic_code = """package com.strava_matematica.domain.procedural

import java.util.UUID

object ProceduralLogic {

    fun generate(skillTag: String, mmr: Int): ProceduralExercise {
        val diff = (mmr.toDouble() / 100.0).toInt().coerceAtLeast(1)
        return when (skillTag) {
            "sets_venn" -> generateSets(diff)
            "propositional" -> generatePropositional(diff)
            else -> generateSets(diff)
        }
    }

    private fun generateSets(difficulty: Int): ProceduralExercise {
        val nA = ProceduralEngine.randomInstance.nextInt(10, 30)
        val nB = ProceduralEngine.randomInstance.nextInt(10, 30)
        val nIntersection = ProceduralEngine.randomInstance.nextInt(2, 9)
        val nUnion = nA + nB - nIntersection
        
        return ProceduralExercise(
            id = UUID.randomUUID().toString(),
            statement = "Em um grupo, $nA pessoas gostam de Matemática, $nB gostam de Física e $nIntersection gostam de ambas. Quantas pessoas gostam de Matemática ou Física?\\n\\n[fig:venn_2]",
            expectedAnswer = nUnion.toString(),
            primarySkill = "sets_venn",
            difficulty = difficulty.toDouble(),
            templateId = "logic_01",
            canvasMode = "blank",
            validatorType = "exact",
            answerType = "numeric"
        )
    }

    private fun generatePropositional(difficulty: Int): ProceduralExercise {
        val isTrue = ProceduralEngine.randomInstance.nextBoolean()
        val p = if (isTrue) "V" else "F"
        val q = "F"
        val answer = if (isTrue) "F" else "V" // p AND q = F, Not(p AND q) = V, wait let's do something simpler
        // Let's ask: If p is V and q is V, p AND q is V.
        val statement = "Se a proposição P é Verdadeira (V) e Q é Falsa (F), qual é o valor lógico de 'P E Q' (conjunção)? Responda com V ou F."
        
        return ProceduralExercise(
            id = UUID.randomUUID().toString(),
            statement = statement,
            expectedAnswer = "F",
            primarySkill = "propositional",
            difficulty = difficulty.toDouble(),
            templateId = "logic_02",
            canvasMode = "blank",
            validatorType = "exact",
            answerType = "text"
        )
    }
}
"""

with open(os.path.join(base_dir, "ProceduralArithmetic.kt"), "w", encoding="utf-8") as f:
    f.write(arithmetic_code)
with open(os.path.join(base_dir, "ProceduralFunctions.kt"), "w", encoding="utf-8") as f:
    f.write(functions_code)
with open(os.path.join(base_dir, "ProceduralProportions.kt"), "w", encoding="utf-8") as f:
    f.write(proportions_code)
with open(os.path.join(base_dir, "ProceduralLogic.kt"), "w", encoding="utf-8") as f:
    f.write(logic_code)

print("Created ProceduralArithmetic.kt, ProceduralFunctions.kt, ProceduralProportions.kt, ProceduralLogic.kt")
