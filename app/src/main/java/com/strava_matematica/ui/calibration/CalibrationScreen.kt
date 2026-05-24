package com.strava_matematica.ui.calibration

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.strava_matematica.design.Spacing
import com.strava_matematica.ui.folha.InkCanvas

private val CALIBRATION_CHARS = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0")

@Composable
fun CalibrationScreen(
    onComplete: (skipped: Boolean) -> Unit,
) {
    var index by remember { mutableIntStateOf(0) }
    val currentChar = CALIBRATION_CHARS[index]
    var clearSignal by remember(index) { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(Spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Spacer(Modifier.height(Spacing.lg))
        Text("Calibração de escrita", style = MaterialTheme.typography.titleLarge)
        Text(
            "Escreva o caractere mostrado no quadrado e avance.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
        )
        Spacer(Modifier.height(Spacing.md))

        Text(currentChar, style = MaterialTheme.typography.displayLarge)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .background(
                    MaterialTheme.colorScheme.surfaceVariant,
                    androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                ),
        ) {
            InkCanvas(
                modifier = Modifier
                    .matchParentSize()
                    .padding(Spacing.md),
                penColor = "#1a1a1a",
                enabled = true,
                clearSignal = clearSignal,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = { onComplete(true) }) { Text("Pular") }
            Text(
                "${index + 1}/${CALIBRATION_CHARS.size}",
                style = MaterialTheme.typography.labelLarge,
            )
            Button(onClick = {
                if (index < CALIBRATION_CHARS.lastIndex) {
                    index++
                } else {
                    onComplete(false)
                }
            }) {
                Text(if (index < CALIBRATION_CHARS.lastIndex) "Avançar" else "Concluir")
            }
        }

        Spacer(Modifier.height(Spacing.sm))
        TextButton(onClick = { clearSignal++ }) {
            Text("Limpar", style = MaterialTheme.typography.labelMedium)
        }
    }
}
