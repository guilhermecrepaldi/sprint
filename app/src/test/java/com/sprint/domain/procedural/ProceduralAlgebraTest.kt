package com.sprint.domain.procedural

import org.junit.Assert.*
import org.junit.Test

class ProceduralAlgebraTest {

    @Test
    fun generateLinearProduzExercicioValido() {
        val ex = ProceduralAlgebra.generate("equacoes_lineares", 1000)
        assertNotNull("Statement não deve ser nulo", ex.statement)
        assertTrue("Statement deve conter 'x'", ex.statement.contains("x"))
        assertNotNull("expectedAnswer deve ser inteiro", ex.expectedAnswer.toIntOrNull())
    }

    @Test
    fun generateQuadraticTemDuasRaizes() {
        val ex = ProceduralAlgebra.generate("equacoes_quadraticas", 1000)
        assertTrue("Resposta deve conter vírgula (duas raízes)", ex.expectedAnswer.contains(","))
    }

    @Test
    fun generateSystemTemDuasRespostas() {
        val ex = ProceduralAlgebra.generate("sistemas_equacoes", 1000)
        assertTrue("Resposta de sistema deve ter x e y",
            ex.expectedAnswer.contains("x") && ex.expectedAnswer.contains("y"))
    }

    @Test
    fun generateFuncaoAfimNaoVazio() {
        val ex = ProceduralAlgebra.generate("funcao_afim", 1000)
        assertTrue("Statement não deve estar vazio", ex.statement.isNotBlank())
        assertTrue("expectedAnswer não deve estar vazio", ex.expectedAnswer.isNotBlank())
    }

    @Test
    fun generateFuncaoModularTemHint() {
        val ex = ProceduralAlgebra.generate("funcao_modular", 1000)
        assertNotNull("funcao_modular deve ter answerFormatHint", ex.answerFormatHint)
    }

    @Test
    fun generateInequacoesTemHint() {
        val ex = ProceduralAlgebra.generate("inequacoes", 1000)
        assertNotNull("inequacoes deve ter answerFormatHint", ex.answerFormatHint)
    }

    @Test
    fun generatePotenciacaoRadiciacaoValido() {
        val ex = ProceduralAlgebra.generate("potenciacao_radiciacao", 1000)
        assertTrue("Statement deve conter sqrt ou ^",
            ex.statement.contains("sqrt") || ex.statement.contains("^"))
    }

    @Test
    fun generateDiferentesMmrProduzDiferentesDificuldades() {
        val facil = ProceduralAlgebra.generate("equacoes_lineares", 500)
        val dificil = ProceduralAlgebra.generate("equacoes_lineares", 2000)
        assertTrue("MMR maior deve gerar dificuldade maior", facil.difficulty <= dificil.difficulty)
    }

    @Test
    fun generateRespeitaSkillTag() {
        val ex = ProceduralAlgebra.generate("polinomios", 1000)
        assertEquals("primarySkill deve ser 'polinomios'", "polinomios", ex.primarySkill)
    }
}
