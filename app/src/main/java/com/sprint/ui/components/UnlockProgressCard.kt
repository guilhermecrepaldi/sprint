package com.sprint.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class UnlockPrereqProgress(
    val skill: String,
    val mastery: Float,
    val exercises: Int,
    val masteryNeeded: Float = 0.90f,
    val exercisesNeeded: Int = 100,
)

data class UnlockSkillProgress(
    val skill: String,
    val unlocked: Boolean,
    val prerequisites: List<UnlockPrereqProgress>,
)

@Composable
fun UnlockProgressCard(
    skills: List<UnlockSkillProgress>,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Progresso de Desbloqueio",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
            )
            Spacer(Modifier.height(12.dp))
            val locked = skills.filter { !it.unlocked }.take(5)
            if (locked.isEmpty()) {
                Text(
                    text = "Todas as skills disponíveis!",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                locked.forEach { skill ->
                    LockedSkillCard(skill)
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun LockedSkillCard(skill: UnlockSkillProgress) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "🔒 ${skill.skill}",
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
            )
            Spacer(Modifier.height(8.dp))
            skill.prerequisites.forEach { prereq ->
                PrereqBar(prereq)
                Spacer(Modifier.height(6.dp))
            }
        }
    }
}

@Composable
private fun PrereqBar(prereq: UnlockPrereqProgress) {
    Column {
        Text(
            text = prereq.skill,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(2.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                val masteryRatio = (prereq.mastery / prereq.masteryNeeded).coerceAtMost(1f)
                @Suppress("DEPRECATION")
                LinearProgressIndicator(
                    progress = masteryRatio,
                    modifier = Modifier.fillMaxWidth().height(6.dp),
                    color = if (prereq.mastery >= prereq.masteryNeeded) Color(0xFF4CAF50) else Color(0xFFFF9800),
                    trackColor = Color(0xFFE0E0E0),
                )
            }
            Text(
                text = "domínio: ${(prereq.mastery * 100).toInt()}%",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(2.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                val exRatio = (prereq.exercises.toFloat() / prereq.exercisesNeeded).coerceAtMost(1f)
                @Suppress("DEPRECATION")
                LinearProgressIndicator(
                    progress = exRatio,
                    modifier = Modifier.fillMaxWidth().height(6.dp),
                    color = if (prereq.exercises >= prereq.exercisesNeeded) Color(0xFF4CAF50) else Color(0xFF2196F3),
                    trackColor = Color(0xFFE0E0E0),
                )
            }
            Text(
                text = "${prereq.exercises}/${prereq.exercisesNeeded}",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
