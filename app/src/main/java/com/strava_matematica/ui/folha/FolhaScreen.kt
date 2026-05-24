package com.strava_matematica.ui.folha

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.strava_matematica.design.FocusColors
import com.strava_matematica.design.Spacing
import com.strava_matematica.model.BackgroundMode
import com.strava_matematica.model.Folha
import com.strava_matematica.model.PenEvent
import com.strava_matematica.model.SessionConfig
import com.strava_matematica.ui.components.ThermometerView

@Composable
fun FolhaScreen(
    folha: Folha,
    config: SessionConfig,
    onSubmit: () -> Unit,
    onPenEvent: (fieldIndex: Int, event: PenEvent) -> Unit = { _, _ -> },
) {
    var activeField by remember { mutableIntStateOf(0) }
    var clearSignal by remember { mutableIntStateOf(0) }
    var undoSignal by remember { mutableIntStateOf(0) }
    var redoSignal by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        val hairline = if (config.backgroundMode == BackgroundMode.DARK) FocusColors.DarkHairline else FocusColors.WhiteHairline
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = Spacing.md, vertical = Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            IconButton(onClick = {}) { Icon(Icons.Outlined.Pause, contentDescription = "Pausar") }
            Column {
                Text("Página", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.54f))
                Text("%02d".format(folha.pageIndex + 1), style = MaterialTheme.typography.titleMedium)
            }
            Column {
                Text("Dif.", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.54f))
                Text("%.1f".format(folha.difficulty), style = MaterialTheme.typography.titleMedium)
            }
            if (config.showThermometer) {
                ThermometerView(value = 0.72, modifier = Modifier.weight(1f))
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }
            Button(onClick = onSubmit) {
                Icon(Icons.Outlined.Check, contentDescription = null)
                Text("Enviar", modifier = Modifier.padding(start = Spacing.sm))
            }
        }
        HorizontalDivider(color = hairline, thickness = 1.dp)

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            folha.fields.forEach { field ->
                ExerciseField(
                    field = field,
                    isActive = field.fieldIndex == activeField,
                    backgroundMode = config.backgroundMode,
                    penColor = config.penColor,
                    clearSignal = if (field.fieldIndex == activeField) clearSignal else 0,
                    undoSignal = if (field.fieldIndex == activeField) undoSignal else 0,
                    redoSignal = if (field.fieldIndex == activeField) redoSignal else 0,
                    onClick = { activeField = field.fieldIndex },
                    onPenEvent = { event -> onPenEvent(field.fieldIndex, event) },
                )
            }
        }

        InkToolbar(
            onUndo = { undoSignal += 1 },
            onRedo = { redoSignal += 1 },
            onClear = { clearSignal += 1 },
        )
    }
}
