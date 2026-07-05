package com.sprint.ui.calibration

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import com.sprint.design.Spacing
import com.sprint.model.CalibrationSample
import com.sprint.ui.folha.ImageUtils
import com.sprint.ui.folha.InkCanvas

private val CALIBRATION_CHARS = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0")

@Composable
fun CalibrationScreen(
    isSubmitting: Boolean = false,
    errorMessage: String? = null,
    onComplete: (skipped: Boolean) -> Unit,
    onSubmitSamples: (List<CalibrationSample>) -> Unit,
) {
    var index by remember { mutableIntStateOf(0) }
    val currentChar = CALIBRATION_CHARS[index]
    var clearSignal by remember(index) { mutableIntStateOf(0) }
    var currentStrokes by remember(index) { mutableStateOf<List<List<Offset>>>(emptyList()) }
    val samples = remember { mutableStateMapOf<String, CalibrationSample>() }

    fun captureCurrentSample() {
        samples[currentChar] = CalibrationSample(
            expectedChar = currentChar,
            imageBase64 = ImageUtils.exportBitmap(currentStrokes),
        )
    }

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
            if (isSubmitting) "Enviando amostras..." else "Escreva o caractere mostrado no quadrado e avance.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
        )
        if (errorMessage != null) {
            Text(
                text = "Não consegui enviar. Verifique a conexão e tente novamente.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }
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
            key(currentChar) {
                InkCanvas(
                    modifier = Modifier
                        .matchParentSize()
                        .padding(Spacing.md),
                    penColor = "#1a1a1a",
                    enabled = !isSubmitting,
                    clearSignal = clearSignal,
                    onSyncStrokes = { strokes, _ -> currentStrokes = strokes },
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(
                enabled = !isSubmitting,
                onClick = { onComplete(true) },
            ) { Text("Pular") }
            Text(
                "${index + 1}/${CALIBRATION_CHARS.size}",
                style = MaterialTheme.typography.labelLarge,
            )
            Button(
                enabled = !isSubmitting,
                onClick = {
                    captureCurrentSample()
                    if (index < CALIBRATION_CHARS.lastIndex) {
                        index++
                    } else {
                        onSubmitSamples(CALIBRATION_CHARS.mapNotNull { samples[it] })
                    }
                },
            ) {
                Text(
                    when {
                        isSubmitting -> "Enviando"
                        index < CALIBRATION_CHARS.lastIndex -> "Avançar"
                        else -> "Concluir"
                    },
                )
            }
        }

        Spacer(Modifier.height(Spacing.sm))
        TextButton(
            enabled = !isSubmitting,
            onClick = {
                currentStrokes = emptyList()
                samples.remove(currentChar)
                clearSignal++
            },
        ) {
            Text("Limpar", style = MaterialTheme.typography.labelMedium)
        }
    }
}
