package com.sprint.ui.simulado

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SimuladoResultScreen(totalTimeMs: Long, exercisesAttempted: Int, onBack: () -> Unit) {
    val minutes = totalTimeMs / 60_000L
    Column(modifier = Modifier.padding(16.dp)) {
        Text("Resultado do simulado", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(12.dp))
        Text("Tempo: %02d:%02d".format(minutes / 60, minutes % 60))
        Text("Exercícios tentados: $exercisesAttempted")
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Voltar") }
    }
}
