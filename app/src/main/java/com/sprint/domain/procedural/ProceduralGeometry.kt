package com.sprint.domain.procedural

import java.util.UUID
import kotlin.random.Random
import kotlin.math.pow
import kotlin.math.sqrt

object ProceduralGeometry {

    fun generate(skillTag: String, mmr: Int): ProceduralExercise {
        val diff = (mmr.toDouble() / 100.0).toInt().coerceAtLeast(1)
        return when (skillTag) {
            "geo_euc_plan", "geometria_plana" -> generateGeometriaPlana(diff)
            "geo_euc_spc", "geometria_espacial" -> generateGeometriaEspacial(diff)
            "geo_ana_cart", "geometria_analitica" -> generateGeometriaAnalitica(diff)
            "geo_ana_eq" -> generateReta(diff)
            "trig_basic", "trigonometria" -> generateTrigonometry(diff)
            "geo_diff_crv", "geo_diff_man", "geo_diff_riem" -> generateGeoDiff(diff)
            "geo_top_spc", "geo_top_cont", "geo_top_comp", "geo_top_alg" -> generateGeoTop(diff)
            else -> generateGeometriaPlana(diff) // Fallback
        }
    }

    private fun generateGeoDiff(difficulty: Int): ProceduralExercise {
        val qType = ProceduralEngine.randomInstance.nextInt(1, 3)
        val statement = if (qType == 1) {
            "Na Geometria Diferencial, a Curvatura Gaussiana (K) de uma esfera de raio R é dada por:\n1) K = 1 / R^2\n2) K = 0\n(Responda 1 ou 2)"
        } else {
            "Um espaço euclidiano plano (como uma folha de papel sem dobras intrínsecas) possui Curvatura Gaussiana igual a:\n1) 1\n2) 0\n(Responda 1 ou 2)"
        }
        val answer = if (qType == 1) "1" else "2"
        return ProceduralExercise(UUID.randomUUID().toString(), statement, answer, "geo_diff", difficulty.toDouble(), "geo_diff_basic", "blank", "exact", "numeric")
    }

    private fun generateGeoTop(difficulty: Int): ProceduralExercise {
        val qType = ProceduralEngine.randomInstance.nextInt(1, 3)
        val statement = if (qType == 1) {
            "Em Topologia, duas figuras são consideradas 'homeomorfas' (topologicamente equivalentes) se podemos deformar uma na outra sem cortes ou colagens. Uma caneca (com 1 alça) é homeomorfa a:\n1) Uma esfera (bola)\n2) Um toro (rosquinha)\n(Responda 1 ou 2)"
        } else {
            "Qual é a característica de Euler (X = V - A + F) de um poliedro convexo simples (como um cubo)?\n1) 2\n2) 0\n(Responda 1 ou 2)"
        }
        val answer = if (qType == 1) "2" else "1"
        return ProceduralExercise(UUID.randomUUID().toString(), statement, answer, "geo_top", difficulty.toDouble(), "geo_top_basic", "blank", "exact", "numeric")
    }

    private fun generateGeometriaPlana(difficulty: Int): ProceduralExercise {
        val rng = ProceduralEngine.randomInstance
        val shape = rng.nextInt(1, 5)
        return when (shape) {
            1 -> { // Triângulo retângulo
                val base = rng.nextInt(2, 10)
                val altura = rng.nextInt(2, 10)
                val area = (base * altura) / 2
                ProceduralExercise(
                    id = UUID.randomUUID().toString(),
                    statement = "Calcule a área do triângulo retângulo de catetos $base e $altura.",
                    expectedAnswer = area.toString(),
                    primarySkill = "geometria_plana",
                    difficulty = difficulty.toDouble(),
                    templateId = "geo_plana_01",
                    canvasMode = "blank",
                    validatorType = "exact",
                    answerType = "numeric"
                )
            }
            2 -> { // Círculo (área)
                val r = rng.nextInt(1, 8)
                val area = r * r
                ProceduralExercise(
                    id = UUID.randomUUID().toString(),
                    statement = "Calcule a área de um círculo de raio $r. (Use π ≈ 3.14 e responda o valor inteiro arredondado, mas aqui π² não, então use a = πr² e responda com π, ex: 25π)",
                    expectedAnswer = "${area}π",
                    primarySkill = "geometria_plana",
                    difficulty = difficulty.toDouble(),
                    templateId = "geo_plana_02",
                    canvasMode = "blank",
                    validatorType = "exact",
                    answerType = "expression"
                )
            }
            3 -> { // Trapézio
                val baseMaior = rng.nextInt(4, 12)
                val baseMenor = rng.nextInt(2, baseMaior)
                val altura = rng.nextInt(2, 8)
                val area = (baseMaior + baseMenor) * altura / 2
                ProceduralExercise(
                    id = UUID.randomUUID().toString(),
                    statement = "Calcule a área de um trapézio de bases $baseMaior e $baseMenor e altura $altura.",
                    expectedAnswer = area.toString(),
                    primarySkill = "geometria_plana",
                    difficulty = difficulty.toDouble(),
                    templateId = "geo_plana_03",
                    canvasMode = "blank",
                    validatorType = "exact",
                    answerType = "numeric"
                )
            }
            else -> { // Retângulo (perímetro)
                val l = rng.nextInt(3, 10)
                val w = rng.nextInt(2, l)
                val perim = 2 * (l + w)
                ProceduralExercise(
                    id = UUID.randomUUID().toString(),
                    statement = "Calcule o perímetro de um retângulo de lados $l e $w.",
                    expectedAnswer = perim.toString(),
                    primarySkill = "geometria_plana",
                    difficulty = difficulty.toDouble(),
                    templateId = "geo_plana_04",
                    canvasMode = "blank",
                    validatorType = "exact",
                    answerType = "numeric"
                )
            }
        }
    }

    private fun generateGeometriaEspacial(difficulty: Int): ProceduralExercise {
        val rng = ProceduralEngine.randomInstance
        val shape = rng.nextInt(1, 5)
        return when (shape) {
            1 -> { // Cubo
                val lado = rng.nextInt(2, 6)
                val volume = lado * lado * lado
                ProceduralExercise(
                    id = UUID.randomUUID().toString(),
                    statement = "Qual o volume de um cubo com aresta de tamanho $lado?",
                    expectedAnswer = volume.toString(),
                    primarySkill = "geometria_espacial",
                    difficulty = difficulty.toDouble(),
                    templateId = "geo_espacial_01",
                    canvasMode = "blank",
                    validatorType = "exact",
                    answerType = "numeric"
                )
            }
            2 -> { // Esfera (volume em termos de π)
                val r = rng.nextInt(1, 5)
                val r3 = r * r * r
                ProceduralExercise(
                    id = UUID.randomUUID().toString(),
                    statement = "Qual o volume de uma esfera de raio $r? (Responda em termos de π, ex: 36π)",
                    expectedAnswer = "${(4 * r3)}π/3",
                    primarySkill = "geometria_espacial",
                    difficulty = difficulty.toDouble(),
                    templateId = "geo_espacial_02",
                    canvasMode = "blank",
                    validatorType = "exact",
                    answerType = "expression"
                )
            }
            3 -> { // Paralelepípedo
                val a = rng.nextInt(2, 6)
                val b = rng.nextInt(2, 6)
                val c = rng.nextInt(2, 5)
                val volume = a * b * c
                ProceduralExercise(
                    id = UUID.randomUUID().toString(),
                    statement = "Qual o volume de um paralelepípedo de dimensões $a × $b × $c?",
                    expectedAnswer = volume.toString(),
                    primarySkill = "geometria_espacial",
                    difficulty = difficulty.toDouble(),
                    templateId = "geo_espacial_03",
                    canvasMode = "blank",
                    validatorType = "exact",
                    answerType = "numeric"
                )
            }
            else -> { // Cilindro (volume em termos de π)
                val r = rng.nextInt(1, 5)
                val h = rng.nextInt(2, 8)
                val r2 = r * r
                ProceduralExercise(
                    id = UUID.randomUUID().toString(),
                    statement = "Qual o volume de um cilindro de raio $r e altura $h? (Responda em termos de π, ex: 24π)",
                    expectedAnswer = "${r2 * h}π",
                    primarySkill = "geometria_espacial",
                    difficulty = difficulty.toDouble(),
                    templateId = "geo_espacial_04",
                    canvasMode = "blank",
                    validatorType = "exact",
                    answerType = "expression"
                )
            }
        }
    }



    /**
     * RPG: Geometria Analítica (Distância entre dois pontos)
     * Utiliza trios pitagóricos para garantir que a resposta (distância) seja sempre um número inteiro.
     */
    private fun generateGeometriaAnalitica(difficulty: Int): ProceduralExercise {
        val trios = listOf(
            Triple(3, 4, 5),
            Triple(6, 8, 10),
            Triple(5, 12, 13),
            Triple(8, 15, 17)
        )
        val trio = trios.random(Random)
        
        // Ponto A (x1, y1)
        val x1 = ProceduralEngine.randomInstance.nextInt(-5, 6)
        val y1 = ProceduralEngine.randomInstance.nextInt(-5, 6)
        
        // Ponto B (x2, y2) -> x2 = x1 + dx, y2 = y1 + dy
        val x2 = x1 + (if (ProceduralEngine.randomInstance.nextBoolean()) trio.first else -trio.first)
        val y2 = y1 + (if (ProceduralEngine.randomInstance.nextBoolean()) trio.second else -trio.second)
        
        val distance = trio.third
        
        return ProceduralExercise(
            id = UUID.randomUUID().toString(),
            statement = "Calcule a distância entre os pontos A($x1, $y1) e B($x2, $y2) no plano cartesiano.",
            expectedAnswer = distance.toString(),
            primarySkill = "geo_ana_cart",
            difficulty = difficulty.toDouble(),
            templateId = "geo_analitica_01",
            canvasMode = "blank",
            validatorType = "exact",
            answerType = "numeric"
        )
    }

    private fun generateReta(difficulty: Int): ProceduralExercise {
        // Encontre o coeficiente angular (m) da reta que passa por A e B
        // RPG: m = (y2 - y1) / (x2 - x1). Sorteamos m e P1, derivamos P2.
        val m = ProceduralEngine.randomInstance.nextInt(-4, 5)
        val dx = ProceduralEngine.randomInstance.nextInt(1, 4) // delta x inteiro
        val dy = m * dx // garante que dy/dx = m
        
        val x1 = ProceduralEngine.randomInstance.nextInt(-5, 6)
        val y1 = ProceduralEngine.randomInstance.nextInt(-5, 6)
        
        val x2 = x1 + dx
        val y2 = y1 + dy
        
        return ProceduralExercise(
            id = UUID.randomUUID().toString(),
            statement = "Calcule o coeficiente angular \\(m\\) da reta que passa pelos pontos A($x1, $y1) e B($x2, $y2).",
            expectedAnswer = m.toString(),
            primarySkill = "geo_ana_eq",
            difficulty = difficulty.toDouble(),
            templateId = "geo_analitica_02",
            canvasMode = "blank",
            validatorType = "exact",
            answerType = "numeric"
        )
    }

    private fun generateTrigonometry(difficulty: Int): ProceduralExercise {
        val rng = ProceduralEngine.randomInstance
        val angle = rng.nextInt(1, 4) * 30
        val sinVal = when (angle) {
            30 -> "1/2"
            60 -> "√3/2"
            90 -> "1"
            else -> "√2/2"
        }
        val cosVal = when (angle) {
            30 -> "√3/2"
            60 -> "1/2"
            90 -> "0"
            else -> "√2/2"
        }
        val askSin = rng.nextBoolean()
        val answer = if (askSin) sinVal else cosVal
        val func = if (askSin) "sen" else "cos"
        return ProceduralExercise(
            id = UUID.randomUUID().toString(),
            statement = "Qual o valor de $func(${angle}°)? (Responda como fração ou número exato)",
            expectedAnswer = answer,
            primarySkill = "trig_razoes",
            difficulty = difficulty.toDouble(),
            templateId = "trig_01",
            canvasMode = "blank",
            validatorType = "exact",
            answerType = "text"
        )
    }
}
