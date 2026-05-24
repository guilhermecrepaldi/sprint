package com.strava_matematica

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.strava_matematica.design.StravaMathTheme
import com.strava_matematica.model.ApiStatus
import com.strava_matematica.model.SessionStatus
import com.strava_matematica.ui.calibration.CalibrationScreen
import com.strava_matematica.ui.config.SessionConfigScreen
import com.strava_matematica.ui.folha.FolhaScreen
import com.strava_matematica.ui.result.PageResultScreen
import com.strava_matematica.ui.summary.SessionSummaryScreen
import com.strava_matematica.viewmodel.SessionViewModel
import com.strava_matematica.viewmodel.FolhaViewModel
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {
    private val sessionViewModel: SessionViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            StravaMathApp(sessionViewModel)
        }
    }
}

@Composable
fun StravaMathApp(viewModel: SessionViewModel, folhaViewModel: FolhaViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()

    StravaMathTheme(backgroundMode = state.config.backgroundMode) {
        when (state.status) {
            SessionStatus.CONFIG,
            SessionStatus.ERROR -> SessionConfigScreen(
                config = state.config,
                onConfigChange = viewModel::updateConfig,
                onStart = viewModel::startSession,
            )

            SessionStatus.CALIBRATION -> CalibrationScreen(
                isSubmitting = state.apiStatus == ApiStatus.CONNECTING,
                errorMessage = state.errorMessage,
                onComplete = { skipped -> viewModel.onCalibrationComplete(skipped) },
                onSubmitSamples = viewModel::submitCalibration,
            )

            SessionStatus.ACTIVE,
            SessionStatus.SUBMITTING -> state.currentFolha?.let { folha ->
                val folhaState by folhaViewModel.uiState.collectAsState()
                FolhaScreen(
                    folha = folha,
                    config = state.config,
                    currentExerciseIndex = folhaState.currentExerciseIndex,
                    fieldScratchStrokes = folhaState.fieldScratchStrokes,
                    fieldAnswerStrokes = folhaState.fieldAnswerStrokes,
                    fieldScratchRedoStacks = folhaState.fieldScratchRedoStacks,
                    fieldAnswerRedoStacks = folhaState.fieldAnswerRedoStacks,
                    onAdvance = {
                        val done = folhaViewModel.advanceExercise(folha.fields.size)
                        if (done) viewModel.submitFolha(folhaViewModel.uiState.value)
                    },
                    onSyncScratch = folhaViewModel::syncScratch,
                    onSyncAnswer = folhaViewModel::syncAnswer,
                    onPenEvent = folhaViewModel::appendEvent,
                    onConfigChange = viewModel::updateConfig,
                )
            }

            SessionStatus.RESULT -> state.lastResult?.let {
                PageResultScreen(
                    result = it,
                    onNext = {
                        folhaViewModel.resetForNextFolha()
                        viewModel.goToNextFolha()
                    },
                    onSummary = {},
                )
            }

            SessionStatus.FINISHED -> SessionSummaryScreen(
                result = state.lastResult,
                onNewSession = viewModel::resetToConfig,
            )
        }
    }
}
