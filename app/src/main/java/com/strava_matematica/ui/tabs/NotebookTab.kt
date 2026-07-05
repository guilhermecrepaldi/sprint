package com.strava_matematica.ui.tabs

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.strava_matematica.model.SessionConfig

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun NotebookTab(
    config: SessionConfig,
    onConfigChange: (SessionConfig) -> Unit,
    onGoToSprint: () -> Unit,
) {
    SettingsTabScaffold(title = "CADERNO", onGoToSprint = onGoToSprint) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            verticalArrangement = Arrangement.spacedBy(28.dp),
        ) {
            // ── Cor da página ────────────────────────────────────────────────
            SectionLabel("COR DA PÁGINA")
            PageColorRow(config, onConfigChange)

            // ── Linhas ───────────────────────────────────────────────────────
            SectionLabel("LINHAS")
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                listOf("nenhuma", "horizontal", "dots", "grade").forEachIndexed { i, mode ->
                    SegmentedButton(
                        selected = config.guideMode == mode,
                        onClick = { onConfigChange(config.copy(guideMode = mode)) },
                        shape = SegmentedButtonDefaults.itemShape(i, 4),
                    ) {
                        Text(
                            text = when (mode) {
                                "nenhuma"    -> "—"
                                "horizontal" -> "≡"
                                "dots"       -> "⁚"
                                else         -> "⊞"
                            },
                            fontSize = 16.sp,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        fontSize = 9.sp,
        letterSpacing = 1.5.sp,
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.30f),
    )
}

private data class PageColor(val label: String, val background: String, val color: Color)

private val PageColors = listOf(
    PageColor("Branco",    "white",     Color(0xFFF7F8F6)),
    PageColor("Parchment", "parchment", Color(0xFFF5EFD0)),
    PageColor("Cinza",     "slate",     Color(0xFFE8EAE6)),
    PageColor("Escuro",    "dark",      Color(0xFF11140F)),
)

@Composable
private fun PageColorRow(config: SessionConfig, onConfigChange: (SessionConfig) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        PageColors.forEach { opt ->
            val isSelected = config.background == opt.background
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(opt.color)
                        .then(
                            if (isSelected) Modifier.border(
                                2.dp,
                                MaterialTheme.colorScheme.onBackground,
                                CircleShape,
                            ) else Modifier.border(
                                1.dp,
                                MaterialTheme.colorScheme.onBackground.copy(alpha = 0.12f),
                                CircleShape,
                            )
                        )
                        .clickable { onConfigChange(config.copy(background = opt.background)) },
                )
                Text(
                    text = opt.label,
                    fontSize = 9.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.40f),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
