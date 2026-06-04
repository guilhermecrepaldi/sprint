package com.strava_matematica.domain.procedural

import java.util.UUID
import kotlin.random.Random
import kotlin.math.pow
import kotlin.math.sqrt

object ProceduralGeometry {

    fun generate(skillTag: String, mmr: Int): ProceduralExercise {
        val diff = (mmr.toDouble() / 100.0).toInt().coerceAtLeast(1)
        return when (skillTag) {
            "geometria_plana" -> generateGeometriaPlana(diff)
            "geometria_espacial" -> generateGeometriaEspacial(diff)
            "geometria_analitica" -> generateGeometriaAnalitica(diff)
            "progressoes_pa_pg" -> generateProgressoes(diff)
            "combinatoria" -> generateCombinatoria(diff)
            "probabilidade" -> generateProbabilidade(diff)
            "trig_razoes" -> generateTrigRazoes(diff)
            "trig_seno_cosseno_tangente" -> generateTrigSenoCosseno(diff)
            "trig_identidades" -> generateTrigIdentidades(diff)
            "trig_equacoes" -> generateTrigEquacoes(diff)
            else -> generateProbabilidade(diff)
        }
    }

    private fun generateGeometriaPlana(difficulty: Int): ProceduralExercise {
        val base = Random.nextInt(2, 10)
        val altura = Random.nextInt(2, 10)
        val area = (base * altura) / 2
        return ProceduralExercise(
            id = UUID.randomUUID().toString(),
            statement = "Calcule a área de um triângulo com base $base e altura $altura.",
            expectedAnswer = area.toString(),
            primarySkill = "geometria_plana",
            difficulty = difficulty.toDouble(),
            templateId = "geo_plana_01",
            canvasMode = "blank",
            validatorType = "exact",
            answerType = "numeric"
        )
    }

    private fun generateGeometriaEspacial(difficulty: Int): ProceduralExercise {
        val lado = Random.nextInt(2, 6)
        val volume = lado * lado * lado
        return ProceduralExercise(
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

    private fun generateGeometriaAnalitica(difficulty: Int): ProceduralExercise {
        val x1 = Random.nextInt(0, 5)
        val y1 = Random.nextInt(0, 5)
        val x2 = x1 + Random.nextInt(3, 5) // distância pitagórica básica (ex: 3,4,5)
        val y2 = y1 + 4
        return ProceduralExercise(
            id = UUID.randomUUID().toString(),
            statement = "Qual o quadrado da distância entre os pontos ($x1, $y1) e ($x2, $y2)?",
            expectedAnswer = ((x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1)).toString(),
            primarySkill = "geometria_analitica",
            difficulty = difficulty.toDouble(),
            templateId = "geo_analitica_01",
            canvasMode = "blank",
            validatorType = "exact",
            answerType = "numeric"
        )
    }

    private fun generateProgressoes(difficulty: Int): ProceduralExercise {
        val a1 = Random.nextInt(1, 5)
        val r = Random.nextInt(2, 5)
        val n = Random.nextInt(4, 10)
        val an = a1 + (n - 1) * r
        return ProceduralExercise(
            id = UUID.randomUUID().toString(),
            statement = "Em uma PA onde \\\$a_1 = $a1\\\$ e a razão é $r, qual é o valor de \\\$a_{$n}\\\$?",
            expectedAnswer = an.toString(),
            primarySkill = "progressoes_pa_pg",
            difficulty = difficulty.toDouble(),
            templateId = "pa_01",
            canvasMode = "blank",
            validatorType = "exact",
            answerType = "numeric"
        )
    }

    private fun generateCombinatoria(difficulty: Int): ProceduralExercise {
        val n = Random.nextInt(4, 7)
        var fat = 1
        for (i in 2..n) fat *= i
        return ProceduralExercise(
            id = UUID.randomUUID().toString(),
            statement = "Calcule o número de permutações simples de $n elementos ($n!).",
            expectedAnswer = fat.toString(),
            primarySkill = "combinatoria",
            difficulty = difficulty.toDouble(),
            templateId = "comb_01",
            canvasMode = "blank",
            validatorType = "exact",
            answerType = "numeric"
        )
    }

    private fun generateProbabilidade(difficulty: Int): ProceduralExercise {
        val faces = 6
        return ProceduralExercise(
            id = UUID.randomUUID().toString(),
            statement = "No lançamento de um dado não viciado de 6 faces, qual a probabilidade de cair um número par? Responda em fração irredutível simplificada a/b.",
            expectedAnswer = "1/2",
            primarySkill = "probabilidade",
            difficulty = difficulty.toDouble(),
            templateId = "prob_01",
            canvasMode = "blank",
            validatorType = "exact",
            answerType = "numeric"
        )
    }

    private fun generateTrigRazoes(difficulty: Int): ProceduralExercise {
        val catOposto = 3
        val catAdjacente = 4
        // hip = 5
        return ProceduralExercise(
            id = UUID.randomUUID().toString(),
            statement = "Em um triângulo retângulo de catetos 3 e 4, qual o valor do seno do ângulo oposto ao cateto 3? (Responda em decimal).\n\n[fig:right_triangle,angle=θ,opp=3,adj=4,hyp=5]",
            expectedAnswer = "0.6",
            primarySkill = "trig_razoes",
            difficulty = difficulty.toDouble(),
            templateId = "trig_razoes_01",
            canvasMode = "blank",
            validatorType = "exact",
            answerType = "numeric"
        )
    }

    private fun generateTrigSenoCosseno(difficulty: Int): ProceduralExercise {
        val angles = listOf(
            Triple("30^\\circ", "1/2", "30"), 
            Triple("60^\\circ", "\\sqrt{3}/2", "60"), 
            Triple("90^\\circ", "1", "90")
        )
        val chosen = angles.random()
        return ProceduralExercise(
            id = UUID.randomUUID().toString(),
            statement = "Qual o valor numérico de $\\sin(${chosen.first})$?\n\n[fig:trig_unit_circle,angle=${chosen.third},show_proj=true]",
            expectedAnswer = chosen.second,
            primarySkill = "trig_seno_cosseno_tangente",
            difficulty = difficulty.toDouble(),
            templateId = "trig_sc_01",
            canvasMode = "blank",
            validatorType = "exact",
            answerType = "text"
        )
    }

    private fun generateTrigIdentidades(difficulty: Int): ProceduralExercise {
        return ProceduralExercise(
            id = UUID.randomUUID().toString(),
            statement = "Sabendo que $\\sin^2(x) + \\cos^2(x) = C$, qual o valor de C?",
            expectedAnswer = "1",
            primarySkill = "trig_identidades",
            difficulty = difficulty.toDouble(),
            templateId = "trig_id_01",
            canvasMode = "blank",
            validatorType = "exact",
            answerType = "numeric"
        )
    }

    private fun generateTrigEquacoes(difficulty: Int): ProceduralExercise {
        return ProceduralExercise(
            id = UUID.randomUUID().toString(),
            statement = "No primeiro quadrante, qual o valor em graus de x se $\\cos(x) = 1/2$?\n\n[fig:trig_wave,func=cos,amp=1,freq=1]",
            expectedAnswer = "60",
            primarySkill = "trig_equacoes",
            difficulty = difficulty.toDouble(),
            templateId = "trig_eq_01",
            canvasMode = "blank",
            validatorType = "exact",
            answerType = "numeric"
        )
    }
}
