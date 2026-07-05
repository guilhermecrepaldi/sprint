package com.sprint.viewmodel

import com.sprint.model.SessionConfig
import com.sprint.model.SessionStatus

/**
 * Pure state reducer for SessionUiState.
 *
 * Extraído do SessionViewModel para permitir testes SEM dependências Android.
 * Todas as funções são puras: (state) -> state.
 *
 * Uso no ViewModel:
 *   _uiState.update { it.selectSkill("algebra") }
 *
 * Uso no teste:
 *   val newState = SessionUiState().selectSkill("algebra")
 *   assertEquals("algebra", newState.selectedSkillTag)
 */
fun SessionUiState.selectSkill(tag: String): SessionUiState =
    copy(selectedSkillTag = tag)

fun SessionUiState.selectDensity(level: String): SessionUiState =
    copy(densityLevel = level)

fun SessionUiState.updateConfig(config: SessionConfig): SessionUiState =
    copy(config = config)

fun SessionUiState.dismissMasterySuggestion(): SessionUiState =
    copy(masteryDetected = false, suggestedNextSkill = null)

fun SessionUiState.stayInCurrentSprint(): SessionUiState =
    copy(scoreRiskVisible = false, scoreRiskDismissedAt = consecutiveFails)

fun SessionUiState.adjustSprint(): SessionUiState =
    copy(scoreRiskVisible = false)

fun SessionUiState.resetToActive(): SessionUiState =
    copy(status = SessionStatus.ACTIVE)

fun SessionUiState.pause(): SessionUiState =
    copy(isPaused = true)

fun SessionUiState.resume(): SessionUiState =
    copy(isPaused = false)

fun SessionUiState.advanceToNextSkill(nextSkill: String): SessionUiState =
    copy(
        selectedSkillTag = nextSkill,
        masteryDetected = false,
        suggestedNextSkill = null,
        consecutiveFails = 0,
    )

fun SessionUiState.recordResult(isCorrect: Boolean): SessionUiState {
    val newResults = recentResults + if (isCorrect) ResultMark.CORRECT else ResultMark.WRONG
    val trimmedResults = newResults.takeLast(7)
    val newCorrect = sessionCorrect + (if (isCorrect) 1 else 0)
    val newTotal = sessionTotal + 1
    val newFails = if (isCorrect) 0 else consecutiveFails + 1

    return copy(
        recentResults = trimmedResults,
        sessionCorrect = newCorrect,
        sessionTotal = newTotal,
        consecutiveFails = newFails,
        scoreRiskVisible = newFails >= 5 && scoreRiskDismissedAt != newFails - 1,
        masteryDetected = trimmedResults.takeLast(5).all { it == ResultMark.CORRECT } && newTotal >= 5,
    )
}
