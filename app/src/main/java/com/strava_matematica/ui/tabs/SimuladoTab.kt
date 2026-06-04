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
import com.strava_matematica.model.SessionConfig
import com.strava_matematica.ui.folha.ExerciseField
import kotlinx.coroutines.delay

@Composable
fun SimuladoTab(
    modifier: Modifier = Modifier,
    config: SessionConfig
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
    var isReviewMode by remember { mutableStateOf(false) }
    val typedAnswers = remember { mutableStateMapOf<Int, String>() }
    
    val availableTags = listOf(
        "soma_basica", "subtracao_basica", "multiplicacao", "divisao",
        "equacao_2_grau", "polinomios"
    )
    val context = LocalContext.current

    if (!isRunning) {
        // MODO CONFIGURAÇÃO
        val showSprintScrolls = remember { mutableStateOf(true) }
        val isSimuladoConfigActive = remember { mutableStateOf(true) }

        com.strava_matematica.ui.folha.ProceduralSimuladoConfigPage(
            config = config,
            showSprintScrolls = showSprintScrolls,
            isSimuladoConfigActive = isSimuladoConfigActive,
            onStartSimulado = { rules, targetTimeStrValue, difficulty ->
                typedAnswers.clear()
                val exercises = mutableListOf<com.strava_matematica.domain.procedural.ProceduralExercise>()
                
                // Geração Procedural Livre - O usuário gera o próprio simulado aleatoriamente
                ProceduralEngine.randomInstance = kotlin.random.Random.Default
                
                for (rule in rules) {
                    val baseMmr = difficulty.toIntOrNull() ?: 1000
                    val step = if (rule.quantity > 1) 1500 / rule.quantity else 0
                    for (i in 0 until rule.quantity) {
                        val progressiveMmr = baseMmr + (i * step)
                        exercises.add(ProceduralEngine.generate(rule.skill, progressiveMmr))
                    }
                }
                
                // Retorna ao aleatório real para a Sprint livre
                ProceduralEngine.randomInstance = kotlin.random.Random.Default
                
                simulatedFields = exercises.mapIndexed { index, ex ->
                    FolhaField(
                        fieldIndex = index,
                        exerciseId = ex.id,
                        statement = ex.statement,
                        skillTags = listOf(ex.primarySkill),
                        expectedAnswer = ex.expectedAnswer
                    )
                }
                
                targetTimeStr = targetTimeStrValue
                elapsedSeconds = 0
                isBreakMode = false
                breakElapsedSeconds = 0
                hasTakenBreak = false
                isReviewMode = false
                isRunning = true
            },
            modifier = modifier
        )
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
                // Header da Prova ou Revisão
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isReviewMode) {
                        Text(
                            text = "Gabarito e Correção",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2E7D32)
                        )
                        Button(onClick = {
                            isRunning = false
                            isReviewMode = false
                        }) {
                            Text("Nova Configuração")
                        }
                    } else {
                        Text(
                            text = "Tempo: ${elapsedSeconds / 60}m ${elapsedSeconds % 60}s",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Button(onClick = {
                            isReviewMode = true
                        }) {
                            Text("Finalizar Simulado")
                        }
                    }
                }

                // Lista de Questões
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (isReviewMode) {
                        itemsIndexed(simulatedFields) { index, field ->
                            val userAnswer = typedAnswers[field.fieldIndex]?.trim() ?: ""
                            val expectedAnswer = field.expectedAnswer?.trim() ?: ""
                            val isCorrect = userAnswer.isNotBlank() && com.strava_matematica.data.local.repository.DeterministicValidator.evaluate(userAnswer, expectedAnswer, "exact")
                            
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        color = if (isCorrect) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${index + 1}.",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black.copy(alpha = 0.7f)
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Sua resposta: ${if (userAnswer.isEmpty()) "(em branco)" else userAnswer}",
                                        fontSize = 16.sp,
                                        color = if (isCorrect) Color(0xFF2E7D32) else Color(0xFFC62828)
                                    )
                                    if (!isCorrect) {
                                        Text(
                                            text = "Gabarito: $expectedAnswer",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color(0xFF2E7D32)
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        itemsIndexed(simulatedFields) { index, field ->
                            // Tablet-First: Otimizado para Galaxy Tab S6 Lite (tela grande e uso de caneta stylus)
                            Box(modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 600.dp, max = 800.dp)) {
                                ExerciseField(
                                    field = field,
                                    isActive = true,
                                    backgroundMode = BackgroundMode.WHITE,
                                    penColor = "#1a1a1a",
                                    isCompact = true,
                                    exercisesPerPage = 1, // Garante proporções de fontes boas
                                    isBlindMode = true,
                                    typedAnswer = typedAnswers[field.fieldIndex] ?: "",
                                    onTypedAnswerChange = { answer -> typedAnswers[field.fieldIndex] = answer }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
