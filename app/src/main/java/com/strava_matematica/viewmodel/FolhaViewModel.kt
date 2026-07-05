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
    val folhaId: String? = null,
    val currentExerciseIndex: Int = 0,
    val retryCount: Int = 0,
    val activeFieldIndex: Int? = null,
    val fieldEvents: Map<Int, List<PenEvent>> = emptyMap(),
    val fieldScratchStrokes: Map<Int, List<List<Offset>>> = emptyMap(),
    val fieldAnswerStrokes: Map<Int, List<List<Offset>>> = emptyMap(),
    val fieldScratchRedoStacks: Map<Int, List<List<Offset>>> = emptyMap(),
    val fieldAnswerRedoStacks: Map<Int, List<List<Offset>>> = emptyMap(),
    val fieldTypedAnswers: Map<Int, String> = emptyMap(),
    val fieldTiming: Map<Int, FieldTiming> = emptyMap(),
    val isSubmitting: Boolean = false,
    val elapsedMs: Long = 0,
) {
    // Alias so SessionViewModel.submitFolha (which uses fieldStrokes) always
    // reads only the answer-box strokes — the ones sent to OCR.
    val fieldStrokes: Map<Int, List<List<Offset>>>
        get() = fieldAnswerStrokes

    // Legacy redo-stack alias (used nowhere yet, but keeps API surface stable)
    val fieldRedoStacks: Map<Int, List<List<Offset>>>
        get() = fieldAnswerRedoStacks
}

class FolhaViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(FolhaUiState())
    val uiState: StateFlow<FolhaUiState> = _uiState

    private fun FolhaUiState.scopedTo(folhaId: String): FolhaUiState =
        if (this.folhaId == folhaId) this else FolhaUiState(folhaId = folhaId)

    fun resetForFolha(folhaId: String) {
        _uiState.value = FolhaUiState(folhaId = folhaId)
    }

    fun activateField(index: Int) {
        _uiState.update { it.copy(activeFieldIndex = index) }
    }

    fun appendEvent(folhaId: String, fieldIndex: Int, event: PenEvent) {
        val now = System.currentTimeMillis()
        _uiState.update { state ->
            val scoped = state.scopedTo(folhaId)
            val events = scoped.fieldEvents[fieldIndex].orEmpty() + event
            val currentTiming = scoped.fieldTiming[fieldIndex] ?: FieldTiming(startedAtMs = now)
            val startedAt = currentTiming.startedAtMs ?: now
            val firstStrokeAt = currentTiming.firstStrokeAtMs
                ?: if (event.eventType == "stroke_start") now - startedAt else null
            val timing = currentTiming.copy(
                startedAtMs = startedAt,
                firstStrokeAtMs = firstStrokeAt,
                totalTimeMs = now - startedAt,
            )

            scoped.copy(
                fieldEvents = scoped.fieldEvents + (fieldIndex to events),
                fieldTiming = scoped.fieldTiming + (fieldIndex to timing),
            )
        }
    }

    fun appendEvent(fieldIndex: Int, event: PenEvent) {
        val folhaId = _uiState.value.folhaId ?: return
        appendEvent(folhaId, fieldIndex, event)
    }

    fun syncScratch(folhaId: String, fieldIndex: Int, strokes: List<List<Offset>>, redoStack: List<List<Offset>>) {
        _uiState.update { state ->
            val scoped = state.scopedTo(folhaId)
            scoped.copy(
                fieldScratchStrokes = scoped.fieldScratchStrokes + (fieldIndex to strokes),
                fieldScratchRedoStacks = scoped.fieldScratchRedoStacks + (fieldIndex to redoStack),
            )
        }
    }

    fun syncScratch(fieldIndex: Int, strokes: List<List<Offset>>, redoStack: List<List<Offset>>) {
        val folhaId = _uiState.value.folhaId ?: return
        syncScratch(folhaId, fieldIndex, strokes, redoStack)
    }

    fun syncAnswer(folhaId: String, fieldIndex: Int, strokes: List<List<Offset>>, redoStack: List<List<Offset>>) {
        _uiState.update { state ->
            val scoped = state.scopedTo(folhaId)
            scoped.copy(
                fieldAnswerStrokes = scoped.fieldAnswerStrokes + (fieldIndex to strokes),
                fieldAnswerRedoStacks = scoped.fieldAnswerRedoStacks + (fieldIndex to redoStack),
            )
        }
    }

    fun syncAnswer(fieldIndex: Int, strokes: List<List<Offset>>, redoStack: List<List<Offset>>) {
        val folhaId = _uiState.value.folhaId ?: return
        syncAnswer(folhaId, fieldIndex, strokes, redoStack)
    }

    fun syncTypedAnswer(folhaId: String, fieldIndex: Int, answer: String) {
        _uiState.update { state ->
            val scoped = state.scopedTo(folhaId)
            scoped.copy(fieldTypedAnswers = scoped.fieldTypedAnswers + (fieldIndex to answer))
        }
    }

    /** Kept for call-sites that haven't migrated yet; routes to answer canvas. */
    fun syncStrokes(fieldIndex: Int, strokes: List<List<Offset>>, redoStack: List<List<Offset>>) =
        syncAnswer(fieldIndex, strokes, redoStack)

    fun syncStrokes(folhaId: String, fieldIndex: Int, strokes: List<List<Offset>>, redoStack: List<List<Offset>>) =
        syncAnswer(folhaId, fieldIndex, strokes, redoStack)

    /** Returns true when the student has passed the last exercise (caller should submit). */
    fun advanceExercise(totalFields: Int): Boolean {
        val next = _uiState.value.currentExerciseIndex + 1
        return if (next >= totalFields) {
            true
        } else {
            _uiState.update { it.copy(currentExerciseIndex = next) }
            false
        }
    }

    fun resetForNextFolha() {
        _uiState.value = FolhaUiState()
    }

    fun clearField(fieldIndex: Int) {
        _uiState.update { state ->
            state.copy(
                fieldEvents = state.fieldEvents - fieldIndex,
                fieldScratchStrokes = state.fieldScratchStrokes - fieldIndex,
                fieldAnswerStrokes = state.fieldAnswerStrokes - fieldIndex,
                fieldScratchRedoStacks = state.fieldScratchRedoStacks - fieldIndex,
                fieldAnswerRedoStacks = state.fieldAnswerRedoStacks - fieldIndex,
                fieldTypedAnswers = state.fieldTypedAnswers - fieldIndex,
                fieldTiming = state.fieldTiming - fieldIndex,
            )
        }
    }

    /** Limpa o campo atual e força recomposição do canvas (modo "só avança se acertar"). */
    fun clearFieldAndRetry(fieldIndex: Int) {
        _uiState.update { state ->
            state.copy(
                retryCount = state.retryCount + 1,
                fieldEvents = state.fieldEvents - fieldIndex,
                fieldScratchStrokes = state.fieldScratchStrokes - fieldIndex,
                fieldAnswerStrokes = state.fieldAnswerStrokes - fieldIndex,
                fieldScratchRedoStacks = state.fieldScratchRedoStacks - fieldIndex,
                fieldAnswerRedoStacks = state.fieldAnswerRedoStacks - fieldIndex,
                fieldTypedAnswers = state.fieldTypedAnswers - fieldIndex,
                fieldTiming = state.fieldTiming - fieldIndex,
            )
        }
    }
}
