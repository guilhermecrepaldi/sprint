package com.strava_matematica.domain.procedural

import java.util.UUID
import kotlin.random.Random
import kotlin.math.pow

object ProceduralAlgebra {

    fun generate(skillTag: String, mmr: Int): ProceduralExercise {
        val diff = (mmr.toDouble() / 100.0).toInt().coerceAtLeast(1)
        return when (skillTag) {
            "fracoes_decimais" -> generateFracoesDecimais(diff)
            "porcentagem_razao" -> generatePorcentagemRazao(diff)
            "potenciacao_radiciacao" -> generatePotenciacaoRadiciacao(diff)
            "equacoes_lineares" -> generateEquacoesLineares(diff)
            "sistemas_equacoes" -> generateSistemasEquacoes(diff)
            "inequacoes" -> generateInequacoes(diff)
            "funcao_afim" -> generateFuncaoAfim(diff)
            "funcao_quadratica" -> generateFuncaoQuadratica(diff)
            "funcao_exponencial" -> generateFuncaoExponencial(diff)
            "funcao_logaritmica" -> generateFuncaoLogaritmica(diff)
            "funcao_modular" -> generateFuncaoModular(diff)
            else -> generateEquacoesLineares(diff)
        }
    }

    private fun generateFracoesDecimais(difficulty: Int, templateId: String = "frac_dec_01"): ProceduralExercise {
        val num = Random.nextInt(1, 10)
        val den = intArrayOf(2, 4, 5, 10).random()
        val res = num.toDouble() / den
        return ProceduralExercise(
            id = UUID.randomUUID().toString(),
            statement = "Converta a fração \\frac{$num}{$den} para decimal.",
            expectedAnswer = res.toString(),
            primarySkill = "fracoes_decimais",
            difficulty = difficulty.toDouble(),
            templateId = templateId,
            canvasMode = "blank",
            validatorType = "exact",
            answerType = "numeric"
        )
    }

    private fun generatePorcentagemRazao(difficulty: Int, templateId: String = "pct_01"): ProceduralExercise {
        val total = intArrayOf(10, 50, 100, 200, 500).random()
        val percent = Random.nextInt(1, 20) * 5
        val ans = (total * percent) / 100
        return ProceduralExercise(
            id = UUID.randomUUID().toString(),
            statement = "Calcule $percent\\% de $total.",
            expectedAnswer = ans.toString(),
            primarySkill = "porcentagem_razao",
            difficulty = difficulty.toDouble(),
            templateId = templateId,
            canvasMode = "blank",
            validatorType = "exact",
            answerType = "numeric"
        )
    }

    private fun generatePotenciacaoRadiciacao(difficulty: Int, templateId: String = "pot_rad_01"): ProceduralExercise {
        val base = Random.nextInt(2, 6)
        val exp = Random.nextInt(2, 4)
        val ans = base.toDouble().pow(exp).toInt()
        return ProceduralExercise(
            id = UUID.randomUUID().toString(),
            statement = "Calcule \\sqrt[$exp]{$ans}.",
            expectedAnswer = base.toString(),
            primarySkill = "potenciacao_radiciacao",
            difficulty = difficulty.toDouble(),
            templateId = templateId,
            canvasMode = "blank",
            validatorType = "exact",
            answerType = "numeric"
        )
    }

    private fun generateEquacoesLineares(difficulty: Int, templateId: String = "eq_lin_01"): ProceduralExercise {
        val a = Random.nextInt(2, 10)
        val ans = Random.nextInt(-10, 10)
        val b = a * ans
        return ProceduralExercise(
            id = UUID.randomUUID().toString(),
            statement = "Resolva para x: ${a}x = $b",
            expectedAnswer = ans.toString(),
            primarySkill = "equacoes_lineares",
            difficulty = difficulty.toDouble(),
            templateId = templateId,
            canvasMode = "blank",
            validatorType = "exact",
            answerType = "numeric"
        )
    }

    private fun generateSistemasEquacoes(difficulty: Int, templateId: String = "sis_eq_01"): ProceduralExercise {
        val x = Random.nextInt(1, 10)
        val y = Random.nextInt(1, 10)
        val r1 = x + y
        val r2 = x - y
        return ProceduralExercise(
            id = UUID.randomUUID().toString(),
            statement = "Resolva o sistema:\\n x + y = $r1 \\n x - y = $r2 \\n Qual o valor de x?",
            expectedAnswer = x.toString(),
            primarySkill = "sistemas_equacoes",
            difficulty = difficulty.toDouble(),
            templateId = templateId,
            canvasMode = "blank",
            validatorType = "exact",
            answerType = "numeric"
        )
    }

    private fun generateInequacoes(difficulty: Int, templateId: String = "ineq_01"): ProceduralExercise {
        val a = Random.nextInt(2, 5)
        val b = Random.nextInt(1, 10)
        val c = Random.nextInt(11, 30)
        val ans = (c - b) / a + 1
        return ProceduralExercise(
            id = UUID.randomUUID().toString(),
            statement = "Qual o menor inteiro que satisfaz: ${a}x + $b > $c?",
            expectedAnswer = ans.toString(),
            primarySkill = "inequacoes",
            difficulty = difficulty.toDouble(),
            templateId = templateId,
            canvasMode = "blank",
            validatorType = "exact",
            answerType = "numeric"
        )
    }

    private fun generateFuncaoAfim(difficulty: Int, templateId: String = "func_afim_01"): ProceduralExercise {
        val a = Random.nextInt(2, 6)
        val b = Random.nextInt(1, 10)
        val x = Random.nextInt(1, 5)
        val ans = a * x + b
        return ProceduralExercise(
            id = UUID.randomUUID().toString(),
            statement = "Dada f(x) = ${a}x + $b, calcule f($x).",
            expectedAnswer = ans.toString(),
            primarySkill = "funcao_afim",
            difficulty = difficulty.toDouble(),
            templateId = templateId,
            canvasMode = "blank",
            validatorType = "exact",
            answerType = "numeric"
        )
    }

    private fun generateFuncaoQuadratica(difficulty: Int, templateId: String = "func_quad_01"): ProceduralExercise {
        val r1 = Random.nextInt(1, 5)
        val r2 = Random.nextInt(1, 5)
        val s = r1 + r2
        val p = r1 * r2
        return ProceduralExercise(
            id = UUID.randomUUID().toString(),
            statement = "Qual a soma das raízes de x^2 - ${s}x + $p = 0?",
            expectedAnswer = s.toString(),
            primarySkill = "funcao_quadratica",
            difficulty = difficulty.toDouble(),
            templateId = templateId,
            canvasMode = "blank",
            validatorType = "exact",
            answerType = "numeric"
        )
    }

    private fun generateFuncaoExponencial(difficulty: Int, templateId: String = "func_exp_01"): ProceduralExercise {
        val base = intArrayOf(2, 3, 5, 10).random()
        val exp = Random.nextInt(1, 4)
        val res = base.toDouble().pow(exp).toInt()
        return ProceduralExercise(
            id = UUID.randomUUID().toString(),
            statement = "Resolva para x: $base^x = $res",
            expectedAnswer = exp.toString(),
            primarySkill = "funcao_exponencial",
            difficulty = difficulty.toDouble(),
            templateId = templateId,
            canvasMode = "blank",
            validatorType = "exact",
            answerType = "numeric"
        )
    }

    private fun generateFuncaoLogaritmica(difficulty: Int, templateId: String = "func_log_01"): ProceduralExercise {
        val base = intArrayOf(2, 3, 5, 10).random()
        val ans = Random.nextInt(1, 4)
        val arg = base.toDouble().pow(ans).toInt()
        return ProceduralExercise(
            id = UUID.randomUUID().toString(),
            statement = "Calcule o valor de \\log_{$base}($arg).",
            expectedAnswer = ans.toString(),
            primarySkill = "funcao_logaritmica",
            difficulty = difficulty.toDouble(),
            templateId = templateId,
            canvasMode = "blank",
            validatorType = "exact",
            answerType = "numeric"
        )
    }

    private fun generateFuncaoModular(difficulty: Int, templateId: String = "func_mod_01"): ProceduralExercise {
        val a = Random.nextInt(1, 5)
        val c = Random.nextInt(6, 15)
        val ans = c + a
        return ProceduralExercise(
            id = UUID.randomUUID().toString(),
            statement = "Qual a maior raiz de |x - $a| = $c?",
            expectedAnswer = ans.toString(),
            primarySkill = "funcao_modular",
            difficulty = difficulty.toDouble(),
            templateId = templateId,
            canvasMode = "blank",
            validatorType = "exact",
            answerType = "numeric"
        )
    }
}
