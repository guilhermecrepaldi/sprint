package com.strava_matematica.ui.folha

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.strava_matematica.design.FocusColors
import com.strava_matematica.design.Spacing
import com.strava_matematica.model.BackgroundMode
import com.strava_matematica.model.FolhaField

@Composable
fun ExerciseField(
    field: FolhaField,
    isActive: Boolean,
    backgroundMode: BackgroundMode,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val fieldColor = if (backgroundMode == BackgroundMode.DARK) FocusColors.DarkField else FocusColors.WhiteField
    val borderColor = if (isActive) MaterialTheme.colorScheme.primary else {
        if (backgroundMode == BackgroundMode.DARK) FocusColors.DarkHairline else FocusColors.WhiteHairline
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(190.dp)
            .background(fieldColor, RoundedCornerShape(8.dp))
            .border(BorderStroke(1.dp, borderColor), RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(Spacing.md),
    ) {
        Text(
            text = "%02d  %s".format(field.fieldIndex + 1, field.statement),
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(Modifier.height(Spacing.sm))
        InkCanvas(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        )
        Spacer(Modifier.height(Spacing.sm))
        Text(
            text = "Resposta final",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
        )
    }
}

@Composable
fun InkCanvas(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val y = size.height * 0.72f
        drawLine(
            color = Color.Gray.copy(alpha = 0.35f),
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = 1.dp.toPx(),
        )
    }
}
