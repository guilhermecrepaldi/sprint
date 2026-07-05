package com.sprint.data.local.repository

import org.junit.Assert.*
import org.junit.Test

class DeterministicValidatorTest {

    @Test
    fun stripSetNotationRemoveSChaves() {
        assertEquals("0, 10, 16", DeterministicValidator.stripSetNotation("S = { 0, 10, 16 }"))
    }

    @Test
    fun stripSetNotationMantemStringSimples() {
        assertEquals("x > 2", DeterministicValidator.stripSetNotation("x > 2"))
    }

    @Test
    fun stripSetNotationRemoveIn() {
        assertEquals("2, 4", DeterministicValidator.stripSetNotation("x \\in { 2, 4 }"))
    }

    @Test
    fun stripSetNotationRemoveBareBraces() {
        assertEquals("0, 10, 16", DeterministicValidator.stripSetNotation("{0, 10, 16}"))
    }

    @Test
    fun evaluateExatoCorreto() {
        assertTrue(DeterministicValidator.evaluate("5", "5", "exact"))
    }

    @Test
    fun evaluateExatoIncorreto() {
        assertFalse(DeterministicValidator.evaluate("4", "5", "exact"))
    }

    @Test
    fun evaluateListaOrdemIrrelevante() {
        assertTrue(DeterministicValidator.evaluate("3, 1", "1, 3", "exact"))
    }

    @Test
    fun evaluateListaComSetNotation() {
        assertTrue(DeterministicValidator.evaluate("S = { 0, 10 }", "0, 10", "exact"))
    }

    @Test
    fun evaluateNumericoComFracoes() {
        assertTrue(DeterministicValidator.evaluate("0.5", "1/2", "numeric"))
    }

    @Test
    fun evaluateNumericoComDecimais() {
        assertTrue(DeterministicValidator.evaluate("0.333333", "1/3", "numeric"))
    }

    @Test
    fun evaluateEquacao() {
        assertTrue(DeterministicValidator.evaluate("x = 5", "5", "equation"))
    }

    @Test
    fun evaluateRegex() {
        assertTrue(DeterministicValidator.evaluate("x > 2", "x > \\d+", "regex"))
    }

    @Test
    fun evaluateComOpcaoPortugues() {
        assertTrue(DeterministicValidator.evaluate("5", "5 ou 6", "exact"))
        assertTrue(DeterministicValidator.evaluate("6", "5 ou 6", "exact"))
    }

    @Test
    fun evaluateStringVaziaRetornaFalse() {
        assertFalse(DeterministicValidator.evaluate("", "5", "exact"))
    }

    @Test
    fun normalizeTrataUnicode() {
        assertEquals("3/4", DeterministicValidator.normalize("\u00BE"))
    }

    @Test
    fun formatFailedMessageRetornaHintParaSetNotation() {
        val msg = DeterministicValidator.formatFailedMessage("0 10 16", "S = { 0, 10, 16 }")
        assertNotNull(msg)
        assertTrue(msg?.contains("0, 10, 16") == true)
    }

    @Test
    fun formatFailedMessageRetornaNullParaNumerico() {
        val msg = DeterministicValidator.formatFailedMessage("4", "5")
        assertNull(msg)
    }

    @Test
    fun evaluateListaPartesDiferentesRetornaFalse() {
        assertFalse(DeterministicValidator.evaluate("1, 2, 3", "1, 2", "exact"))
    }
}
