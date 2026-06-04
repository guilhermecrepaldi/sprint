package com.strava_matematica.design

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.composed

/**
 * Modifier customizado para aplicar Efeito de Vidro (Glassmorphism).
 * É otimizado para o Light Mode (com fundo sutil branco e sombra).
 */
fun Modifier.glassmorphism(
    cornerRadius: Dp = 16.dp,
    blurRadius: Float = 20f,
    alpha: Float = 0.65f,
    borderColor: Color = Color.White.copy(alpha = 0.5f),
    backgroundColor: Color = Color.White.copy(alpha = alpha)
): Modifier = composed {
    this
        .background(color = backgroundColor, shape = RoundedCornerShape(cornerRadius))
        .border(
            width = 1.dp,
            color = borderColor,
            shape = RoundedCornerShape(cornerRadius)
        )
}
