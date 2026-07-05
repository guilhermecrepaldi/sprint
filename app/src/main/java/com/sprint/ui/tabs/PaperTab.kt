package com.sprint.ui.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sprint.model.SessionConfig

private data class PaperOption(val label: String, val background: String, val text: String)

private val PaperOptions = listOf(
    PaperOption("Branco",    background = "white", text = "#1a1a1a"),
    PaperOption("Escuro",    background = "dark",  text = "#f2f5ef"),
)

@Composable
fun PaperTab(
    config: SessionConfig,
    onConfigChange: (SessionConfig) -> Unit,
    onGoToSprint: () -> Unit,
) {
    SettingsTabScaffold(title = "PAPEL", onGoToSprint = onGoToSprint) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
        ) {
            PaperOptions.forEach { opt ->
                val isSelected = config.background == opt.background
                val bgColor = if (opt.background == "dark") Color(0xFF11140F) else Color(0xFFF7F8F6)
                val textColor = parseHexSafe(opt.text)

                Box(
                    modifier = Modifier
                        .size(width = 120.dp, height = 160.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(bgColor)
                        .then(
                            if (isSelected) Modifier.border(
                                2.dp,
                                MaterialTheme.colorScheme.onBackground,
                                RoundedCornerShape(8.dp),
                            ) else Modifier.border(
                                1.dp,
                                MaterialTheme.colorScheme.onBackground.copy(alpha = 0.12f),
                                RoundedCornerShape(8.dp),
                            )
                        )
                        .clickable { onConfigChange(config.copy(background = opt.background)) },
                    contentAlignment = Alignment.Center,
                ) {
                    // Simulated exercise text
                    Box(contentAlignment = Alignment.TopCenter) {
                        // Simulated pill
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(20.dp)
                                .padding(top = 8.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(width = 20.dp, height = 2.dp)
                                    .background(textColor.copy(alpha = 0.18f), RoundedCornerShape(1.dp))
                            )
                        }
                    }
                    Text(
                        text = "x + 3 = 0",
                        color = textColor,
                        fontSize = 16.sp,
                    )
                }
            }
        }
    }
}
