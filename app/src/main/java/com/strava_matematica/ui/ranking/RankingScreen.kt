package com.strava_matematica.ui.ranking

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.strava_matematica.design.Spacing
import com.strava_matematica.model.RankingEntry
import com.strava_matematica.model.WeeklyRanking
import kotlinx.coroutines.launch

private val Gold   = Color(0xFFFFD700)
private val Silver = Color(0xFFB0B0B0)
private val Bronze = Color(0xFFCD7F32)
private val DimColor = Color(0xFF666666)
private val RowBg  = Color(0xFF1A1A1A)

@Composable
fun RankingScreen(onBack: () -> Unit) {
    var ranking by remember { mutableStateOf<WeeklyRanking?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        scope.launch {
            try {
                // Simula ranking local de alta fidelidade para o Kumon Mode offline
                ranking = WeeklyRanking(
                    weekStart = "2026-05-25",
                    entries = listOf(
                        RankingEntry(rank = 1, studentName = "Arthur Pendragon", slug = "arthur", xpWeek = 450, xpTotal = 3200),
                        RankingEntry(rank = 2, studentName = "Beatriz Silva", slug = "beatriz", xpWeek = 380, xpTotal = 1950),
                        RankingEntry(rank = 3, studentName = "Carlos Oliveira", slug = "carlos", xpWeek = 310, xpTotal = 1500),
                        RankingEntry(rank = 4, studentName = "Você (Offline)", slug = "voce", xpWeek = 280, xpTotal = 1250),
                        RankingEntry(rank = 5, studentName = "Diana Prince", slug = "diana", xpWeek = 150, xpTotal = 800)
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
            .padding(Spacing.lg),
    ) {
        // ── Header ────────────────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "🏆",
                fontSize = 24.sp,
            )
            Spacer(Modifier.width(Spacing.sm))
            Column {
                Text("Ranking semanal", style = MaterialTheme.typography.headlineSmall)
                ranking?.weekStart?.let { date ->
                    Text(
                        text = "desde $date",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                    )
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
                    text = "Ranking indisponível.",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.54f),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            ranking != null -> {
                val entries = ranking!!.entries
                if (entries.isEmpty()) {
                    Text(
                        text = "Nenhum aluno público ainda.\nAtive o perfil público no cadastro.",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.54f),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        itemsIndexed(entries) { _, entry ->
                            RankingRow(entry = entry)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RankingRow(entry: RankingEntry) {
    val medalColor = when (entry.rank) {
        1 -> Gold
        2 -> Silver
        3 -> Bronze
        else -> DimColor
    }
    val medalText = when (entry.rank) {
        1 -> "🥇"
        2 -> "🥈"
        3 -> "🥉"
        else -> "#${entry.rank}"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(RowBg)
            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Medalha / posição
        Box(
            modifier = Modifier.size(36.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (entry.rank <= 3) {
                Text(medalText, fontSize = 22.sp)
            } else {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(medalColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "${entry.rank}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = medalColor,
                    )
                }
            }
        }

        Spacer(Modifier.width(Spacing.md))

        // Nome
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.studentName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (entry.slug != null) {
                Text(
                    text = "@${entry.slug}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                )
            }
        }

        // XP da semana
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "+${entry.xpWeek} XP",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF4CAF50),
            )
            Text(
                text = "${entry.xpTotal} total",
                style = MaterialTheme.typography.labelSmall,
                color = DimColor,
            )
        }
    }
}
