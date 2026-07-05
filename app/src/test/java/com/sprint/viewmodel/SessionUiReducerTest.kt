package com.sprint.viewmodel

import org.junit.Assert.*
import org.junit.Test

/**
 * Tests for SessionUiState reducer — pure state transitions.
 * No Android dependencies required.
 */
class SessionUiReducerTest {

    private val empty = SessionUiState()

    @Test
    fun selectSkill_updatesTag() {
        val result = empty.selectSkill("algebra")
        assertEquals("algebra", result.selectedSkillTag)
    }

    @Test
    fun selectDensity_updatesLevel() {
        val result = empty.selectDensity("high")
        assertEquals("high", result.densityLevel)
    }

    @Test
    fun recordResult_correct_incrementsCount() {
        val result = empty.recordResult(isCorrect = true)
        assertEquals(1, result.sessionCorrect)
        assertEquals(1, result.sessionTotal)
        assertEquals(0, result.consecutiveFails)
    }

    @Test
    fun recordResult_wrong_incrementsFails() {
        val result = empty.recordResult(isCorrect = false)
        assertEquals(0, result.sessionCorrect)
        assertEquals(1, result.sessionTotal)
        assertEquals(1, result.consecutiveFails)
    }

    @Test
    fun recordResult_5fails_triggersScoreRisk() {
        var state = empty
        repeat(5) { state = state.recordResult(isCorrect = false) }
        assertTrue("Score risk should be visible after 5 fails", state.scoreRiskVisible)
    }

    @Test
    fun recordResult_5correct_triggersMastery() {
        var state = empty
        // First 4 correct + 5th = 5 total, all correct
        state = state.copy(sessionTotal = 4, recentResults = listOf(ResultMark.CORRECT, ResultMark.CORRECT, ResultMark.CORRECT, ResultMark.CORRECT))
        state = state.recordResult(isCorrect = true)
        assertTrue("Mastery should be detected after 5 consecutive correct", state.masteryDetected)
    }

    @Test
    fun stayInSprint_dismissesRisk() {
        var state = empty.copy(consecutiveFails = 5, scoreRiskVisible = true)
        state = state.stayInCurrentSprint()
        assertFalse("Score risk should be dismissed", state.scoreRiskVisible)
        assertEquals("Dismissed at should match fails", 5, state.scoreRiskDismissedAt)
    }

    @Test
    fun pause_resume_togglesState() {
        val paused = empty.pause()
        assertTrue(paused.isPaused)
        val resumed = paused.resume()
        assertFalse(resumed.isPaused)
    }

    @Test
    fun resetToActive_changesStatus() {
        val result = empty.resetToActive()
        assertEquals(SessionStatus.ACTIVE_SPRINT, result.status)
    }

    @Test
    fun recordResult_keepsOnlyLast7() {
        var state = empty
        repeat(10) { state = state.recordResult(isCorrect = it % 2 == 0) }
        assertEquals("Should keep only last 7 results", 7, state.recentResults.size)
    }

    @Test
    fun dismissMastery_clearsFlags() {
        val result = empty.copy(
            masteryDetected = true,
            suggestedNextSkill = "derivadas",
        ).dismissMasterySuggestion()
        assertFalse(result.masteryDetected)
        assertNull(result.suggestedNextSkill)
    }

    @Test
    fun advanceToNextSkill_updatesState() {
        val result = empty.copy(
            masteryDetected = true,
            suggestedNextSkill = "integral",
            consecutiveFails = 3,
        ).advanceToNextSkill("integral")
        assertEquals("integral", result.selectedSkillTag)
        assertFalse(result.masteryDetected)
        assertNull(result.suggestedNextSkill)
        assertEquals(0, result.consecutiveFails)
    }
}
