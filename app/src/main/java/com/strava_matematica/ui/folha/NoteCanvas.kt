package com.strava_matematica.ui.folha

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Minimal ink canvas for sprint notes.
 * No undo, no erase, no answer split — just raw writing.
 * Caller receives strokes on [onStrokesChanged] for persistence.
 */
@Composable
fun NoteCanvas(
    modifier: Modifier = Modifier,
    penColor: Color,
    penWidth: Dp = 2.dp,
    initialStrokes: List<List<Offset>> = emptyList(),
    onStrokesChanged: (List<List<Offset>>) -> Unit = {},
) {
    val strokes = remember(initialStrokes) { mutableStateListOf<List<Offset>>().also { it.addAll(initialStrokes) } }
    val current = remember { mutableStateListOf<Offset>() }

    Canvas(
        modifier = modifier.pointerInput(penColor, penWidth) {
            detectDragGestures(
                onDragStart = { offset ->
                    current.clear()
                    current.add(offset)
                },
                onDrag = { change, _ ->
                    change.consume()
                    current.add(change.position)
                },
                onDragEnd = {
                    if (current.isNotEmpty()) {
                        strokes.add(current.toList())
                        current.clear()
                        onStrokesChanged(strokes.toList())
                    }
                },
            )
        },
    ) {
        val strokeStyle = Stroke(
            width = penWidth.toPx(),
            cap = StrokeCap.Round,
            join = StrokeJoin.Round,
        )
        // Committed strokes
        strokes.forEach { pts ->
            if (pts.size < 2) return@forEach
            drawPath(
                path = Path().apply {
                    moveTo(pts[0].x, pts[0].y)
                    pts.drop(1).forEach { lineTo(it.x, it.y) }
                },
                color = penColor,
                style = strokeStyle,
            )
        }
        // In-progress stroke
        if (current.size >= 2) {
            drawPath(
                path = Path().apply {
                    moveTo(current[0].x, current[0].y)
                    current.drop(1).forEach { lineTo(it.x, it.y) }
                },
                color = penColor,
                style = strokeStyle,
            )
        }
    }
}
