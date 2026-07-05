package com.strava_matematica.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.strava_matematica.model.DrillBatch
import com.strava_matematica.model.DrillFlushRequest
import com.strava_matematica.model.DrillFlushResult
import com.strava_matematica.model.DrillItemResult
import com.strava_matematica.network.ApiClient
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class DrillPhase { LOADING, ACTIVE, FLUSHING, DONE, ERROR }

data class DrillState(
    val phase: DrillPhase = DrillPhase.LOADING,
    val batch: DrillBatch? = null,
    val currentIndex: Int = 0,
    val currentInput: String = "",
    val results: List<DrillItemResult> = emptyList(),
    // Últimos 10 resultados para o indicador visual de momentum.
    val recentCorrect: List<Boolean> = emptyList(),
    val startedAtMs: Long = 0L,
    val itemStartMs: Long = 0L,
    val elapsedMs: Long = 0L,     // tempo total decorrido
    val flushResult: DrillFlushResult? = null,
    val error: String? = null,
) {
    val currentItem get() = batch?.items?.getOrNull(currentIndex)
    val total get() = batch?.items?.size ?: 0
    val done get() = currentIndex >= total && total > 0
    val elapsedSeconds get() = (elapsedMs / 1000L).toInt()
}

class DrillViewModel : ViewModel() {

    private val api = ApiClient.create()
    private val _state = MutableStateFlow(DrillState())
    val state: StateFlow<DrillState> = _state

    private var tickerJob: Job? = null

    fun loadBatch(count: Int = 30, level: String = "basic") {
        viewModelScope.launch {
            _state.update { it.copy(phase = DrillPhase.LOADING, error = null, flushResult = null) }
            try {
                val batch = api.getDrillBatch(count = count, level = level)
                val now = System.currentTimeMillis()
                _state.update {
                    it.copy(
                        phase = DrillPhase.ACTIVE,
                        batch = batch,
                        currentIndex = 0,
                        currentInput = "",
                        results = emptyList(),
                        recentCorrect = emptyList(),
                        startedAtMs = now,
                        itemStartMs = now,
                        elapsedMs = 0L,
                    )
                }
                startTicker()
            } catch (e: Exception) {
                _state.update { it.copy(phase = DrillPhase.ERROR, error = e.message) }
            }
        }
    }

    /** Chamado pelo TextField a cada mudança de texto. */
    fun onInputChange(text: String) {
        // Permite apenas dígitos e um '-' inicial para números negativos.
        val filtered = buildString {
            text.forEachIndexed { i, c ->
                if (c.isDigit() || (c == '-' && i == 0)) append(c)
            }
        }
        _state.update { it.copy(currentInput = filtered) }

        val item = _state.value.currentItem ?: return
        if (filtered.length == item.autoSubmitChars && filtered != "-") {
            submitAnswer(filtered)
        }
    }

    /** Submit explícito (botão Enter / ação do teclado). */
    fun submitCurrent() {
        val input = _state.value.currentInput
        if (input.isNotEmpty() && input != "-") submitAnswer(input)
    }

    private fun submitAnswer(writtenAnswer: String) {
        val state = _state.value
        val item = state.currentItem ?: return
        val now = System.currentTimeMillis()
        val timeMs = (now - state.itemStartMs).toInt().coerceAtLeast(50)

        val isCorrect = writtenAnswer.trim() == item.expectedAnswer.trim()
        val result = DrillItemResult(
            itemId = item.itemId,
            writtenAnswer = writtenAnswer,
            isCorrect = isCorrect,
            timeMs = timeMs,
        )

        val newResults = state.results + result
        val newRecent = (state.recentCorrect + isCorrect).takeLast(10)
        val nextIndex = state.currentIndex + 1

        _state.update {
            it.copy(
                results = newResults,
                recentCorrect = newRecent,
                currentIndex = nextIndex,
                currentInput = "",
                itemStartMs = now,
            )
        }

        if (nextIndex >= (state.batch?.items?.size ?: 0)) {
            flush(state.studentId, state.batch!!, newResults, state.startedAtMs)
        }
    }

    private fun flush(
        studentId: String,
        batch: DrillBatch,
        results: List<DrillItemResult>,
        startedAtMs: Long,
    ) {
        tickerJob?.cancel()
        _state.update { it.copy(phase = DrillPhase.FLUSHING) }

        viewModelScope.launch {
            try {
                val res = api.flushDrill(
                    DrillFlushRequest(
                        studentId = studentId,
                        batchId = batch.batchId,
                        level = batch.level,
                        startedAtMs = startedAtMs,
                        results = results,
                    )
                )
                _state.update { it.copy(phase = DrillPhase.DONE, flushResult = res) }
            } catch (e: Exception) {
                // Flush failed — show result locally anyway (telemetria perdida, UX não quebra)
                val correct = results.count { it.isCorrect }
                _state.update {
                    it.copy(
                        phase = DrillPhase.DONE,
                        flushResult = DrillFlushResult(
                            sessionId = "",
                            total = results.size,
                            correct = correct,
                            accuracy = correct.toFloat() / results.size,
                            totalTimeMs = results.sumOf { it.timeMs },
                            avgTimeMs = results.sumOf { it.timeMs } / results.size,
                            xpEarned = correct,
                        ),
                    )
                }
            }
        }
    }

    fun reset() {
        tickerJob?.cancel()
        _state.update { DrillState() }
    }

    private fun startTicker() {
        tickerJob?.cancel()
        val start = System.currentTimeMillis()
        tickerJob = viewModelScope.launch {
            while (true) {
                delay(100L)
                _state.update { it.copy(elapsedMs = System.currentTimeMillis() - start) }
            }
        }
    }

    override fun onCleared() {
        tickerJob?.cancel()
        super.onCleared()
    }

    // Student ID vem do SessionViewModel — injetado antes de loadBatch().
    private var _studentId: String = ""
    private val DrillState.studentId get() = _studentId

    fun setStudentId(id: String) {
        _studentId = id
    }
}
