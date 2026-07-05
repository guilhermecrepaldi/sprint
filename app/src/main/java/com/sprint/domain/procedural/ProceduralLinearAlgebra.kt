package com.sprint.domain.procedural

import java.util.UUID
import kotlin.random.Random

object ProceduralLinearAlgebra {

    fun generate(skillTag: String, mmr: Int, random: Random = Random): ProceduralExercise {
        val diff = (mmr.toDouble() / 100.0).toInt().coerceAtLeast(1)
        
        return when (skillTag) {
            "soma_produto_matrizes" -> generateSomaProdutoMatrizes(diff, random)
            "determinantes" -> generateDeterminantes(diff, random)
            "operacoes_vetoriais" -> generateOperacoesVetoriais(diff, random)
            "alg_lin_vec", "alg_lin_spc", "alg_lin_trans", "alg_lin_eig" -> generateLinAlgAdvanced(diff, random)
            "alg_abs_grp", "alg_abs_ring", "alg_abs_gal" -> generateAbstractAlgebra(diff, random)
            else -> generateOperacoesVetoriais(diff, random)
        }
    }

    private fun generateLinAlgAdvanced(difficulty: Int, random: Random): ProceduralExercise {
        val qType = ProceduralEngine.randomInstance.nextInt(1, 3)
        val statement = if (qType == 1) {
            "Na Álgebra Linear, um conjunto de vetores é linearmente independente (LI) se e somente se o determinante da matriz formada por esses vetores como colunas for:\n1) Igual a zero\n2) Diferente de zero\n(Responda 1 ou 2)"
        } else {
            "Se 'v' é um autovetor de uma matriz A associado ao autovalor λ, então a equação característica é dada por:\n1) Av = λv\n2) Aλ = v\n(Responda 1 ou 2)"
        }
        val answer = if (qType == 1) "2" else "1"
        return ProceduralExercise(UUID.randomUUID().toString(), statement, answer, "alg_lin", difficulty.toDouble(), "lin_alg_adv", "blank", "exact", "numeric")
    }

    private fun generateAbstractAlgebra(difficulty: Int, random: Random): ProceduralExercise {
        val qType = ProceduralEngine.randomInstance.nextInt(1, 3)
        val statement = if (qType == 1) {
            "Na Álgebra Abstrata, a estrutura algébrica formada por um conjunto equipado com uma operação associativa e que possui elemento neutro e inverso para cada elemento é chamada de:\n1) Grupo\n2) Anel\n(Responda 1 ou 2)"
        } else {
            "Qual o Teorema Fundamental da Álgebra (consequência da teoria de corpos)?\n1) Todo polinômio não constante com coeficientes complexos tem pelo menos uma raiz complexa.\n2) Todo número inteiro pode ser fatorado de forma única em primos.\n(Responda 1 ou 2)"
        }
        val answer = "1"
        return ProceduralExercise(UUID.randomUUID().toString(), statement, answer, "alg_abs", difficulty.toDouble(), "abs_alg_basic", "blank", "exact", "numeric")
    }

    private fun generateSomaProdutoMatrizes(difficulty: Int, random: Random): ProceduralExercise {
        val isSum = random.nextBoolean()
        return if (isSum) {
            val a11 = random.nextInt(-5, 6)
            val a12 = random.nextInt(-5, 6)
            val a21 = random.nextInt(-5, 6)
            val a22 = random.nextInt(-5, 6)
            
            val b11 = random.nextInt(-5, 6)
            val b12 = random.nextInt(-5, 6)
            val b21 = random.nextInt(-5, 6)
            val b22 = random.nextInt(-5, 6)
            
            val ans11 = a11 + b11
            val ans12 = a12 + b12
            val ans21 = a21 + b21
            val ans22 = a22 + b22
            
            val statement = "Seja A = \\(\\begin{bmatrix} $a11 & $a12 \\\\ $a21 & $a22 \\end{bmatrix}\\) e B = \\(\\begin{bmatrix} $b11 & $b12 \\\\ $b21 & $b22 \\end{bmatrix}\\). Calcule a soma dos elementos da matriz A+B."
            val expectedAnswer = (ans11 + ans12 + ans21 + ans22).toString()
            
            ProceduralExercise(
                id = UUID.randomUUID().toString(),
                statement = statement,
                expectedAnswer = expectedAnswer,
                primarySkill = "soma_produto_matrizes",
                difficulty = difficulty.toDouble(),
                templateId = "matrix_sum_01",
                canvasMode = "blank",
                validatorType = "exact",
                answerType = "numeric"
            )
        } else {
            val a11 = random.nextInt(-3, 4)
            val a12 = random.nextInt(-3, 4)
            val a21 = random.nextInt(-3, 4)
            val a22 = random.nextInt(-3, 4)
            
            val b11 = random.nextInt(-3, 4)
            val b12 = random.nextInt(-3, 4)
            val b21 = random.nextInt(-3, 4)
            val b22 = random.nextInt(-3, 4)
            
            // C = A * B
            val c11 = a11 * b11 + a12 * b21
            val c12 = a11 * b12 + a12 * b22
            val c21 = a21 * b11 + a22 * b21
            val c22 = a21 * b12 + a22 * b22
            
            val statement = "Dadas as matrizes A = \\(\\begin{bmatrix} $a11 & $a12 \\\\ $a21 & $a22 \\end{bmatrix}\\) e B = \\(\\begin{bmatrix} $b11 & $b12 \\\\ $b21 & $b22 \\end{bmatrix}\\). Calcule o elemento \\(c_{11}\\) da matriz \\(C = A \\cdot B\\)."
            val expectedAnswer = c11.toString()
            
            ProceduralExercise(
                id = UUID.randomUUID().toString(),
                statement = statement,
                expectedAnswer = expectedAnswer,
                primarySkill = "soma_produto_matrizes",
                difficulty = difficulty.toDouble() + 1.0,
                templateId = "matrix_prod_01",
                canvasMode = "blank",
                validatorType = "exact",
                answerType = "numeric"
            )
        }
    }

    private fun generateDeterminantes(difficulty: Int, random: Random): ProceduralExercise {
        val is3x3 = difficulty >= 5 && random.nextBoolean()
        
        return if (!is3x3) {
            val a = random.nextInt(-5, 6)
            val b = random.nextInt(-5, 6)
            val c = random.nextInt(-5, 6)
            val d = random.nextInt(-5, 6)
            val det = a * d - b * c
            
            val statement = "Calcule o determinante da matriz \\(\\begin{bmatrix} $a & $b \\\\ $c & $d \\end{bmatrix}\\)."
            ProceduralExercise(
                id = UUID.randomUUID().toString(),
                statement = statement,
                expectedAnswer = det.toString(),
                primarySkill = "determinantes",
                difficulty = difficulty.toDouble(),
                templateId = "det_2x2",
                canvasMode = "blank",
                validatorType = "exact",
                answerType = "numeric"
            )
        } else {
            // Sarrus
            val m = Array(3) { IntArray(3) { random.nextInt(-3, 4) } }
            val det = m[0][0]*m[1][1]*m[2][2] + m[0][1]*m[1][2]*m[2][0] + m[0][2]*m[1][0]*m[2][1] -
                      m[0][2]*m[1][1]*m[2][0] - m[0][0]*m[1][2]*m[2][1] - m[0][1]*m[1][0]*m[2][2]
                      
            val statement = "Calcule o determinante da matriz 3x3:\n\n\\(\\begin{bmatrix} ${m[0][0]} & ${m[0][1]} & ${m[0][2]} \\\\ ${m[1][0]} & ${m[1][1]} & ${m[1][2]} \\\\ ${m[2][0]} & ${m[2][1]} & ${m[2][2]} \\end{bmatrix}\\)"
            ProceduralExercise(
                id = UUID.randomUUID().toString(),
                statement = statement,
                expectedAnswer = det.toString(),
                primarySkill = "determinantes",
                difficulty = difficulty.toDouble() + 2.0,
                templateId = "det_3x3",
                canvasMode = "blank",
                validatorType = "exact",
                answerType = "numeric"
            )
        }
    }

    private fun generateOperacoesVetoriais(difficulty: Int, random: Random): ProceduralExercise {
        val vx = random.nextInt(1, 6)
        val vy = random.nextInt(1, 6)
        val ux = random.nextInt(1, 6)
        val uy = random.nextInt(-5, 0)
        
        val dotProduct = vx * ux + vy * uy
        
        val statement = "Dados os vetores \\(\\vec{v} = ($vx, $vy)\\) e \\(\\vec{u} = ($ux, $uy)\\), calcule o produto escalar \\(\\vec{v} \\cdot \\vec{u}\\).\n\n[fig:vector_2d,vx=$vx,vy=$vy,ux=$ux,uy=$uy]"
        
        return ProceduralExercise(
            id = UUID.randomUUID().toString(),
            statement = statement,
            expectedAnswer = dotProduct.toString(),
            primarySkill = "operacoes_vetoriais",
            difficulty = difficulty.toDouble(),
            templateId = "vector_dot",
            canvasMode = "blank",
            validatorType = "exact",
            answerType = "numeric"
        )
    }
}
