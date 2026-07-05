package com.sprint.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sprint.domain.procedural.ProceduralEngine
import com.sprint.domain.procedural.ProceduralExercise
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.random.Random

data class SimuladoRule(
    val skill: String,
    val quantity: Int
)

data class SimuladoUiState(
    val isGenerating: Boolean = false,
    val generatedCount: Int = 0,
    val totalTarget: Int = 0,
    val exercises: List<ProceduralExercise> = emptyList(),
    val currentPage: Int = 0, // Paginação para não travar a UI
    val pageSize: Int = 20
)

class SimuladoViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(SimuladoUiState())
    val uiState: StateFlow<SimuladoUiState> = _uiState.asStateFlow()

    fun generateSimulado(rules: List<SimuladoRule>, difficulty: String) {
        val total = rules.sumOf { it.quantity }
        _uiState.update { it.copy(isGenerating = true, totalTarget = total, generatedCount = 0, exercises = emptyList(), currentPage = 0) }

        viewModelScope.launch(Dispatchers.IO) {
            val generated = mutableListOf<ProceduralExercise>()
            val baseMmr = difficulty.toIntOrNull() ?: 1000

            // Limpa semente
            ProceduralEngine.randomInstance = Random.Default

            for (rule in rules) {
                val step = if (rule.quantity > 1) 1500 / rule.quantity else 0
                for (i in 0 until rule.quantity) {
                    try {
                        val progressiveMmr = baseMmr + (i * step)
                        generated.add(ProceduralEngine.generate(rule.skill, progressiveMmr))
                    } catch (e: Exception) {
                        android.util.Log.e("SimuladoVM", "generate failed skill=" + rule.skill + " i=" + i, e)
                    }
                    
                    if (generated.size % 100 == 0) {
                        _uiState.update { it.copy(generatedCount = generated.size) }
                    }
                }
            }

            _uiState.update { 
                it.copy(
                    isGenerating = false, 
                    generatedCount = generated.size,
                    exercises = generated
                ) 
            }
        }
    }

    fun nextPage() {
        val state = _uiState.value
        val maxPage = (state.exercises.size - 1) / state.pageSize
        if (state.currentPage < maxPage) {
            _uiState.update { it.copy(currentPage = it.currentPage + 1) }
        }
    }

    fun previousPage() {
        val state = _uiState.value
        if (state.currentPage > 0) {
            _uiState.update { it.copy(currentPage = it.currentPage - 1) }
        }
    }
}
