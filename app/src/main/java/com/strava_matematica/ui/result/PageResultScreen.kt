package com.strava_matematica.ui.result

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.strava_matematica.design.FocusColors
import com.strava_matematica.design.Spacing
import com.strava_matematica.model.SubmitResult
import com.strava_matematica.ui.components.ThermometerView

@Composable
fun PageResultScreen(
    result: SubmitResult,
    onNext: () -> Unit,
    onSummary: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Text("Página concluída", style = MaterialTheme.typography.headlineMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
            Text("Score ${result.pageScore}", style = MaterialTheme.typography.titleMedium)
            ThermometerView(value = result.thermometer.value, modifier = Modifier.weight(1f))
        }
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            items(result.results) { item ->
                Column(
                    Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f),
                            shape = RoundedCornerShape(8.dp),
                        )
                        .padding(Spacing.md),
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        Icon(
                            imageVector = if (item.isCorrect) Icons.Outlined.CheckCircle else Icons.Outlined.ErrorOutline,
                            contentDescription = null,
                            tint = if (item.isCorrect) FocusColors.Progress else FocusColors.Error,
                        )
                        Text("%02d".format(item.fieldIndex + 1), style = MaterialTheme.typography.titleMedium)
                        Text("Campo", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f))
                        Spacer(Modifier.weight(1f))
                        Text("${item.score}", style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(Modifier.width(Spacing.sm))
                    Text("Reconhecida: ${item.recognizedAnswer ?: "Não consegui ler"}")
                    Text("Esperada: ${item.expectedAnswer}", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f))
                    if (item.errorType != null) {
                        Text(item.errorType, color = FocusColors.Warning, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
        Button(
            onClick = if (result.sessionStatus == "finished") onSummary else onNext,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (result.sessionStatus == "finished") "Ver resumo" else "Próxima folha")
        }
    }
}
