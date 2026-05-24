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
    onClick: () -> Unit,
    onPenEvent: (PenEvent) -> Unit = {},
) {
    val fieldColor = if (backgroundMode == BackgroundMode.DARK) FocusColors.DarkField else FocusColors.WhiteField
    val surfaceColor = if (backgroundMode == BackgroundMode.DARK) FocusColors.DarkSurface else FocusColors.WhiteSurface
    val hairline = if (backgroundMode == BackgroundMode.DARK) FocusColors.DarkHairline else FocusColors.WhiteHairline
    val borderColor = if (isActive) MaterialTheme.colorScheme.primary else {
        hairline
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(190.dp)
            .background(surfaceColor, RoundedCornerShape(8.dp))
            .border(BorderStroke(1.dp, borderColor), RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(Spacing.md),
    ) {
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
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(fieldColor, RoundedCornerShape(4.dp))
                .border(BorderStroke(1.dp, hairline.copy(alpha = 0.65f)), RoundedCornerShape(4.dp)),
        ) {
            InkCanvas(
                modifier = Modifier.matchParentSize().padding(Spacing.xs),
                penColor = penColor,
                enabled = isActive,
                onPenEvent = onPenEvent,
            )
        }
        Spacer(Modifier.height(Spacing.sm))
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Resposta final",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
            )
            Spacer(Modifier.width(Spacing.md))
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(18.dp),
            ) {
                Canvas(modifier = Modifier.matchParentSize()) {
                    drawLine(
                        color = hairline,
                        start = Offset(0f, size.height - 1.dp.toPx()),
                        end = Offset(size.width, size.height - 1.dp.toPx()),
                        strokeWidth = 1.dp.toPx(),
                    )
                }
            }
        }
    }
}

@Composable
fun InkCanvas(
    modifier: Modifier = Modifier,
    penColor: String = "#1a1a1a",
    penWidth: Float = 2.2f,
    enabled: Boolean = true,
    onPenEvent: (PenEvent) -> Unit = {},
) {
    val strokes = remember { mutableStateListOf<List<Offset>>() }
    val inkColor = remember(penColor) { parseHexColor(penColor) }
    var currentStroke = remember { mutableListOf<Offset>() }
    var strokeStartedAt = remember { 0L }
    var previousPoint = remember { Offset.Zero }
    var previousUptime = remember { 0L }

    Canvas(
        modifier = modifier.pointerInput(enabled, penColor, penWidth) {
            if (!enabled) return@pointerInput
            detectDragGestures(
                onDragStart = { offset ->
                    strokeStartedAt = System.currentTimeMillis()
                    previousUptime = strokeStartedAt
                    previousPoint = offset
                    currentStroke = mutableListOf(offset)
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
