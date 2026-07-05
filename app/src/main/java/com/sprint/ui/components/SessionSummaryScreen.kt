package com.sprint.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sprint.model.SessionSummary
import com.sprint.model.FragileSkill

@Composable
fun SessionSummaryScreen(
    summary: SessionSummary,
    onContinue: () -> Unit,
    onReviewNow: (String) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Sessão Concluída!",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(24.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        StatCircle(
                            label = "Acertos",
                            value = "${summary.correctCount}/${summary.totalExercises}",
                            color = if (summary.accuracy >= 0.7f) Color(0xFF4CAF50)
                                    else if (summary.accuracy >= 0.4f) Color(0xFFFF9800)
                                    else Color(0xFFF44336),
                        )
                        StatCircle(
                            label = "Precisão",
                            value = "${(summary.accuracy * 100).toInt()}%",
                            color = if (summary.accuracy >= 0.7f) Color(0xFF4CAF50)
                                    else if (summary.accuracy >= 0.4f) Color(0xFFFF9800)
                                    else Color(0xFFF44336),
                        )
                        StatCircle(
                            label = "Tempo médio",
                            value = "${summary.avgTimeMs / 1000}s",
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            if (summary.dominantErrorType != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFFF3E0),
                    ),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Erro mais comum",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color(0xFFE65100),
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = summary.dominantErrorType,
                            fontSize = 16.sp,
                            color = Color(0xFFBF360C),
                        )
                        if (summary.dominantErrorSkill != null) {
                            Text(
                                text = "na skill: ${summary.dominantErrorSkill}",
                                fontSize = 13.sp,
                                color = Color(0xFF795548),
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            if (summary.fragileSkills.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFE8F5E9),
                    ),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Skills para atenção",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                        )
                        Spacer(Modifier.height(8.dp))
                        summary.fragileSkills.forEach { skill ->
                            FragileSkillRow(skill)
                            Spacer(Modifier.height(4.dp))
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = onContinue,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text("Continuar", fontSize = 16.sp)
            }

            if (summary.recommendedSkill != null) {
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { onReviewNow(summary.recommendedSkill) },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text(
                        "Revisar: ${summary.recommendedSkill}",
                        fontSize = 16.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun StatCircle(
    label: String,
    value: String,
    color: Color,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(color.copy(alpha = 0.15f), RoundedCornerShape(36.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = color,
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun FragileSkillRow(skill: FragileSkill) {
    val color = when (skill.priority) {
        "alta" -> Color(0xFFF44336)
        "media" -> Color(0xFFFF9800)
        else -> Color(0xFF4CAF50)
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(color, RoundedCornerShape(4.dp)),
            )
            Spacer(Modifier.width(8.dp))
            Column {
                Text(skill.skill, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                Text(
                    "domínio: ${(skill.effectiveMastery * 100).toInt()}%",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Text(skill.suggestion.take(30), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
