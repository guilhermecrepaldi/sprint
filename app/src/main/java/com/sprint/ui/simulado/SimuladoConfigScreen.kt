package com.sprint.ui.simulado

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sprint.model.SessionConfig

private val OPTIONS = listOf(5, 10, 15, 20)

@Composable
fun SimuladoConfigScreen(onStart: (SessionConfig) -> Unit) {
    val exercises = remember { mutableIntStateOf(OPTIONS.first()) }
    val minutes = remember { mutableIntStateOf(OPTIONS.first()) }
    val blind = remember { mutableStateOf(true) }

    Column(modifier = Modifier.padding(16.dp)) {
        Text("Simulado", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(8.dp))
        OptionRow("Exercícios", OPTIONS, exercises.intValue) { exercises.intValue = it }
        Spacer(modifier = Modifier.height(8.dp))
        OptionRow("Tempo (min)", OPTIONS, minutes.intValue) { minutes.intValue = it }
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Sem feedback")
            Switch(checked = blind.value, onCheckedChange = { blind.value = it })
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {
                val ms = minutes.intValue * 60_000
                onStart(
                    SessionConfig(
                        durationMode = "timed",
                        durationLimitMs = ms,
                        exercisesPerPage = exercises.intValue,
                        showCorrectCount = false,
                        showPercentage = false,
                        blindMode = blind.value,
                        requireCorrectToAdvance = false,
                    )
                )
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Iniciar") }
    }
}

@Composable
private fun OptionRow(label: String, options: List<Int>, selected: Int, onSelect: (Int) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, modifier = Modifier.weight(1f))
        options.forEach { value ->
            FilterChip(selected = selected == value, onClick = { onSelect(value) }, label = { Text(value.toString()) })
        }
    }
}
