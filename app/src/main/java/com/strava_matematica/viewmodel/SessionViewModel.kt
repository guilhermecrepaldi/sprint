package com.strava_matematica.viewmodel

import androidx.lifecycle.ViewModel
import com.strava_matematica.model.ApiStatus
import com.strava_matematica.model.FieldResult
import com.strava_matematica.model.Folha
import com.strava_matematica.model.FolhaField
import com.strava_matematica.model.SessionConfig
import com.strava_matematica.model.SessionStatus
import com.strava_matematica.model.SubmitResult
import com.strava_matematica.model.Thermometer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID

data class SessionUiState(
    val studentId: String = UUID.randomUUID().toString(),
    val sessionId: String? = null,
    val config: SessionConfig = SessionConfig(),
    val currentFolha: Folha? = null,
    val lastResult: SubmitResult? = null,
    val status: SessionStatus = SessionStatus.CONFIG,
    val apiStatus: ApiStatus = ApiStatus.CONNECTING,
    val errorMessage: String? = null,
)

class SessionViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(SessionUiState(apiStatus = ApiStatus.OK))
    val uiState: StateFlow<SessionUiState> = _uiState

    fun updateConfig(config: SessionConfig) {
        _uiState.update { it.copy(config = config) }
    }

    fun startDemoSession() {
        val config = _uiState.value.config
        _uiState.update {
            it.copy(
                sessionId = UUID.randomUUID().toString(),
                currentFolha = demoFolha(pageIndex = 0, difficulty = config.difficultyStart, count = config.exercisesPerPage),
                lastResult = null,
                status = SessionStatus.ACTIVE,
                errorMessage = null,
            )
        }
    }

    fun submitDemoFolha() {
        val state = _uiState.value
        val folha = state.currentFolha ?: return
        val results = folha.fields.map {
            FieldResult(
                fieldIndex = it.fieldIndex,
                recognizedAnswer = "x = 5",
                expectedAnswer = "x = 5",
                isCorrect = true,
                score = 1000,
            )
        }
        val nextPage = folha.pageIndex + 1
        val finished = state.config.pagesLimit != null && nextPage >= state.config.pagesLimit
        _uiState.update {
            it.copy(
                lastResult = SubmitResult(
                    results = results,
                    pageScore = 1000,
                    thermometer = Thermometer(value = 0.86, trend = "up"),
                    restartTriggered = false,
                    sessionStatus = if (finished) "finished" else "active",
                    nextFolha = if (finished) null else demoFolha(nextPage, folha.difficulty + state.config.difficultyStep, state.config.exercisesPerPage),
                ),
                status = if (finished) SessionStatus.FINISHED else SessionStatus.RESULT,
            )
        }
    }

    fun goToNextFolha() {
        val result = _uiState.value.lastResult ?: return
        _uiState.update {
            it.copy(
                currentFolha = result.nextFolha,
                status = if (result.nextFolha == null) SessionStatus.FINISHED else SessionStatus.ACTIVE,
            )
        }
    }

    fun resetToConfig() {
        _uiState.value = SessionUiState(apiStatus = ApiStatus.OK)
    }

    private fun demoFolha(pageIndex: Int, difficulty: Double, count: Int): Folha {
        val statements = listOf(
            "Resolva: 3x + 7 = 22",
            "Calcule: 2/3 + 5/6",
            "Fatore: x^2 + 5x + 6",
            "Simplifique: (3x^2y) / (xy)",
            "Resolva: x^2 - 9 = 0",
            "Expanda: (x + 4)(x - 2)",
            "Resolva: 5 - 2x = 17",
            "Fatore: x^2 - 16",
        )
        return Folha(
            folhaId = UUID.randomUUID().toString(),
            pageIndex = pageIndex,
            difficulty = difficulty,
            fields = List(count) { index ->
                FolhaField(
                    fieldIndex = index,
                    exerciseId = UUID.randomUUID().toString(),
                    statement = statements[index % statements.size],
                    skillTags = listOf("equacao_1_grau"),
                    estimatedTimeMs = 30000,
                )
            },
        )
    }
}
