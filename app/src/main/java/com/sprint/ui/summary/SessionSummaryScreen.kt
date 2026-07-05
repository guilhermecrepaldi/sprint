package com.sprint.ui.summary

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.sprint.design.Spacing
import com.sprint.model.SubmitResult

@Composable
fun SessionSummaryScreen(
    result: SubmitResult?,
    onNewSession: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Text("Treino concluído", style = MaterialTheme.typography.headlineMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
            Metric("Score médio", result?.pageScore?.toString() ?: "-")
            Metric("Termômetro", result?.thermometer?.value?.let { "%.2f".format(it) } ?: "-")
        }
        Text("Skills para revisar", style = MaterialTheme.typography.titleMedium)
        Text("A sessão demo marcou equação de 1º grau como foco principal.")
        Column(Modifier.weight(1f)) {}
        Button(onClick = onNewSession, modifier = Modifier.fillMaxWidth()) {
            Text("Novo treino")
        }
        OutlinedButton(onClick = onNewSession, modifier = Modifier.fillMaxWidth()) {
            Text("Ajustar configuração")
        }
    }
}

@Composable
private fun Metric(label: String, value: String) {
    Column {
        Text(label, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f))
        Text(value, style = MaterialTheme.typography.headlineMedium)
    }
}
