package com.strava_matematica.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.strava_matematica.model.ApiStatus
import com.strava_matematica.model.FieldResult
import com.strava_matematica.model.Folha
import com.strava_matematica.model.FolhaField
import com.strava_matematica.model.SessionConfig
import com.strava_matematica.model.SessionStatus
import com.strava_matematica.model.SubmitResult
import com.strava_matematica.model.SessionStartRequest
import com.strava_matematica.model.SubmitRequest
import com.strava_matematica.model.FieldSubmit
import com.strava_matematica.model.Thermometer
import com.strava_matematica.network.ApiClient
import com.strava_matematica.ui.folha.ImageUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class SessionUiState(
    val studentId: String = UUID.randomUUID().toString(),
    val sessionId: String? = null,
    val config: SessionConfig = SessionConfig(),
    val currentFolha: Folha? = null,
    val lastResult: SubmitResult? = null,
    val status: SessionStatus = SessionStatus.CONFIG,
    val apiStatus: ApiStatus = ApiStatus.OK,
    val errorMessage: String? = null,
)

class SessionViewModel : ViewModel() {
    private val api = ApiClient.create()
    private val _uiState = MutableStateFlow(SessionUiState())
    val uiState: StateFlow<SessionUiState> = _uiState

    fun updateConfig(config: SessionConfig) {
        _uiState.update { it.copy(config = config) }
    }

    fun startSession() {
        val config = _uiState.value.config
        _uiState.update { it.copy(apiStatus = ApiStatus.CONNECTING, errorMessage = null) }
        viewModelScope.launch {
            try {
                val req = SessionStartRequest(studentId = _uiState.value.studentId, config = config)
                val res = api.startSession(req)
                _uiState.update {
                    it.copy(
                        sessionId = res.sessionId,
                        currentFolha = res.firstFolha,
                        status = SessionStatus.ACTIVE,
                        apiStatus = ApiStatus.OK
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(status = SessionStatus.ERROR, errorMessage = e.message, apiStatus = ApiStatus.ERROR) }
            }
        }
    }

    fun submitFolha(folhaState: FolhaUiState) {
        val state = _uiState.value
        val folha = state.currentFolha ?: return
        val sessionId = state.sessionId ?: return

        _uiState.update { it.copy(status = SessionStatus.SUBMITTING, apiStatus = ApiStatus.CONNECTING, errorMessage = null) }

        viewModelScope.launch {
            try {
                val fields = folha.fields.map { field ->
                    val strokes = folhaState.fieldStrokes[field.fieldIndex].orEmpty()
                    val imageBase64 = ImageUtils.exportBitmap(strokes)
                    FieldSubmit(
                        fieldIndex = field.fieldIndex,
                        exerciseId = field.exerciseId,
                        imageBase64 = imageBase64,
                        totalTimeMs = folhaState.fieldTiming[field.fieldIndex]?.totalTimeMs ?: 10000L,
                        timeToFirstStrokeMs = folhaState.fieldTiming[field.fieldIndex]?.firstStrokeAtMs ?: 2000L,
                        penEvents = folhaState.fieldEvents[field.fieldIndex].orEmpty()
                    )
                }

                val req = SubmitRequest(
                    folhaId = folha.folhaId,
                    submittedAtMs = System.currentTimeMillis(),
                    fields = fields
                )
                val res = api.submitFolha(sessionId, req)
                val finished = res.sessionStatus == "finished"
                
                _uiState.update {
                    it.copy(
                        lastResult = res,
                        status = if (finished) SessionStatus.FINISHED else SessionStatus.RESULT,
                        apiStatus = ApiStatus.OK
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message, status = SessionStatus.ACTIVE, apiStatus = ApiStatus.ERROR) }
            }
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
}
