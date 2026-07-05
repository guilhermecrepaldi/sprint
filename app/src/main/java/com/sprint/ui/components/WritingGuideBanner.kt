package com.sprint.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun WritingGuideBanner(modifier: Modifier = Modifier) {
    val visible = remember { mutableStateOf(true) }
    LaunchedEffect(Unit) { delay(8000L); visible.value = false }

    AnimatedVisibility(visible = visible.value, enter = fadeIn(), exit = fadeOut()) {
        Surface(
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFFE3F2FD),
            tonalElevation = 2.dp,
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(Icons.Outlined.Info, contentDescription = "Dica", tint = Color(0xFF1565C0), modifier = Modifier.size(20.dp))
                Text(
                    text = "Escreva um caractere por vez, da esquerda para a direita: " +
                           "-126 • S = { x , 10 , n² } • 3/4 • a/b • x ∈ ℝ • [a,b] • n² + m³. " +
                           "Isso ajuda o reconhecimento a não se confundir.",
                    fontSize = 12.sp,
                    color = Color(0xFF1565C0),
                    lineHeight = 16.sp,
                )
            }
        }
    }
}
