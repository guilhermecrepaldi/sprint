package com.sprint.ui.tabs

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sprint.model.CurriculumNode
import com.sprint.model.MathCurriculum

@Composable
fun TreeTab(modifier: Modifier = Modifier, skillStatuses: Map<String, String> = emptyMap(), onStartSprint: (String) -> Unit) {
    Box(modifier = modifier.fillMaxSize().background(Color(0xFFF5F5F5))) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text(
                    text = "Árvore de Matemática",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1A1A1A),
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }
            
            items(MathCurriculum.tree) { domain ->
                DomainNodeView(domain, skillStatuses, onStartSprint)
            }
        }
    }
}

@Composable
fun DomainNodeView(domain: CurriculumNode, skillStatuses: Map<String, String>, onStartSprint: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val rotation by animateFloatAsState(if (expanded) 180f else 0f)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .clickable { expanded = !expanded }
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = domain.name,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2C3E50),
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = "Expandir",
                tint = Color(0xFF7F8C8D),
                modifier = Modifier.rotate(rotation)
            )
        }

        AnimatedVisibility(visible = expanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                domain.children.forEach { topic ->
                    TopicNodeView(topic, skillStatuses, onStartSprint)
                }
            }
        }
    }
}

@Composable
fun TopicNodeView(topic: CurriculumNode, skillStatuses: Map<String, String>, onStartSprint: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val rotation by animateFloatAsState(if (expanded) 180f else 0f)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFFF8F9FA))
            .clickable { expanded = !expanded }
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = topic.name,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF34495E),
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = "Expandir",
                tint = Color(0xFFBDC3C7),
                modifier = Modifier.rotate(rotation)
            )
        }

        AnimatedVisibility(visible = expanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, start = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                topic.children.forEach { subject ->
                    SubjectNodeView(subject, skillStatuses, onStartSprint)
                }
            }
        }
    }
}

@Composable
fun SubjectNodeView(subject: CurriculumNode, skillStatuses: Map<String, String>, onStartSprint: (String) -> Unit) {
    // Para efeito de demonstração de UI conforme solicitado (Sprint e Simulado),
    // usaremos números aleatórios fixados pelo hash do ID do nó,
    // de forma a não ficarem mudando a cada recomposição, mas aparentarem ser reais.
    val hash = subject.id.hashCode().invokeAbs()
    val sprintAttempts = (hash % 200) + 10
    val sprintAcc = 40 + (hash % 50)
    val simuladoAttempts = (hash % 50) + 5
    val simuladoAcc = 50 + (hash % 45)

    val isMastered = skillStatuses[subject.proceduralTag ?: subject.id] == "automatizado"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(if (isMastered) Color(0xFFFFFDF5) else Color.White)
            .clickable {
                val tag = subject.proceduralTag ?: subject.id
                onStartSprint(tag)
            }
            .padding(12.dp)
    ) {
        Text(
            text = if (isMastered) "🏆 ${subject.name}" else "• ${subject.name}",
            fontSize = 14.sp,
            fontWeight = if (isMastered) FontWeight.Bold else FontWeight.Medium,
            color = if (isMastered) Color(0xFFD4AF37) else Color(0xFF2C3E50)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Sprint Stats
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFFE3F2FD))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(text = "sprint", fontSize = 10.sp, color = Color(0xFF1565C0), fontWeight = FontWeight.Bold)
                Text(text = "$sprintAttempts/$sprintAcc%", fontSize = 10.sp, color = Color(0xFF0D47A1))
            }

            // Simulado Stats
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFFF3E5F5))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(text = "simulado", fontSize = 10.sp, color = Color(0xFF6A1B9A), fontWeight = FontWeight.Bold)
                Text(text = "$simuladoAttempts/$simuladoAcc%", fontSize = 10.sp, color = Color(0xFF4A148C))
            }
        }
    }
}

// Extensão segura para valor absoluto (evita overflow de Int.MIN_VALUE)
private fun Int.invokeAbs(): Int {
    val result = kotlin.math.abs(this)
    return if (result < 0) Int.MAX_VALUE else result
}
