package com.sprint.ui.folha

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.sprint.model.PenEvent
import kotlin.math.hypot

/**
 * Ink canvas with full gesture support: stylus, eraser, undo/redo, guide modes.
 *
 * Extracted from ExerciseField.kt to eliminate duplication with NoteCanvas/ZoomableCanvas.
 *
 * Supported guide modes:
 * - "lined" — horizontal lines like notebook paper
 * - "dots" — dot grid
 * - "single" — single baseline (default)
 * - "none" — blank canvas
 */
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
    onTap: (() -> Unit)? = null,
) {
    val strokes = remember { mutableStateListOf(*initialStrokes.toTypedArray()) }
    val redoStack = remember { mutableStateListOf(*initialRedoStack.toTypedArray()) }
    
    LaunchedEffect(initialStrokes) {
        if (initialStrokes != strokes.toList()) {
            strokes.clear()
            strokes.addAll(initialStrokes)
        }
    }
    LaunchedEffect(initialRedoStack) {
        if (initialRedoStack != redoStack.toList()) {
            redoStack.clear()
            redoStack.addAll(initialRedoStack)
        }
    }
    
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
                val event = awaitPointerEvent(PointerEventPass.Initial)
                val down = event.changes.firstOrNull { it.pressed } ?: return@awaitEachGesture
                
                val type = down.type
                val isPen = type == androidx.compose.ui.input.pointer.PointerType.Stylus || 
                            type == androidx.compose.ui.input.pointer.PointerType.Eraser ||
                            type == androidx.compose.ui.input.pointer.PointerType.Mouse ||
                            type == androidx.compose.ui.input.pointer.PointerType.Touch
                if (!isPen) return@awaitEachGesture
                down.consume()

                val motionEvent = try {
                    val field = event.javaClass.getDeclaredField("motionEvent")
                    field.isAccessible = true
                    field.get(event) as? android.view.MotionEvent
                } catch (e: Exception) { null }
                val isStylusButton = motionEvent?.let {
                    (it.buttonState and android.view.MotionEvent.BUTTON_STYLUS_PRIMARY != 0) ||
                    (it.buttonState and android.view.MotionEvent.BUTTON_SECONDARY != 0)
                } ?: false
                val activeErasing = isErasing || type == androidx.compose.ui.input.pointer.PointerType.Eraser || isStylusButton

                if (activeErasing) {
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
                    val loopEvent = awaitPointerEvent(PointerEventPass.Initial)
                    val change = loopEvent.changes.firstOrNull { it.id == down.id } ?: break
                    val motionEventLoop = try {
                        val field = loopEvent.javaClass.getDeclaredField("motionEvent")
                        field.isAccessible = true
                        field.get(loopEvent) as? android.view.MotionEvent
                    } catch (e: Exception) { null }
                    val isStylusButtonLoop = motionEventLoop?.let {
                        (it.buttonState and android.view.MotionEvent.BUTTON_STYLUS_PRIMARY != 0) ||
                        (it.buttonState and android.view.MotionEvent.BUTTON_SECONDARY != 0)
                    } ?: false
                    val activeErasingLoop = isErasing || type == androidx.compose.ui.input.pointer.PointerType.Eraser || isStylusButtonLoop

                    if (activeErasingLoop) {
                        if (change.pressed) { eraseNear(change.position, strokes, eraserRadiusPx); change.consume() }
                        else { onSyncStrokes(strokes.toList(), redoStack.toList()); break }
                        continue
                    }
                    if (change.position != previousPoint) {
                        val now = System.currentTimeMillis()
                        val offset = change.position
                        val elapsed = (now - previousUptime).coerceAtLeast(1L)
                        val distance = hypot(offset.x - previousPoint.x, offset.y - previousPoint.y)
                        val velocity = distance / elapsed
                        currentStroke.add(offset)
                        if (strokes.isNotEmpty()) strokes[strokes.lastIndex] = currentStroke.toList()
                        previousPoint = offset
                        previousUptime = now
                        onPenEvent(offset.toPenEvent(strokeStartedAt, velocity, "stroke_move", change.pressure))
                    }
                    change.consume()
                    if (!change.pressed) {
                        if (currentStroke.isNotEmpty())
                            onPenEvent(currentStroke.last().toPenEvent(strokeStartedAt, 0f, "stroke_end"))
                        val duration = System.currentTimeMillis() - strokeStartedAt
                        val dist = if (currentStroke.isNotEmpty()) hypot(
                            currentStroke.last().x - currentStroke.first().x,
                            currentStroke.last().y - currentStroke.first().y
                        ) else 0f
                        val isTap = duration < 320L && dist < 12.dp.toPx()
                        if (isTap && onTap != null) {
                            if (strokes.isNotEmpty()) strokes.removeAt(strokes.lastIndex)
                            onSyncStrokes(strokes.toList(), redoStack.toList())
                            onTap()
                        } else onSyncStrokes(strokes.toList(), redoStack.toList())
                        break
                    }
                }
            }
        },
    ) {
        when (guideMode) {
            "lined" -> {
                val gap = 32.dp.toPx(); var y = gap
                while (y < size.height) {
                    drawLine(Color.Gray.copy(alpha = 0.22f), Offset(0f, y), Offset(size.width, y), 1.dp.toPx())
                    y += gap
                }
            }
            "dots" -> {
                val gap = 28.dp.toPx(); var x = gap / 2
                while (x < size.width) { var y = gap / 2
                    while (y < size.height) {
                        drawCircle(inkColor.copy(alpha = 0.12f), 1.5f, Offset(x, y)); y += gap
                    }; x += gap
                }
            }
            else -> {
                val y = size.height * 0.72f
                drawLine(Color.Gray.copy(alpha = 0.35f), Offset(0f, y), Offset(size.width, y), 1.dp.toPx())
            }
        }
        strokes.forEach { stroke ->
            if (stroke.size == 1) drawCircle(inkColor, penWidth.dp.toPx() / 2f, stroke.first())
            else if (stroke.size > 1) {
                drawPath(Path().apply {
                    moveTo(stroke.first().x, stroke.first().y)
                    stroke.drop(1).forEach { lineTo(it.x, it.y) }
                }, inkColor, style = Stroke(width = penWidth.dp.toPx(), cap = StrokeCap.Round))
            }
        }
    }
}

/**
 * Split canvas with scratch (top) and answer (bottom) areas.
 * Uses [InkCanvas] for each area and [SplitHeightHandle] for resizing.
 */
@Composable
fun InkCanvasSplit(
    modifier: Modifier = Modifier,
    scratchRatio: Float = 0.7f,
    penColor: String = "#1a1a1a",
    penWidth: Float = 2.2f,
    isErasing: Boolean = false,
    clearSignal: Int = 0,
    undoSignal: Int = 0,
    redoSignal: Int = 0,
    initialScratchStrokes: List<List<Offset>> = emptyList(),
    initialAnswerStrokes: List<List<Offset>> = emptyList(),
    initialScratchRedoStack: List<List<Offset>> = emptyList(),
    initialAnswerRedoStack: List<List<Offset>> = emptyList(),
    guideMode: String = "single",
    onScratchChanged: (List<List<Offset>>, List<List<Offset>>) -> Unit = { _, _ -> },
    onAnswerChanged: (List<List<Offset>>, List<List<Offset>>) -> Unit = { _, _ -> },
    onPenEvent: (PenEvent) -> Unit = {},
) {
    var ratio by remember { mutableFloatStateOf(scratchRatio) }
    val inkColor = remember(penColor) { parseHexColor(penColor) }

    Column(modifier = modifier.fillMaxWidth()) {
        // Scratch area (top)
        Box(modifier = Modifier.weight(ratio).fillMaxWidth().padding(bottom = 4.dp)) {
            InkCanvas(
                modifier = Modifier.fillMaxSize(),
                penColor = penColor,
                penWidth = penWidth,
                isErasing = isErasing,
                clearSignal = clearSignal,
                undoSignal = undoSignal,
                redoSignal = redoSignal,
                initialStrokes = initialScratchStrokes,
                initialRedoStack = initialScratchRedoStack,
                guideMode = guideMode,
                onSyncStrokes = onScratchChanged,
                onPenEvent = onPenEvent,
            )
        }
        // Divider handle
        SplitHeightHandle(
            ink = inkColor,
            ratio = ratio,
            onRatioChange = { ratio = it },
            modifier = Modifier.fillMaxWidth(),
        )
        // Answer area (bottom)
        Box(modifier = Modifier.weight(1f - ratio).fillMaxWidth().padding(top = 4.dp)) {
            InkCanvas(
                modifier = Modifier.fillMaxSize(),
                penColor = penColor,
                penWidth = penWidth,
                isErasing = isErasing,
                clearSignal = clearSignal,
                undoSignal = undoSignal,
                redoSignal = redoSignal,
                initialStrokes = initialAnswerStrokes,
                initialRedoStack = initialAnswerRedoStack,
                guideMode = guideMode,
                onSyncStrokes = onAnswerChanged,
                onPenEvent = onPenEvent,
            )
        }
    }
}

internal fun Offset.toPenEvent(
    strokeStartedAt: Long, velocity: Float, eventType: String, pressure: Float? = null
): PenEvent = PenEvent(
    ts = (System.currentTimeMillis() - strokeStartedAt).coerceAtLeast(0L),
    x = x, y = y, pressure = pressure, tilt = null, velocity = velocity, eventType = eventType,
)

internal fun eraseNear(point: Offset, strokes: MutableList<List<Offset>>, radiusPx: Float) {
    val r2 = radiusPx * radiusPx
    strokes.removeAll { stroke -> stroke.any { pt ->
        val dx = pt.x - point.x; val dy = pt.y - point.y; dx * dx + dy * dy <= r2
    }}
}

internal fun parseHexColor(hex: String): Color =
    runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrDefault(Color(0xFF1A1A1A))
