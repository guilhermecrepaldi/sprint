package com.sprint.domain.procedural

import org.junit.Assert.*
import org.junit.Test

class EloMatchmakerTest {

    @Test
    fun acertoAumentaMmr() {
        val novo = EloMatchmaker.calculateNewMmr(
            currentMmr = 1000,
            isCorrect = true,
            timeSpentMs = 15_000,
        )
        assertTrue("Acerto deve aumentar MMR", novo > 1000)
    }

    @Test
    fun erroDiminuiMmr() {
        val novo = EloMatchmaker.calculateNewMmr(
            currentMmr = 1000,
            isCorrect = false,
            timeSpentMs = 15_000,
        )
        assertTrue("Erro deve diminuir MMR", novo < 1000)
    }

    @Test
    fun mmrNuncaAbaixoDe800() {
        val novo = EloMatchmaker.calculateNewMmr(
            currentMmr = 800,
            isCorrect = false,
            timeSpentMs = 15_000,
        )
        assertEquals("MMR mínimo é 800", 800, novo)
    }

    @Test
    fun velocidadeBonusAumentaMmr() {
        val rapido = EloMatchmaker.calculateNewMmr(
            currentMmr = 1000,
            isCorrect = true,
            timeSpentMs = 5_000,
        )
        val devagar = EloMatchmaker.calculateNewMmr(
            currentMmr = 1000,
            isCorrect = true,
            timeSpentMs = 25_000,
        )
        assertTrue("Resposta rápida deve dar mais MMR", rapido > devagar)
    }

    @Test
    fun chuteRapidoNoErroPenalizaMais() {
        val chute = EloMatchmaker.calculateNewMmr(
            currentMmr = 1000,
            isCorrect = false,
            timeSpentMs = 3_000,
        )
        val normal = EloMatchmaker.calculateNewMmr(
            currentMmr = 1000,
            isCorrect = false,
            timeSpentMs = 15_000,
        )
        assertTrue("Erro rápido (chute) penaliza mais", chute < normal)
    }

    @Test
    fun inefficacyPenalizaExponencial() {
        val primeira = EloMatchmaker.calculateNewMmr(
            currentMmr = 1000,
            isCorrect = false,
            timeSpentMs = 15_000,
            isUnderInefficacy = true,
            consecutiveFailsInInefficacy = 1,
        )
        val quinta = EloMatchmaker.calculateNewMmr(
            currentMmr = 1000,
            isCorrect = false,
            timeSpentMs = 15_000,
            isUnderInefficacy = true,
            consecutiveFailsInInefficacy = 5,
        )
        assertTrue("Inefficacy exponencial: 5a falha dói mais", quinta < primeira)
    }

    @Test
    fun inefficacyLimitadoA250() {
        val resultado = EloMatchmaker.calculateNewMmr(
            currentMmr = 1100,
            isCorrect = false,
            timeSpentMs = 15_000,
            isUnderInefficacy = true,
            consecutiveFailsInInefficacy = 20,
        )
        assertTrue("Penalidade limitada a 250", resultado >= 850)
    }

    @Test
    fun masterScoreToMmrMapeiaCorretamente() {
        assertEquals(800, EloMatchmaker.masterScoreToMmr(0.0))
        assertEquals(2000, EloMatchmaker.masterScoreToMmr(1.0))
        assertEquals(1400, EloMatchmaker.masterScoreToMmr(0.5))
    }

    @Test
    fun mmrToMasterScoreInverso() {
        assertEquals(0.0, EloMatchmaker.mmrToMasterScore(800), 0.001)
        assertEquals(1.0, EloMatchmaker.mmrToMasterScore(2000), 0.001)
    }
}
