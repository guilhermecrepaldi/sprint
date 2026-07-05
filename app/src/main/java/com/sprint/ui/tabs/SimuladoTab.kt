package com.sprint.ui.tabs

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.sprint.data.local.repository.DeterministicValidator
import com.sprint.domain.procedural.ProceduralEngine
import com.sprint.model.Folha
import com.sprint.model.FolhaField
import com.sprint.model.SessionConfig
import com.sprint.ui.simulado.SimuladoExportScreen
import com.sprint.viewmodel.FolhaUiState
import com.sprint.viewmodel.FolhaViewModel
import com.sprint.viewmodel.SessionViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.random.Random

private val OPTIONS = listOf(5, 10, 15, 20)

@Composable
fun SimuladoTab(
    modifier: Modifier = Modifier,
    config: SessionConfig,
    sessionViewModel: SessionViewModel,
    folhaViewModel: FolhaViewModel,
    folhaState: FolhaUiState,
    onGoToSprint: () -> Unit,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var screenMode by remember { mutableStateOf("config") }
    var qtyExercises by remember { mutableIntStateOf(OPTIONS.first()) }
    var minutes by remember { mutableIntStateOf(OPTIONS.first()) }
    var fields by remember { mutableStateOf<List<FolhaField>>(emptyList()) }
    var isGen by remember { mutableStateOf(false) }

    if (screenMode == "config") {
        Column(Modifier.padding(24.dp)) {
            Text("Simulado", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(12.dp))
            Text("Questoes"); OptionRow(OPTIONS, qtyExercises) { qtyExercises = it }
            Spacer(Modifier.height(12.dp))
            Text("Tempo (min)"); OptionRow(OPTIONS, minutes) { minutes = it }
            Spacer(Modifier.height(20.dp))
            if (isGen) { CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally)); Text("Gerando...") }
            else Button(onClick = {
                isGen = true
                coroutineScope.launch {
                    val exs = withContext(Dispatchers.IO) {
                        ProceduralEngine.randomInstance = Random.Default
                        (0 until qtyExercises).map { i -> ProceduralEngine.generate("soma_subtracao", 500 + i * 200) }
                    }
                    fields = exs.mapIndexed { i, e ->
                        FolhaField(i, e.id, statement = e.statement, skillTags = listOf(e.primarySkill), expectedAnswer = e.expectedAnswer)
                    }
                    isGen = false
                    screenMode = "export"
                }
            }, Modifier.fillMaxWidth()) { Text("Gerar Simulado") }
        }
        return@SimuladoTab
    }

    if (screenMode == "export") {
        val folha = remember(fields) {
            Folha("simulado_" + System.currentTimeMillis(), 0, 1.0, fields)
        }
        SimuladoExportScreen(folha, onDoInsideApp = {
            // Cria sessao com config do simulado e vai pro Sprint
            val simConfig = com.sprint.model.SessionConfig(
                durationMode = "timed",
                durationLimitMs = minutes * 60000,
                exercisesPerPage = fields.size,
                blindMode = true,
                showCorrectCount = false,
                showPercentage = false,
                requireCorrectToAdvance = false,
                penColor = "#1a1a1a",
                penWidth = 2.2f,
                guideMode = "nenhuma",
            )
            sessionViewModel.updateConfig(simConfig)
            // Injeta a folha no ViewModel — mesma engine do Sprint
            folhaViewModel.resetForFolha(folha.folhaId)
            // Cria sessao na DB, depois redireciona ao Sprint
            sessionViewModel._setSimuladoState(folha, simConfig, onReady = onGoToSprint)
        })
        return@SimuladoTab
    }
}

@Composable
private fun OptionRow(options: List<Int>, selected: Int, onSelect: (Int) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { v -> FilterChip(selected = (v == selected), onClick = { onSelect(v) }, label = { Text(v.toString()) }) }
    }
}
