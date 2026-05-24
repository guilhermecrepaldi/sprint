package com.strava_matematica.ui.folha

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.strava_matematica.design.FocusColors
import com.strava_matematica.design.Spacing
import com.strava_matematica.model.BackgroundMode
import com.strava_matematica.model.Folha
import com.strava_matematica.model.PenEvent
import com.strava_matematica.model.SessionConfig
import com.strava_matematica.ui.components.ThermometerView

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
    onSyncScratch: (fieldIndex: Int, strokes: List<List<Offset>>, redoStack: List<List<Offset>>) -> Unit = { _, _, _ -> },
    onSyncAnswer: (fieldIndex: Int, strokes: List<List<Offset>>, redoStack: List<List<Offset>>) -> Unit = { _, _, _ -> },
    onPenEvent: (fieldIndex: Int, event: PenEvent) -> Unit = { _, _ -> },
    onConfigChange: (SessionConfig) -> Unit = {},
) {
    val field = folha.fields[currentExerciseIndex]
    // Signals reset whenever the exercise changes, giving each field a fresh canvas state.
    var clearSignal by remember(currentExerciseIndex) { mutableIntStateOf(0) }
    var undoSignal by remember(currentExerciseIndex) { mutableIntStateOf(0) }
    var redoSignal by remember(currentExerciseIndex) { mutableIntStateOf(0) }
    var showSettings by remember { mutableStateOf(false) }
    val hairline = if (config.backgroundMode == BackgroundMode.DARK) FocusColors.DarkHairline else FocusColors.WhiteHairline

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        // ── Top bar ──────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = Spacing.md, vertical = Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            IconButton(onClick = {}) { Icon(Icons.Outlined.Pause, contentDescription = "Pausar") }
            Column {
                Text(
                    "Pág.",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.54f),
                )
                Text("%02d".format(folha.pageIndex + 1), style = MaterialTheme.typography.titleMedium)
            }
            Column {
                Text(
                    "Dif.",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.54f),
                )
                Text("%.1f".format(folha.difficulty), style = MaterialTheme.typography.titleMedium)
            }
            Column {
                Text(
                    "Ex.",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.54f),
                )
                Text(
                    "${currentExerciseIndex + 1}/${folha.fields.size}",
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            if (config.showThermometer) {
                ThermometerView(value = 0.72, modifier = Modifier.weight(1f))
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }
            IconButton(onClick = { showSettings = true }) {
                Icon(Icons.Outlined.Settings, contentDescription = "Configurações")
            }
        }
        HorizontalDivider(color = hairline, thickness = 1.dp)

        // ── Full-screen exercise ─────────────────────────────────────────────
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            ExerciseField(
                field = field,
                isActive = true,
                backgroundMode = config.backgroundMode,
                penColor = config.penColor,
                modifier = Modifier.fillMaxSize(),
                initialScratchStrokes = fieldScratchStrokes[field.fieldIndex].orEmpty(),
                initialAnswerStrokes = fieldAnswerStrokes[field.fieldIndex].orEmpty(),
                initialScratchRedoStack = fieldScratchRedoStacks[field.fieldIndex].orEmpty(),
                initialAnswerRedoStack = fieldAnswerRedoStacks[field.fieldIndex].orEmpty(),
                clearSignal = clearSignal,
                undoSignal = undoSignal,
                redoSignal = redoSignal,
                onClick = {},
                onSyncScratch = { strokes, redoStack ->
                    onSyncScratch(field.fieldIndex, strokes, redoStack)
                },
                onSyncAnswer = { strokes, redoStack ->
                    onSyncAnswer(field.fieldIndex, strokes, redoStack)
                },
                onPenEvent = { event -> onPenEvent(field.fieldIndex, event) },
            )
            // Transparent overlay: detects the advance gestures without
            // blocking pen drawing below.
            AdvanceGestureOverlay(
                modifier = Modifier.fillMaxSize(),
                onAdvance = onAdvance,
            )
        }

        // ── Ink toolbar ──────────────────────────────────────────────────────
        InkToolbar(
            onUndo = { undoSignal += 1 },
            onRedo = { redoSignal += 1 },
            onClear = { clearSignal += 1 },
        )
    }

    // ── Settings bottom sheet ─────────────────────────────────────────────
    if (showSettings) {
        FolhaSettingsSheet(
            config = config,
            onConfigChange = onConfigChange,
            onDismiss = { showSettings = false },
        )
    }
}

/**
 * Transparent overlay that detects two advance gestures:
 *   • 2-finger horizontal swipe ≥ 80 dp
 *   • 3 rapid taps (finger downs) within 600 ms
 *
 * Single-touch events are NOT consumed so the InkCanvas below can still draw.
 */
@Composable
private fun AdvanceGestureOverlay(
    modifier: Modifier = Modifier,
    onAdvance: () -> Unit,
) {
    Box(
        modifier = modifier.pointerInput(onAdvance) {
            val tapTimes = ArrayDeque<Long>()
            val pointerStarts = mutableMapOf<PointerId, Float>()

            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent()
                    val now = System.currentTimeMillis()

                    // Track each pointer's starting X for swipe distance calculation.
                    event.changes.forEach { ch ->
                        if (ch.changedToDown()) pointerStarts[ch.id] = ch.position.x
                        else if (!ch.pressed) pointerStarts.remove(ch.id)
                    }

                    val pressed = event.changes.filter { it.pressed }

                    if (pressed.size >= 2) {
                        // ── 2-finger horizontal swipe ────────────────────────
                        val maxDx = pressed.mapNotNull { ch ->
                            pointerStarts[ch.id]?.let { startX ->
                                kotlin.math.abs(ch.position.x - startX)
                            }
                        }.maxOrNull() ?: 0f

                        if (maxDx > 80.dp.toPx()) {
                            event.changes.forEach { it.consume() }
                            pointerStarts.clear()
                            tapTimes.clear()
                            onAdvance()
                        }
                        // Multi-touch breaks any in-progress tap sequence.
                        if (event.changes.any { it.changedToDown() }) tapTimes.clear()
                    } else {
                        // ── 3-tap sequence ───────────────────────────────────
                        if (event.changes.any { it.changedToDown() }) {
                            while (tapTimes.isNotEmpty() && now - tapTimes.first() > 600L) {
                                tapTimes.removeFirst()
                            }
                            tapTimes.addLast(now)
                            if (tapTimes.size >= 3) {
                                tapTimes.clear()
                                onAdvance()
                            }
                        }
                    }
                }
            }
        },
    )
}
