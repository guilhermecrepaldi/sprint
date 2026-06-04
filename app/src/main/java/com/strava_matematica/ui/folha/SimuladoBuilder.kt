package com.strava_matematica.ui.folha

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import com.strava_matematica.model.SessionConfig
import java.util.UUID

@Serializable
data class SimuladoSequenceRule(
    val id: String = UUID.randomUUID().toString(),
    val group: String = "FUNDAMENTOS",
    val skill: String = "soma_subtracao",
    val digits: String = "1",
    val terms: String = "2",
    val numberSet: String = "naturais",
    val quantity: Int = 10
)

@Composable
fun CompactDropdown(
    label: String,
    options: List<Pair<String, String>>,
    selectedKey: String,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = options.firstOrNull { it.first == selectedKey }?.second ?: selectedKey
    
    Box {
        Text(
            text = "$label: $selectedLabel",
            fontSize = 12.sp,
            modifier = Modifier
                .clickable { expanded = true }
                .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f), RoundedCornerShape(4.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp),
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { (key, optLabel) ->
                DropdownMenuItem(
                    text = { Text(optLabel, fontSize = 12.sp) },
                    onClick = {
                        onSelected(key)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun SequenceRuleRow(
    rule: SimuladoSequenceRule,
    onUpdate: (SimuladoSequenceRule) -> Unit,
    onDelete: () -> Unit
) {
    val ink = MaterialTheme.colorScheme.onBackground
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(vertical = 4.dp)
            .background(ink.copy(alpha = 0.02f), RoundedCornerShape(8.dp))
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        CompactDropdown("Cat", SPRINT_GROUPS, rule.group) {
            val newGroup = it
            val firstSkill = SPRINT_SKILLS_BY_GROUP[newGroup]?.firstOrNull()?.first ?: ""
            onUpdate(rule.copy(group = newGroup, skill = firstSkill))
        }
        
        val skills = SPRINT_SKILLS_BY_GROUP[rule.group] ?: emptyList()
        CompactDropdown("Tema", skills, rule.skill) { onUpdate(rule.copy(skill = it)) }
        CompactDropdown("Decimais", SPRINT_DIGITS, rule.digits) { onUpdate(rule.copy(digits = it)) }
        CompactDropdown("Termos", SPRINT_VALUES, rule.terms) { onUpdate(rule.copy(terms = it)) }
        CompactDropdown("Conjunto", SPRINT_NUMBER_SETS, rule.numberSet) { onUpdate(rule.copy(numberSet = it)) }
        
        Row(
            verticalAlignment = Alignment.CenterVertically, 
            modifier = Modifier.background(ink.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
        ) {
            IconButton(onClick = { if (rule.quantity > 1) onUpdate(rule.copy(quantity = rule.quantity - 1)) }, modifier = Modifier.size(28.dp)) {
                Text("-", fontSize = 16.sp, color = ink)
            }
            Text("${rule.quantity}", fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(32.dp), textAlign = TextAlign.Center, color = ink)
            IconButton(onClick = { onUpdate(rule.copy(quantity = rule.quantity + 1)) }, modifier = Modifier.size(28.dp)) {
                Text("+", fontSize = 16.sp, color = ink)
            }
        }
        
        IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
            Icon(Icons.Outlined.Clear, contentDescription = "Remover", tint = Color(0xFFC62828), modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
fun ProceduralSimuladoConfigPage(
    config: SessionConfig,
    showSprintScrolls: MutableState<Boolean>,
    isSimuladoConfigActive: MutableState<Boolean>,
    onStartSimulado: (List<SimuladoSequenceRule>, String, String) -> Unit = { _, _, _ -> },
    modifier: Modifier = Modifier
) {
    val ink = MaterialTheme.colorScheme.onBackground
    val targetTime = remember { mutableStateOf("30") }
    val selectedDifficulty = remember { mutableStateOf("1500") } // starting MMR
    val quantities = remember { mutableStateMapOf<String, Int>() }
    val totalQuestions = quantities.values.sum()

    Column(modifier = modifier.background(MaterialTheme.colorScheme.background)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Simulado: Construção Curricular",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = ink.copy(alpha = 0.8f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("TEMPO ALVO (MINUTOS)", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = ink.copy(alpha = 0.4f))
                    OutlinedTextField(
                        value = targetTime.value,
                        onValueChange = { targetTime.value = it },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("DIFICULDADE INICIAL (MMR)", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = ink.copy(alpha = 0.4f))
                    OutlinedTextField(
                        value = selectedDifficulty.value,
                        onValueChange = { selectedDifficulty.value = it },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp)
                    )
                }
            }
        }

        CurriculumTreeSelector(
            modifier = Modifier.weight(1f),
            mode = SelectorMode.MULTI_QUANTITY,
            quantities = quantities,
            onQuantityChanged = { id, count -> quantities[id] = count }
        )

        Button(
            onClick = {
                val rules = quantities.filter { it.value > 0 }.map {
                    SimuladoSequenceRule(skill = it.key, quantity = it.value)
                }
                if (rules.isNotEmpty()) {
                    isSimuladoConfigActive.value = false
                    showSprintScrolls.value = false
                    onStartSimulado(rules, targetTime.value, selectedDifficulty.value)
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            enabled = totalQuestions > 0
        ) {
            Text(text = "Iniciar Simulado Progressivo ($totalQuestions Questões)", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}
