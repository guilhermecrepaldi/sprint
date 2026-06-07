package com.strava_matematica.domain.procedural

import java.util.UUID
import kotlin.random.Random

object ProceduralCalculus {

    fun generate(skillTag: String, mmr: Int, random: Random = Random): ProceduralExercise {
        val type = skillTag
        val diff = (mmr.toDouble() / 100.0).toInt().coerceAtLeast(1)
        val problem = when (skillTag) {
            "calc_pre_func", "calc_pre_elem", "calc_pre_seq" -> generatePreCalc(diff, random)
            "calc_dif_lim", "nocao_de_limite" -> generateLimite(diff, random)
            "calc_dif_der", "derivadas_basicas" -> generateDerivadaBasica(diff, random)
            "derivadas_regra_cadeia" -> generateDerivadaCadeia(diff, random)
            "calc_dif_int", "integrais_indefinidas" -> generateIntegral(diff, random)
            "calc_dif_mul" -> generateDifMul(diff, random)
            "calc_eq_ode", "calc_eq_pde", "calc_eq_trans" -> generateEquacoes(diff, random)
            "calc_real_lim", "calc_real_met", "calc_real_comp" -> generateRealAnalise(diff, random)
            else -> generateDerivadaBasica(diff, random) // Fallback
        }
        
        return ProceduralExercise(
            id = UUID.randomUUID().toString(),
            statement = problem.first,
            expectedAnswer = problem.second,
            primarySkill = skillTag,
            difficulty = (mmr.toDouble() / 100.0) + 1.0,
            templateId = skillTag + "_basic",
            canvasMode = "blank",
            validatorType = "exact",
            answerType = "numeric"
        )
    }

    private fun generatePreCalc(difficulty: Int, random: Random): Pair<String, String> {
        val qType = random.nextInt(1, 3)
        return if (qType == 1) {
            "Uma sequência aritmética começa com a_1 = 3 e tem razão r = 4. Qual o valor do 5º termo (a_5)?" to "19"
        } else {
            "Qual é o domínio da função f(x) = 1/(x - 2)?\n1) R - {2}\n2) R\n(Responda 1 ou 2)" to "1"
        }
    }

    private fun generateDifMul(difficulty: Int, random: Random): Pair<String, String> {
        val c = random.nextInt(2, 5)
        return "Se f(x, y) = x^2 + $c*y, qual é o valor numérico da derivada parcial de f em relação a y, ou seja, ∂f/∂y?" to "$c"
    }

    private fun generateEquacoes(difficulty: Int, random: Random): Pair<String, String> {
        val k = random.nextInt(2, 6)
        return "A solução geral da Equação Diferencial Ordinária (EDO) linear y' = $k*y é do tipo:\n1) y = C*e^(${k}x)\n2) y = C*x^$k\n(Responda 1 ou 2)" to "1"
    }

    private fun generateRealAnalise(difficulty: Int, random: Random): Pair<String, String> {
        return "Na Análise Real, pelo Teorema de Bolzano-Weierstrass, toda sequência limitada de números reais possui uma subsequência:\n1) Divergente\n2) Convergente\n(Responda 1 ou 2)" to "2"
    }

    /**
     * RPG: Limites. Sorteia um x0 inteiro e a resposta (L) inteira. 
     * Deriva a equação f(x) linear.
     */
    private fun generateLimite(difficulty: Int, random: Random): Pair<String, String> {
        val x0 = random.nextInt(1, 6)
        val a = random.nextInt(2, 6)
        val L = random.nextInt(10, 30)
        
        // a*x0 + b = L => b = L - a*x0
        val b = L - a * x0
        val sign = if (b >= 0) "+ $b" else "- ${kotlin.math.abs(b)}"
        
        return Pair("Calcule o limite:\n\n\\(\\lim_{x \\to $x0} ($a x $sign)\\)", "$L")
    }

    /**
     * RPG: Derivada (Regra do Tombo). 
     * Sorteia resposta (y') em x0. Deriva a equação original.
     * f(x) = ax^2 + bx. f'(x) = 2ax + b.
     */
    private fun generateDerivadaBasica(difficulty: Int, random: Random): Pair<String, String> {
        val x0 = random.nextInt(1, 4)
        val expectedYprime = random.nextInt(5, 20)
        
        // y' = 2ax0 + b => b = y' - 2ax0
        val a = random.nextInt(1, 4)
        val b = expectedYprime - 2 * a * x0
        val sign = if (b >= 0) "+ ${b}x" else "- ${kotlin.math.abs(b)}x"
        
        return Pair("Dada a função \\(f(x) = $a x^2 $sign\\), calcule o valor da derivada \\(f'($x0)\\).", "$expectedYprime")
    }

    /**
     * RPG: Integral Definida simples.
     * Sorteia função cuja integral dá inteiro (ex: f(x) = 3ax^2, int = ax^3).
     */
    private fun generateIntegral(difficulty: Int, random: Random): Pair<String, String> {
        val a = random.nextInt(1, 5)
        val lower = 0
        val upper = random.nextInt(1, 4)
        
        // f(x) = 3*a*x^2. Integral F(x) = a*x^3
        val coef = 3 * a
        val result = a * (upper * upper * upper)
        
        return Pair("Calcule o valor numérico da integral definida:\n\n\\(\\int_{$lower}^{$upper} $coef x^2 \\,dx\\)", "$result")
    }

    private fun generateContinuidade(random: Random): Pair<String, String> {
        val a = random.nextInt(1, 4)
        val x0 = random.nextInt(1, 3)
        val result = a * x0 * x0
        return Pair("Para qual valor de c a função \\(f(x) = \\begin{cases} $a x^2 & x \\neq $x0 \\\\ c & x = $x0 \\end{cases}\\) é contínua em \\(x = $x0\\)?", "$result")
    }

    private fun generateRegraCadeia(random: Random): Pair<String, String> {
        val a = random.nextInt(2, 5)
        val n = random.nextInt(2, 4)
        val coef = a * n
        val exp = n - 1
        return Pair("Calcule a derivada usando a regra da cadeia: \\(f(x) = \\sin($a x^n)\\)", if (exp == 1) "${coef}x \\cos($a x^n)" else "${coef}x^$exp \\cos($a x^n)")
    }

    private fun generateProdutoQuociente(random: Random): Pair<String, String> {
        val a = random.nextInt(2, 5)
        return Pair("Calcule a derivada do produto: \\(f(x) = x^2 \\sin($a x)\\)", "2x \\sin($a x) + $a x^2 \\cos($a x)")
    }

    private fun generateAplicacoesDerivadas(random: Random): Pair<String, String> {
        val a = random.nextInt(1, 4)
        val b = random.nextInt(1, 5)
        val x0 = random.nextInt(1, 3)
        val result = 2 * a * x0 + b
        return Pair("Se a posição de uma partícula é \\(s(t) = $a t^2 + $b t\\), qual a velocidade em \\(t = $x0\\)?", "$result")
    }

    private fun generateIntegralDefinida(random: Random): Pair<String, String> {
        val aEven = random.nextInt(1, 4) * 2
        val upper = random.nextInt(1, 3)
        val result = (aEven / 2) * upper * upper
        return Pair("Calcule a integral definida: \\(\\int_{0}^{$upper} $aEven x dx\\)\n\n[fig:area_under_curve,a=0,b=$upper]", "$result")
    }

    private fun generateAplicacoesIntegrais(random: Random): Pair<String, String> {
        val v = random.nextInt(2, 6)
        val t = random.nextInt(2, 5)
        val dist = v * t
        return Pair("Se a velocidade de um carro é constante em \\(v(t) = $v\\) m/s, qual a distância percorrida de \\(t = 0\\) a \\(t = $t\\) s?", "$dist")
    }

    private fun generateDerivadaCadeia(difficulty: Int, random: Random): Pair<String, String> {
        val a = random.nextInt(2, 5)
        val exp = random.nextInt(2, 4)
        val x0 = random.nextInt(1, 3)
        
        // f(x) = (ax + 1)^exp
        // f'(x) = exp * (ax + 1)^(exp - 1) * a
        val inner = a * x0 + 1
        val innerExp = Math.pow(inner.toDouble(), (exp - 1).toDouble()).toInt()
        val ans = exp * innerExp * a
        
        return Pair("Dada a função \\( f(x) = ( x + 1)^ \\), calcule o valor da derivada \\( f'() \\) usando a Regra da Cadeia.", "")
    }
}
