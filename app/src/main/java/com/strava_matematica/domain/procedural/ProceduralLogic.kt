package com.strava_matematica.domain.procedural

import java.util.UUID

object ProceduralLogic {

    fun generate(skillTag: String, mmr: Int): ProceduralExercise {
        val diff = (mmr.toDouble() / 100.0).toInt().coerceAtLeast(1)
        return when (skillTag) {
            "sets_venn" -> generateSets(diff)
            "propositional" -> generatePropositional(diff)
            "fnd_set_op" -> generateSetOp(diff)
            "fnd_set_card" -> generateSetCard(diff)
            "fnd_set_zfc" -> generateSetZFC(diff)
            "fnd_log_prop" -> generateLogProp(diff)
            "fnd_log_pred" -> generateLogPred(diff)
            "fnd_log_fo" -> generateLogFO(diff)
            else -> generateSets(diff)
        }
    }

    private fun generateLogProp(difficulty: Int): ProceduralExercise {
        val ops = listOf("E (Conjunção)", "OU (Disjunção)", "IMPLICA (Condicional)")
        val p = ProceduralEngine.randomInstance.nextBoolean()
        val q = ProceduralEngine.randomInstance.nextBoolean()
        val op = ops.random()
        
        val answer = when (op) {
            "E (Conjunção)" -> if (p && q) "V" else "F"
            "OU (Disjunção)" -> if (p || q) "V" else "F"
            "IMPLICA (Condicional)" -> if (p && !q) "F" else "V" // V -> F is F, else V
            else -> "V"
        }
        
        val pStr = if (p) "Verdadeira (V)" else "Falsa (F)"
        val qStr = if (q) "Verdadeira (V)" else "Falsa (F)"
        
        return ProceduralExercise(
            id = UUID.randomUUID().toString(),
            statement = "Se a proposição P é $pStr e a proposição Q é $qStr,\nqual é o valor lógico da operação: P $op Q?\n\n(Responda V ou F)",
            expectedAnswer = answer,
            primarySkill = "fnd_log_prop",
            difficulty = difficulty.toDouble(),
            templateId = "log_prop_01",
            canvasMode = "blank",
            validatorType = "exact",
            answerType = "text"
        )
    }

    private fun generateLogPred(difficulty: Int): ProceduralExercise {
        val isUniversal = ProceduralEngine.randomInstance.nextBoolean()
        
        val (statement, answer) = if (isUniversal) {
            "Qual é a negação lógica da proposição universal:\n'Todo número par é múltiplo de 2' (∀x, P(x))?\n\n1) Nenhum número par é múltiplo de 2.\n2) Existe pelo menos um número par que não é múltiplo de 2 (∃x, ~P(x)).\n\n(Responda 1 ou 2)" to "2"
        } else {
            "Qual é a negação lógica da proposição existencial:\n'Existe um cisne que é negro' (∃x, C(x))?\n\n1) Todo cisne não é negro (∀x, ~C(x)).\n2) Existe um cisne que é branco.\n\n(Responda 1 ou 2)" to "1"
        }

        return ProceduralExercise(
            id = UUID.randomUUID().toString(),
            statement = statement,
            expectedAnswer = answer,
            primarySkill = "fnd_log_pred",
            difficulty = difficulty.toDouble(),
            templateId = "log_pred_01",
            canvasMode = "blank",
            validatorType = "exact",
            answerType = "numeric"
        )
    }

    private fun generateLogFO(difficulty: Int): ProceduralExercise {
        val isModusPonens = ProceduralEngine.randomInstance.nextBoolean()
        
        val (statement, answer) = if (isModusPonens) {
            "Na Lógica de Primeira Ordem, temos a premissa 'A → B' (Se chover, a rua molha) e o fato 'A' (Choveu).\nA conclusão lógica 'B' (A rua molhou) é obtida através da regra de inferência chamada:\n\n1) Modus Ponens\n2) Modus Tollens\n\n(Responda 1 ou 2)" to "1"
        } else {
            "Na Lógica de Primeira Ordem, temos a premissa 'A → B' (Se chover, a rua molha) e o fato '~B' (A rua não molhou).\nA conclusão lógica '~A' (Não choveu) é obtida através da regra de inferência chamada:\n\n1) Modus Ponens\n2) Modus Tollens\n\n(Responda 1 ou 2)" to "2"
        }

        return ProceduralExercise(
            id = UUID.randomUUID().toString(),
            statement = statement,
            expectedAnswer = answer,
            primarySkill = "fnd_log_fo",
            difficulty = difficulty.toDouble(),
            templateId = "log_fo_01",
            canvasMode = "blank",
            validatorType = "exact",
            answerType = "numeric"
        )
    }

    private fun generateSetOp(difficulty: Int): ProceduralExercise {
        val aStart = ProceduralEngine.randomInstance.nextInt(1, 5)
        val aEnd = aStart + ProceduralEngine.randomInstance.nextInt(3, 6)
        val bStart = aStart + ProceduralEngine.randomInstance.nextInt(1, 3)
        val bEnd = bStart + ProceduralEngine.randomInstance.nextInt(3, 6)
        
        val setA = (aStart..aEnd).toList()
        val setB = (bStart..bEnd).toList()
        
        val operations = listOf("união", "interseção")
        val isUnion = ProceduralEngine.randomInstance.nextBoolean()
        val opName = if (isUnion) "união" else "interseção"
        val opSymbol = if (isUnion) "\\cup" else "\\cap"
        
        val resultSet = if (isUnion) setA.union(setB).toSet() else setA.intersect(setB).toSet()
        val sum = resultSet.sum()
        
        return ProceduralExercise(
            id = UUID.randomUUID().toString(),
            statement = "Dados os conjuntos numéricos A = {${setA.joinToString(", ")}} e B = {${setB.joinToString(", ")}},\nqual é a soma dos elementos do conjunto $opName (A $opSymbol B)?",
            expectedAnswer = sum.toString(),
            primarySkill = "fnd_set_op",
            difficulty = difficulty.toDouble(),
            templateId = "set_op_01",
            canvasMode = "blank",
            validatorType = "exact",
            answerType = "numeric"
        )
    }

    private fun generateSetCard(difficulty: Int): ProceduralExercise {
        val start = ProceduralEngine.randomInstance.nextInt(1, 10)
        val end = start + ProceduralEngine.randomInstance.nextInt(15, 40)
        val parity = if (ProceduralEngine.randomInstance.nextBoolean()) "pares" else "ímpares"
        val remainder = if (parity == "pares") 0 else 1
        
        val count = (start..end).count { it % 2 == remainder }
        
        return ProceduralExercise(
            id = UUID.randomUUID().toString(),
            statement = "Seja A o conjunto dos números $parity compreendidos entre $start e $end (inclusos).\nQual é a cardinalidade de A, ou seja, n(A)?",
            expectedAnswer = count.toString(),
            primarySkill = "fnd_set_card",
            difficulty = difficulty.toDouble(),
            templateId = "set_card_01",
            canvasMode = "blank",
            validatorType = "exact",
            answerType = "numeric"
        )
    }

    private fun generateSetZFC(difficulty: Int): ProceduralExercise {
        // ZFC and Russell's paradox is mostly theoretical/conceptual.
        val questionType = ProceduralEngine.randomInstance.nextInt(1, 3)
        
        val (statement, answer) = if (questionType == 1) {
            "O Paradoxo de Russell abalou a Teoria Ingênua dos Conjuntos de Cantor ao perguntar se o conjunto de todos os conjuntos que não contêm a si mesmos contém a si mesmo.\nEsse paradoxo revela uma:\n1) Tautologia matemática\n2) Contradição lógica\n\n(Responda 1 ou 2)" to "2"
        } else {
            "Na axiomática de Zermelo-Fraenkel (ZFC), qual axioma proíbe a existência de um conjunto que contém todos os conjuntos (evitando o Paradoxo de Russell)?\n1) Axioma da Escolha\n2) Axioma da Regularidade (Fundação)\n\n(Responda 1 ou 2)" to "2"
        }

        return ProceduralExercise(
            id = UUID.randomUUID().toString(),
            statement = statement,
            expectedAnswer = answer,
            primarySkill = "fnd_set_zfc",
            difficulty = difficulty.toDouble(),
            templateId = "set_zfc_01",
            canvasMode = "blank",
            validatorType = "exact",
            answerType = "numeric"
        )
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
