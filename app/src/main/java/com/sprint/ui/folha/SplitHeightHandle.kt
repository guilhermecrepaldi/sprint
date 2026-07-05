package com.sprint.ui.folha

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

/**
 * Drag handle to resize the scratch/answer split ratio.
 * Extracted from ExerciseField.kt.
 */
@Composable
fun SplitHeightHandle(
    ink: Color,
    ratio: Float,
    onRatioChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
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
