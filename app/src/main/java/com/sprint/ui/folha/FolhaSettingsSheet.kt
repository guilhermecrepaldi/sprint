package com.sprint.ui.folha

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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sprint.design.Spacing
import com.sprint.model.SessionConfig

private val PenColorOptions = listOf("#1a1a1a", "#1565C0", "#2E7D32", "#B71C1C")

// background_id → display colour (preview swatch)
private val BackgroundOptions = listOf(
    "white"     to Color(0xFFF8F8F8),
    "parchment" to Color(0xFFF5EFD0),
    "slate"     to Color(0xFFE8EAE6),
    "dark"      to Color(0xFF1A1A1A),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolhaSettingsSheet(
    config: SessionConfig,
    onConfigChange: (SessionConfig) -> Unit,
    onDismiss: () -> Unit,
    onEndSession: () -> Unit = {},
    sessionCorrect: Int = 0,
    sessionTotal: Int = 0,
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
            // ── SESSÃO ATUAL ─────────────────────────────────────────────────
            if (sessionTotal > 0) {
                val pct = (sessionCorrect * 100f / sessionTotal).toInt()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "$sessionCorrect corretas de $sessionTotal",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = "$pct%",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (pct >= 70) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.error,
                    )
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = Spacing.sm))
            }

            // ── APARÊNCIA ────────────────────────────────────────────────────
            Text(
                text = "APARÊNCIA",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.54f),
            )
            Spacer(Modifier.height(Spacing.xs))

            // Background colour picker (4 swatches: white, parchment, slate, dark)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = "Fundo", modifier = Modifier.weight(1f))
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    BackgroundOptions.forEach { (id, color) ->
                        val isSelected = config.background == id
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(color, CircleShape)
                                .then(
                                    if (isSelected) Modifier.border(
                                        2.dp,
                                        MaterialTheme.colorScheme.primary,
                                        CircleShape,
                                    ) else Modifier.border(
                                        1.dp,
                                        Color.Black.copy(alpha = 0.12f),
                                        CircleShape,
                                    )
                                )
                                .clickable { onConfigChange(config.copy(background = id)) },
                        )
                    }
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
            ToggleRow(label = "Só avança se acertar", checked = config.requireCorrectToAdvance) {
                onConfigChange(config.copy(requireCorrectToAdvance = it))
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

            HorizontalDivider(modifier = Modifier.padding(vertical = Spacing.sm))

            // ── SESSÃO ───────────────────────────────────────────────────────
            TextButton(
                onClick = { onEndSession(); onDismiss() },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = "Encerrar sessão",
                    color = MaterialTheme.colorScheme.error,
                )
            }

            Spacer(Modifier.height(Spacing.md))
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
