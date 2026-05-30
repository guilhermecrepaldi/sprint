package com.strava_matematica

import android.os.Bundle
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.strava_matematica.design.StravaMathTheme
import com.strava_matematica.model.ApiStatus
import com.strava_matematica.model.SessionStatus
import com.strava_matematica.ui.canvas.PlatformMap
import com.strava_matematica.ui.canvas.ZoomableCanvas
import com.strava_matematica.ui.folha.FolhaScreen
import com.strava_matematica.ui.tabs.DashboardTab
import com.strava_matematica.ui.tabs.GesturesTab
import com.strava_matematica.ui.tabs.MathTreeTab
import com.strava_matematica.ui.tabs.NotebookTab
import com.strava_matematica.ui.tabs.NotesTab
import com.strava_matematica.ui.tabs.PenTab
import com.strava_matematica.viewmodel.FolhaUiState
import com.strava_matematica.viewmodel.FolhaViewModel
import com.strava_matematica.viewmodel.ResultMark
import com.strava_matematica.viewmodel.SessionViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.rememberCoroutineScope
import com.strava_matematica.recognizer.MlKitRecognizer
import kotlinx.coroutines.launch

// ── Top rail tabs ──────────────────────────────────────────────────────────────
private enum class SprintTab(val label: String) {
    PEN("Caneta"),
    PAPER("Papel"),
    GESTURES("Gestos"),
    DASHBOARD("Painel"),
    SPRINT("Sprint"),
    NOTES("Notas"),
}

class MainActivity : ComponentActivity() {
    private val sessionViewModel: SessionViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { SprintApp(sessionViewModel) }
    }
}

@Composable
fun SprintApp(
    sessionViewModel: SessionViewModel,
    folhaViewModel: FolhaViewModel = viewModel(),
) {
    val state      by sessionViewModel.uiState.collectAsState()
    val folhaState by folhaViewModel.uiState.collectAsState()
    val haptic     = LocalHapticFeedback.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val handwritingRecognizer = remember { MlKitRecognizer(context) }

    // Auto-advance through result / finished — instantâneo (sem delay)
    LaunchedEffect(state.status) {
        when (state.status) {
            SessionStatus.RESULT -> {
                val allCorrect = state.lastResult?.results?.all { it.isCorrect } ?: true
                // B4: vibração imediata (antes do delay para chegar antes do avanço)
                if (allCorrect) {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                } else {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                }
                // Pausa breve para o aluno ver o que foi reconhecido antes de avançar
                kotlinx.coroutines.delay(450L)
                if (state.config.requireCorrectToAdvance && !allCorrect) {
                    val fieldIndex = state.currentFolha?.fields
                        ?.getOrNull(folhaState.currentExerciseIndex)?.fieldIndex ?: 0
                    folhaViewModel.clearFieldAndRetry(fieldIndex)
                    sessionViewModel.resetToActive()
                } else {
                    folhaViewModel.resetForNextFolha()
                    sessionViewModel.goToNextFolha()
                }
            }
            SessionStatus.FINISHED -> {
                folhaViewModel.resetForNextFolha()
                sessionViewModel.startSessionFromDashboard()
            }
            else -> {}
        }
    }
    LaunchedEffect(state.currentFolha?.folhaId) {
        state.currentFolha?.folhaId?.let { folhaId ->
            if (folhaState.folhaId != folhaId) {
                folhaViewModel.resetForFolha(folhaId)
            }
        }
    }

    var selectedTab by rememberSaveable { mutableStateOf(SprintTab.SPRINT) }
    val goToSprint: () -> Unit = {
        selectedTab = SprintTab.SPRINT
    }

    StravaMathTheme(backgroundMode = state.config.backgroundMode) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
        ) {
            TopRail(
                selectedTab = selectedTab,
                onSelect = { selectedTab = it },
                modifier = Modifier.fillMaxWidth(),
            )

            Crossfade(
                targetState = selectedTab,
                animationSpec = tween(durationMillis = 320),
                label = "tab-fade",
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) { tab ->
                Box(Modifier.fillMaxSize()) {
                    when (tab) {
                    SprintTab.PEN -> PenTab(
                        config = state.config,
                        onConfigChange = sessionViewModel::updateConfig,
                        onGoToSprint = goToSprint,
                    )

                    SprintTab.PAPER -> NotebookTab(
                        config = state.config,
                        onConfigChange = sessionViewModel::updateConfig,
                        onGoToSprint = goToSprint,
                    )

                    SprintTab.GESTURES -> GesturesTab(
                        gestureConfig = state.gestureConfig,
                        onGestureChange = sessionViewModel::updateGesture,
                        onGoToSprint = goToSprint,
                    )

                    SprintTab.DASHBOARD -> DashboardTab(
                        history = state.sprintHistory,
                        skillAttempts = state.skillAttempts,
                        skillAccuracy = state.skillAccuracy,
                        activityDays = state.activityDays,
                        notes = state.notes,
                        onGoToSprint = goToSprint,
                        onStartSession = sessionViewModel::startSessionFromDashboard,
                    )

                    SprintTab.SPRINT -> {
                        val folha = state.currentFolha
                        if (folha != null) {
                            val effectiveFolhaState = if (folhaState.folhaId == folha.folhaId) {
                                folhaState
                            } else {
                                FolhaUiState(folhaId = folha.folhaId)
                            }
                            val sessionStartedAtMs = remember(state.sessionId) { System.currentTimeMillis() }
                            val limit = state.config.exercisesPerPage
                                .coerceAtMost(folha.fields.size)
                            val doAdvance: () -> Unit = {
                                val done = folhaViewModel.advanceExercise(limit)
                                if (done) {
                                    val latestFolhaState = folhaViewModel.uiState.value
                                    val submitState = if (latestFolhaState.folhaId == folha.folhaId) {
                                        latestFolhaState
                                    } else {
                                        FolhaUiState(folhaId = folha.folhaId)
                                    }
                                    sessionViewModel.submitFolha(submitState)
                                }
                            }
                            ZoomableCanvas(
                                isInSession = true,
                                onAdvance = doAdvance,
                                onPause = sessionViewModel::pauseSession,
                                onResume = sessionViewModel::resumeSession,
                                mapContent = {
                                    PlatformMap(
                                        currentSkill = state.selectedSkillTag,
                                        skillStatuses = state.skillStatuses,
                                        reviewSkills = state.reviewSkills,
                                        onSkillSelect = sessionViewModel::selectSkill,
                                        onModeSelect = sessionViewModel::selectDensity,
                                    )
                                },
                                focusContent = {
                                    FolhaScreen(
                                        folha = folha,
                                        config = state.config,
                                        currentExerciseIndex = effectiveFolhaState.currentExerciseIndex,
                                        fieldScratchStrokes = effectiveFolhaState.fieldScratchStrokes,
                                        fieldAnswerStrokes = effectiveFolhaState.fieldAnswerStrokes,
                                        fieldScratchRedoStacks = effectiveFolhaState.fieldScratchRedoStacks,
                                        fieldAnswerRedoStacks = effectiveFolhaState.fieldAnswerRedoStacks,
                                        fieldTypedAnswers = effectiveFolhaState.fieldTypedAnswers,
                                        onAdvance = doAdvance,
                                        selectedSkillTag = state.selectedSkillTag,
                                        densityLevel = state.densityLevel,
                                        onApplySprintScrollSelection = sessionViewModel::applySprintScrollSelection,
                                        onSyncScratch = { fieldIndex, strokes, redoStack ->
                                            folhaViewModel.syncScratch(folha.folhaId, fieldIndex, strokes, redoStack)
                                        },
                                        onSyncAnswer = { fieldIndex, strokes, redoStack ->
                                            folhaViewModel.syncAnswer(folha.folhaId, fieldIndex, strokes, redoStack)
                                            if (strokes.isNotEmpty()) {
                                                coroutineScope.launch {
                                                    val text = handwritingRecognizer.recognize(strokes)
                                                    if (text != null) {
                                                        folhaViewModel.syncTypedAnswer(folha.folhaId, fieldIndex, text)
                                                    }
                                                }
                                            }
                                        },
                                        onTypedAnswerChange = { fieldIndex, answer ->
                                            folhaViewModel.syncTypedAnswer(folha.folhaId, fieldIndex, answer)
                                        },
                                        onPenEvent = { fieldIndex, event ->
                                            folhaViewModel.appendEvent(folha.folhaId, fieldIndex, event)
                                        },
                                        onConfigChange = sessionViewModel::updateConfig,
                                        onEndSession = {
                                            folhaViewModel.resetForNextFolha()
                                            sessionViewModel.startSessionFromDashboard()
                                        },
                                        sessionCorrect = state.sessionCorrect,
                                        sessionTotal = state.sessionTotal,
                                        sessionStartedAtMs = sessionStartedAtMs,
                                        sessionId = state.sessionId,
                                        folhaIndex = state.currentFolha?.pageIndex ?: 0,
                                        onAddNote = sessionViewModel::addNote,
                                        gestureConfig = state.gestureConfig,
                                        retrySignal = folhaState.retryCount,
                                        recentResults = state.recentResults,
                                        skillAccuracy = state.skillAccuracy,
                                        skillAttempts = state.skillAttempts,
                                        skillFluency = state.skillAccuracy,
                                        masteryDetected = state.masteryDetected,
                                        suggestedNextSkill = state.suggestedNextSkill,
                                        scoreRiskVisible = state.scoreRiskVisible,
                                        onDismissMastery = sessionViewModel::dismissMasterySuggestion,
                                        onAdvanceToNextSkill = sessionViewModel::advanceToNextSkill,
                                        onStayInSprintAfterScoreWarning = sessionViewModel::stayInCurrentSprintAfterScoreWarning,
                                        onAdjustSprintAfterScoreWarning = sessionViewModel::adjustSprintAfterScoreWarning,
                                    )
                                },
                            )
                        } else if (state.apiStatus == ApiStatus.ERROR) {
                            SprintErrorState(
                                message = state.errorMessage,
                                onRetry = sessionViewModel::startSessionFromDashboard,
                            )
                        } else if (state.apiStatus == ApiStatus.CONNECTING) {
                            SprintLoadingState()
                        }
                        // else: papel em branco (sessão lançada, aguardando folha)

                    }

                    SprintTab.NOTES -> NotesTab(
                        notes = state.notes,
                        onGoToSprint = goToSprint,
                    )
                }
                    if (tab == SprintTab.SPRINT &&
                        (state.status == SessionStatus.RESULT || state.status == SessionStatus.FINISHED) &&
                        state.lastResult != null
                    ) {
                        val correct = state.lastResult?.results?.count { it.isCorrect } ?: 0
                        val total = state.lastResult?.results?.size ?: 0
                        SprintFeedbackOverlay(
                            correct = correct,
                            total = total,
                            recognized = state.lastResult?.results?.firstOrNull()?.recognizedAnswer,
                            modifier = Modifier.align(Alignment.Center),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SprintFeedbackOverlay(
    correct: Int,
    total: Int,
    recognized: String?,
    modifier: Modifier = Modifier,
) {
    val ink = MaterialTheme.colorScheme.onBackground
    val allCorrect = total > 0 && correct == total
    val interpreted = recognized?.takeIf { it.isNotBlank() } ?: "não consegui ler"
    val resultColor = if (allCorrect) ink.copy(alpha = 0.62f) else MaterialTheme.colorScheme.error.copy(alpha = 0.72f)
    val detail = if (total > 1) "$correct/$total" else null
    Column(
        modifier = modifier
            .background(
                color = MaterialTheme.colorScheme.background.copy(alpha = 0.92f),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
            )
            .padding(horizontal = 18.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = interpreted,
            fontSize = 16.sp,
            letterSpacing = 0.sp,
            color = resultColor,
            maxLines = 1,
        )
        if (detail != null) {
            Text(
                text = detail,
                fontSize = 10.sp,
                letterSpacing = 0.sp,
                color = ink.copy(alpha = 0.32f),
            )
        }
    }
}

@Composable
private fun TopRail(
    selectedTab: SprintTab,
    onSelect: (SprintTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tabs = remember { SprintTab.entries.toList() }
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = SprintTab.SPRINT.ordinal)
    var tapCount by remember { mutableIntStateOf(0) }
    var lastTapAt by remember { mutableLongStateOf(0L) }
    var scrollDrivenSelection by remember { mutableStateOf(false) }
    val tabWidth = 84.dp

    LaunchedEffect(listState, tabs) {
        snapshotFlow {
            val visible = listState.layoutInfo.visibleItemsInfo
            if (visible.isEmpty()) {
                listState.firstVisibleItemIndex
            } else {
                val center = listState.layoutInfo.viewportStartOffset +
                    (listState.layoutInfo.viewportEndOffset - listState.layoutInfo.viewportStartOffset) / 2
                visible.minBy { item -> kotlin.math.abs((item.offset + item.size / 2) - center) }.index
            }
        }
            .distinctUntilChanged()
            .collect { index ->
                if (listState.isScrollInProgress && index in tabs.indices) {
                    kotlinx.coroutines.delay(180L)
                    val visible = listState.layoutInfo.visibleItemsInfo
                    val center = listState.layoutInfo.viewportStartOffset +
                        (listState.layoutInfo.viewportEndOffset - listState.layoutInfo.viewportStartOffset) / 2
                    val centeredIndex = visible
                        .minByOrNull { item -> kotlin.math.abs((item.offset + item.size / 2) - center) }
                        ?.index
                    if (centeredIndex == index) {
                        scrollDrivenSelection = true
                        onSelect(tabs[index])
                    }
                }
            }
    }

    LaunchedEffect(selectedTab) {
        if (scrollDrivenSelection) {
            scrollDrivenSelection = false
        } else {
            listState.animateScrollToItem(selectedTab.ordinal)
        }
    }

    fun selectWithTripleTap(tab: SprintTab) {
        val now = System.currentTimeMillis()
        tapCount = if (now - lastTapAt <= 420L) tapCount + 1 else 1
        lastTapAt = now
        if (tapCount >= 3) {
            tapCount = 0
            onSelect(SprintTab.SPRINT)
        } else {
            onSelect(tab)
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(58.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        val sidePadding = ((maxWidth - tabWidth) / 2).coerceAtLeast(0.dp)
        LazyRow(
            state = listState,
            contentPadding = PaddingValues(horizontal = sidePadding),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(top = 5.dp),
        ) {
            itemsIndexed(tabs) { _, tab ->
                val selected = tab == selectedTab
                Box(
                    modifier = Modifier
                        .size(width = tabWidth, height = 48.dp)
                        .clickable { selectWithTripleTap(tab) }
                        .drawBehind {
                            val ink = androidx.compose.ui.graphics.Color.Black
                            val markAlpha = if (selected) 0.24f else 0.07f
                            val radius = 2.4.dp.toPx()
                            val gap = 11.dp.toPx()
                            val cy = 12.dp.toPx()
                            repeat(3) { i ->
                                drawCircle(
                                    color = ink.copy(alpha = markAlpha),
                                    radius = radius,
                                    center = androidx.compose.ui.geometry.Offset(
                                        x = size.width / 2 + (i - 1) * gap,
                                        y = cy,
                                    ),
                                )
                            }
                            if (selected) {
                                drawLine(
                                    color = ink.copy(alpha = 0.22f),
                                    start = androidx.compose.ui.geometry.Offset(size.width * 0.30f, size.height - 6.dp.toPx()),
                                    end = androidx.compose.ui.geometry.Offset(size.width * 0.70f, size.height - 6.dp.toPx()),
                                    strokeWidth = 1.dp.toPx(),
                                )
                            }
                        },
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    Text(
                        text = tab.label,
                        fontSize = 8.sp,
                        letterSpacing = 0.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(
                            alpha = if (selected) 0.24f else 0.0f,
                        ),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
private fun SprintErrorState(message: String?, onRetry: () -> Unit) {
    val ink = MaterialTheme.colorScheme.onBackground
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                text = "sem conexão",
                fontSize = 13.sp,
                color = ink.copy(alpha = 0.35f),
                letterSpacing = 1.sp,
            )
            if (message != null) {
                Text(
                    text = message,
                    fontSize = 10.sp,
                    color = ink.copy(alpha = 0.20f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 40.dp),
                )
            }
            TextButton(onClick = onRetry) {
                Text("tentar novamente", fontSize = 11.sp, color = ink.copy(alpha = 0.45f))
            }
        }
    }
}

@Composable
private fun SprintLoadingState() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(
            modifier = Modifier.size(24.dp),
            strokeWidth = 1.5.dp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.30f),
        )
    }
}
