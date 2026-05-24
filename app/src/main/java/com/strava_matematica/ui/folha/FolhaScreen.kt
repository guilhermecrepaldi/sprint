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
import com.strava_matematica.design.Spacing
import com.strava_matematica.model.Folha
import com.strava_matematica.model.SessionConfig
import com.strava_matematica.ui.components.ThermometerView

@Composable
fun FolhaScreen(
    folha: Folha,
    config: SessionConfig,
    onSubmit: () -> Unit,
) {
    var activeField by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.md, vertical = Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            IconButton(onClick = {}) { Icon(Icons.Outlined.Pause, contentDescription = "Pausar") }
            Text("Página ${folha.pageIndex + 1}", style = MaterialTheme.typography.titleMedium)
            Text("Dif. ${"%.1f".format(folha.difficulty)}", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f))
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
                    onClick = { activeField = field.fieldIndex },
                )
            }
        }

        InkToolbar(onClear = {})
    }
}
