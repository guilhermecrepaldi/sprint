package com.strava_matematica

import android.os.Bundle
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.scale
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.lifecycle.viewmodel.compose.viewModel
import com.strava_matematica.design.StravaMathTheme
import com.strava_matematica.model.ApiStatus
import com.strava_matematica.model.SessionStatus
import com.strava_matematica.ui.canvas.PlatformMap
import com.strava_matematica.ui.canvas.ZoomableCanvas
import com.strava_matematica.ui.folha.FolhaScreen
import com.strava_matematica.ui.tabs.DashboardTab
import com.strava_matematica.ui.tabs.GesturesTab
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
import com.strava_matematica.ui.components.LiveLogDialog
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton

// ── Top rail tabs ──────────────────────────────────────────────────────────────
private enum class SprintTab(val label: String) {
    SETUP("Ajustes"),
    DASHBOARD("Painel"),
    MATHTREE("Árvore"),
    SPRINT("Sprint"),
    NOTES("Notas"),
    SIMULADO("Simulado")
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
    

    
    val lifecycleOwner = LocalLifecycleOwner.current

    var showLiveLogDialog by remember { androidx.compose.runtime.mutableStateOf(false) }

    if (showLiveLogDialog) {
        LiveLogDialog(onDismiss = { showLiveLogDialog = false })
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) {
                sessionViewModel.logAnomaly("ON_PAUSE")
            } else if (event == Lifecycle.Event.ON_STOP) {
                sessionViewModel.logAnomaly("ON_STOP")
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Auto-advance through result / finished — instantâneo (sem delay)
    LaunchedEffect(state.status) {
        when (state.status) {
            SessionStatus.RESULT -> {
                val allCorrect = state.lastResult?.results?.all { it.isCorrect } ?: true
                // B4: vibração imediata (antes do avanço)
                if (allCorrect) {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                } else {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                }
                
                if (state.config.requireCorrectToAdvance && !allCorrect) {
                    // Pausa breve para o aluno ver o que foi reconhecido antes de limpar se errou
                    kotlinx.coroutines.delay(450L)
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
            Box(modifier = Modifier.fillMaxWidth()) {
                TopRail(
                    selectedTab = selectedTab,
                    onSelect = { selectedTab = it },
                    modifier = Modifier.fillMaxWidth(),
                )
                IconButton(
                    onClick = { showLiveLogDialog = true },
                    modifier = Modifier.align(Alignment.CenterEnd).padding(end = 8.dp, top = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share, 
                        contentDescription = "Live Log / Logshare",
                        tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                }
            }

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
                    SprintTab.SETUP -> {
                        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                            androidx.compose.foundation.layout.Box(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)) {
                                PenTab(
                                    config = state.config,
                                    onConfigChange = sessionViewModel::updateConfig,
                                    onGoToSprint = goToSprint,
                                )
                            }
                            androidx.compose.foundation.layout.Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f)))
                            androidx.compose.foundation.layout.Box(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)) {
                                NotebookTab(
                                    config = state.config,
                                    onConfigChange = sessionViewModel::updateConfig,
                                    onGoToSprint = goToSprint
                                )
                            }
                            androidx.compose.foundation.layout.Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f)))
                            androidx.compose.foundation.layout.Box(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)) {
                                GesturesTab(
                                    gestureConfig = state.gestureConfig,
                                    onGestureChange = sessionViewModel::updateGesture,
                                    onGoToSprint = goToSprint
                                )
                            }
                        }
                    }

                    SprintTab.DASHBOARD -> DashboardTab(
                        history = state.sprintHistory,
                        skillAttempts = state.skillAttempts,
                        skillAccuracy = state.skillAccuracy,
                        activityDays = state.activityDays,
                        notes = state.notes,
                        onGoToSprint = goToSprint,
                        onStartSession = sessionViewModel::startSessionFromDashboard,
                    )

                    SprintTab.MATHTREE -> com.strava_matematica.ui.tabs.TreeTab(
                        modifier = Modifier.fillMaxSize(),
                        onStartSprint = { proceduralTag ->
                            sessionViewModel.selectSkill(proceduralTag)
                            goToSprint()
                        }
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
                                        onStartSimulado = sessionViewModel::startSimulado,
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
                                        onConfigChange = { newConfig ->
                                            sessionViewModel.updateConfig(newConfig)
                                            folhaViewModel.resetForNextFolha()
                                            sessionViewModel.startSessionFromDashboard()
                                        },
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
                    
                    SprintTab.SIMULADO -> com.strava_matematica.ui.tabs.SimuladoTab(
                        modifier = Modifier.fillMaxSize(),
                        config = state.config
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
            .height(64.dp), // Height increased slightly to fit bigger letters
        contentAlignment = Alignment.TopCenter,
    ) {
        val sidePadding = ((maxWidth - tabWidth) / 2).coerceAtLeast(0.dp)
        LazyRow(
            state = listState,
            contentPadding = PaddingValues(horizontal = sidePadding),
            horizontalArrangement = Arrangement.spacedBy(12.dp), // Increased gap for bigger text
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(top = 8.dp),
        ) {
            itemsIndexed(tabs) { _, tab ->
                val selected = tab == selectedTab
                val interactionSource = remember { MutableInteractionSource() }
                val isPressed by interactionSource.collectIsPressedAsState()
                
                // Spring animations
                val scale by animateFloatAsState(
                    targetValue = if (isPressed) 0.90f else if (selected) 1.05f else 1f,
                    animationSpec = spring(dampingRatio = 0.55f, stiffness = 400f),
                    label = "TopRailTabScale"
                )
                val bgAlpha by animateFloatAsState(
                    targetValue = if (selected) 0.12f else 0.0f,
                    animationSpec = tween(durationMillis = 200),
                    label = "TopRailTabBg"
                )

                Box(
                    modifier = Modifier
                        .size(width = tabWidth, height = 48.dp)
                        .scale(scale)
                        .background(
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = bgAlpha),
                            shape = RoundedCornerShape(24.dp)
                        )
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null,
                            onClick = { selectWithTripleTap(tab) }
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = tab.label,
                        fontSize = 17.sp, // Letras maiores
                        fontWeight = if (selected) androidx.compose.ui.text.font.FontWeight.ExtraBold else androidx.compose.ui.text.font.FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground.copy(
                            alpha = if (selected) 1.0f else 0.70f, // Não escurecendo muito (NÃO veremos as últimas no scroll resolvido)
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
