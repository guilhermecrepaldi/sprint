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
    var randomInstance: Random = Random.Default


    fun generate(skillTag: String, mmr: Int, config: SessionConfig = SessionConfig()): ProceduralExercise {
        return when (skillTag) {
            "soma_subtracao" -> generateSomaSubtracao(mmr, config.digitsCount, config.valuesCount, config.numberSet)
            "multiplicacao_divisao" -> generateMultiplicacaoDivisao(mmr, config.digitsCount)

            // Algebra
            "equacoes_quadraticas", "equacao_2_grau",
            "fatoracao_produtos_notaveis", "polinomios",
            "fracoes_decimais", "porcentagem_razao", "potenciacao_radiciacao",
            "equacoes_lineares", "sistemas_equacoes", "inequacoes", "funcao_afim",
            "funcao_quadratica", "funcao_exponencial", "funcao_logaritmica", "funcao_modular" ->
                ProceduralAlgebra.generate(skillTag, mmr)

            // Geometry
            "geo_euc_plan", "geo_euc_spc", "geo_euc_non",
            "geo_ana_cart", "geo_ana_eq", "geo_ana_con",
            "geo_diff_crv", "geo_diff_man", "geo_diff_riem",
            "geo_top_spc", "geo_top_cont", "geo_top_comp", "geo_top_alg",
            "geometria_plana", "geometria_espacial", "geometria_analitica",
            "progressoes_pa_pg", "trig_razoes", "trig_seno_cosseno_tangente", "trig_identidades", "trig_equacoes" ->
                ProceduralGeometry.generate(skillTag, mmr)

            // Calculus
            "calc_pre_func", "calc_pre_elem", "calc_pre_seq",
            "calc_dif_lim", "calc_dif_der", "calc_dif_int", "calc_dif_mul",
            "calc_eq_ode", "calc_eq_pde", "calc_eq_trans",
            "calc_real_lim", "calc_real_met", "calc_real_comp",
            "nocao_de_limite", "continuidade", "derivadas_basicas",
            "derivadas_regra_cadeia", "derivadas_produto_quociente",
            "aplicacoes_derivadas", "integrais_indefinidas",
            "integrais_definidas", "aplicacoes_integrais" ->
                ProceduralCalculus.generate(skillTag, mmr)

            // Statistics & Combinatorics
            "stat_comb_fund", "stat_comb_perm", "stat_comb_bin",
            "stat_prob_cond", "stat_prob_var", "stat_prob_dist", "stat_prob",
            "combinatoria", "probabilidade" ->
                ProceduralStats.generate(skillTag, mmr)

            // Linear Algebra
            "soma_produto_matrizes", "determinantes", "operacoes_vetoriais" ->
                ProceduralLinearAlgebra.generate(skillTag, mmr)

            else -> generatePlaceholder(skillTag, mmr)
        }
    }

    private fun generatePlaceholder(skillTag: String, mmr: Int): ProceduralExercise {
        return ProceduralExercise(
            id = UUID.randomUUID().toString(),
            statement = "Conteúdo não programado na Engine: $skillTag\nMMR Alvo: $mmr",
            expectedAnswer = "0",
            primarySkill = skillTag,
            difficulty = mmr.toDouble(),
            templateId = "placeholder",
            canvasMode = "blank"
        )
    }

    private fun generateSomaSubtracao(mmr: Int, digitsCount: Int, valuesCount: Int, numberSet: String): ProceduralExercise {
        val count = valuesCount.coerceIn(2, 6)
        val ops = List(count - 1) { if (ProceduralEngine.randomInstance.nextBoolean()) "+" else "-" }
        
        when (numberSet) {
            "racionais" -> {
                // Geração de Frações Didáticas Kumon
                val denoms = listOf(2, 3, 4, 5, 6, 8)
                val fractions = List(count) {
                    val den = denoms.random()
                    val num = ProceduralEngine.randomInstance.nextInt(1, den * digitsCount)
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
                    val isNeg = ProceduralEngine.randomInstance.nextBoolean()
                    val rawVal = ProceduralEngine.randomInstance.nextInt(minBound * 10, maxBound * 10 + 1) / 10.0
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
                    val isNeg = ProceduralEngine.randomInstance.nextBoolean()
                    val magnitude = ProceduralEngine.randomInstance.nextInt(minBound, maxBound + 1)
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
                
                val terms = List(count) { ProceduralEngine.randomInstance.nextInt(minBound, maxBound + 1) }
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

        val a = ProceduralEngine.randomInstance.nextInt(minBound, maxBound + 1)
        val b = ProceduralEngine.randomInstance.nextInt(2, if (digitsCount <= 1) 9 else 12)
        val isMult = ProceduralEngine.randomInstance.nextBoolean()

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
