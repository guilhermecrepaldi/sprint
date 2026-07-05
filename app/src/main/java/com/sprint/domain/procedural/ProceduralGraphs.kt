package com.sprint.domain.procedural

import java.util.UUID

object ProceduralGraphs {

    fun generate(skillTag: String, mmr: Int): ProceduralExercise {
        val diff = (mmr.toDouble() / 100.0).toInt().coerceAtLeast(1)
        
        val (statement, expectedAnswer) = when (skillTag) {
            "fnd_graph_vert" -> {
                val v = ProceduralEngine.randomInstance.nextInt(4, 10)
                val e = ProceduralEngine.randomInstance.nextInt(v - 1, (v * (v - 1)) / 2)
                "Um grafo simples G possui $v vértices e $e arestas. Qual é a soma dos graus de todos os vértices desse grafo?" to (2 * e).toString()
            }
            "fnd_graph_path" -> {
                val n = ProceduralEngine.randomInstance.nextInt(3, 7)
                "Em um grafo completo K_$n, quantos caminhos simples de comprimento 1 existem (ou seja, qual é o número de arestas)?" to ((n * (n - 1)) / 2).toString()
            }
            "fnd_graph_col" -> {
                val n = ProceduralEngine.randomInstance.nextInt(4, 8)
                val type = ProceduralEngine.randomInstance.nextInt(1, 3)
                if (type == 1) {
                    "Qual é o número cromático de um grafo completo K_$n (o número mínimo de cores para colorir os vértices de forma que vértices adjacentes tenham cores diferentes)?" to n.toString()
                } else {
                    "Qual é o número cromático de um grafo ciclo C_${n * 2} (ciclo par)?" to "2"
                }
            }
            else -> "Questão de Teoria dos Grafos genérica." to "1"
        }

        return ProceduralExercise(
            id = UUID.randomUUID().toString(),
            statement = statement,
            expectedAnswer = expectedAnswer,
            primarySkill = skillTag,
            difficulty = diff.toDouble(),
            templateId = "graph_basic",
            canvasMode = "blank",
            validatorType = "exact",
            answerType = "numeric"
        )
    }
}
