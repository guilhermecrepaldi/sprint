package com.strava_matematica.ui.folha

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.strava_matematica.design.FocusColors
import com.strava_matematica.design.Spacing
import com.strava_matematica.model.BackgroundMode
import com.strava_matematica.model.FolhaField
import com.strava_matematica.model.PenEvent
import kotlin.math.hypot


@Composable
fun ExerciseField(
    field: FolhaField,
    isActive: Boolean,
    backgroundMode: BackgroundMode,
    penColor: String,
    modifier: Modifier = Modifier,
    // Split-canvas params
    initialScratchStrokes: List<List<Offset>> = emptyList(),
    initialAnswerStrokes: List<List<Offset>> = emptyList(),
    initialScratchRedoStack: List<List<Offset>> = emptyList(),
    initialAnswerRedoStack: List<List<Offset>> = emptyList(),
    clearSignal: Int = 0,
    undoSignal: Int = 0,
    redoSignal: Int = 0,
    onClick: () -> Unit = {},
    onSyncScratch: (List<List<Offset>>, List<List<Offset>>) -> Unit = { _, _ -> },
    onSyncAnswer: (List<List<Offset>>, List<List<Offset>>) -> Unit = { _, _ -> },
    // Legacy alias: callers that still pass onSyncStrokes are wired to the answer canvas
    onSyncStrokes: (List<List<Offset>>, List<List<Offset>>) -> Unit = { _, _ -> },
    onPenEvent: (PenEvent) -> Unit = {},
) {
    val fieldColor = if (backgroundMode == BackgroundMode.DARK) FocusColors.DarkField else FocusColors.WhiteField
    val surfaceColor = if (backgroundMode == BackgroundMode.DARK) FocusColors.DarkSurface else FocusColors.WhiteSurface
    val hairline = if (backgroundMode == BackgroundMode.DARK) FocusColors.DarkHairline else FocusColors.WhiteHairline
    val borderColor = if (isActive) MaterialTheme.colorScheme.primary else hairline

    // Merge legacy onSyncStrokes into onSyncAnswer so old callers still work.
    val answerSync: (List<List<Offset>>, List<List<Offset>>) -> Unit = { s, r ->
        onSyncAnswer(s, r)
        onSyncStrokes(s, r)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(surfaceColor, RoundedCornerShape(8.dp))
            .border(BorderStroke(1.dp, borderColor), RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(Spacing.md),
    ) {
        // ── Header ──────────────────────────────────────────────────────────
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "%02d".format(field.fieldIndex + 1),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
            )
            Spacer(Modifier.width(Spacing.sm))
            Text(
                text = field.skillTags.firstOrNull()?.replace("_", " ") ?: "exercício",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.50f),
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = field.statement,
                style = MaterialTheme.typography.titleMedium,
            )
        }
        Spacer(Modifier.height(Spacing.sm))

        // ── Scratch area — 65% of remaining height ───────────────────────
        Box(
            modifier = Modifier
                .weight(0.65f)
                .fillMaxWidth()
                .background(fieldColor, RoundedCornerShape(4.dp))
                .border(BorderStroke(1.dp, hairline.copy(alpha = 0.65f)), RoundedCornerShape(4.dp)),
        ) {
            InkCanvas(
                modifier = Modifier.matchParentSize().padding(Spacing.xs),
                penColor = penColor,
                enabled = isActive,
                clearSignal = clearSignal,
                undoSignal = undoSignal,
                redoSignal = redoSignal,
                initialStrokes = initialScratchStrokes,
                initialRedoStack = initialScratchRedoStack,
                onSyncStrokes = onSyncScratch,
                onPenEvent = onPenEvent,
            )
        }

        Spacer(Modifier.height(Spacing.sm))
        Text(
            text = "Resposta final",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
        )
        Spacer(Modifier.height(Spacing.xs))

        // ── Answer box — 35% of remaining height, primary border ─────────
        Box(
            modifier = Modifier
                .weight(0.35f)
                .fillMaxWidth()
                .background(fieldColor, RoundedCornerShape(4.dp))
                .border(BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary), RoundedCornerShape(4.dp)),
        ) {
            InkCanvas(
                modifier = Modifier.matchParentSize().padding(Spacing.xs),
                penColor = penColor,
                enabled = isActive,
                clearSignal = clearSignal,
                undoSignal = 0,   // undo/redo operate only on scratch
                redoSignal = 0,
                initialStrokes = initialAnswerStrokes,
                initialRedoStack = initialAnswerRedoStack,
                onSyncStrokes = answerSync,
                onPenEvent = onPenEvent,
            )
        }
    }
}

@Composable
fun InkCanvas(
    modifier: Modifier = Modifier,
    penColor: String = "#1a1a1a",
    penWidth: Float = 2.2f,
    enabled: Boolean = true,
    clearSignal: Int = 0,
    undoSignal: Int = 0,
    redoSignal: Int = 0,
    initialStrokes: List<List<Offset>> = emptyList(),
    initialRedoStack: List<List<Offset>> = emptyList(),
    onSyncStrokes: (List<List<Offset>>, List<List<Offset>>) -> Unit = { _, _ -> },
    onPenEvent: (PenEvent) -> Unit = {},
) {
    val strokes = remember(initialStrokes) { mutableStateListOf(*initialStrokes.toTypedArray()) }
    val redoStack = remember(initialRedoStack) { mutableStateListOf(*initialRedoStack.toTypedArray()) }
    val inkColor = remember(penColor) { parseHexColor(penColor) }
    var currentStroke = remember { mutableListOf<Offset>() }
    var strokeStartedAt = remember { 0L }
    var previousPoint = remember { Offset.Zero }
    var previousUptime = remember { 0L }

    LaunchedEffect(clearSignal) {
        if (clearSignal > 0) {
            strokes.clear()
            redoStack.clear()
            onSyncStrokes(strokes.toList(), redoStack.toList())
            onPenEvent(Offset.Zero.toPenEvent(System.currentTimeMillis(), 0f, "clear"))
        }
    }

    LaunchedEffect(undoSignal) {
        if (undoSignal > 0 && strokes.isNotEmpty()) {
            redoStack.add(strokes.removeAt(strokes.lastIndex))
            onSyncStrokes(strokes.toList(), redoStack.toList())
            onPenEvent(Offset.Zero.toPenEvent(System.currentTimeMillis(), 0f, "undo"))
        }
    }

    LaunchedEffect(redoSignal) {
        if (redoSignal > 0 && redoStack.isNotEmpty()) {
            strokes.add(redoStack.removeAt(redoStack.lastIndex))
            onSyncStrokes(strokes.toList(), redoStack.toList())
            onPenEvent(Offset.Zero.toPenEvent(System.currentTimeMillis(), 0f, "redo"))
        }
    }

    Canvas(
        modifier = modifier.pointerInput(enabled, penColor, penWidth) {
            if (!enabled) return@pointerInput
            detectDragGestures(
                onDragStart = { offset ->
                    strokeStartedAt = System.currentTimeMillis()
                    previousUptime = strokeStartedAt
                    previousPoint = offset
                    currentStroke = mutableListOf(offset)
                    redoStack.clear()
                    strokes.add(currentStroke.toList())
                    onPenEvent(offset.toPenEvent(strokeStartedAt, 0f, "stroke_start"))
                },
                onDrag = { change, _ ->
                    change.consume()
                    val now = System.currentTimeMillis()
                    val offset = change.position
                    val elapsed = (now - previousUptime).coerceAtLeast(1L)
                    val distance = hypot(offset.x - previousPoint.x, offset.y - previousPoint.y)
                    val velocity = distance / elapsed
                    currentStroke.add(offset)
                    if (strokes.isNotEmpty()) {
                        strokes[strokes.lastIndex] = currentStroke.toList()
                    }
                    previousPoint = offset
                    previousUptime = now
                    onPenEvent(offset.toPenEvent(strokeStartedAt, velocity, "stroke_move"))
                },
                onDragEnd = {
                    if (currentStroke.isNotEmpty()) {
                        onSyncStrokes(strokes.toList(), redoStack.toList())
                        onPenEvent(currentStroke.last().toPenEvent(strokeStartedAt, 0f, "stroke_end"))
                    }
                },
            )
        },
    ) {
        val y = size.height * 0.72f
        drawLine(
            color = Color.Gray.copy(alpha = 0.35f),
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = 1.dp.toPx(),
        )
        strokes.forEach { stroke ->
            if (stroke.size == 1) {
                drawCircle(color = inkColor, radius = penWidth.dp.toPx() / 2f, center = stroke.first())
            } else if (stroke.size > 1) {
                val path = Path().apply {
                    moveTo(stroke.first().x, stroke.first().y)
                    stroke.drop(1).forEach { lineTo(it.x, it.y) }
                }
                drawPath(
                    path = path,
                    color = inkColor,
                    style = Stroke(width = penWidth.dp.toPx(), cap = StrokeCap.Round),
                )
            }
        }
    }
}

private fun Offset.toPenEvent(strokeStartedAt: Long, velocity: Float, eventType: String): PenEvent {
    return PenEvent(
        ts = (System.currentTimeMillis() - strokeStartedAt).coerceAtLeast(0L),
        x = x,
        y = y,
        pressure = null,
        tilt = null,
        velocity = velocity,
        eventType = eventType,
    )
}

private fun parseHexColor(hex: String): Color {
    return runCatching { Color(android.graphics.Color.parseColor(hex)) }
        .getOrDefault(Color(0xFF1A1A1A))
}
