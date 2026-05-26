package com.strava_matematica.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.strava_matematica.design.Spacing
import com.strava_matematica.model.HeatmapDay
import com.strava_matematica.model.PublicProfile
import com.strava_matematica.model.TrackProgress
import com.strava_matematica.network.ApiClient
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(
    slug: String,
    onBack: () -> Unit,
) {
    var profile by remember { mutableStateOf<PublicProfile?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(slug) {
        scope.launch {
            try {
                profile = ApiClient.create().getProfile(slug)
            } catch (e: Exception) {
                error = e.message
            } finally {
                loading = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(Spacing.lg),
    ) {
        // ── Header ────────────────────────────────────────────────────────────
        Text("Progresso", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(Spacing.xl))

        when {
            loading -> {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            error != null -> {
                Text(
                    text = "Perfil não disponível.\nAdicione um slug público no seu cadastro.",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.54f),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            profile != null -> {
                val p = profile!!

                // ── Stats rápidos ─────────────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                ) {
                    StatChip(label = "XP", value = "${p.xpTotal}", modifier = Modifier.weight(1f))
                    StatChip(label = "Sequência", value = "${p.stats.streakDays}d", modifier = Modifier.weight(1f))
                    StatChip(label = "Total", value = "${p.stats.totalExercises}", modifier = Modifier.weight(1f))
                }

                Spacer(Modifier.height(Spacing.xl))

                // ── Heatmap ────────────────────────────────────────────────────
                Text(
                    "Constância",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(Spacing.sm))
                Heatmap(days = p.heatmap)

                Spacer(Modifier.height(Spacing.xl))

                // ── Trilhas ────────────────────────────────────────────────────
                Text(
                    "Trilhas",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(Spacing.sm))

                p.tracks.forEach { track ->
                    TrackRow(track = track)
                    Spacer(Modifier.height(Spacing.sm))
                }
            }
        }
    }
}

// ── Heatmap ───────────────────────────────────────────────────────────────────

@Composable
private fun Heatmap(days: List<HeatmapDay>) {
    val countByDate: Map<String, Int> = days.associate { it.date to it.count }
    val maxCount = days.maxOfOrNull { it.count }?.coerceAtLeast(1) ?: 1

    // Monta as 52 semanas (364 dias, começa de segunda da semana mais antiga)
    val today = java.time.LocalDate.now()
    val startDate = today.minusDays(363)

    val cellSize = 10.dp
    val gap = 2.dp

    Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
        repeat(52) { weekIdx ->
            Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                repeat(7) { dayOfWeek ->
                    val date = startDate.plusDays((weekIdx * 7 + dayOfWeek).toLong())
                    val key = date.toString()
                    val count = countByDate[key] ?: 0
                    val intensity = if (count == 0) 0f else (count.toFloat() / maxCount).coerceIn(0.15f, 1f)
                    Box(
                        modifier = Modifier
                            .size(cellSize)
                            .clip(RoundedCornerShape(2.dp))
                            .background(heatColor(intensity)),
                    )
                }
            }
        }
    }
}

private fun heatColor(intensity: Float): Color {
    if (intensity == 0f) return Color(0xFF1E1E1E)
    // Verde que vai do escuro (baixo) ao vivo (alto)
    val base = Color(0xFF1B5E20)
    val vivid = Color(0xFF4CAF50)
    return Color(
        red = base.red + (vivid.red - base.red) * intensity,
        green = base.green + (vivid.green - base.green) * intensity,
        blue = base.blue + (vivid.blue - base.blue) * intensity,
    )
}

// ── Track row ─────────────────────────────────────────────────────────────────

@Composable
private fun TrackRow(track: TrackProgress) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = track.name,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "${track.attemptedSkills}/${track.totalSkills}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.54f),
            )
        }
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { track.progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp)),
            color = Color(0xFF4CAF50),
            trackColor = Color(0xFF1E1E1E),
        )
    }
}

// ── Chip de stat ──────────────────────────────────────────────────────────────

@Composable
private fun StatChip(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(Spacing.md),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.54f),
        )
    }
}
