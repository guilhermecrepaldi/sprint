package com.strava_matematica.ui.config

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.strava_matematica.design.Spacing
import com.strava_matematica.model.SessionConfig

@Composable
fun SessionConfigScreen(
    config: SessionConfig,
    onConfigChange: (SessionConfig) -> Unit,
    onStart: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Text("Treino", style = MaterialTheme.typography.headlineMedium)
        Text("Monte a folha e comece.", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f))

        SectionLabel("Feedback")
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            FilterChip(
                selected = config.showThermometer,
                onClick = { onConfigChange(config.copy(showThermometer = true)) },
                label = { Text("Visível") },
            )
            FilterChip(
                selected = !config.showThermometer,
                onClick = { onConfigChange(config.copy(showThermometer = false)) },
                label = { Text("Blind") },
            )
        }

        SectionLabel("Aparência")
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            FilterChip(
                selected = config.background == "white",
                onClick = { onConfigChange(config.copy(background = "white")) },
                label = { Text("White") },
            )
            FilterChip(
                selected = config.background == "dark",
                onClick = { onConfigChange(config.copy(background = "dark")) },
                label = { Text("Dark") },
            )
        }

        SectionLabel("Dificuldade ${"%.1f".format(config.difficultyStart)}")
        Slider(
            value = config.difficultyStart.toFloat(),
            onValueChange = { onConfigChange(config.copy(difficultyStart = it.toDouble())) },
            valueRange = 1f..10f,
            steps = 17,
        )

        SectionLabel("Exercícios por folha")
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            listOf(3, 5, 8, 10).forEach { count ->
                FilterChip(
                    selected = config.exercisesPerPage == count,
                    onClick = { onConfigChange(config.copy(exercisesPerPage = count)) },
                    label = { Text(count.toString()) },
                )
            }
        }

        Spacer(Modifier.weight(1f))
        Button(
            onClick = onStart,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
        ) {
            Icon(Icons.Outlined.PlayArrow, contentDescription = null)
            Text("Iniciar treino", modifier = Modifier.padding(start = Spacing.sm))
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f))
}
