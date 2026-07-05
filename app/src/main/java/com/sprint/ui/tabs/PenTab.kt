package com.sprint.ui.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sprint.model.SessionConfig

private val PenColors = listOf(
    "#1a1a1a", "#1565C0", "#2E7D32", "#B71C1C",
    "#6A1B9A", "#E65100", "#00695C", "#4E342E",
)

@Composable
fun PenTab(
    config: SessionConfig,
    onConfigChange: (SessionConfig) -> Unit,
    onGoToSprint: () -> Unit,
) {
    SettingsTabScaffold(title = "CANETA", onGoToSprint = onGoToSprint) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            val rows = PenColors.chunked(4)
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                rows.forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        row.forEach { hex ->
                            val isSelected = config.penColor == hex
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(parseHexSafe(hex), CircleShape)
                                    .then(
                                        if (isSelected) Modifier.border(
                                            2.dp,
                                            MaterialTheme.colorScheme.onBackground,
                                            CircleShape,
                                        ) else Modifier
                                    )
                                    .clickable { onConfigChange(config.copy(penColor = hex)) },
                            )
                        }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "traço",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.38f),
                    )
                    Text(
                        text = "${config.penWidth.toInt()}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.26f),
                        textAlign = TextAlign.End,
                    )
                }
                Slider(
                    value = config.penWidth,
                    onValueChange = { value ->
                        onConfigChange(config.copy(penWidth = value.coerceIn(1.0f, 8.0f)))
                    },
                    valueRange = 1.0f..8.0f,
                    steps = 6,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "borracha na tela",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.38f),
                    )
                    Text(
                        text = "atalho: segurar",
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.22f),
                    )
                }
                Switch(
                    checked = config.showEraserButton,
                    onCheckedChange = { checked ->
                        onConfigChange(config.copy(showEraserButton = checked))
                    },
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .width(180.dp)
                        .height(config.penWidth.dp)
                        .background(parseHexSafe(config.penColor)),
                )
            }
        }
    }
}

internal fun parseHexSafe(hex: String): Color =
    runCatching { Color(android.graphics.Color.parseColor(hex)) }
        .getOrDefault(Color(0xFF1A1A1A))
