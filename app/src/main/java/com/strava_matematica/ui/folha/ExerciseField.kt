package com.strava_matematica.ui.folha

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    penWidth: Float = 2.2f,
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
    isErasing: Boolean = false,
    userGuideMode: String = "nenhuma",
) {
    val isDark = backgroundMode == BackgroundMode.DARK
    val fieldColor = MaterialTheme.colorScheme.background
    val surfaceColor = MaterialTheme.colorScheme.background
    // Single ink color — everything reads from here, varying only alpha
    val ink = if (isDark) FocusColors.DarkTextPrimary else FocusColors.WhiteTextPrimary
    val isFullPage = field.canvasMode == "full_page"
    val isLined = field.canvasMode == "lined"
    val context = LocalContext.current
    val defaultScratchRatio = if (isLined) 0.80f else if (field.statement.length > 90) 0.66f else 0.75f
    val scratchRatio = remember(field.fieldIndex) {
        mutableFloatStateOf(SplitRatioPrefs.get(context, field.fieldIndex, defaultScratchRatio))
    }
    // Map config.guideMode → InkCanvas guideMode values. "nenhuma" defers to field.canvasMode logic.
    val mappedGuideMode: String? = when (userGuideMode) {
        "horizontal", "grade" -> "lined"
        "dots" -> "dots"
        else -> null   // "nenhuma" — use field-based default
    }

    // Merge legacy onSyncStrokes into onSyncAnswer so old callers still work.
    val hasAnswerStroke = remember(initialAnswerStrokes) { mutableStateOf(initialAnswerStrokes.isNotEmpty()) }
    val answerSync: (List<List<Offset>>, List<List<Offset>>) -> Unit = { s, r ->
        if (s.isNotEmpty()) hasAnswerStroke.value = true
        onSyncAnswer(s, r)
        onSyncStrokes(s, r)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(surfaceColor, RoundedCornerShape(8.dp))
            .padding(Spacing.md),
    ) {
        // ── Statement ────────────────────────────────────────────────────────
        Text(
            text = renderLatex(field.statement),
            fontSize = if (isActive) 24.sp else 22.sp,
            lineHeight = if (isActive) 32.sp else 30.sp,
            fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
            color = ink,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(Spacing.sm))

        if (!isFullPage) {
            // ── Scratch area ─────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .weight(scratchRatio.floatValue)
                    .fillMaxWidth()
                    .background(fieldColor, RoundedCornerShape(4.dp)),
            ) {
                InkCanvas(
                    modifier = Modifier.matchParentSize().padding(Spacing.xs),
                    penColor = penColor,
                    penWidth = penWidth,
                    enabled = isActive,
                    isErasing = isErasing,
                    clearSignal = clearSignal,
                    undoSignal = undoSignal,
                    redoSignal = redoSignal,
                    initialStrokes = initialScratchStrokes,
                    initialRedoStack = initialScratchRedoStack,
                    guideMode = mappedGuideMode ?: if (isLined) "lined" else "dots",
                    onSyncStrokes = onSyncScratch,
                    onPenEvent = onPenEvent,
                )
            }
            SplitHeightHandle(
                ink = ink,
                ratio = scratchRatio.floatValue,
                onRatioChange = {
                    scratchRatio.floatValue = it
                    SplitRatioPrefs.set(context, field.fieldIndex, it)
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        // ── Answer box ───────────────────────────────────────────────────
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .weight(if (isFullPage) 1f else 1f - scratchRatio.floatValue)
                .fillMaxWidth()
                .background(fieldColor, RoundedCornerShape(4.dp)),
        ) {
            InkCanvas(
                modifier = Modifier.matchParentSize().padding(Spacing.xs),
                penColor = penColor,
                penWidth = penWidth,
                enabled = isActive,
                isErasing = isErasing,
                clearSignal = clearSignal,
                undoSignal = if (isFullPage) undoSignal else 0,
                redoSignal = if (isFullPage) redoSignal else 0,
                initialStrokes = initialAnswerStrokes,
                initialRedoStack = initialAnswerRedoStack,
                guideMode = mappedGuideMode ?: if (isFullPage || isLined) "lined" else "dots",
                onSyncStrokes = answerSync,
                onPenEvent = onPenEvent,
            )
            if (!isFullPage && !hasAnswerStroke.value) {
                Text(
                    text = "resposta",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Light,
                    color = ink.copy(alpha = 0.18f),
                )
            }
        }
    }
}

@Composable
private fun SplitHeightHandle(
    ink: Color,
    ratio: Float,
    onRatioChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(30.dp)
            .drawBehind {
                val y = size.height / 2f
                drawLine(
                    color = ink.copy(alpha = 0.14f),
                    start = Offset(30.dp.toPx(), y),
                    end = Offset(size.width, y),
                    strokeWidth = 1.dp.toPx(),
                )
            },
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .size(28.dp)
                .pointerInput(ratio) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val delta = dragAmount.y / 420f
                        onRatioChange((ratio + delta).coerceIn(0.42f, 0.88f))
                    }
                }
                .drawBehind {
                    drawCircle(
                        color = ink.copy(alpha = 0.24f),
                        radius = 8.dp.toPx(),
                        center = Offset(size.width / 2f, size.height / 2f),
                    )
                },
        )
    }
}

@Composable
fun InkCanvas(
    modifier: Modifier = Modifier,
    penColor: String = "#1a1a1a",
    penWidth: Float = 2.2f,
    enabled: Boolean = true,
    isErasing: Boolean = false,
    clearSignal: Int = 0,
    undoSignal: Int = 0,
    redoSignal: Int = 0,
    initialStrokes: List<List<Offset>> = emptyList(),
    initialRedoStack: List<List<Offset>> = emptyList(),
    guideMode: String = "single",
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
        modifier = modifier.pointerInput(enabled, penColor, penWidth, isErasing) {
            if (!enabled) return@pointerInput
            val eraserRadiusPx = 24.dp.toPx()
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                down.consume()

                if (isErasing) {
                    eraseNear(down.position, strokes, eraserRadiusPx)
                } else {
                    strokeStartedAt = System.currentTimeMillis()
                    previousUptime = strokeStartedAt
                    previousPoint = down.position
                    currentStroke = mutableListOf(down.position)
                    redoStack.clear()
                    strokes.add(currentStroke.toList())
                    onPenEvent(down.position.toPenEvent(strokeStartedAt, 0f, "stroke_start", down.pressure))
                }

                while (true) {
                    val event = awaitPointerEvent()
                    val change = event.changes.firstOrNull { it.id == down.id } ?: break

                    if (isErasing) {
                        if (change.pressed) {
                            eraseNear(change.position, strokes, eraserRadiusPx)
                            change.consume()
                        } else {
                            onSyncStrokes(strokes.toList(), redoStack.toList())
                            break
                        }
                        continue
                    }

                    if (change.position != previousPoint) {
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
                        onPenEvent(offset.toPenEvent(strokeStartedAt, velocity, "stroke_move", change.pressure))
                    }
                    change.consume()

                    if (!change.pressed) {
                        onSyncStrokes(strokes.toList(), redoStack.toList())
                        if (currentStroke.isNotEmpty()) {
                            onPenEvent(currentStroke.last().toPenEvent(strokeStartedAt, 0f, "stroke_end"))
                        }
                        break
                    }
                }
            }
        },
    ) {
        when (guideMode) {
            "lined" -> {
                val gap = 32.dp.toPx()
                var y = gap
                while (y < size.height) {
                    drawLine(
                        color = Color.Gray.copy(alpha = 0.22f),
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1.dp.toPx(),
                    )
                    y += gap
                }
            }
            "dots" -> {
                val gap = 28.dp.toPx()
                var x = gap / 2
                while (x < size.width) {
                    var y = gap / 2
                    while (y < size.height) {
                        drawCircle(
                            color = inkColor.copy(alpha = 0.12f),
                            radius = 1.5f,
                            center = Offset(x, y),
                        )
                        y += gap
                    }
                    x += gap
                }
            }
            else -> {
                val y = size.height * 0.72f
                drawLine(
                    color = Color.Gray.copy(alpha = 0.35f),
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1.dp.toPx(),
                )
            }
        }
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

private fun Offset.toPenEvent(
    strokeStartedAt: Long,
    velocity: Float,
    eventType: String,
    pressure: Float? = null,
): PenEvent {
    return PenEvent(
        ts = (System.currentTimeMillis() - strokeStartedAt).coerceAtLeast(0L),
        x = x,
        y = y,
        pressure = pressure,
        tilt = null,
        velocity = velocity,
        eventType = eventType,
    )
}

private fun eraseNear(point: Offset, strokes: MutableList<List<Offset>>, radiusPx: Float) {
    val r2 = radiusPx * radiusPx
    strokes.removeAll { stroke ->
        stroke.any { pt ->
            val dx = pt.x - point.x
            val dy = pt.y - point.y
            dx * dx + dy * dy <= r2
        }
    }
}

private fun parseHexColor(hex: String): Color {
    return runCatching { Color(android.graphics.Color.parseColor(hex)) }
        .getOrDefault(Color(0xFF1A1A1A))
}
