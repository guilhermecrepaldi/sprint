package com.strava_matematica.ui.profile

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import com.strava_matematica.model.ProfileStats
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
                // Simula um perfil de alta fidelidade offline para o Kumon Mode
                profile = PublicProfile(
                    studentName = "Estudante Zen",
                    slug = slug,
                    xpTotal = 1250,
                    memberSince = "Maio 2026",
                    stats = ProfileStats(
                        totalExercises = 84,
                        streakDays = 7,
                        xpTotal = 1250
                    ),
                    heatmap = (0..60).map { i ->
                        com.strava_matematica.model.HeatmapDay(
                            date = java.time.LocalDate.now().minusDays(i.toLong()).toString(),
                            count = (0..20).random(),
                            countMorning = (0..10).random(),
                            countAfternoon = (0..10).random(),
                            countNight = (0..10).random()
                        )
                    },
                    tracks = listOf(
                        TrackProgress(slug = "fundamentos", name = "Fundamentos", totalSkills = 5, attemptedSkills = 4, progress = 0.9f),
                        TrackProgress(slug = "algebra", name = "Álgebra", totalSkills = 5, attemptedSkills = 2, progress = 0.4f),
                        TrackProgress(slug = "calculo", name = "Cálculo", totalSkills = 9, attemptedSkills = 0, progress = 0.0f)
                    ),
                    recentSprints = listOf(
                        com.strava_matematica.model.SprintHistoryItem(sessionId = "1", startedAt = "Hoje às 14:30", skill = "Álgebra - Equação do 2º Grau", accuracy = 18, exercisesDone = 20, durationMin = 15, isActive = false),
                        com.strava_matematica.model.SprintHistoryItem(sessionId = "2", startedAt = "Ontem às 19:15", skill = "Matrizes e Determinantes", accuracy = 20, exercisesDone = 20, durationMin = 10, isActive = false),
                        com.strava_matematica.model.SprintHistoryItem(sessionId = "3", startedAt = "Segunda às 10:00", skill = "Fundamentos - Frações", accuracy = 12, exercisesDone = 20, durationMin = 18, isActive = false)
                    )
                )
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                // Placeholder fotinho (Letra Inicial ou ícone)
                Text(
                    text = profile?.studentName?.take(1)?.uppercase() ?: "U",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.width(Spacing.md))
            Column {
                Text(
                    text = profile?.studentName ?: "Carregando...",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Membro desde ${profile?.memberSince ?: ""}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Spacer(Modifier.height(Spacing.sm))
                // Botoes Sociais
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    androidx.compose.material3.Button(
                        onClick = { /* TODO: Vincular Face */ },
                        shape = RoundedCornerShape(8.dp),
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Color(0xFF1877F2))
                    ) {
                        Text("Vincular Facebook")
                    }
                    androidx.compose.material3.OutlinedButton(
                        onClick = { /* TODO: Vincular Google */ },
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Text("Vincular Google")
                    }
                }
            }
        }
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Constância",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    
                    val context = androidx.compose.ui.platform.LocalContext.current
                    androidx.compose.material3.TextButton(onClick = {
                        try {
                            val sprintsText = p.recentSprints.take(3).joinToString("\n") { "✅ ${it.skill}: ${it.accuracy}/${it.exercisesDone} em ${it.durationMin}min" }
                            val shareText = "🔥 Confira meu progresso no LOVE CLASS!\n\n" +
                                            "👤 ${p.studentName}\n" +
                                            "⭐ Nível Mestre (XP: ${p.xpTotal})\n" +
                                            "📈 Sequência de ${p.stats.streakDays} dias de estudos!\n\n" +
                                            "Meus últimos Sprints:\n$sprintsText\n\n" +
                                            "Baixe o app e venha treinar comigo!"
                            
                            val sendIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(android.content.Intent.EXTRA_TEXT, shareText)
                            }
                            context.startActivity(android.content.Intent.createChooser(sendIntent, "Compartilhar Perfil"))
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }) {
                        Text("COMPARTILHAR", fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.height(Spacing.xl))
                Text("HISTÓRICO DE ESTUDO", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
                Spacer(Modifier.height(Spacing.sm))
                
                var selectedDate by remember { mutableStateOf<String?>(null) }
                
                Heatmap(
                    days = p.heatmap,
                    selectedDate = selectedDate,
                    onDateClick = { date -> selectedDate = if (selectedDate == date) null else date }
                )
                
                AnimatedVisibility(visible = selectedDate != null) {
                    val dateVal = selectedDate ?: ""
                    val dayData = p.heatmap.find { it.date == dateVal }
                    val total = dayData?.let { it.countMorning + it.countAfternoon + it.countNight } ?: 0
                    
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = Spacing.md)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                            .padding(Spacing.md)
                    ) {
                        Text(
                            text = "🗓️ Resumo: $dateVal",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(8.dp))
                        if (total > 0) {
                            Text("Total de exercícios: $total", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground)
                            Spacer(Modifier.height(4.dp))
                            Text("Manhã: ${dayData!!.countMorning}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f))
                            Text("Tarde: ${dayData.countAfternoon}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f))
                            Text("Noite: ${dayData.countNight}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f))
                        } else {
                            Text("Nenhum exercício registrado neste dia.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f))
                        }
                    }
                }

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

                // ── Sprints Recentes ───────────────────────────────────────────
                if (p.recentSprints.isNotEmpty()) {
                    Spacer(Modifier.height(Spacing.xl))
                    Text(
                        "Últimos Sprints",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(Spacing.sm))
                    
                    p.recentSprints.forEach { sprint ->
                        SprintRow(sprint = sprint)
                        Spacer(Modifier.height(Spacing.sm))
                    }
                }
            }
        }
    }
}

// ── Heatmap ───────────────────────────────────────────────────────────────────

@Composable
private fun Heatmap(days: List<HeatmapDay>, selectedDate: String?, onDateClick: (String) -> Unit) {
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    val dayMap = days.associateBy { it.date }
    val maxCount = days.maxOfOrNull { maxOf(it.countMorning, it.countAfternoon, it.countNight) }?.coerceAtLeast(1) ?: 1

    val today = java.time.LocalDate.now()
    val cellSize = 22.dp
    val gap = 4.dp

    androidx.compose.foundation.lazy.LazyRow(
        horizontalArrangement = Arrangement.spacedBy(gap),
        modifier = Modifier.fillMaxWidth(),
        reverseLayout = true // Começa da direita (hoje) para a esquerda (passado)
    ) {
        items(365) { daysAgo ->
            val date = today.minusDays(daysAgo.toLong())
            val heatmapDay = dayMap[date.toString()]
            val dateStr = date.toString()
            val isSelected = selectedDate == dateStr
            
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color.Transparent)
                    .clickable { onDateClick(dateStr) }
                    .padding(4.dp),
                verticalArrangement = Arrangement.spacedBy(gap),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Manhã
                val morningIntensity = heatmapDay?.let { it.countMorning.toFloat() / maxCount }?.coerceIn(0.15f, 1f) ?: 0f
                Box(modifier = Modifier.size(cellSize).clip(RoundedCornerShape(4.dp)).background(heatColor(morningIntensity, isDark)))
                
                // Tarde
                val afternoonIntensity = heatmapDay?.let { it.countAfternoon.toFloat() / maxCount }?.coerceIn(0.15f, 1f) ?: 0f
                Box(modifier = Modifier.size(cellSize).clip(RoundedCornerShape(4.dp)).background(heatColor(afternoonIntensity, isDark)))
                
                // Noite
                val nightIntensity = heatmapDay?.let { it.countNight.toFloat() / maxCount }?.coerceIn(0.15f, 1f) ?: 0f
                Box(modifier = Modifier.size(cellSize).clip(RoundedCornerShape(4.dp)).background(heatColor(nightIntensity, isDark)))
                
                // Data (Dia/Mês)
                Text(
                    text = "${date.dayOfMonth}/${date.monthValue}",
                    fontSize = 10.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

private fun heatColor(intensity: Float, isDark: Boolean): Color {
    if (intensity == 0f) return if (isDark) Color(0xFF1E1E1E) else Color(0xFFEEEEEE)
    // Verde que vai do claro/escuro (baixo) ao vivo (alto)
    val base = if (isDark) Color(0xFF1B5E20) else Color(0xFFA5D6A7)
    val vivid = if (isDark) Color(0xFF4CAF50) else Color(0xFF2E7D32)
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

// ── Sprint row ─────────────────────────────────────────────────────────────────

@Composable
private fun SprintRow(sprint: com.strava_matematica.model.SprintHistoryItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(Spacing.md),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = sprint.skill,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "${sprint.startedAt} • ${sprint.durationMin} min",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "${sprint.accuracy}/${sprint.exercisesDone}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (sprint.exercisesDone > 0 && sprint.accuracy.toFloat() / sprint.exercisesDone >= 0.8f) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "Acertos",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            )
        }
    }
}
