package com.sprint.domain.procedural

import org.junit.Assert.*
import org.junit.Test

class MathBktEngineTest {

    @Test
    fun acertoAumentaMastery() {
        val result = MathBktEngine.updateMastery("soma_subtracao", 0.3, isCorrect = true)
        assertTrue("Mastery deve aumentar após acerto", result > 0.3)
    }

    @Test
    fun erroDiminuiMastery() {
        val result = MathBktEngine.updateMastery("soma_subtracao", 0.8, isCorrect = false)
        assertTrue("Mastery deve diminuir após erro", result < 0.8)
    }

    @Test
    fun decaySoApos7Dias() {
        val result = MathBktEngine.applyTemporalDecay("soma_subtracao", 0.8, daysSinceLastPractice = 5)
        assertEquals("Sem decaimento antes de 7 dias", 0.8, result, 0.001)
    }

    @Test
    fun decayApos7Dias() {
        val result = MathBktEngine.applyTemporalDecay("soma_subtracao", 0.8, daysSinceLastPractice = 14)
        assertTrue("Decaimento deve ocorrer após 7 dias", result < 0.8)
    }

    @Test
    fun decayNuncaAbaixoDeL0() {
        val result = MathBktEngine.applyTemporalDecay("soma_subtracao", 0.1, daysSinceLastPractice = 365)
        assertTrue("Decay nunca deve ir abaixo de l0 (0.4)", result >= 0.4)
    }

    @Test
    fun masteryConvergePara1ComAcertosConsecutivos() {
        var m = 0.0
        repeat(50) { m = MathBktEngine.updateMastery("soma_subtracao", m, isCorrect = true) }
        assertTrue("Mastery converge para 1 com acertos", m > 0.95)
    }

    @Test
    fun masteryConvergePara0ComErrosConsecutivos() {
        var m = 0.5
        repeat(50) { m = MathBktEngine.updateMastery("soma_subtracao", m, isCorrect = false) }
        // BKT converges to transition probability t=0.25 for this skill
        assertTrue("Mastery converge para floor do BKT (t=0.25) com erros", m < 0.3)
    }

    @Test
    fun parametrosDiferentesPorSkill() {
        val basic = MathBktEngine.getInitialMastery("soma_subtracao")
        val adv = MathBktEngine.getInitialMastery("derivadas_basicas")
        assertTrue("Basic deve ter l0 maior que advanced", basic > adv)
    }

    @Test
    fun updateErrorFocusIncrementaNoMesmoErro() {
        val (focus, count) = MathBktEngine.updateErrorFocus(
            isCorrect = false,
            errorType = "sinal",
            currentFocus = "sinal",
            currentCount = 2,
        )
        assertEquals("Foco deve manter", "sinal", focus)
        assertEquals("Contagem deve incrementar", 3, count)
    }

    @Test
    fun updateErrorFocusReiniciaEmErroDiferente() {
        val (focus, count) = MathBktEngine.updateErrorFocus(
            isCorrect = false,
            errorType = "fracao",
            currentFocus = "sinal",
            currentCount = 3,
        )
        assertEquals("Foco deve mudar para novo erro", "fracao", focus)
        assertEquals("Contagem deve reiniciar em 1", 1, count)
    }

    @Test
    fun updateErrorFocusDecaiNoAcerto() {
        val (focus, count) = MathBktEngine.updateErrorFocus(
            isCorrect = true,
            errorType = null,
            currentFocus = "sinal",
            currentCount = 3,
        )
        assertEquals("Foco mantém no acerto", "sinal", focus)
        assertEquals("Contagem decai em 1", 2, count)
    }

    @Test
    fun updateErrorFocusNuncaAbaixoDeZero() {
        val (_, count) = MathBktEngine.updateErrorFocus(
            isCorrect = true,
            errorType = null,
            currentFocus = null,
            currentCount = 0,
        )
        assertEquals("Contagem nunca abaixo de 0", 0, count)
    }
}
