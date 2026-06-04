package com.strava_matematica.ui.tabs

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.strava_matematica.model.SprintHistoryItem
import com.strava_matematica.model.HeatmapDay
import com.strava_matematica.viewmodel.SprintNote
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.roundToInt
import com.strava_matematica.design.glassmorphism
import androidx.compose.ui.draw.scale

// ── Section enum ──────────────────────────────────────────────────────────────

private enum class DashSection { PERFIL, HISTORICO }

private val SectionLabels = mapOf(
    DashSection.PERFIL    to "Perfil",
    DashSection.HISTORICO to "Histórico",
)

// ── Helpers ───────────────────────────────────────────────────────────────────

/** Convert ISO-8601 startedAt into a Portuguese day label ("Hoje", "Ontem", "Segunda"…). */
private fun groupLabelFor(startedAt: String): String {
    return try {
        val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(startedAt.take(10))
            ?: return startedAt.take(10)
        val diffDays = ((Calendar.getInstance().time.time - date.time) / 86_400_000L).toInt()
        when {
            diffDays == 0 -> "Hoje"
            diffDays == 1 -> "Ontem"
            else -> SimpleDateFormat("EEEE", Locale("pt", "BR"))
                .format(date)
                .replaceFirstChar { it.uppercase() }
        }
    } catch (_: Exception) {
        startedAt.take(10)
    }
}

/** Convert snake_case skill tag to human-readable label. */
private fun displaySkill(tag: String): String =
    tag.replace("_", " ").replaceFirstChar { it.uppercase() }

/** Extract average seconds per exercise for last 7 sessions (filter out zero exercises). */
private fun averageTimePerSession(history: List<SprintHistoryItem>): List<Pair<String, Float>> {
    val last7 = history.takeLast(7)
    return last7
        .filter { it.exercisesDone > 0 }
        .map { item ->
            val label = if (item.startedAt.length >= 10) item.startedAt.substring(5, 10) else item.startedAt  // MM-DD
            val avgSecPerExercise = (item.durationMin * 60f) / item.exercisesDone
            label to avgSecPerExercise
        }
}

// ── Root composable ───────────────────────────────────────────────────────────

@Composable
fun DashboardTab(
    history: List<SprintHistoryItem> = emptyList(),
    skillAttempts: Map<String, Int> = emptyMap(),
    skillAccuracy: Map<String, Float> = emptyMap(),
    activityDays: List<HeatmapDay> = emptyList(),
    notes: List<SprintNote> = emptyList(),
    onGoToSprint: () -> Unit,
    onStartSession: () -> Unit,
) {
    var section by remember { mutableStateOf<DashSection?>(DashSection.PERFIL) }

    if (section == null) {
        // ── Navigation list — Perfil · Histórico ─────────────────────────────
        DashboardList(
            currentSection = null,
            onGoToSprint = onGoToSprint,
            onSelect = { section = it },
        )
    } else {
        // ── Section content ───────────────────────────────────────────────────
        Crossfade(targetState = section, animationSpec = tween(220)) { active ->
            when (active) {
                DashSection.PERFIL    -> PerfilSection(
                    history = history,
                    skillAttempts = skillAttempts,
                    skillAccuracy = skillAccuracy,
                    activityDays = activityDays,
                    notes = notes,
                    onBack = { section = null },
                )
                DashSection.HISTORICO -> HistoricoSection(
                    history = history,
                    onBack = { section = null },
                    onGoToSprint = onGoToSprint,
                    onStartSession = onStartSession,
                )
                null -> {}
            }
        }
    }
}

// ── Navigation list ───────────────────────────────────────────────────────────

@Composable
private fun DashboardList(
    currentSection: DashSection?,
    onGoToSprint: () -> Unit,
    onSelect: (DashSection) -> Unit,
) {
    val ink = MaterialTheme.colorScheme.onBackground

    Column(
        modifier = Modifier.fillMaxSize(),
    ) {
        TabPill(onTap = onGoToSprint)

        Spacer(Modifier.height(16.dp))

        DashSection.entries.forEachIndexed { i, sec ->
            val isActive = sec == currentSection
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelect(sec) }
                    .padding(horizontal = 24.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Active marker
                Text(
                    text = if (isActive) "—" else " ",
                    fontSize = 11.sp,
                    color = ink.copy(alpha = 0.40f),
                    modifier = Modifier.width(16.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = SectionLabels[sec] ?: "",
                    fontSize = 16.sp,
                    color = if (isActive) ink else ink.copy(alpha = 0.60f),
                    fontWeight = if (isActive) FontWeight.Medium else FontWeight.Normal,
                )
            }
            if (i < DashSection.entries.lastIndex) {
                HorizontalDivider(
                    thickness = 0.5.dp,
                    color = ink.copy(alpha = 0.07f),
                    modifier = Modifier.padding(start = 24.dp),
                )
            }
        }
    }
}

// ── Perfil section ────────────────────────────────────────────────────────────

@Composable
private fun PerfilSection(
    history: List<SprintHistoryItem>,
    skillAttempts: Map<String, Int>,
    skillAccuracy: Map<String, Float>,
    activityDays: List<HeatmapDay>,
    notes: List<SprintNote>,
    onBack: () -> Unit,
) {
    val ink = MaterialTheme.colorScheme.onBackground
    val totalExercises = skillAttempts.values.sum().takeIf { it > 0 }
        ?: history.sumOf { it.exercisesDone }
    val totalMinutes = history.sumOf { it.durationMin }
    val weightedAccuracy = weightedAccuracy(history, skillAttempts, skillAccuracy)
    val mostPracticed = skillAttempts.maxByOrNull { it.value }?.key
        ?: history.groupBy { it.skill }.maxByOrNull { entry -> entry.value.sumOf { it.exercisesDone } }?.key
        ?: "variado"
    val activeSessions = history.count { it.isActive }
    val velocityData = averageTimePerSession(history)
    val overallAvg = if (velocityData.isNotEmpty()) velocityData.map { it.second }.average().toFloat() else 0f

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        TabPill(onTap = onBack)  // toque no pill = volta para lista

        Spacer(Modifier.height(16.dp))

        // Profile Header (GitHub Style)
        ProfileHeader(ink)
        
        Spacer(Modifier.height(24.dp))
        
        // Popular Repositories (Recent Exercises)
        RecentExercises(history, ink)

        Spacer(Modifier.height(32.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            StatRow("Exercícios feitos", totalExercises.toString())
            StatRow("Acerto médio", if (weightedAccuracy >= 0) "$weightedAccuracy%" else "—")
            StatRow("Tempo registrado", formatMinutes(totalMinutes))
            StatRow("Sessões no histórico", history.size.toString())
            StatRow("Sessões ativas", activeSessions.toString())
            StatRow("Tema mais praticado", displaySkill(mostPracticed))
            StatRow("Velocidade média", if (overallAvg > 0) "${overallAvg.roundToInt()} seg/ex" else "—")

            // GitHub Calendar Heatmap
            if (activityDays.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    "1,143 contributions in the last year", // GitHub style label
                    fontSize = 14.sp,
                    color = ink.copy(alpha = 0.80f),
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(8.dp))
                GitHubCalendarMap(activityDays, ink)
            }

            // Velocity graph
            if (velocityData.size >= 2) {
                Spacer(Modifier.height(12.dp))
                Text(
                    "Velocidade (seg/ex)",
                    fontSize = 10.sp,
                    color = ink.copy(alpha = 0.30f),
                )
                Spacer(Modifier.height(8.dp))
                VelocityGraph(velocityData, ink)
            }

            if (notes.isNotEmpty()) {
                Spacer(Modifier.height(24.dp))
                Text(
                    "Insights & Anotações de Deep Work",
                    fontSize = 11.sp,
                    color = ink.copy(alpha = 0.35f),
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(8.dp))
                
                notes.reversed().take(10).forEach { note ->
                    val dateStr = try {
                        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                        sdf.format(java.util.Date(note.timestamp))
                    } catch (e: Exception) {
                        ""
                    }
                    val interactionSource = remember { MutableInteractionSource() }
                    val isPressed by interactionSource.collectIsPressedAsState()
                    val scale by animateFloatAsState(
                        targetValue = if (isPressed) 0.98f else 1f,
                        animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
                        label = "BounceNote"
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .scale(scale)
                            .glassmorphism(
                                cornerRadius = 12.dp,
                                blurRadius = 15f,
                                alpha = 0.5f,
                                borderColor = ink.copy(alpha = 0.08f),
                                backgroundColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                            )
                            .clickable(
                                interactionSource = interactionSource,
                                indication = null,
                                onClick = {}
                            )
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = displaySkill(note.exerciseStatement),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = ink.copy(alpha = 0.70f)
                            )
                            Text(
                                text = dateStr,
                                fontSize = 10.sp,
                                color = ink.copy(alpha = 0.30f)
                            )
                        }
                        
                        note.noteText?.let { text ->
                            Text(
                                text = text,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Light,
                                color = ink.copy(alpha = 0.85f),
                                modifier = Modifier.padding(start = 2.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
private fun GitHubCalendarMap(days: List<HeatmapDay>, ink: androidx.compose.ui.graphics.Color) {
    if (days.isEmpty()) return
    
    // Ensure we take up to 140 days (20 weeks)
    val recentDays = days.takeLast(140)
    val columns = recentDays.chunked(7)
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, ink.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
            .padding(16.dp)
    ) {
        androidx.compose.foundation.lazy.LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(columns) { col ->
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    col.forEach { day ->
                        val intensity = if (day.count <= 0) 0.05f else (0.2f + (day.count.toFloat() / 10f) * 0.8f).coerceIn(0.2f, 1f)
                        val cellColor = if (day.count <= 0) ink.copy(alpha = 0.05f) else androidx.compose.ui.graphics.Color(0xFF39D353).copy(alpha = intensity)
                        
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .background(cellColor, RoundedCornerShape(3.dp))
                        )
                    }
                }
            }
        }
        
        Spacer(Modifier.height(16.dp))
        
        // "Less ... More" legend
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Less", fontSize = 12.sp, color = ink.copy(alpha = 0.5f))
            Spacer(Modifier.width(6.dp))
            listOf(0.05f, 0.3f, 0.5f, 0.7f, 1.0f).forEach { alpha ->
                val cellColor = if (alpha == 0.05f) ink.copy(alpha = 0.05f) else androidx.compose.ui.graphics.Color(0xFF39D353).copy(alpha = alpha)
                Box(
                    modifier = Modifier
                        .padding(horizontal = 2.dp)
                        .size(12.dp)
                        .background(cellColor, RoundedCornerShape(2.dp))
                )
            }
            Spacer(Modifier.width(6.dp))
            Text("More", fontSize = 12.sp, color = ink.copy(alpha = 0.5f))
        }
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    val ink = MaterialTheme.colorScheme.onBackground
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = label, fontSize = 18.sp, color = ink.copy(alpha = 0.70f))
        Text(text = value, fontSize = 20.sp, color = ink.copy(alpha = 0.90f), fontWeight = FontWeight.Bold)
    }
    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.07f))
}

@Composable
private fun VelocityGraph(data: List<Pair<String, Float>>, ink: androidx.compose.ui.graphics.Color) {
    val canvasHeight = 56.dp
    val graphColor = ink.copy(alpha = 0.40f)
    val labelColor = ink.copy(alpha = 0.25f)

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(canvasHeight)
    ) {
        val canvasWidth = size.width
        val canvasHeightPx = size.height

        if (data.size < 2) return@Canvas

        val values = data.map { it.second }
        val minVal = values.minOrNull() ?: 0f
        val maxVal = values.maxOrNull() ?: 1f
        val range = maxVal - minVal + 0.001f

        val points = data.mapIndexed { i, (_, value) ->
            val x = (canvasWidth / (data.size - 1)) * i
            val normalizedValue = (value - minVal) / range
            val y = canvasHeightPx * (1f - normalizedValue)
            x to y
        }

        // Draw line connecting points
        for (i in 0 until points.size - 1) {
            val (x1, y1) = points[i]
            val (x2, y2) = points[i + 1]
            drawLine(
                color = graphColor,
                start = androidx.compose.ui.geometry.Offset(x1, y1),
                end = androidx.compose.ui.geometry.Offset(x2, y2),
                strokeWidth = 1.5f,
            )
        }

        // Draw circles at data points
        points.forEach { (x, y) ->
            drawCircle(
                color = graphColor,
                radius = 2f,
                center = androidx.compose.ui.geometry.Offset(x, y),
            )
        }

        // Draw labels below points
        val paint = android.graphics.Paint().apply {
            textSize = 20f
            alpha = 64
            textAlign = android.graphics.Paint.Align.CENTER
            color = android.graphics.Color.GRAY
        }

        data.forEachIndexed { i, (label, _) ->
            val x = (canvasWidth / (data.size - 1)) * i
            val labelY = canvasHeightPx + 16f
            drawContext.canvas.nativeCanvas.drawText(label, x, labelY, paint)
        }
    }
}

// ── Histórico section ─────────────────────────────────────────────────────────

@Composable
private fun HistoricoSection(
    history: List<SprintHistoryItem>,
    onBack: () -> Unit,
    onGoToSprint: () -> Unit,
    onStartSession: () -> Unit,
) {
    val ink = MaterialTheme.colorScheme.onBackground
    val groups = history.groupBy { groupLabelFor(it.startedAt) }
    val groupOrder = history.map { groupLabelFor(it.startedAt) }.distinct()

    Column(modifier = Modifier.fillMaxSize()) {
        TabPill(onTap = onBack)  // toque no pill = volta para lista

        LazyColumn(modifier = Modifier.fillMaxWidth()) {
            groupOrder.forEach { groupLabel ->
                val items = groups[groupLabel] ?: return@forEach

                item {
                    Text(
                        text = groupLabel,
                        fontSize = 10.sp,
                        letterSpacing = 1.sp,
                        color = ink.copy(alpha = 0.30f),
                        modifier = Modifier.padding(
                            start = 24.dp, end = 24.dp,
                            top = 20.dp, bottom = 4.dp,
                        ),
                    )
                }

                items(items, key = { it.sessionId }) { sprint ->
                    SprintListItem(
                        sprint = sprint,
                        onTap = { if (sprint.isActive) onGoToSprint() else onStartSession() },
                    )
                }
            }
            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}

@Composable
private fun SprintListItem(sprint: SprintHistoryItem, onTap: () -> Unit) {
    val ink = MaterialTheme.colorScheme.onBackground
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 500f),
        label = "BounceListItem"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .glassmorphism(
                cornerRadius = 14.dp,
                alpha = if (sprint.isActive) 0.8f else 0.4f,
                borderColor = ink.copy(alpha = if (sprint.isActive) 0.15f else 0.05f),
                backgroundColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onTap
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = if (sprint.isActive) "—" else " ",
            fontSize = 11.sp,
            color = ink.copy(alpha = 0.40f),
            modifier = Modifier.width(16.dp),
        )
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(
                text = displaySkill(sprint.skill),
                fontSize = 14.sp,
                color = if (sprint.isActive) ink else ink.copy(alpha = 0.70f),
                fontWeight = if (sprint.isActive) FontWeight.Medium else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = buildStatsLine(sprint.exercisesDone, sprint.durationMin, sprint.density, sprint.template),
                fontSize = 11.sp,
                color = ink.copy(alpha = 0.30f),
            )
        }
        Text(
            text = "${sprint.accuracy}%",
            fontSize = 11.sp,
            color = when {
                sprint.accuracy >= 80 -> ink.copy(alpha = 0.55f)
                sprint.accuracy >= 65 -> ink.copy(alpha = 0.38f)
                else -> MaterialTheme.colorScheme.error.copy(alpha = 0.65f)
            },
        )
    }
}

private fun buildStatsLine(exercisesDone: Int, durationMin: Int, density: String, template: String?): String {
    val h = durationMin / 60
    val m = durationMin % 60
    val dur = if (h > 0) "${h}h ${m}min" else "${m}min"
    val zoom = if (density == "exata" && template != null) " · zoom" else ""
    return "$exercisesDone ex · $dur · $density$zoom"
}

private fun weightedAccuracy(
    history: List<SprintHistoryItem>,
    skillAttempts: Map<String, Int>,
    skillAccuracy: Map<String, Float>,
): Int {
    val attemptTotal = skillAttempts.values.sum()
    if (attemptTotal > 0) {
        val weighted = skillAttempts.entries.sumOf { (skill, attempts) ->
            (skillAccuracy[skill] ?: 0f).toDouble() * attempts
        }
        return ((weighted / attemptTotal) * 100).toInt().coerceIn(0, 100)
    }
    val exerciseTotal = history.sumOf { it.exercisesDone }
    if (exerciseTotal <= 0) return -1
    val weighted = history.sumOf { it.accuracy * it.exercisesDone }
    return (weighted / exerciseTotal).coerceIn(0, 100)
}

private fun formatMinutes(totalMinutes: Int): String {
    val h = totalMinutes / 60
    val m = totalMinutes % 60
    return if (h > 0) "${h}h ${m}min" else "${m}min"
}

@Composable
private fun ProfileHeader(ink: androidx.compose.ui.graphics.Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Avatar mockado
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .size(80.dp)
                .background(ink.copy(alpha = 0.08f), androidx.compose.foundation.shape.CircleShape)
                .border(1.dp, ink.copy(alpha = 0.15f), androidx.compose.foundation.shape.CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text("E", fontSize = 36.sp, fontWeight = FontWeight.Bold, color = ink.copy(alpha = 0.5f))
        }
        Column {
            Text(
                text = "Estudante",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = ink.copy(alpha = 0.95f)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "aluno · he/him",
                fontSize = 16.sp,
                color = ink.copy(alpha = 0.55f)
            )
        }
    }
}

@Composable
private fun RecentExercises(history: List<SprintHistoryItem>, ink: androidx.compose.ui.graphics.Color) {
    val recent = history.distinctBy { it.skill }.takeLast(4).reversed()
    if (recent.isEmpty()) return
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
        Text("Exercícios feitos por último", fontSize = 16.sp, color = ink.copy(alpha = 0.85f), fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(16.dp))
        androidx.compose.foundation.lazy.LazyRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(recent) { sprint ->
                Column(
                    modifier = Modifier
                        .width(260.dp)
                        .border(1.dp, ink.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                        .padding(16.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                        Text(
                            text = displaySkill(sprint.skill),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = androidx.compose.ui.graphics.Color(0xFF0969DA), // GitHub Blue
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(8.dp))
                        androidx.compose.foundation.layout.Box(
                            modifier = Modifier
                                .border(1.dp, ink.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("Public", fontSize = 10.sp, color = ink.copy(alpha = 0.6f))
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        androidx.compose.foundation.layout.Box(modifier = Modifier.size(12.dp).background(androidx.compose.ui.graphics.Color(0xFFE34C26), androidx.compose.foundation.shape.CircleShape)) // Mock language color
                        Spacer(Modifier.width(6.dp))
                        Text("Math", fontSize = 12.sp, color = ink.copy(alpha = 0.6f))
                        Spacer(Modifier.width(16.dp))
                        Text("${sprint.exercisesDone} ex", fontSize = 12.sp, color = ink.copy(alpha = 0.6f))
                    }
                }
            }
        }
    }
}
