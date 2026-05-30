package com.strava_matematica.domain.procedural

import com.strava_matematica.model.SessionConfig
import java.util.UUID
import java.util.Locale
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
            "soma_subtracao" -> generateSomaSubtracao(mmr, config.digitsCount, config.valuesCount, config.numberSet)
            "multiplicacao_divisao" -> generateMultiplicacaoDivisao(mmr, config.digitsCount)
            else -> generateSomaSubtracao(mmr, config.digitsCount, config.valuesCount, config.numberSet) // Fallback
        }
    }

    private fun generateSomaSubtracao(mmr: Int, digitsCount: Int, valuesCount: Int, numberSet: String): ProceduralExercise {
        val count = valuesCount.coerceIn(2, 6)
        val ops = List(count - 1) { if (Random.nextBoolean()) "+" else "-" }
        
        when (numberSet) {
            "racionais" -> {
                // Geração de Frações Didáticas Kumon
                val denoms = listOf(2, 3, 4, 5, 6, 8)
                val fractions = List(count) {
                    val den = denoms.random()
                    val num = Random.nextInt(1, den * digitsCount)
                    Fraction(num, den).simplify()
                }
                
                var currentFraction = fractions[0]
                val statementBuilder = StringBuilder()
                statementBuilder.append("\\frac{${fractions[0].num}}{${fractions[0].den}}")
                
                for (i in 0 until ops.size) {
                    val op = ops[i]
                    val frac = fractions[i + 1]
                    statementBuilder.append(" ").append(op).append(" \\frac{${frac.num}}{${frac.den}}")
                    currentFraction = if (op == "+") {
                        currentFraction.plus(frac)
                    } else {
                        currentFraction.minus(frac)
                    }
                }
                
                statementBuilder.append(" = ?")
                
                return ProceduralExercise(
                    id = UUID.randomUUID().toString(),
                    statement = statementBuilder.toString(),
                    expectedAnswer = currentFraction.toString(),
                    primarySkill = "soma_subtracao",
                    difficulty = (mmr.toDouble() / 100.0) + (digitsCount * 0.8) + (count * 1.0),
                    templateId = "basic_fraction_addition",
                    canvasMode = "blank",
                    validatorType = "exact",
                    answerType = "numeric"
                )
            }
            "decimais" -> {
                // Geração de Decimais com 1 casa decimal
                val minBound = if (digitsCount <= 1) 1 else Math.pow(10.0, (digitsCount - 1).toDouble()).toInt()
                val maxBound = Math.pow(10.0, digitsCount.toDouble()).toInt() - 1
                
                val decimals = List(count) {
                    val isNeg = Random.nextBoolean()
                    val rawVal = Random.nextInt(minBound * 10, maxBound * 10 + 1) / 10.0
                    if (isNeg) -rawVal else rawVal
                }
                
                var ans = decimals[0]
                val statementBuilder = StringBuilder()
                statementBuilder.append(formatDecimal(decimals[0], isFirst = true))
                
                for (i in 0 until ops.size) {
                    val op = ops[i]
                    val dec = decimals[i + 1]
                    statementBuilder.append(" ").append(op).append(" ").append(formatDecimal(dec, isFirst = false))
                    ans = if (op == "+") ans + dec else ans - dec
                }
                
                statementBuilder.append(" = ?")
                val formattedAnswer = java.lang.String.format(Locale.US, "%.1f", ans)
                    .replace(".0", "")
                
                return ProceduralExercise(
                    id = UUID.randomUUID().toString(),
                    statement = statementBuilder.toString(),
                    expectedAnswer = formattedAnswer,
                    primarySkill = "soma_subtracao",
                    difficulty = (mmr.toDouble() / 100.0) + (digitsCount * 0.6) + (count * 0.8),
                    templateId = "basic_decimal_addition",
                    canvasMode = "blank",
                    validatorType = "exact",
                    answerType = "numeric"
                )
            }
            "inteiros" -> {
                // Geração de Inteiros positivos e negativos
                val minBound = if (digitsCount <= 1) 1 else Math.pow(10.0, (digitsCount - 1).toDouble()).toInt()
                val maxBound = Math.pow(10.0, digitsCount.toDouble()).toInt() - 1
                
                val integers = List(count) {
                    val isNeg = Random.nextBoolean()
                    val magnitude = Random.nextInt(minBound, maxBound + 1)
                    if (isNeg) -magnitude else magnitude
                }
                
                var ans = integers[0]
                val statementBuilder = StringBuilder()
                statementBuilder.append(formatInteger(integers[0], isFirst = true))
                
                for (i in 0 until ops.size) {
                    val op = ops[i]
                    val term = integers[i + 1]
                    statementBuilder.append(" ").append(op).append(" ").append(formatInteger(term, isFirst = false))
                    ans = if (op == "+") ans + term else ans - term
                }
                
                statementBuilder.append(" = ?")
                
                return ProceduralExercise(
                    id = UUID.randomUUID().toString(),
                    statement = statementBuilder.toString(),
                    expectedAnswer = ans.toString(),
                    primarySkill = "soma_subtracao",
                    difficulty = (mmr.toDouble() / 100.0) + (digitsCount * 0.5) + (count * 0.8),
                    templateId = "basic_integer_addition",
                    canvasMode = "blank",
                    validatorType = "exact",
                    answerType = "numeric"
                )
            }
            "mix" -> {
                // Sorteia o tipo de cada folha de forma variada
                val rolledType = listOf("inteiros", "decimais", "racionais").random()
                return generateSomaSubtracao(mmr, digitsCount, valuesCount, rolledType)
            }
            else -> {
                // Naturais (default Kumon)
                val minBound = if (digitsCount <= 1) 1 else Math.pow(10.0, (digitsCount - 1).toDouble()).toInt()
                val maxBound = Math.pow(10.0, digitsCount.toDouble()).toInt() - 1
                
                val terms = List(count) { Random.nextInt(minBound, maxBound + 1) }
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
                
                return ProceduralExercise(
                    id = UUID.randomUUID().toString(),
                    statement = statementBuilder.toString(),
                    expectedAnswer = ans.toString(),
                    primarySkill = "soma_subtracao",
                    difficulty = (mmr.toDouble() / 100.0) + (digitsCount * 0.5) + (count * 0.8),
                    templateId = "basic_addition_subtraction",
                    canvasMode = "blank",
                    validatorType = "exact",
                    answerType = "numeric"
                )
            }
        }
    }

    private fun generateMultiplicacaoDivisao(mmr: Int, digitsCount: Int): ProceduralExercise {
        val minBound = if (digitsCount <= 1) 2 else Math.pow(10.0, (digitsCount - 1).toDouble()).toInt()
        val maxBound = Math.pow(10.0, digitsCount.toDouble()).toInt() - 1

        val a = Random.nextInt(minBound, maxBound + 1)
        val b = Random.nextInt(2, if (digitsCount <= 1) 9 else 12)
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

    // ── Helpers ──────────────────────────────────────────────────────────────

    private fun formatInteger(valInt: Int, isFirst: Boolean): String {
        return if (valInt < 0) {
            if (isFirst) "$valInt" else "($valInt)"
        } else {
            "$valInt"
        }
    }

    private fun formatDecimal(valDouble: Double, isFirst: Boolean): String {
        val label = java.lang.String.format(Locale.US, "%.1f", valDouble).replace(".0", "")
        return if (valDouble < 0) {
            if (isFirst) label else "($label)"
        } else {
            label
        }
    }

    private fun gcd(a: Int, b: Int): Int {
        var x = Math.abs(a)
        var y = Math.abs(b)
        while (y != 0) {
            val temp = y
            y = x % y
            x = temp
        }
        return x
    }

    // Representação exata de Fração Didática
    data class Fraction(val num: Int, val den: Int) {
        fun simplify(): Fraction {
            if (den == 0) return this
            val d = gcd(num, den)
            val sign = if (den < 0) -1 else 1
            return Fraction((num / d) * sign, (den / d) * sign)
        }
        
        fun plus(other: Fraction): Fraction {
            val n = num * other.den + other.num * den
            val d = den * other.den
            return Fraction(n, d).simplify()
        }
        
        fun minus(other: Fraction): Fraction {
            val n = num * other.den - other.num * den
            val d = den * other.den
            return Fraction(n, d).simplify()
        }
        
        override fun toString(): String {
            val simp = simplify()
            return if (simp.den == 1) simp.num.toString() else "${simp.num}/${simp.den}"
        }
    }
}
