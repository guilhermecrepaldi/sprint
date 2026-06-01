package com.strava_matematica.domain.procedural

import java.util.UUID
import kotlin.random.Random

object ProceduralCalculus {

    fun generate(skillTag: String, mmr: Int, random: Random = Random): ProceduralExercise {
        val type = skillTag
        val problem = when (type) {
            "nocao_de_limite" -> generateLimite(random)
            "continuidade" -> generateContinuidade(random)
            "derivadas_basicas" -> generateDerivadaBasica(random)
            "derivadas_regra_cadeia" -> generateRegraCadeia(random)
            "derivadas_produto_quociente" -> generateProdutoQuociente(random)
            "aplicacoes_derivadas" -> generateAplicacoesDerivadas(random)
            "integrais_indefinidas" -> generateIntegralIndefinida(random)
            "integrais_definidas" -> generateIntegralDefinida(random)
            "aplicacoes_integrais" -> generateAplicacoesIntegrais(random)
            else -> Pair("Calcule: \\(1 + 1\\)", "2")
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

    private fun generateLimite(random: Random): Pair<String, String> {
        val a = random.nextInt(1, 5)
        val b = random.nextInt(1, 5)
        val x0 = random.nextInt(1, 4)
        val result = a * x0 + b
        return Pair("Calcule o limite: \\(\\lim_{x \\to $x0} ($a x + $b)\\)", "$result")
    }

    private fun generateContinuidade(random: Random): Pair<String, String> {
        val a = random.nextInt(1, 4)
        val x0 = random.nextInt(1, 3)
        val result = a * x0 * x0
        return Pair("Para qual valor de c a função \\(f(x) = \\begin{cases} $a x^2 & x \\neq $x0 \\\\ c & x = $x0 \\end{cases}\\) é contínua em \\(x = $x0\\)?", "$result")
    }

    private fun generateDerivadaBasica(random: Random): Pair<String, String> {
        val type = random.nextInt(3)
        return when (type) {
            0 -> {
                val a = random.nextInt(2, 6)
                val n = random.nextInt(2, 5)
                val coef = a * n
                val exp = n - 1
                Pair("Calcule a derivada: \\(f(x) = $a x^n\\)", if (exp == 1) "${coef}x" else "${coef}x^$exp")
            }
            1 -> {
                val a = random.nextInt(2, 5)
                Pair("Calcule a derivada: \\(f(x) = $a \\sin(x)\\)", "$a \\cos(x)")
            }
            else -> {
                val a = random.nextInt(2, 5)
                Pair("Calcule a derivada: \\(f(x) = $a \\cos(x)\\)", "-$a \\sin(x)")
            }
        }
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

    private fun generateIntegralIndefinida(random: Random): Pair<String, String> {
        val a = random.nextInt(1, 5)
        val n = random.nextInt(1, 4)
        val exp = n + 1
        return Pair("Calcule a integral indefinida: \\(\\int $a x^n dx\\)", "\\frac{$a}{$exp} x^$exp + C")
    }

    private fun generateIntegralDefinida(random: Random): Pair<String, String> {
        val aEven = random.nextInt(1, 4) * 2
        val upper = random.nextInt(1, 3)
        val result = (aEven / 2) * upper * upper
        return Pair("Calcule a integral definida: \\(\\int_{0}^{$upper} $aEven x dx\\)", "$result")
    }

    private fun generateAplicacoesIntegrais(random: Random): Pair<String, String> {
        val v = random.nextInt(2, 6)
        val t = random.nextInt(2, 5)
        val dist = v * t
        return Pair("Se a velocidade de um carro é constante em \\(v(t) = $v\\) m/s, qual a distância percorrida de \\(t = 0\\) a \\(t = $t\\) s?", "$dist")
    }
}
