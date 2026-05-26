package com.strava_matematica.ui.tabs

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.strava_matematica.model.SprintHistoryItem
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

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

// ── Root composable ───────────────────────────────────────────────────────────

@Composable
fun DashboardTab(
    history: List<SprintHistoryItem> = emptyList(),
    skillAttempts: Map<String, Int> = emptyMap(),
    skillAccuracy: Map<String, Float> = emptyMap(),
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

    Column(modifier = Modifier.fillMaxSize()) {
        TabPill(onTap = onBack)  // toque no pill = volta para lista

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
        Text(text = label, fontSize = 14.sp, color = ink.copy(alpha = 0.55f))
        Text(text = value, fontSize = 14.sp, color = ink.copy(alpha = 0.80f), fontWeight = FontWeight.Medium)
    }
    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.07f))
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
                    HorizontalDivider(
                        thickness = 0.5.dp,
                        color = ink.copy(alpha = 0.07f),
                        modifier = Modifier.padding(start = 24.dp),
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onTap)
            .padding(horizontal = 24.dp, vertical = 12.dp),
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
