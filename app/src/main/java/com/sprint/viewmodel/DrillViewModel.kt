package com.sprint.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sprint.model.DrillBatch
import com.sprint.model.DrillFlushRequest
import com.sprint.model.DrillFlushResult
import com.sprint.model.DrillItemResult
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

    private val _state = MutableStateFlow(DrillState())
    val state: StateFlow<DrillState> = _state

    private var tickerJob: Job? = null

    fun loadBatch(count: Int = 30, level: String = "basic") {
        viewModelScope.launch {
            _state.update { it.copy(phase = DrillPhase.LOADING, error = null, flushResult = null) }
            try {
                // Gerador offline local de DrillItems
                val items = mutableListOf<com.sprint.model.DrillItem>()
                val random = java.util.Random()
                for (i in 0 until count) {
                    val op = if (level == "basic") {
                        random.nextInt(2) // 0: +, 1: -
                    } else {
                        random.nextInt(3) // 0: +, 1: -, 2: *
                    }
                    val num1 = if (level == "basic") random.nextInt(9) + 1 else random.nextInt(19) + 2
                    val num2 = if (level == "basic") random.nextInt(9) + 1 else random.nextInt(19) + 2
                    
                    val statement: String
                    val expected: String
                    val tag: String
                    if (op == 0) {
                        statement = "$num1 + $num2"
                        expected = (num1 + num2).toString()
                        tag = "soma"
                    } else if (op == 1) {
                        val n1 = maxOf(num1, num2)
                        val n2 = minOf(num1, num2)
                        statement = "$n1 - $n2"
                        expected = (n1 - n2).toString()
                        tag = "subtracao"
                    } else {
                        statement = "$num1 * $num2"
                        expected = (num1 * num2).toString()
                        tag = "multiplicacao"
                    }
                    items.add(
                        com.sprint.model.DrillItem(
                            itemId = "local_${System.currentTimeMillis()}_$i",
                            statement = statement,
                            expectedAnswer = expected,
                            skillTag = tag,
                            difficulty = if (level == "basic") 0.1f else 0.5f,
                            autoSubmitChars = expected.length
                        )
                    )
                }
                val batch = com.sprint.model.DrillBatch(
                    batchId = "local_batch_${System.currentTimeMillis()}",
                    level = level,
                    count = count,
                    items = items,
                    generatedAt = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US).format(java.util.Date())
                )
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

    fun onInputChange(text: String) {
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
            val correct = results.count { it.isCorrect }
            _state.update {
                it.copy(
                    phase = DrillPhase.DONE,
                    flushResult = DrillFlushResult(
                        sessionId = "local_session_${System.currentTimeMillis()}",
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

    private var _studentId: String = ""
    private val DrillState.studentId get() = _studentId

    fun setStudentId(id: String) {
        _studentId = id
    }
}
