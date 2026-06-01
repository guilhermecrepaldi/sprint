package com.strava_matematica.ui.tabs

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.strava_matematica.design.Spacing
import com.strava_matematica.domain.procedural.ProceduralEngine
import com.strava_matematica.model.BackgroundMode
import com.strava_matematica.model.FolhaField
import com.strava_matematica.ui.folha.ExerciseField
import kotlinx.coroutines.delay

@Composable
fun SimuladoTab(
    modifier: Modifier = Modifier
) {
    var isRunning by remember { mutableStateOf(false) }
    var selectedTag by remember { mutableStateOf("equacao_2_grau") }
    var numQuestions by remember { mutableStateOf("5") }
    var targetTimeStr by remember { mutableStateOf("30") }
    var simulatedFields by remember { mutableStateOf<List<FolhaField>>(emptyList()) }
    var elapsedSeconds by remember { mutableStateOf(0) }
    var isBreakMode by remember { mutableStateOf(false) }
    var breakElapsedSeconds by remember { mutableStateOf(0) }
    var hasTakenBreak by remember { mutableStateOf(false) }
    
    val availableTags = listOf(
        "soma_basica", "subtracao_basica", "multiplicacao", "divisao",
        "equacao_2_grau", "polinomios"
    )

    val context = LocalContext.current

    if (!isRunning) {
        // MODO CONFIGURAÇÃO
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(Spacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Configuração do Simulado", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(Spacing.xl))

            Text("Tópico/Matéria:")
            availableTags.forEach { tag ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = selectedTag == tag,
                        onClick = { selectedTag = tag }
                    )
                    Text(tag)
                }
            }

            Spacer(Modifier.height(Spacing.lg))
            OutlinedTextField(
                value = numQuestions,
                onValueChange = { numQuestions = it },
                label = { Text("Número de Questões") },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
            )
            Spacer(Modifier.height(Spacing.sm))
            OutlinedTextField(
                value = targetTimeStr,
                onValueChange = { targetTimeStr = it },
                label = { Text("Tempo Alvo (minutos)") },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
            )

            Spacer(Modifier.height(Spacing.xl))

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Button(onClick = {
                    val count = numQuestions.toIntOrNull() ?: 5
                    val exercises = (0 until count).map { ProceduralEngine.generate(selectedTag, 1500) }
                    
                    var mdContent = "# Simulado - $selectedTag\n\n"
                    exercises.forEachIndexed { index, ex ->
                        mdContent += "### Questão ${index + 1}\n${ex.statement}\n\n"
                    }
                    mdContent += "---\n# Gabarito\n\n"
                    exercises.forEachIndexed { index, ex ->
                        mdContent += "**${index + 1}:** ${ex.expectedAnswer}\n"
                    }

                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, "Simulado Matemática")
                        putExtra(Intent.EXTRA_TEXT, mdContent)
                    }
                    context.startActivity(Intent.createChooser(sendIntent, "Exportar Simulado"))
                }) {
                    Text("Exportar TXT/MD")
                }

                Button(
                    onClick = {
                        val count = numQuestions.toIntOrNull() ?: 5
                        val exercises = (0 until count).map { ProceduralEngine.generate(selectedTag, 1500) }
                        simulatedFields = exercises.mapIndexed { index, ex ->
                            FolhaField(
                                fieldIndex = index,
                                exerciseId = ex.id,
                                statement = ex.statement,
                                skillTags = listOf(ex.primarySkill),
                                expectedAnswer = ex.expectedAnswer
                            )
                        }
                        elapsedSeconds = 0
                        isBreakMode = false
                        breakElapsedSeconds = 0
                        hasTakenBreak = false
                        isRunning = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Iniciar no App", fontWeight = FontWeight.Bold)
                }
            }
        }
    } else {
        // MODO EXECUÇÃO OU PAUSA
        val targetTimeMinutes = targetTimeStr.toIntOrNull() ?: 30
        val needsBreak = targetTimeMinutes > 20
        val breakTriggerTime = (targetTimeMinutes * 60) / 2 // Pausa na metade do tempo

        LaunchedEffect(isRunning, isBreakMode) {
            while(isRunning) {
                delay(1000L)
                if (isBreakMode) {
                    breakElapsedSeconds++
                } else {
                    elapsedSeconds++
                    // Dispara a pausa obrigatória se o tempo passou de 20min e atingiu a metade do tempo
                    if (needsBreak && !hasTakenBreak && elapsedSeconds >= breakTriggerTime) {
                        isBreakMode = true
                    }
                }
            }
        }

        if (isBreakMode) {
            // TELA DE PAUSA COM ADS
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .padding(Spacing.lg),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Pausa Obrigatória para o Cérebro!",
                    color = Color.White,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(Spacing.md))
                Text(
                    text = "Como você solicitou um tempo longo (mais de 20 minutos), você precisa descansar a mente por 2 minutos para otimizar o aprendizado BKT.",
                    color = Color.White.copy(alpha = 0.8f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(Modifier.height(Spacing.xl))

                // Área do ADS
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp)
                        .background(Color.DarkGray, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("--- ESPAÇO DE PATROCÍNIO (ADS) ---", color = Color.LightGray, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Text("Descanse a vista. Beba água.", color = Color.White)
                    }
                }

                Spacer(Modifier.height(Spacing.xl))
                val timeLeft = 120 - breakElapsedSeconds
                if (timeLeft > 0) {
                    Text(
                        text = "O simulado retorna em: ${timeLeft}s",
                        color = Color.Yellow,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Button(
                        onClick = {
                            hasTakenBreak = true
                            isBreakMode = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Retomar Simulado", fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            // TELA DE PROVA (Simulado)
            Column(
            modifier = modifier.fillMaxSize()
        ) {
            // Header Timer
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Tempo: ${elapsedSeconds / 60}m ${elapsedSeconds % 60}s",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Button(onClick = {
                    // Finaliza e volta pra config
                    isRunning = false
                }) {
                    Text("Finalizar Simulado")
                }
            }

            // Lista de Questões
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                itemsIndexed(simulatedFields) { index, field ->
                    // Usando o ExerciseField 50/50 em um container de altura fixa
                    Box(modifier = Modifier
                        .fillMaxWidth()
                        .height(600.dp)) {
                        ExerciseField(
                            field = field,
                            isActive = true,
                            backgroundMode = BackgroundMode.WHITE,
                            penColor = "#1a1a1a",
                            isCompact = true,
                            exercisesPerPage = 1 // Garante proporções de fontes boas
                        )
                    }
                }
            }
        }
    }
}
}
