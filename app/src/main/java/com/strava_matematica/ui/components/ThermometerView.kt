package com.strava_matematica.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.strava_matematica.design.FocusColors

@Composable
fun ThermometerView(
    value: Double,
    modifier: Modifier = Modifier,
) {
    val color = when {
        value < 0.45 -> FocusColors.Error
        value < 0.70 -> FocusColors.Warning
        else -> FocusColors.Progress
    }
    Box(
        modifier = modifier
            .widthIn(max = 120.dp)
            .height(6.dp)
            .clip(RoundedCornerShape(99.dp))
            .background(Color.Black.copy(alpha = 0.12f)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(value.toFloat().coerceIn(0f, 1f))
                .height(6.dp)
                .clip(RoundedCornerShape(99.dp))
                .background(color),
        )
    }
}
