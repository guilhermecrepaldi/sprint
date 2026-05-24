package com.strava_matematica.ui.folha

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.strava_matematica.design.Spacing
import com.strava_matematica.model.SessionConfig

private val PenColorOptions = listOf("#1a1a1a", "#1565C0", "#2E7D32", "#B71C1C")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolhaSettingsSheet(
    config: SessionConfig,
    onConfigChange: (SessionConfig) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.lg, vertical = Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            // ── APARÊNCIA ────────────────────────────────────────────────────
            Text(
                text = "APARÊNCIA",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.54f),
            )
            Spacer(Modifier.height(Spacing.xs))

            // Background toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = "Fundo", modifier = Modifier.weight(1f))
                SingleChoiceSegmentedButtonRow {
                    SegmentedButton(
                        selected = config.background == "white",
                        onClick = { onConfigChange(config.copy(background = "white")) },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    ) { Text("Branco") }
                    SegmentedButton(
                        selected = config.background == "dark",
                        onClick = { onConfigChange(config.copy(background = "dark")) },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    ) { Text("Escuro") }
                }
            }

            // Pen color picker
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = "Caneta", modifier = Modifier.weight(1f))
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    PenColorOptions.forEach { hex ->
                        val isSelected = config.penColor == hex
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(parseHexColorSafe(hex), CircleShape)
                                .then(
                                    if (isSelected) Modifier.border(
                                        2.dp,
                                        MaterialTheme.colorScheme.primary,
                                        CircleShape,
                                    ) else Modifier
                                )
                                .clickable { onConfigChange(config.copy(penColor = hex)) },
                        )
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = Spacing.sm))

            // ── VISIBILIDADE ─────────────────────────────────────────────────
            Text(
                text = "VISIBILIDADE",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.54f),
            )
            Spacer(Modifier.height(Spacing.xs))

            ToggleRow(label = "Termômetro", checked = config.showThermometer) {
                onConfigChange(config.copy(showThermometer = it))
            }
            ToggleRow(label = "Acertos / Erros", checked = config.showCorrectCount) {
                onConfigChange(config.copy(showCorrectCount = it))
            }
            ToggleRow(label = "Porcentagem de domínio", checked = config.showPercentage) {
                onConfigChange(config.copy(showPercentage = it))
            }
            ToggleRow(label = "Modo cego total", checked = config.blindMode) {
                onConfigChange(config.copy(blindMode = it))
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = Spacing.sm))

            // ── QUESTÕES POR FOLHA ───────────────────────────────────────────
            Text(
                text = "QUESTÕES POR FOLHA",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.54f),
            )
            Spacer(Modifier.height(Spacing.xs))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                IconButton(onClick = {
                    if (config.exercisesPerPage > 1)
                        onConfigChange(config.copy(exercisesPerPage = config.exercisesPerPage - 1))
                }) {
                    Icon(Icons.Outlined.Remove, contentDescription = "Diminuir")
                }
                Text(
                    text = "${config.exercisesPerPage}",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.width(40.dp),
                    textAlign = TextAlign.Center,
                )
                IconButton(onClick = {
                    if (config.exercisesPerPage < 10)
                        onConfigChange(config.copy(exercisesPerPage = config.exercisesPerPage + 1))
                }) {
                    Icon(Icons.Outlined.Add, contentDescription = "Aumentar")
                }
            }

            Spacer(Modifier.height(Spacing.lg))
        }
    }
}

@Composable
private fun ToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

private fun parseHexColorSafe(hex: String): Color =
    runCatching { Color(android.graphics.Color.parseColor(hex)) }
        .getOrDefault(Color(0xFF1A1A1A))
