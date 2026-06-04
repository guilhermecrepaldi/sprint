package com.strava_matematica.ui.folha

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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.strava_matematica.model.CurriculumNode
import com.strava_matematica.model.MathCurriculum

enum class SelectorMode {
    SINGLE_SELECTION, // Para Sprint (RadioButton)
    MULTI_QUANTITY // Para Simulado (Contador numérico)
}

@Composable
fun CurriculumTreeSelector(
    modifier: Modifier = Modifier,
    mode: SelectorMode,
    selectedSingleId: String? = null,
    onSingleSelected: (String) -> Unit = {},
    quantities: Map<String, Int> = emptyMap(),
    onQuantityChanged: (String, Int) -> Unit = { _, _ -> }
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        MathCurriculum.tree.forEach { domain ->
            DomainSelectorNode(
                domain = domain,
                mode = mode,
                selectedSingleId = selectedSingleId,
                onSingleSelected = onSingleSelected,
                quantities = quantities,
                onQuantityChanged = onQuantityChanged
            )
        }
    }
}

@Composable
fun DomainSelectorNode(
    domain: CurriculumNode,
    mode: SelectorMode,
    selectedSingleId: String?,
    onSingleSelected: (String) -> Unit,
    quantities: Map<String, Int>,
    onQuantityChanged: (String, Int) -> Unit
) {
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
                    TopicSelectorNode(
                        topic = topic,
                        mode = mode,
                        selectedSingleId = selectedSingleId,
                        onSingleSelected = onSingleSelected,
                        quantities = quantities,
                        onQuantityChanged = onQuantityChanged
                    )
                }
            }
        }
    }
}

@Composable
fun TopicSelectorNode(
    topic: CurriculumNode,
    mode: SelectorMode,
    selectedSingleId: String?,
    onSingleSelected: (String) -> Unit,
    quantities: Map<String, Int>,
    onQuantityChanged: (String, Int) -> Unit
) {
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
                    SubjectSelectorNode(
                        subject = subject,
                        mode = mode,
                        selectedSingleId = selectedSingleId,
                        onSingleSelected = onSingleSelected,
                        quantities = quantities,
                        onQuantityChanged = onQuantityChanged
                    )
                }
            }
        }
    }
}

@Composable
fun SubjectSelectorNode(
    subject: CurriculumNode,
    mode: SelectorMode,
    selectedSingleId: String?,
    onSingleSelected: (String) -> Unit,
    quantities: Map<String, Int>,
    onQuantityChanged: (String, Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(Color.White)
            .clickable {
                if (mode == SelectorMode.SINGLE_SELECTION) {
                    onSingleSelected(subject.id)
                }
            }
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = subject.name,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF2C3E50),
            modifier = Modifier.weight(1f)
        )

        if (mode == SelectorMode.SINGLE_SELECTION) {
            RadioButton(
                selected = (selectedSingleId == subject.id),
                onClick = { onSingleSelected(subject.id) }
            )
        } else {
            val count = quantities[subject.id] ?: 0
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { if (count > 0) onQuantityChanged(subject.id, count - 1) },
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier.size(28.dp)
                ) {
                    Text("-", fontSize = 16.sp)
                }
                Text(
                    text = count.toString().padStart(2, '0'),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(24.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Button(
                    onClick = { if (count < 100) onQuantityChanged(subject.id, count + 1) },
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier.size(28.dp)
                ) {
                    Text("+", fontSize = 16.sp)
                }
            }
        }
    }
}
