package com.sprint.ui.simulado

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.sprint.model.Folha

@Composable
fun SimuladoExportScreen(
    folha: Folha,
    onDoInsideApp: () -> Unit,
) {
    val context = LocalContext.current

    Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Simulado gerado!", style = MaterialTheme.typography.headlineSmall)
        Text("Questões: ${folha.fields.size}", style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.height(8.dp))

        // Opção 1: Exportar com gabarito
        Button(
            onClick = {
                val sb = StringBuilder()
                sb.appendLine("SIMULADO - ${folha.folhaId}")
                sb.appendLine()
                folha.fields.forEachIndexed { i, f ->
                    sb.appendLine("${i + 1}. ${f.statement}")
                    sb.appendLine("   Resposta: ${f.expectedAnswer ?: "---"}")
                    sb.appendLine()
                }
                val text = sb.toString()
                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, text)
                }
                context.startActivity(Intent.createChooser(sendIntent, "Exportar simulado"))
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Exportar questões + gabarito")
        }

        // Opção 2: Fazer no app
        OutlinedButton(
            onClick = onDoInsideApp,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
        ) {
            Text("Fazer dentro do app")
        }
    }
}
