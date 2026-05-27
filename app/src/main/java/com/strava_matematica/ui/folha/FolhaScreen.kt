package com.strava_matematica.ui.folha

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import com.strava_matematica.design.Spacing
import com.strava_matematica.model.Folha
import com.strava_matematica.viewmodel.ResultMark
import com.strava_matematica.model.FolhaField
import com.strava_matematica.model.GestureConfig
import com.strava_matematica.model.PenEvent
import com.strava_matematica.model.SessionConfig
import com.strava_matematica.viewmodel.SprintNote
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

@Composable
fun FolhaScreen(
    folha: Folha,
    config: SessionConfig,
    currentExerciseIndex: Int = 0,
    fieldScratchStrokes: Map<Int, List<List<Offset>>> = emptyMap(),
    fieldAnswerStrokes: Map<Int, List<List<Offset>>> = emptyMap(),
    fieldScratchRedoStacks: Map<Int, List<List<Offset>>> = emptyMap(),
    fieldAnswerRedoStacks: Map<Int, List<List<Offset>>> = emptyMap(),
    onAdvance: () -> Unit,
    selectedSkillTag: String = "soma_subtracao",
    densityLevel: String = "medium",
    onApplySprintScrollSelection: (skillTag: String, density: String, exactCurrent: Boolean, field: FolhaField) -> Unit = { _, _, _, _ -> },
    onSyncScratch: (fieldIndex: Int, strokes: List<List<Offset>>, redoStack: List<List<Offset>>) -> Unit = { _, _, _ -> },
    onSyncAnswer: (fieldIndex: Int, strokes: List<List<Offset>>, redoStack: List<List<Offset>>) -> Unit = { _, _, _ -> },
    onPenEvent: (fieldIndex: Int, event: PenEvent) -> Unit = { _, _ -> },
    onConfigChange: (SessionConfig) -> Unit = {},
    onEndSession: () -> Unit = {},
    sessionCorrect: Int = 0,
    sessionTotal: Int = 0,
    sessionStartedAtMs: Long = System.currentTimeMillis(),
    sessionId: String? = null,
    folhaIndex: Int = 0,
    onAddNote: (SprintNote) -> Unit = {},
    gestureConfig: GestureConfig = GestureConfig(),
    retrySignal: Int = 0,
    recentResults: List<ResultMark> = emptyList(),
) {
    val field = folha.fields[currentExerciseIndex]
    // retrySignal muda quando o usuário erra + requireCorrectToAdvance=true — força recomposição do canvas.
    val exerciseRenderKey = "${folha.folhaId}:${field.exerciseId}:${field.fieldIndex}:$retrySignal"
    // Signals reset whenever the exercise changes, giving each field a fresh canvas state.
    val clearSignal = remember(exerciseRenderKey) { mutableIntStateOf(0) }
    val undoSignal = remember(exerciseRenderKey) { mutableIntStateOf(0) }
    val redoSignal = remember(exerciseRenderKey) { mutableIntStateOf(0) }
    val isErasing = remember { mutableStateOf(false) }
    val showSessionRegister = remember { mutableStateOf(false) }
    val showSprintScrolls = remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        // ── Full-screen exercise ─────────────────────────────────────────────
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .sprintGestureInput(
                    gestureConfig = gestureConfig,
                    onAdvance = onAdvance,
                    onToggleEraser = { isErasing.value = !isErasing.value },
                ),
        ) {
            if (showSprintScrolls.value) {
                SprintScrollConfigPage(
                    currentField = field,
                    selectedSkillTag = selectedSkillTag,
                    densityLevel = densityLevel,
                    modifier = Modifier.fillMaxSize(),
                    onApply = { skill, density, exact ->
                        showSprintScrolls.value = false
                        onApplySprintScrollSelection(skill, density, exact, field)
                    },
                )
            } else {
                // key() forces full recomposition (new InkCanvas + fresh pointerInput) on exercise change.
                // Without it, pointerInput holds a stale strokes reference from the previous exercise.
                key(exerciseRenderKey) {
                    ExerciseField(
                        field = field,
                        isActive = true,
                        backgroundMode = config.backgroundMode,
                        penColor = config.penColor,
                        penWidth = config.penWidth,
                        modifier = Modifier.fillMaxSize(),
                        initialScratchStrokes = fieldScratchStrokes[field.fieldIndex].orEmpty(),
                        initialAnswerStrokes = fieldAnswerStrokes[field.fieldIndex].orEmpty(),
                        initialScratchRedoStack = fieldScratchRedoStacks[field.fieldIndex].orEmpty(),
                        initialAnswerRedoStack = fieldAnswerRedoStacks[field.fieldIndex].orEmpty(),
                        clearSignal = clearSignal.intValue,
                        undoSignal = undoSignal.intValue,
                        redoSignal = redoSignal.intValue,
                        isErasing = isErasing.value,
                        onClick = {},
                        onSyncScratch = { strokes, redoStack ->
                            onSyncScratch(field.fieldIndex, strokes, redoStack)
                        },
                        onSyncAnswer = { strokes, redoStack ->
                            onSyncAnswer(field.fieldIndex, strokes, redoStack)
                        },
                        onPenEvent = { event -> onPenEvent(field.fieldIndex, event) },
                        userGuideMode = config.guideMode,
                    )
                }
            }
            if (!showSprintScrolls.value && config.showEraserButton) {
                EraserToggle(
                    isErasing = isErasing.value,
                    onToggle = { isErasing.value = !isErasing.value },
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = Spacing.sm),
                )
            }
            // EnterSquare flutua no meio vertical, colado na borda direita
            if (!showSprintScrolls.value) {
                EnterSquare(
                    onAdvance = onAdvance,
                    onTripleTap = { showSprintScrolls.value = true },
                    onRegisterVisibilityChange = { showSessionRegister.value = it },
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = Spacing.sm),
                )
            }
            if (!showSprintScrolls.value && showSessionRegister.value) {
                SessionRegisterOverlay(
                    correct = sessionCorrect,
                    total = sessionTotal,
                    startedAtMs = sessionStartedAtMs,
                    currentExercise = sessionTotal + 1,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 92.dp),
                )
            }
            // Histórico visual dos últimos 7 resultados — canto inferior esquerdo
            if (!showSprintScrolls.value && recentResults.isNotEmpty()) {
                ResultHistoryRow(
                    results = recentResults,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = Spacing.md, bottom = Spacing.md),
                )
            }
            // Toggle inline livre/precisa acertar — ícone pequeno no canto superior direito
            // Sincronizado com FolhaSettingsSheet via config.requireCorrectToAdvance
            if (!showSprintScrolls.value) {
                IconButton(
                    onClick = { onConfigChange(config.copy(requireCorrectToAdvance = !config.requireCorrectToAdvance)) },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = Spacing.sm, end = 52.dp)
                        .size(32.dp),
                ) {
                    Icon(
                        imageVector = if (config.requireCorrectToAdvance) Icons.Outlined.Lock else Icons.Outlined.LockOpen,
                        contentDescription = if (config.requireCorrectToAdvance) "Precisa acertar para avançar" else "Avança mesmo errando",
                        tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.30f),
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
}

/**
 * Fila horizontal dos últimos N resultados (máx 7).
 * Mais antigo à esquerda / menor — mais recente à direita / maior.
 * □ acerto · ○ erro · × não preenchido
 */
@Composable
private fun ResultHistoryRow(
    results: List<ResultMark>,
    modifier: Modifier = Modifier,
) {
    val n = results.size.coerceAtLeast(1)
    val minSizeDp = 9f
    val maxSizeDp = 26f

    // Cores pastel alinhadas ao tema SPRINT (neutras, sem distração)
    val colorCorrect = Color(0xFF6B8C5A)   // verde oliva suave
    val colorWrong   = Color(0xFF9E6B5A)   // terracota suave
    val colorEmpty   = Color(0xFFAA9E8C)   // bege acinzentado

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        results.forEachIndexed { i, mark ->
            // index 0 = mais antigo (menor), index n-1 = mais recente (maior)
            val t = if (n > 1) i.toFloat() / (n - 1) else 1f
            val sizeDp = (minSizeDp + t * (maxSizeDp - minSizeDp)).dp
            val strokeDp = (sizeDp.value / 7f).coerceAtLeast(1.2f).dp
            val color = when (mark) {
                ResultMark.CORRECT -> colorCorrect
                ResultMark.WRONG   -> colorWrong
                ResultMark.EMPTY   -> colorEmpty
            }
            Canvas(modifier = Modifier.size(sizeDp)) {
                val sw = strokeDp.toPx()
                when (mark) {
                    ResultMark.CORRECT -> drawRect(
                        color = color,
                        style = Stroke(width = sw),
                    )
                    ResultMark.WRONG -> drawCircle(
                        color = color,
                        style = Stroke(width = sw),
                    )
                    ResultMark.EMPTY -> {
                        val pad = sw
                        drawLine(color, Offset(pad, pad), Offset(size.width - pad, size.height - pad), sw)
                        drawLine(color, Offset(size.width - pad, pad), Offset(pad, size.height - pad), sw)
                    }
                }
            }
        }
    }
}

private val SPRINT_SKILLS = listOf(
    "soma_subtracao" to "soma",
    "multiplicacao_divisao" to "mult",
    "fracoes_decimais" to "frac",
    "porcentagem_razao" to "%",
    "potenciacao_radiciacao" to "pot",
    "equacoes_lineares" to "eq1",
    "sistemas_equacoes" to "sist",
    "fatoracao_produtos_notaveis" to "fat",
    "inequacoes" to "ineq",
    "equacoes_quadraticas" to "eq2",
    "funcao_afim" to "afim",
    "funcao_quadratica" to "quad",
    "trig_razoes" to "trig",
    "derivadas_basicas" to "deriv",
    "integrais_indefinidas" to "int",
)

private val SPRINT_DENSITIES = listOf(
    "high" to "leve",
    "medium" to "fixa",
    "low" to "densa",
)

@Composable
private fun SprintScrollConfigPage(
    currentField: FolhaField,
    selectedSkillTag: String,
    densityLevel: String,
    onApply: (skillTag: String, density: String, exactCurrent: Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val initialSkill = selectedSkillTag.takeIf { tag -> SPRINT_SKILLS.any { it.first == tag } }
        ?: currentField.skillTags.firstOrNull()
        ?: SPRINT_SKILLS.first().first
    val selectedSkill = remember { mutableStateOf(initialSkill) }
    val selectedDensity = remember { mutableStateOf(densityLevel.takeIf { it in listOf("high", "medium", "low") } ?: "medium") }
    // Padrão "tema" — o usuário escolhe a skill no scroll e ela é aplicada.
    // "exato" só faz sentido quando o aluno quer praticar EXATAMENTE o template atual.
    val exactCurrent = remember { mutableStateOf(false) }

    Box(modifier = modifier.background(MaterialTheme.colorScheme.background)) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(28.dp),
        ) {
            SprintScrollRow(
                label = "tema",
                options = SPRINT_SKILLS,
                selectedKey = selectedSkill.value,
                onSelected = { selectedSkill.value = it },
            )
            SprintScrollRow(
                label = "densidade",
                options = SPRINT_DENSITIES,
                selectedKey = selectedDensity.value,
                onSelected = { selectedDensity.value = it },
            )
            SprintScrollRow(
                label = "zoom",
                options = listOf("theme" to "tema", "exact" to "exato"),
                selectedKey = if (exactCurrent.value) "exact" else "theme",
                onSelected = { exactCurrent.value = it == "exact" },
            )
        }
        EnterSquare(
            onAdvance = { onApply(selectedSkill.value, selectedDensity.value, exactCurrent.value) },
            onTripleTap = { onApply(selectedSkill.value, selectedDensity.value, exactCurrent.value) },
            onRegisterVisibilityChange = {},
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = Spacing.sm),
        )
    }
}

@Composable
private fun SprintScrollRow(
    label: String,
    options: List<Pair<String, String>>,
    selectedKey: String,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectedIndex = options.indexOfFirst { it.first == selectedKey }.coerceAtLeast(0)
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = selectedIndex)
    val itemWidth = 82.dp

    LaunchedEffect(listState, options) {
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
                if (index in options.indices) onSelected(options[index].first)
            }
    }

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val sidePadding = ((maxWidth - itemWidth) / 2).coerceAtLeast(0.dp)
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = label,
                fontSize = 9.sp,
                letterSpacing = 0.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.22f),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
            LazyRow(
                state = listState,
                contentPadding = PaddingValues(horizontal = sidePadding),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                itemsIndexed(options) { _, option ->
                    val selected = option.first == selectedKey
                    Box(
                        modifier = Modifier
                            .size(width = itemWidth, height = 38.dp)
                            .drawBehind {
                                val ink = androidx.compose.ui.graphics.Color.Black
                                val alpha = if (selected) 0.24f else 0.07f
                                drawLine(
                                    color = ink.copy(alpha = alpha),
                                    start = androidx.compose.ui.geometry.Offset(size.width * 0.24f, size.height - 6.dp.toPx()),
                                    end = androidx.compose.ui.geometry.Offset(size.width * 0.76f, size.height - 6.dp.toPx()),
                                    strokeWidth = if (selected) 1.4.dp.toPx() else 0.8.dp.toPx(),
                                )
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = option.second,
                            fontSize = if (selected) 18.sp else 12.sp,
                            letterSpacing = 0.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = if (selected) 0.50f else 0.18f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }
}

/**
 * Intercepts multi-touch events (Initial pass) before InkCanvas sees them.
 * - 2-finger tap (quick, no movement) → onAdvance (if configured)
 * - 1-finger interactions fall through untouched to InkCanvas.
 */
private fun Modifier.sprintGestureInput(
    gestureConfig: GestureConfig,
    onAdvance: () -> Unit,
    onToggleEraser: () -> Unit,
): Modifier = pointerInput(gestureConfig, onAdvance, onToggleEraser) {
    val movePx = 20.dp.toPx()
    val tapTimeoutMs = 280L
    val longPressMs = 520L
    awaitPointerEventScope {
        while (true) {
            val down = awaitPointerEvent(PointerEventPass.Initial)
            val pressed = down.changes.filter { it.pressed }
            if (pressed.size == 1 &&
                gestureConfig.gestureFor(GestureConfig.ACTION_TOGGLE_ERASER) ==
                GestureConfig.GESTURE_LONG_PRESS
            ) {
                val first = pressed.first()
                val startPosition = first.position
                val startTime = System.currentTimeMillis()
                var movedTooMuch = false
                var toggled = false

                while (true) {
                    val ev = awaitPointerEvent(PointerEventPass.Initial)
                    val current = ev.changes.firstOrNull { it.id == first.id }
                    if (current == null || !current.pressed) break
                    if ((current.position - startPosition).getDistance() > movePx) {
                        movedTooMuch = true
                    }
                    if (!movedTooMuch && !toggled && System.currentTimeMillis() - startTime >= longPressMs) {
                        ev.changes.forEach { it.consume() }
                        onToggleEraser()
                        toggled = true
                    }
                }
                continue
            }

            if (pressed.size < 2) continue   // 1-finger → InkCanvas handles it

            // 2+ fingers detected — track until all lift
            val startPositions = pressed.map { it.position }
            val startTime = System.currentTimeMillis()
            var movedTooMuch = false

            while (true) {
                val ev = awaitPointerEvent(PointerEventPass.Initial)
                ev.changes.forEachIndexed { i, c ->
                    if (i < startPositions.size &&
                        (c.position - startPositions[i]).getDistance() > movePx) {
                        movedTooMuch = true
                    }
                }
                if (ev.changes.none { it.pressed }) {
                    val duration = System.currentTimeMillis() - startTime
                    if (!movedTooMuch && duration < tapTimeoutMs) {
                        // Confirmed 2-finger tap
                        if (gestureConfig.gestureFor(GestureConfig.ACTION_ADVANCE) ==
                            GestureConfig.GESTURE_TWO_FINGER_TAP) {
                            ev.changes.forEach { it.consume() }
                            onAdvance()
                        }
                    }
                    break
                }
            }
        }
    }
}

@Composable
private fun EraserToggle(
    isErasing: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ink = MaterialTheme.colorScheme.onBackground
    Box(
        modifier = modifier
            .size(38.dp)
            .border(
                width = 1.dp,
                color = ink.copy(alpha = if (isErasing) 0.42f else 0.18f),
                shape = CircleShape,
            )
            .background(
                color = ink.copy(alpha = if (isErasing) 0.14f else 0.04f),
                shape = CircleShape,
            )
            .pointerInput(onToggle) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    onToggle()
                }
            },
    )
}

/**
 * Quadrado de confirmação — qualquer traço dentro dispara o avanço.
 * Fica no canto inferior direito da tela, ao lado da toolbar.
 */
@Composable
private fun EnterSquare(
    onAdvance: () -> Unit,
    onTripleTap: () -> Unit = {},
    onRegisterVisibilityChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val tapCount = remember { mutableIntStateOf(0) }
    val pendingTapJob = remember { mutableStateOf<Job?>(null) }

    Box(
        modifier = modifier
            .size(72.dp)
            .border(
                width = 1.5.dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.60f),
                shape = CircleShape,
            )
            .background(
                color = MaterialTheme.colorScheme.surface,
                shape = CircleShape,
            )
            .pointerInput(onAdvance, onTripleTap, onRegisterVisibilityChange) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val startAt = System.currentTimeMillis()
                    val holdMs = 430L
                    var registerShown = false

                    while (true) {
                        val event = awaitPointerEvent()
                        if (!registerShown && System.currentTimeMillis() - startAt >= holdMs) {
                            registerShown = true
                            event.changes.forEach { it.consume() }
                            onRegisterVisibilityChange(true)
                        }
                        if (event.changes.none { it.pressed }) {
                            if (registerShown) {
                                onRegisterVisibilityChange(false)
                            } else {
                                down.consume()
                                tapCount.intValue += 1
                                pendingTapJob.value?.cancel()
                                if (tapCount.intValue >= 3) {
                                    tapCount.intValue = 0
                                    onTripleTap()
                                } else {
                                    pendingTapJob.value = scope.launch {
                                        delay(320L)
                                        if (tapCount.intValue == 1) {
                                            onAdvance()
                                        }
                                        tapCount.intValue = 0
                                    }
                                }
                            }
                            break
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {}
}

@Composable
private fun SessionRegisterOverlay(
    correct: Int,
    total: Int,
    startedAtMs: Long,
    currentExercise: Int,
    modifier: Modifier = Modifier,
) {
    val now = remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(startedAtMs) {
        while (true) {
            now.longValue = System.currentTimeMillis()
            delay(1000)
        }
    }

    val elapsedSeconds = ((now.longValue - startedAtMs) / 1000).coerceAtLeast(0)
    val minutes = elapsedSeconds / 60
    val seconds = elapsedSeconds % 60
    val accuracy = if (total > 0) ((correct * 100) / total) else 0
    val ink = MaterialTheme.colorScheme.onBackground

    Column(
        modifier = modifier
            .background(
                color = MaterialTheme.colorScheme.background.copy(alpha = 0.92f),
                shape = RoundedCornerShape(8.dp),
            )
            .border(
                width = 1.dp,
                color = ink.copy(alpha = 0.12f),
                shape = RoundedCornerShape(8.dp),
            )
            .padding(horizontal = 12.dp, vertical = 9.dp),
    ) {
        Text(
            text = "$correct/$total · $accuracy%",
            fontSize = 13.sp,
            color = ink.copy(alpha = 0.55f),
        )
        Text(
            text = "${minutes}m ${seconds}s · ex $currentExercise",
            fontSize = 10.sp,
            color = ink.copy(alpha = 0.34f),
        )
    }
}
