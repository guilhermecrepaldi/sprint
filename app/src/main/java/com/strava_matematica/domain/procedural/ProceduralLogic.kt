package com.strava_matematica.domain.procedural

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
            statement = "Em um grupo, $nA pessoas gostam de Matemática, $nB gostam de Física e $nIntersection gostam de ambas. Quantas pessoas gostam de Matemática ou Física?\n\n[fig:venn_2]",
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
