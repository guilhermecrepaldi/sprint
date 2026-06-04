package com.strava_matematica.domain.procedural

import java.util.UUID
import kotlin.random.Random
import kotlin.math.abs

object ProceduralAlgebra {

    fun generate(skillTag: String, mmr: Int, random: Random = Random): ProceduralExercise {
        return when (skillTag) {
            "equacoes_lineares" -> generateLinear(mmr, random)
            "sistemas_equacoes" -> generateSystem(mmr, random)
            "equacoes_quadraticas", "equacao_2_grau" -> generateQuadratic(mmr, random)
            "fatoracao_produtos_notaveis", "polinomios" -> generatePolynomial(mmr, random)
            "fracoes_decimais" -> generateFracoesDecimais(mmr, random)
            "porcentagem_razao" -> generatePorcentagemRazao(mmr, random)
            "potenciacao_radiciacao" -> generatePotenciacaoRadiciacao(mmr, random)
            "inequacoes" -> generateInequacoes(mmr, random)
            else -> generateLinear(mmr, random) // Fallback Lote 2 functions
        }
    }

    /**
     * RPG: Equações Lineares (ax + b = c)
     * 1. Sorteamos a resposta x (inteira).
     * 2. Sorteamos a e b.
     * 3. Calculamos c = a*x + b.
     */
    private fun generateLinear(mmr: Int, random: Random): ProceduralExercise {
        val diffLvl = mmr / 500 // 0 a 6
        
        // Bounds
        val xBound = if (diffLvl < 2) 1..10 else -15..15
        val aBound = if (diffLvl < 2) 2..5 else if (diffLvl < 4) -6..6 else -12..12

        var x = random.nextInt(xBound.first, xBound.last + 1)
        if (x == 0) x = 1

        var a = random.nextInt(aBound.first, aBound.last + 1)
        if (a == 0 || a == 1) a = 2 // Avoid trivial 1x

        var b = random.nextInt(aBound.first, aBound.last + 1)
        
        val c = a * x + b

        // Formatting equation nicely
        val bSign = if (b >= 0) "+ $b" else "- ${abs(b)}"
        val statement = "Resolva a equação do 1º grau para encontrar o valor de \\(x\\):\n\n\\( $a x $bSign = $c \\)"

        return ProceduralExercise(
            id = UUID.randomUUID().toString(),
            statement = statement,
            expectedAnswer = x.toString(),
            primarySkill = "equacoes_lineares",
            difficulty = mmr.toDouble(),
            templateId = "alg_lin_01",
            canvasMode = "blank",
            validatorType = "exact",
            answerType = "numeric"
        )
    }

    /**
     * RPG: Sistemas Lineares 2x2
     * a1*x + b1*y = c1
     * a2*x + b2*y = c2
     * 1. Sortear respostas x, y inteiras.
     * 2. Sortear coeficientes.
     * 3. Calcular c1 e c2.
     */
    private fun generateSystem(mmr: Int, random: Random): ProceduralExercise {
        val diffLvl = mmr / 500
        val bound = if (diffLvl < 2) 1..5 else -8..8

        var x = random.nextInt(bound.first, bound.last + 1)
        var y = random.nextInt(bound.first, bound.last + 1)
        // Ensure they are not too trivial (like x=0, y=0)
        if (x == 0 && y == 0) { x = 2; y = 3 }

        // Coefs
        val cBound = if (diffLvl < 3) 1..4 else -5..5
        var a1 = random.nextInt(cBound.first, cBound.last + 1)
        if (a1 == 0) a1 = 1
        var b1 = random.nextInt(cBound.first, cBound.last + 1)
        if (b1 == 0) b1 = 1

        var a2 = random.nextInt(cBound.first, cBound.last + 1)
        if (a2 == 0) a2 = 2
        var b2 = random.nextInt(cBound.first, cBound.last + 1)
        if (b2 == 0) b2 = -1

        // Check if system is singular (a1/a2 = b1/b2 -> a1*b2 = a2*b1)
        if (a1 * b2 == a2 * b1) {
            b2 += 1 // break singularity
        }

        val c1 = a1 * x + b1 * y
        val c2 = a2 * x + b2 * y

        fun formatTerm(coef: Int, vr: String, isFirst: Boolean = false): String {
            if (coef == 0) return ""
            val sign = if (coef < 0) "-" else if (!isFirst) "+" else ""
            val ac = abs(coef)
            val num = if (ac == 1) "" else "$ac"
            return "$sign $num$vr".trim()
        }

        val eq1 = "${formatTerm(a1, "x", true)} ${formatTerm(b1, "y")} = $c1".replace("  ", " ").trim()
        val eq2 = "${formatTerm(a2, "x", true)} ${formatTerm(b2, "y")} = $c2".replace("  ", " ").trim()

        val statement = "Resolva o sistema linear e encontre o valor de \\(x\\) e \\(y\\):\n\n\\( \\begin{cases} $eq1 \\\\ $eq2 \\end{cases} \\)"
        // The expected answer format for MLKit recognizing systems. Typically students write "x=2, y=3" or "(2,3)".
        // For exact matching, we just store "x=$x, y=$y".
        val expectedAnswer = "x=$x, y=$y"

        return ProceduralExercise(
            id = UUID.randomUUID().toString(),
            statement = statement,
            expectedAnswer = expectedAnswer,
            primarySkill = "sistemas_equacoes",
            difficulty = mmr.toDouble(),
            templateId = "alg_sys_01",
            canvasMode = "blank",
            validatorType = "system_2d",
            answerType = "text"
        )
    }

    /**
     * RPG: Equações Quadráticas (a*x^2 + b*x + c = 0)
     * 1. Sortear raízes reais r1 e r2.
     * 2. Sortear 'a' (geralmente a=1 em low MMR).
     * 3. a(x - r1)(x - r2) = a*x^2 - a(r1+r2)x + a(r1*r2) = 0
     * Logo: b = -a(r1+r2), c = a(r1*r2)
     */
    private fun generateQuadratic(mmr: Int, random: Random): ProceduralExercise {
        val diffLvl = mmr / 500
        val rootBound = if (diffLvl < 2) 1..6 else -10..10
        val a = if (diffLvl < 4) 1 else arrayOf(1, 2, -1, -2).random(random)

        val r1 = random.nextInt(rootBound.first, rootBound.last + 1)
        val r2 = random.nextInt(rootBound.first, rootBound.last + 1)
        
        val b = -a * (r1 + r2)
        val c = a * (r1 * r2)

        fun formatMonomial(coef: Int, vr: String, isFirst: Boolean): String {
            if (coef == 0) return ""
            val sign = if (coef < 0) "-" else if (!isFirst) "+" else ""
            val ac = abs(coef)
            val num = if (ac == 1 && vr.isNotEmpty()) "" else "$ac"
            return "$sign $num$vr ".trimStart()
        }

        val eq = "${formatMonomial(a, "x^2", true)}${formatMonomial(b, "x", false)}${formatMonomial(c, "", false)} = 0".trim()

        val expectedAnswer = if (r1 == r2) "$r1" else "${minOf(r1, r2)}, ${maxOf(r1, r2)}"
        val statement = "Encontre as raízes da equação do 2º grau:\n\n\\( $eq \\)"

        return ProceduralExercise(
            id = UUID.randomUUID().toString(),
            statement = statement,
            expectedAnswer = expectedAnswer,
            primarySkill = "equacoes_quadraticas",
            difficulty = mmr.toDouble(),
            templateId = "alg_quad_01",
            canvasMode = "blank",
            validatorType = "roots_unordered",
            answerType = "numeric"
        )
    }

    /**
     * RPG: Polinômios / Fatoração (Produtos Notáveis)
     * Quadrado da Soma: (x + a)^2 = x^2 + 2ax + a^2
     * Diferença de Quadrados: (x + a)(x - a) = x^2 - a^2
     */
    private fun generatePolynomial(mmr: Int, random: Random): ProceduralExercise {
        val type = random.nextInt(2) // 0 = quadrado soma/diferença, 1 = dif quadrados
        val a = random.nextInt(2, 10)
        
        return if (type == 0) {
            val sign = if (random.nextBoolean()) "+" else "-"
            val c = a * a
            val b = if (sign == "+") 2 * a else -2 * a
            
            val bStr = if (b > 0) "+ ${b}x" else "- ${abs(b)}x"
            val statement = "Fatore o trinômio quadrado perfeito:\n\n\\( x^2 $bStr + $c \\)"
            val expectedAnswer = "(x $sign $a)^2"

            ProceduralExercise(
                id = UUID.randomUUID().toString(),
                statement = statement,
                expectedAnswer = expectedAnswer,
                primarySkill = "fatoracao_produtos_notaveis",
                difficulty = mmr.toDouble(),
                templateId = "alg_poly_01",
                canvasMode = "blank",
                validatorType = "exact_text",
                answerType = "text"
            )
        } else {
            val c = a * a
            val statement = "Fatore a diferença de quadrados:\n\n\\( x^2 - $c \\)"
            val expectedAnswer = "(x - $a)(x + $a)" // or (x+a)(x-a)

            ProceduralExercise(
                id = UUID.randomUUID().toString(),
                statement = statement,
                expectedAnswer = expectedAnswer,
                primarySkill = "fatoracao_produtos_notaveis",
                difficulty = mmr.toDouble(),
                templateId = "alg_poly_02",
                canvasMode = "blank",
                validatorType = "product_unordered",
                answerType = "text"
            )
        }
    }

    private fun generateFracoesDecimais(mmr: Int, random: Random): ProceduralExercise {
        val diffLvl = mmr / 500
        val type = random.nextInt(2)
        if (type == 0) {
            // Fraction to decimal
            val den = intArrayOf(2, 4, 5, 8, 10, 20, 25).random(random)
            val num = random.nextInt(1, den * 3)
            val decimal = num.toDouble() / den.toDouble()
            val decimalStr = if (decimal % 1.0 == 0.0) decimal.toInt().toString() else decimal.toString()
            return ProceduralExercise(
                id = UUID.randomUUID().toString(),
                statement = "Converta a fração para número decimal:\n\n\\( \\frac{}{} \\)",
                expectedAnswer = decimalStr,
                primarySkill = "fracoes_decimais",
                difficulty = mmr.toDouble(),
                templateId = "alg_frac_01"
            )
        } else {
            // Decimal to fraction
            val num = random.nextInt(1, 20)
            val den = intArrayOf(2, 4, 5).random(random)
            val decimal = num.toDouble() / den.toDouble()
            val decimalStr = if (decimal % 1.0 == 0.0) decimal.toInt().toString() else decimal.toString()
            
            // gcd
            var a = num
            var b = den
            while (b > 0) { val temp = b; b = a % b; a = temp }
            val numSimp = num / a
            val denSimp = den / a
            
            return ProceduralExercise(
                id = UUID.randomUUID().toString(),
                statement = "Converta o número decimal para fração irredutível (ex: a/b):\n\n\\(  \\)",
                expectedAnswer = "/",
                primarySkill = "fracoes_decimais",
                difficulty = mmr.toDouble(),
                templateId = "alg_frac_02"
            )
        }
    }

    private fun generatePorcentagemRazao(mmr: Int, random: Random): ProceduralExercise {
        val type = random.nextInt(2)
        if (type == 0) {
            val pct = intArrayOf(5, 10, 15, 20, 25, 30, 40, 50, 75).random(random)
            val base = random.nextInt(2, 20) * 10
            val ans = (pct * base) / 100
            return ProceduralExercise(
                id = UUID.randomUUID().toString(),
                statement = "Calcule o valor de:\n\n\\( \\% \\text{ de }  \\)",
                expectedAnswer = ans.toString(),
                primarySkill = "porcentagem_razao",
                difficulty = mmr.toDouble(),
                templateId = "alg_pct_01"
            )
        } else {
            val ratioBase = random.nextInt(2, 6)
            val multiplier = random.nextInt(2, 10)
            val p1 = 2 * multiplier
            val p2 = ratioBase * multiplier
            return ProceduralExercise(
                id = UUID.randomUUID().toString(),
                statement = "Simplifique a razão para a forma irredutível a/b:\n\n\\( \\frac{}{} \\)",
                // GCD
                expectedAnswer = "2/".replace("2/2", "1/1").replace("2/4", "1/2").replace("2/6", "1/3"),
                primarySkill = "porcentagem_razao",
                difficulty = mmr.toDouble(),
                templateId = "alg_razao_01"
            )
        }
    }

    private fun generatePotenciacaoRadiciacao(mmr: Int, random: Random): ProceduralExercise {
        val type = random.nextInt(2)
        if (type == 0) {
            val base = random.nextInt(2, 6)
            val exp = random.nextInt(2, 5)
            val ans = Math.pow(base.toDouble(), exp.toDouble()).toInt()
            return ProceduralExercise(
                id = UUID.randomUUID().toString(),
                statement = "Calcule o valor da potência:\n\n\\( ^ \\)",
                expectedAnswer = ans.toString(),
                primarySkill = "potenciacao_radiciacao",
                difficulty = mmr.toDouble(),
                templateId = "alg_pot_01"
            )
        } else {
            val rootBase = random.nextInt(2, 13)
            val ans = rootBase
            val inside = rootBase * rootBase
            return ProceduralExercise(
                id = UUID.randomUUID().toString(),
                statement = "Calcule o valor da raiz quadrada:\n\n\\( \\sqrt{} \\)",
                expectedAnswer = ans.toString(),
                primarySkill = "potenciacao_radiciacao",
                difficulty = mmr.toDouble(),
                templateId = "alg_rad_01"
            )
        }
    }

    private fun generateInequacoes(mmr: Int, random: Random): ProceduralExercise {
        val a = random.nextInt(2, 6)
        val x = random.nextInt(-5, 6)
        val b = random.nextInt(-10, 10)
        val c = a * x + b
        val sign = if (random.nextBoolean()) ">" else "<"
        val bStr = if (b >= 0) "+ " else "- "
        return ProceduralExercise(
            id = UUID.randomUUID().toString(),
            statement = "Resolva a inequação para x:\n\n\\(  x    \\)",
            expectedAnswer = "x",
            primarySkill = "inequacoes",
            difficulty = mmr.toDouble(),
            templateId = "alg_ineq_01",
            validatorType = "exact"
        )
    }
}
