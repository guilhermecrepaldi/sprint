package com.strava_matematica.viewmodel

import androidx.compose.ui.geometry.Offset
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
    val fieldStrokes: Map<Int, List<List<Offset>>> = emptyMap(),
    val fieldRedoStacks: Map<Int, List<List<Offset>>> = emptyMap(),
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

    fun syncStrokes(fieldIndex: Int, strokes: List<List<Offset>>, redoStack: List<List<Offset>>) {
        _uiState.update { state ->
            state.copy(
                fieldStrokes = state.fieldStrokes + (fieldIndex to strokes),
                fieldRedoStacks = state.fieldRedoStacks + (fieldIndex to redoStack)
            )
        }
    }

    fun clearField(fieldIndex: Int) {
        _uiState.update { state ->
            state.copy(
                fieldEvents = state.fieldEvents - fieldIndex,
                fieldStrokes = state.fieldStrokes - fieldIndex,
                fieldRedoStacks = state.fieldRedoStacks - fieldIndex
            )
        }
    }
}
