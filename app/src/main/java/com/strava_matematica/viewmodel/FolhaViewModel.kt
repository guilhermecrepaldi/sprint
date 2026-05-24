package com.strava_matematica.viewmodel

import androidx.lifecycle.ViewModel
import com.strava_matematica.model.PenEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

data class FieldTiming(
    val startedAtMs: Long? = null,
    val firstStrokeAtMs: Long? = null,
    val totalTimeMs: Long = 0,
)

data class FolhaUiState(
    val activeFieldIndex: Int? = null,
    val fieldEvents: Map<Int, List<PenEvent>> = emptyMap(),
    val fieldTiming: Map<Int, FieldTiming> = emptyMap(),
    val isSubmitting: Boolean = false,
    val elapsedMs: Long = 0,
)

class FolhaViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(FolhaUiState())
    val uiState: StateFlow<FolhaUiState> = _uiState

    fun activateField(index: Int) {
        _uiState.update { it.copy(activeFieldIndex = index) }
    }

    fun appendEvent(fieldIndex: Int, event: PenEvent) {
        _uiState.update { state ->
            val events = state.fieldEvents[fieldIndex].orEmpty() + event
            state.copy(fieldEvents = state.fieldEvents + (fieldIndex to events))
        }
    }

    fun clearField(fieldIndex: Int) {
        _uiState.update { state ->
            state.copy(fieldEvents = state.fieldEvents - fieldIndex)
        }
    }
}
