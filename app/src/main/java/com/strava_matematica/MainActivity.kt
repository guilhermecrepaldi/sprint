package com.strava_matematica

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.strava_matematica.design.StravaMathTheme
import com.strava_matematica.model.SessionStatus
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
                        if (done) viewModel.submitFolha(folhaState)
                    },
                    onSyncScratch = folhaViewModel::syncScratch,
                    onSyncAnswer = folhaViewModel::syncAnswer,
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
