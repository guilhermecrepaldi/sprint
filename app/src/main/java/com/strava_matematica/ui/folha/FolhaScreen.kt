package com.strava_matematica.ui.folha

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.itemsIndexed as gridItemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.Button
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import com.strava_matematica.design.Spacing
import com.strava_matematica.model.Folha
import com.strava_matematica.viewmodel.ResultMark
import com.strava_matematica.model.FolhaField
import com.strava_matematica.model.GestureConfig
import com.strava_matematica.model.PenEvent
import com.strava_matematica.model.SessionConfig
import com.strava_matematica.viewmodel.SprintNote
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

@Composable
fun FolhaScreen(
    folha: Folha,
    config: SessionConfig,
    currentExerciseIndex: Int = 0,
    fieldScratchStrokes: Map<Int, List<List<Offset>>> = emptyMap(),
    fieldAnswerStrokes: Map<Int, List<List<Offset>>> = emptyMap(),
    fieldScratchRedoStacks: Map<Int, List<List<Offset>>> = emptyMap(),
    fieldAnswerRedoStacks: Map<Int, List<List<Offset>>> = emptyMap(),
    fieldTypedAnswers: Map<Int, String> = emptyMap(),
    onAdvance: () -> Unit,
    selectedSkillTag: String = "soma_subtracao",
    densityLevel: String = "medium",
    onApplySprintScrollSelection: (skillTag: String, density: String, exactCurrent: Boolean, difficultyStart: Double?, digitsCount: Int, valuesCount: Int, numberSet: String, field: FolhaField?) -> Unit = { _, _, _, _, _, _, _, _ -> },
    onSyncScratch: (fieldIndex: Int, strokes: List<List<Offset>>, redoStack: List<List<Offset>>) -> Unit = { _, _, _ -> },
    onSyncAnswer: (fieldIndex: Int, strokes: List<List<Offset>>, redoStack: List<List<Offset>>) -> Unit = { _, _, _ -> },
    onTypedAnswerChange: (fieldIndex: Int, answer: String) -> Unit = { _, _ -> },
    onPenEvent: (fieldIndex: Int, event: PenEvent) -> Unit = { _, _ -> },
    onConfigChange: (SessionConfig) -> Unit = {},
    onEndSession: () -> Unit = {},
    sessionCorrect: Int = 0,
    sessionTotal: Int = 0,
    sessionStartedAtMs: Long = System.currentTimeMillis(),
    sessionId: String? = null,
    folhaIndex: Int = 0,
    onAddNote: (SprintNote) -> Unit = {},
    gestureConfig: GestureConfig = GestureConfig(),
    retrySignal: Int = 0,
    recentResults: List<ResultMark> = emptyList(),
    skillAccuracy: Map<String, Float> = emptyMap(),
    skillAttempts: Map<String, Int> = emptyMap(),
    skillFluency: Map<String, Float> = emptyMap(),
    masteryDetected: Boolean = false,
    suggestedNextSkill: String? = null,
    scoreRiskVisible: Boolean = false,
    onDismissMastery: () -> Unit = {},
    onAdvanceToNextSkill: () -> Unit = {},
    onStayInSprintAfterScoreWarning: () -> Unit = {},
    onAdjustSprintAfterScoreWarning: () -> Unit = {},
) {
    val field = folha.fields[currentExerciseIndex]
    // retrySignal muda quando o usuário erra + requireCorrectToAdvance=true — força recomposição do canvas.
    val exerciseRenderKey = "${folha.folhaId}:${field.exerciseId}:${field.fieldIndex}:$retrySignal"
    // Signals reset whenever the exercise changes, giving each field a fresh canvas state.
    val clearSignal = remember(exerciseRenderKey) { mutableIntStateOf(0) }
    val undoSignal = remember(exerciseRenderKey) { mutableIntStateOf(0) }
    val redoSignal = remember(exerciseRenderKey) { mutableIntStateOf(0) }
    val isErasing = remember { mutableStateOf(false) }
    val showSessionRegister = remember { mutableStateOf(false) }
    val showSprintScrolls = remember { mutableStateOf(false) }
    val showNotePad = remember { mutableStateOf(false) }
    val noteInput = remember { mutableStateOf("") }

    // --- ENGINE DE PILOTO AUTOMÁTICO E SIMULADO ---
    val isSimulationActive = remember { mutableStateOf(false) }
    val isSimuladoConfigActive = remember { mutableStateOf(false) }

    LaunchedEffect(isSimulationActive.value, showSprintScrolls.value, selectedSkillTag, currentExerciseIndex) {
        if (!isSimulationActive.value || showSprintScrolls.value) return@LaunchedEffect

        // 1. Obter a resposta correta da questão
        val expected = field.expectedAnswer.orEmpty()
        if (expected.isEmpty()) return@LaunchedEffect

        // 2. Aguarda delay para quem está assistindo ler o enunciado
        kotlinx.coroutines.delay(1800L)

        // 3. Simular a digitação da resposta correta na caixa
        onTypedAnswerChange(field.fieldIndex, expected)

        // 4. Aguarda delay pós-escrita
        kotlinx.coroutines.delay(1200L)

        // 5. Simular o clique/avanço no EnterSquare (chama o onAdvance)
        onAdvance()

        // 6. Após avançar o exercício, calcular a próxima skill na taxonomia flat das 34 skills do catálogo!
        val allSkills = SPRINT_SKILLS_BY_GROUP.values.flatten().map { it.first }
        val currentIdx = allSkills.indexOf(selectedSkillTag).coerceAtLeast(0)
        val nextIdx = currentIdx + 1

        if (nextIdx < allSkills.size) {
            val nextSkill = allSkills[nextIdx]
            kotlinx.coroutines.delay(1500L) // Aguarda a renderização do novo estado da folha
            
            // Simular a troca de matéria sequencial via onApplySprintScrollSelection!
            onApplySprintScrollSelection(
                nextSkill,
                densityLevel,
                false, // exactCurrent = false
                null,  // difficultyStart = null
                config.digitsCount,
                config.valuesCount,
                config.numberSet,
                field
            )
        } else {
            // Esgotou todas as 34 matérias! Desativa a simulação e exibe sucesso
            isSimulationActive.value = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        if (isSimulationActive.value) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(androidx.compose.ui.graphics.Color(0xFF388E3C).copy(alpha = 0.95f))
                    .padding(horizontal = Spacing.md, vertical = Spacing.xs),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🤖 PILOTO AUTOMÁTICO ATIVO · Simulando usuário navegando pelas matérias...",
                    color = androidx.compose.ui.graphics.Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "PARAR",
                    color = androidx.compose.ui.graphics.Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier
                        .clickable { isSimulationActive.value = false }
                        .padding(4.dp)
                )
            }
        }

        // ── Full-screen exercise ─────────────────────────────────────────────
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .sprintGestureInput(
                    gestureConfig = gestureConfig,
                    onAdvance = onAdvance,
                    onToggleEraser = { isErasing.value = !isErasing.value },
                ),
        ) {
            val ink = MaterialTheme.colorScheme.onBackground
            // Layout 50/50 Global Sprint
            val limit = config.exercisesPerPage.coerceAtMost(20).coerceAtMost(folha.fields.size)
            val displayedFields = folha.fields.take(limit)
            
            Column(modifier = Modifier.fillMaxSize()) {
                // METADE SUPERIOR: Grade de exercícios com scroll vertical
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                        columns = androidx.compose.foundation.lazy.grid.GridCells.Adaptive(minSize = 340.dp),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(Spacing.md),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        gridItemsIndexed(displayedFields) { index, currentField ->
                            val itemKey = "${folha.folhaId}:${currentField.exerciseId}:${currentField.fieldIndex}"
                            androidx.compose.runtime.key(itemKey) {
                                ExerciseField(
                                    field = currentField,
                                    isActive = true,
                                    backgroundMode = config.backgroundMode,
                                    penColor = config.penColor,
                                    penWidth = config.penWidth,
                                    modifier = Modifier.fillMaxWidth().height(80.dp),
                                    isKPlus = false,
                                    isCompact = true,
                                    exercisesPerPage = config.exercisesPerPage,
                                    initialScratchStrokes = emptyList(),
                                    initialAnswerStrokes = fieldAnswerStrokes[currentField.fieldIndex].orEmpty(),
                                    initialScratchRedoStack = emptyList(),
                                    initialAnswerRedoStack = fieldAnswerRedoStacks[currentField.fieldIndex].orEmpty(),
                                    typedAnswer = fieldTypedAnswers[currentField.fieldIndex].orEmpty(),
                                    clearSignal = clearSignal.intValue, // Local clear for answer
                                    undoSignal = undoSignal.intValue,
                                    redoSignal = redoSignal.intValue,
                                    isErasing = isErasing.value,
                                    onSyncScratch = { _, _ -> },
                                    onSyncAnswer = { strokes, redo -> onSyncAnswer(currentField.fieldIndex, strokes, redo) },
                                    onTypedAnswerChange = { answer -> onTypedAnswerChange(currentField.fieldIndex, answer) },
                                    onPenEvent = { event -> onPenEvent(currentField.fieldIndex, event) },
                                    userGuideMode = config.guideMode
                                )
                            }
                        }
                    }
                }
                
                // Divisória Visual
                Box(modifier = Modifier.fillMaxWidth().height(2.dp).background(ink.copy(alpha = 0.1f)))

                // METADE INFERIOR: Rascunho Global
                Box(modifier = Modifier.weight(1f).fillMaxWidth().background(ink.copy(alpha = 0.02f))) {
                    // O rascunho global será mapeado para o índice 0.
                    InkCanvas(
                        modifier = Modifier.matchParentSize().padding(4.dp),
                        penColor = config.penColor,
                        penWidth = config.penWidth,
                        enabled = true,
                        isErasing = isErasing.value,
                        clearSignal = clearSignal.intValue, // Botão apagar tela
                        undoSignal = undoSignal.intValue,   // Botão desfazer
                        redoSignal = redoSignal.intValue,
                        initialStrokes = fieldScratchStrokes[0].orEmpty(), // Usa o índice 0 como repositório
                        initialRedoStack = fieldScratchRedoStacks[0].orEmpty(),
                        guideMode = config.guideMode.takeIf { it != "nenhuma" } ?: "single",
                        onSyncStrokes = { strokes, redo -> onSyncScratch(0, strokes, redo) },
                        onPenEvent = { event -> onPenEvent(0, event) }
                    )
                    
                    // Botões de Desfazer/Apagar Flutuantes no Rascunho
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        androidx.compose.material3.TextButton(
                            onClick = { undoSignal.intValue++ },
                            modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                        ) {
                            Text("Desfazer", fontWeight = FontWeight.Bold, color = ink.copy(alpha = 0.8f))
                        }
                        androidx.compose.material3.TextButton(
                            onClick = { clearSignal.intValue++ },
                            modifier = Modifier.background(Color(0xFFFFEBEE), RoundedCornerShape(8.dp))
                        ) {
                            Text("Apagar Tela", fontWeight = FontWeight.Bold, color = Color(0xFFC62828))
                        }
                    }
                }
            }

            // Google AI Studio settings overlay lateral direito
            if (showSprintScrolls.value) {
                // Scrim sutil de escurecimento de fundo
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.35f))
                        .clickable { showSprintScrolls.value = false }
                )
                
                if (isSimuladoConfigActive.value) {
                    SimuladoConfigPage(
                        config = config,
                        showSprintScrolls = showSprintScrolls,
                        isSimuladoConfigActive = isSimuladoConfigActive,
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(360.dp)
                            .align(Alignment.CenterEnd)
                            .background(
                                color = MaterialTheme.colorScheme.background.copy(alpha = 0.98f),
                                shape = RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp)
                            )
                    )
                } else {
                    SprintScrollConfigPage(
                        config = config,
                        selectedSkillTag = selectedSkillTag,
                        densityLevel = densityLevel,
                        skillAccuracy = skillAccuracy,
                        skillAttempts = skillAttempts,
                        skillFluency = skillFluency,
                        isSimulationActive = isSimulationActive,
                        showSprintScrolls = showSprintScrolls,
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(360.dp)
                            .align(Alignment.CenterEnd)
                            .background(
                                color = MaterialTheme.colorScheme.background.copy(alpha = 0.98f),
                                shape = RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp)
                            ),
                        onApply = { skill, density, layoutMode, difficultyStart, digits, values, numberSet ->
                            showSprintScrolls.value = false
                            val effectiveDensity = if (layoutMode == "kplus") "kplus" else density
                            onApplySprintScrollSelection(skill, effectiveDensity, layoutMode == "exact", difficultyStart, digits, values, numberSet, field)
                        },
                    )
                }
            }
            if (!showSprintScrolls.value && config.showEraserButton) {
                EraserToggle(
                    isErasing = isErasing.value,
                    onToggle = { isErasing.value = !isErasing.value },
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = Spacing.sm),
                )
            }
            // EnterSquare flutua no meio vertical, colado na borda direita
            if (!showSprintScrolls.value) {
                EnterSquare(
                    onAdvance = onAdvance,
                    onTripleTap = { showSprintScrolls.value = true },
                    onRegisterVisibilityChange = { showSessionRegister.value = it },
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = Spacing.sm),
                )
            }
            if (!showSprintScrolls.value && showSessionRegister.value) {
                SessionRegisterOverlay(
                    correct = sessionCorrect,
                    total = sessionTotal,
                    startedAtMs = sessionStartedAtMs,
                    currentExercise = sessionTotal + 1,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 92.dp),
                )
            }
            // Mastery chip — 5 corretos seguidos, sugestão não-bloqueante
            if (!showSprintScrolls.value && masteryDetected && suggestedNextSkill != null) {
                MasteryChip(
                    nextSkill = suggestedNextSkill,
                    onAdvance = onAdvanceToNextSkill,
                    onDismiss = onDismissMastery,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = Spacing.sm),
                )
            }
            if (!showSprintScrolls.value && scoreRiskVisible) {
                ScoreRiskChip(
                    onStay = onStayInSprintAfterScoreWarning,
                    onAdjust = {
                        onAdjustSprintAfterScoreWarning()
                        showSprintScrolls.value = true
                    },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = Spacing.md + 42.dp),
                )
            }
            // Histórico visual dos últimos 7 resultados — canto inferior esquerdo
            if (!showSprintScrolls.value && recentResults.isNotEmpty()) {
                ResultHistoryRow(
                    results = recentResults,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = Spacing.md, bottom = Spacing.md),
                )
            }
            // Toggle inline livre/precisa acertar — ícone pequeno no canto superior direito
            // Sincronizado com FolhaSettingsSheet via config.requireCorrectToAdvance
            if (!showSprintScrolls.value) {
                IconButton(
                    onClick = { onConfigChange(config.copy(requireCorrectToAdvance = !config.requireCorrectToAdvance)) },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = Spacing.sm, end = 52.dp)
                        .size(32.dp),
                ) {
                    Icon(
                        imageVector = if (config.requireCorrectToAdvance) Icons.Outlined.Lock else Icons.Outlined.LockOpen,
                        contentDescription = if (config.requireCorrectToAdvance) "Precisa acertar para avançar" else "Avança mesmo errando",
                        tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.30f),
                        modifier = Modifier.size(16.dp),
                    )
                }

                // Botão de bloco de notas rápido durante o Sprint
                IconButton(
                    onClick = { showNotePad.value = true },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = Spacing.sm, end = 92.dp)
                        .size(32.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = "Anotar insight do exercício",
                        tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.30f),
                        modifier = Modifier.size(16.dp),
                    )
                }

                // Seletor circular de questões por folha (LazyRow de 1 a 40)
                LazyRow(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = Spacing.sm, end = 142.dp)
                        .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.04f), RoundedCornerShape(16.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                        .width(220.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val modularPages = listOf(1, 2, 3, 4, 6, 8, 10, 12, 15, 18, 20, 24, 30, 35, 40)
                    items(modularPages.size) { index ->
                        val num = modularPages[index]
                        val isSelected = config.exercisesPerPage == num
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(28.dp)
                                .background(
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else androidx.compose.ui.graphics.Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable {
                                    onConfigChange(config.copy(exercisesPerPage = num))
                                }
                        ) {
                            Text(
                                text = num.toString(),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.60f)
                            )
                        }
                    }
                }
            }

            if (showNotePad.value) {
                // Scrim de fundo translúcido
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.40f))
                        .clickable { showNotePad.value = false },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier
                            .width(420.dp)
                            .background(
                                color = MaterialTheme.colorScheme.background,
                                shape = RoundedCornerShape(16.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .clickable(enabled = false) {} // Impede cliques de passarem para o fundo
                            .padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Anotar insight do exercício",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        
                        Text(
                            text = "Exercício: ${renderLatex(field.statement)}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.60f)
                        )
                        
                        OutlinedTextField(
                            value = noteInput.value,
                            onValueChange = { noteInput.value = it },
                            placeholder = { Text("O que você observou neste exercício? Ex: Lembrete de sinais, regra de potência...", fontSize = 13.sp) },
                            modifier = Modifier.fillMaxWidth().height(120.dp),
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp),
                            shape = RoundedCornerShape(8.dp)
                        )
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = { showNotePad.value = false }) {
                                Text("Cancelar", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
                            }
                            Spacer(Modifier.width(12.dp))
                            Button(
                                onClick = {
                                    if (noteInput.value.isNotBlank()) {
                                        onAddNote(
                                            com.strava_matematica.viewmodel.SprintNote(
                                                sessionId = sessionId,
                                                folhaIndex = folhaIndex,
                                                exerciseIndex = currentExerciseIndex,
                                                exerciseStatement = field.statement,
                                                noteText = noteInput.value
                                            )
                                        )
                                        noteInput.value = ""
                                        showNotePad.value = false
                                    }
                                },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Confirmar")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScoreRiskChip(
    onStay: () -> Unit,
    onAdjust: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ink = MaterialTheme.colorScheme.onBackground
    Column(
        modifier = modifier
            .background(
                color = MaterialTheme.colorScheme.background.copy(alpha = 0.94f),
                shape = RoundedCornerShape(8.dp),
            )
            .border(1.dp, ink.copy(alpha = 0.10f), RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = "5 erros consecutivos podem afetar seu score",
            fontSize = 11.sp,
            letterSpacing = 0.sp,
            color = ink.copy(alpha = 0.46f),
            textAlign = TextAlign.Center,
        )
        Text(
            text = "permanecer nesta Sprint?",
            fontSize = 10.sp,
            letterSpacing = 0.sp,
            color = ink.copy(alpha = 0.32f),
            textAlign = TextAlign.Center,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(
                text = "permanecer",
                fontSize = 10.sp,
                letterSpacing = 0.sp,
                color = ink.copy(alpha = 0.46f),
                modifier = Modifier.pointerInput(onStay) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        onStay()
                    }
                },
            )
            Text(
                text = "ajustar",
                fontSize = 10.sp,
                letterSpacing = 0.sp,
                color = ink.copy(alpha = 0.62f),
                modifier = Modifier.pointerInput(onAdjust) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        onAdjust()
                    }
                },
            )
        }
    }
}

/**
 * Fila horizontal dos últimos N resultados (máx 7).
 * Mais antigo à esquerda / menor — mais recente à direita / maior.
 * □ acerto · ○ erro · × não preenchido
 */
@Composable
private fun ResultHistoryRow(
    results: List<ResultMark>,
    modifier: Modifier = Modifier,
) {
    val n = results.size.coerceAtLeast(1)
    val minSizeDp = 9f
    val maxSizeDp = 26f

    // Cores pastel alinhadas ao tema SPRINT (neutras, sem distração)
    val colorCorrect = Color(0xFF6B8C5A)   // verde oliva suave
    val colorWrong   = Color(0xFF9E6B5A)   // terracota suave
    val colorEmpty   = Color(0xFFAA9E8C)   // bege acinzentado

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        results.forEachIndexed { i, mark ->
            // index 0 = mais antigo (menor), index n-1 = mais recente (maior)
            val t = if (n > 1) i.toFloat() / (n - 1) else 1f
            val sizeDp = (minSizeDp + t * (maxSizeDp - minSizeDp)).dp
            val strokeDp = (sizeDp.value / 7f).coerceAtLeast(1.2f).dp
            val color = when (mark) {
                ResultMark.CORRECT -> colorCorrect
                ResultMark.WRONG   -> colorWrong
                ResultMark.EMPTY   -> colorEmpty
            }
            Canvas(modifier = Modifier.size(sizeDp)) {
                val sw = strokeDp.toPx()
                when (mark) {
                    ResultMark.CORRECT -> drawRect(
                        color = color,
                        style = Stroke(width = sw),
                    )
                    ResultMark.WRONG -> drawCircle(
                        color = color,
                        style = Stroke(width = sw),
                    )
                    ResultMark.EMPTY -> {
                        val pad = sw
                        drawLine(color, Offset(pad, pad), Offset(size.width - pad, size.height - pad), sw)
                        drawLine(color, Offset(size.width - pad, pad), Offset(pad, size.height - pad), sw)
                    }
                }
            }
        }
    }
}

/**
 * Chip não-bloqueante que aparece quando o aluno acerta 5 seguidos.
 * Sugere avançar para o próximo tema, mas o usuário decide.
 */
@Composable
private fun MasteryChip(
    nextSkill: String,
    onAdvance: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val label = nextSkill.replace('_', ' ')
    val ink = MaterialTheme.colorScheme.onBackground
    Row(
        modifier = modifier
            .background(
                color = MaterialTheme.colorScheme.background.copy(alpha = 0.92f),
                shape = RoundedCornerShape(20.dp),
            )
            .border(1.dp, ink.copy(alpha = 0.12f), RoundedCornerShape(20.dp))
            .padding(horizontal = 14.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            letterSpacing = 0.sp,
            color = ink.copy(alpha = 0.45f),
        )
        Text(
            text = "→",
            fontSize = 13.sp,
            letterSpacing = 0.sp,
            color = ink.copy(alpha = 0.60f),
            modifier = Modifier
                .pointerInput(onAdvance) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        onAdvance()
                    }
                }
                .padding(horizontal = 2.dp),
        )
        Text(
            text = "×",
            fontSize = 13.sp,
            letterSpacing = 0.sp,
            color = ink.copy(alpha = 0.30f),
            modifier = Modifier
                .pointerInput(onDismiss) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        onDismiss()
                    }
                }
                .padding(horizontal = 2.dp),
        )
    }
}

private val SPRINT_DIFFICULTIES = listOf(
    "auto"  to "auto",
    "1.0"   to "fácil",
    "3.0"   to "médio",
    "5.5"   to "difícil",
    "8.0"   to "expert",
)

private val SPRINT_GROUPS = listOf(
    "SIMULADO" to "simulado",
    "FUNDAMENTOS" to "fundamentos",
    "ALGEBRA" to "álgebra",
    "FUNCOES" to "funções",
    "GEOMETRIA" to "geometria",
    "COMBINATORIA" to "combinatória",
    "TRIGONOMETRIA" to "trigo",
    "CALCULO" to "cálculo",
)

private val SPRINT_SKILLS_BY_GROUP = mapOf(
    "FUNDAMENTOS" to listOf(
        "soma_subtracao" to "soma",
        "multiplicacao_divisao" to "mult",
        "fracoes_decimais" to "frac",
        "porcentagem_razao" to "%",
        "potenciacao_radiciacao" to "pot",
    ),
    "ALGEBRA" to listOf(
        "equacoes_lineares" to "eq1",
        "sistemas_equacoes" to "sist",
        "fatoracao_produtos_notaveis" to "fat",
        "inequacoes" to "ineq",
        "equacoes_quadraticas" to "eq2",
    ),
    "FUNCOES" to listOf(
        "funcao_afim" to "afim",
        "funcao_quadratica" to "quad",
        "funcao_exponencial" to "exp",
        "funcao_logaritmica" to "log",
        "funcao_modular" to "mod",
    ),
    "GEOMETRIA" to listOf(
        "geometria_plana" to "plana",
        "geometria_espacial" to "espa",
        "geometria_analitica" to "anali",
    ),
    "COMBINATORIA" to listOf(
        "progressoes_pa_pg" to "pa/pg",
        "combinatoria" to "comb",
        "probabilidade" to "prob",
    ),
    "TRIGONOMETRIA" to listOf(
        "trig_razoes" to "trig",
        "trig_seno_cosseno_tangente" to "sen/cos",
        "trig_identidades" to "iden",
        "trig_equacoes" to "eq trig",
    ),
    "CALCULO" to listOf(
        "nocao_de_limite" to "limite",
        "continuidade" to "cont",
        "derivadas_basicas" to "deriv",
        "derivadas_regra_cadeia" to "cadeia",
        "derivadas_produto_quociente" to "prod/quoc",
        "aplicacoes_derivadas" to "ap deriv",
        "integrais_indefinidas" to "int ind",
        "integrais_definidas" to "int def",
        "aplicacoes_integrais" to "ap int",
    ),
)

private val SPRINT_DENSITIES = listOf(
    "high" to "leve",
    "medium" to "fixa",
    "low" to "densa",
)

private val SPRINT_ZOOMS = listOf(
    "theme" to "tema",
    "exact" to "exato",
    "kplus" to "k+",
)

private val SPRINT_DIGITS = listOf(
    "1" to "unidades",
    "2" to "dezenas",
    "3" to "centenas",
    "4" to "milhares",
)

private val SPRINT_VALUES = listOf(
    "2" to "2 valores",
    "3" to "3 valores",
    "4" to "4 valores",
    "5" to "5 valores",
)

private val SPRINT_NUMBER_SETS = listOf(
    "naturais" to "naturais",
    "inteiros" to "inteiros",
    "decimais" to "decimais",
    "mix" to "mix",
)

@Composable
private fun SprintScrollConfigPage(
    config: SessionConfig,
    selectedSkillTag: String,
    densityLevel: String,
    skillAccuracy: Map<String, Float>,
    skillAttempts: Map<String, Int>,
    skillFluency: Map<String, Float>,
    isSimulationActive: androidx.compose.runtime.MutableState<Boolean>,
    showSprintScrolls: androidx.compose.runtime.MutableState<Boolean>,
    onApply: (skillTag: String, density: String, layoutMode: String, difficultyStart: Double?, digitsCount: Int, valuesCount: Int, numberSet: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val initialGroup = SPRINT_SKILLS_BY_GROUP.entries.firstOrNull { it.value.any { s -> s.first == selectedSkillTag } }?.key ?: "FUNDAMENTOS"
    val selectedGroup = remember { mutableStateOf(initialGroup) }

    val initialSkill = selectedSkillTag.takeIf { tag -> SPRINT_SKILLS_BY_GROUP.values.flatten().any { it.first == tag } }
        ?: SPRINT_SKILLS_BY_GROUP.values.first().first().first
    val selectedSkill = remember { mutableStateOf(initialSkill) }
    val selectedDensity = remember { mutableStateOf(densityLevel.takeIf { it in listOf("high", "medium", "low") } ?: "medium") }
    val selectedZoom = remember { mutableStateOf(if (densityLevel == "exact") "exact" else if (densityLevel == "kplus") "kplus" else "theme") }
    val selectedDifficulty = remember { mutableStateOf("auto") }
    val selectedDigits = remember { mutableStateOf(config.digitsCount.toString()) }
    val selectedValues = remember { mutableStateOf(config.valuesCount.toString()) }
    val selectedNumberSet = remember { mutableStateOf(config.numberSet) }

    val ink = MaterialTheme.colorScheme.onBackground

    Box(modifier = modifier.background(MaterialTheme.colorScheme.background)) {
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .padding(top = Spacing.md, bottom = Spacing.md, end = 100.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            SprintScrollRow(
                label = "categoria",
                options = SPRINT_GROUPS,
                selectedKey = selectedGroup.value,
                onSelected = { group ->
                    selectedGroup.value = group
                    val firstSkillOfGroup = SPRINT_SKILLS_BY_GROUP[group]?.firstOrNull()?.first ?: "soma_subtracao"
                    selectedSkill.value = firstSkillOfGroup
                },
            )

            androidx.compose.runtime.key(selectedGroup.value) {
                if (selectedGroup.value != "simulado") {
                    SprintScrollRow(
                        label = "tema específico",
                        options = SPRINT_SKILLS_BY_GROUP[selectedGroup.value] ?: emptyList(),
                        selectedKey = selectedSkill.value,
                        onSelected = { selectedSkill.value = it },
                    )
                }
            }

            if (selectedGroup.value == "simulado") {
                SimuladoConfigPage(
                    config = config,
                    showSprintScrolls = showSprintScrolls,
                    isSimuladoConfigActive = isSimulationActive,
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                SprintScrollRow(
                    label = "casas decimais",
                    options = SPRINT_DIGITS,
                    selectedKey = selectedDigits.value,
                    onSelected = { selectedDigits.value = it },
                )

                SprintScrollRow(
                    label = "termos da conta",
                    options = SPRINT_VALUES,
                    selectedKey = selectedValues.value,
                    onSelected = { selectedValues.value = it },
                )

                SprintScrollRow(
                    label = "conjunto numérico",
                    options = SPRINT_NUMBER_SETS,
                    selectedKey = selectedNumberSet.value,
                    onSelected = { selectedNumberSet.value = it },
                )
                // Painel Cognitivo Premium
                val acc = skillAccuracy[selectedSkill.value]
                val att = skillAttempts[selectedSkill.value] ?: 0
                val fluency = skillFluency[selectedSkill.value] ?: 0f
                val mmr = com.strava_matematica.domain.procedural.EloMatchmaker.masterScoreToMmr(fluency.toDouble())
                val isUnderInefficacy = (att >= 5 && (acc ?: 0f) < 0.40f) || (att > 0 && fluency < 0.15f)
                val isMastery = fluency >= 0.85f

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.md)
                        .background(
                            color = when {
                                isUnderInefficacy -> androidx.compose.ui.graphics.Color(0xFFD32F2F).copy(alpha = 0.08f)
                                isMastery -> androidx.compose.ui.graphics.Color(0xFF388E3C).copy(alpha = 0.08f)
                                else -> MaterialTheme.colorScheme.onBackground.copy(alpha = 0.03f)
                            },
                            shape = RoundedCornerShape(8.dp)
                        )
                        .border(
                            width = 1.dp,
                            color = when {
                                isUnderInefficacy -> androidx.compose.ui.graphics.Color(0xFFD32F2F).copy(alpha = 0.24f)
                                isMastery -> androidx.compose.ui.graphics.Color(0xFF388E3C).copy(alpha = 0.24f)
                                else -> MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f)
                            },
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(Spacing.md),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "MMR: $mmr",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                isUnderInefficacy -> androidx.compose.ui.graphics.Color(0xFFD32F2F)
                                isMastery -> androidx.compose.ui.graphics.Color(0xFF388E3C)
                                else -> ink.copy(alpha = 0.64f)
                            }
                        )
                        Text(
                            text = "$att tentativas" + (acc?.let { " · ${(it * 100).toInt()}% acerto" } ?: ""),
                            fontSize = 11.sp,
                            color = ink.copy(alpha = 0.45f)
                        )
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = when {
                            isUnderInefficacy -> "⚠️ ALERTA: Ineficácia Crítica (Quarentena). Erros reduzem MMR de forma exponencial!"
                            isMastery -> "🏆 DOMÍNIO COGNITIVO: Mastery sólido alcançado."
                            else -> "📈 FIXAÇÃO: Pratique com consistência para atingir o Mastery."
                        },
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = when {
                            isUnderInefficacy -> androidx.compose.ui.graphics.Color(0xFFD32F2F)
                            isMastery -> androidx.compose.ui.graphics.Color(0xFF388E3C)
                            else -> MaterialTheme.colorScheme.onBackground.copy(alpha = 0.35f)
                        },
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }

            SprintScrollRow(
                label = "densidade",
                options = SPRINT_DENSITIES,
                selectedKey = selectedDensity.value,
                onSelected = { selectedDensity.value = it },
            )
            SprintScrollRow(
                label = "zoom",
                options = SPRINT_ZOOMS,
                selectedKey = selectedZoom.value,
                onSelected = { selectedZoom.value = it },
            )
            SprintScrollRow(
                label = "dificuldade",
                options = SPRINT_DIFFICULTIES,
                selectedKey = selectedDifficulty.value,
                onSelected = { selectedDifficulty.value = it },
            )
        }
        }
        EnterSquare(
            onAdvance = {
                val diff = selectedDifficulty.value.toDoubleOrNull()
                onApply(
                    selectedSkill.value,
                    selectedDensity.value,
                    selectedZoom.value,
                    diff,
                    selectedDigits.value.toIntOrNull() ?: 2,
                    selectedValues.value.toIntOrNull() ?: 2,
                    selectedNumberSet.value
                )
            },
            onTripleTap = {
                if (selectedGroup.value != "simulado") {
                    val diff = selectedDifficulty.value.toDoubleOrNull()
                    onApply(
                        selectedSkill.value,
                        selectedDensity.value,
                        selectedZoom.value,
                        diff,
                        selectedDigits.value.toIntOrNull() ?: 2,
                        selectedValues.value.toIntOrNull() ?: 2,
                        selectedNumberSet.value
                    )
                }
            },
            onRegisterVisibilityChange = {},
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = Spacing.sm),
        )
    }
}

@Composable
private fun SprintScrollRow(
    label: String,
    options: List<Pair<String, String>>,
    selectedKey: String,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectedIndex = options.indexOfFirst { it.first == selectedKey }.coerceAtLeast(0)
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = selectedIndex)
    val itemWidth = 160.dp

    LaunchedEffect(listState, options) {
        snapshotFlow {
            val visible = listState.layoutInfo.visibleItemsInfo
            if (visible.isEmpty()) {
                listState.firstVisibleItemIndex
            } else {
                val center = listState.layoutInfo.viewportStartOffset +
                    (listState.layoutInfo.viewportEndOffset - listState.layoutInfo.viewportStartOffset) / 2
                visible.minBy { item -> kotlin.math.abs((item.offset + item.size / 2) - center) }.index
            }
        }
            .distinctUntilChanged()
            .collect { index ->
                if (index in options.indices) onSelected(options[index].first)
            }
    }

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val sidePadding = ((maxWidth - itemWidth) / 2).coerceAtLeast(0.dp)
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = label,
                fontSize = 11.sp,
                letterSpacing = 0.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.22f),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
            LazyRow(
                state = listState,
                contentPadding = PaddingValues(horizontal = sidePadding),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                itemsIndexed(options) { index, option ->
                    val selected = option.first == selectedKey
                    val ink = MaterialTheme.colorScheme.onBackground
                    Box(
                        modifier = Modifier
                            .size(width = itemWidth, height = 64.dp)
                            .graphicsLayer {
                                val visibleItems = listState.layoutInfo.visibleItemsInfo
                                val itemInfo = visibleItems.firstOrNull { it.index == index }
                                if (itemInfo != null) {
                                    val center = listState.layoutInfo.viewportStartOffset +
                                        (listState.layoutInfo.viewportEndOffset - listState.layoutInfo.viewportStartOffset) / 2f
                                    val itemCenter = itemInfo.offset + itemInfo.size / 2f
                                    val distance = kotlin.math.abs(center - itemCenter)
                                    val maxDistance = itemInfo.size * 2.0f // Fades out completely when 2 items away
                                    val fraction = 1f - (distance / maxDistance).coerceIn(0f, 1f)
                                    
                                    val scale = 0.5f + (0.5f * fraction)
                                    scaleX = scale
                                    scaleY = scale
                                    alpha = java.lang.Math.pow(fraction.toDouble(), 1.5).toFloat()
                                } else {
                                    alpha = 0f
                                }
                            }
                            .drawBehind {
                                if (selected) {
                                    drawLine(
                                        color = ink.copy(alpha = 0.64f),
                                        start = androidx.compose.ui.geometry.Offset(size.width * 0.25f, size.height - 4.dp.toPx()),
                                        end = androidx.compose.ui.geometry.Offset(size.width * 0.75f, size.height - 4.dp.toPx()),
                                        strokeWidth = 2.dp.toPx(),
                                    )
                                }
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = option.second,
                            fontSize = 26.sp,
                            letterSpacing = 0.sp,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Light,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = if (selected) 0.95f else 0.32f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SimuladoConfigPage(
    config: SessionConfig,
    showSprintScrolls: androidx.compose.runtime.MutableState<Boolean>,
    isSimuladoConfigActive: androidx.compose.runtime.MutableState<Boolean>,
    modifier: Modifier = Modifier
) {
    val ink = MaterialTheme.colorScheme.onBackground
    val scrollState = rememberScrollState()

    val topics = listOf(
        "soma" to "Soma Básica",
        "sub" to "Subtração",
        "mult" to "Multiplicação",
        "div" to "Divisão",
        "eq1" to "Equação 1º Grau",
        "eq2" to "Equação 2º Grau",
        "frac" to "Frações",
        "pot" to "Potenciação",
        "rad" to "Radiciação",
        "prob" to "Probabilidade"
    )

    val topicCounts = remember { mutableStateOf(topics.associate { it.first to 0 }) }
    val targetTime = remember { mutableStateOf("30") }
    val selectedDifficulty = remember { mutableStateOf("auto") }
    val selectedDigits = remember { mutableStateOf(config.digitsCount.toString()) }

    Box(modifier = modifier.background(MaterialTheme.colorScheme.background)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Configuração do Simulado",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = ink.copy(alpha = 0.8f)
            )

            Text("TÓPICOS E QUESTÕES", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = ink.copy(alpha = 0.4f))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ink.copy(alpha = 0.03f), RoundedCornerShape(16.dp))
                    .border(1.dp, ink.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                topics.forEach { (key, label) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = label, fontSize = 13.sp, color = ink.copy(alpha = 0.7f))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            IconButton(
                                onClick = {
                                    val current = topicCounts.value[key] ?: 0
                                    if (current > 0) topicCounts.value = topicCounts.value.toMutableMap().apply { put(key, current - 1) }
                                },
                                modifier = Modifier.size(24.dp).background(ink.copy(alpha = 0.05f), CircleShape)
                            ) { Text("-", fontSize = 12.sp, color = ink.copy(alpha = 0.6f)) }
                            Text(text = "${topicCounts.value[key] ?: 0}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = ink.copy(alpha = 0.8f), modifier = Modifier.width(20.dp), textAlign = TextAlign.Center)
                            IconButton(
                                onClick = {
                                    val current = topicCounts.value[key] ?: 0
                                    topicCounts.value = topicCounts.value.toMutableMap().apply { put(key, current + 1) }
                                },
                                modifier = Modifier.size(24.dp).background(ink.copy(alpha = 0.05f), CircleShape)
                            ) { Text("+", fontSize = 12.sp, color = ink.copy(alpha = 0.6f)) }
                        }
                    }
                }
            }

            Text("TEMPO ALVO (MINUTOS)", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = ink.copy(alpha = 0.4f))
            OutlinedTextField(
                value = targetTime.value,
                onValueChange = { targetTime.value = it },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp)
            )

            Text("CONFIGURAÇÕES GERAIS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = ink.copy(alpha = 0.4f))
            SprintScrollRow(
                label = "dificuldade",
                options = SPRINT_DIFFICULTIES,
                selectedKey = selectedDifficulty.value,
                onSelected = { selectedDifficulty.value = it }
            )

            SprintScrollRow(
                label = "casas decimais",
                options = SPRINT_DIGITS,
                selectedKey = selectedDigits.value,
                onSelected = { selectedDigits.value = it }
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    println("Exportar TXT/MD clicado!")
                },
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = ink.copy(alpha = 0.1f)),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Exportar TXT/MD", color = ink.copy(alpha = 0.7f), fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = {
                    showSprintScrolls.value = false
                    isSimuladoConfigActive.value = false
                    // TODO: trigger actual Simulado generation based on topicCounts
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Iniciar no App", color = androidx.compose.ui.graphics.Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

/**
 * Intercepts 1-finger long-press before InkCanvas sees it → toggles eraser.
 * 2-finger advance migrado para ZoomableCanvas (pinch/tap unificado).
 */
private fun Modifier.sprintGestureInput(
    gestureConfig: GestureConfig,
    onAdvance: () -> Unit,
    onToggleEraser: () -> Unit,
): Modifier = pointerInput(gestureConfig, onToggleEraser) {
    val movePx = 20.dp.toPx()
    val longPressMs = 520L
    awaitPointerEventScope {
        while (true) {
            val down = awaitPointerEvent(PointerEventPass.Initial)
            val pressed = down.changes.filter { it.pressed }
            if (pressed.size != 1) continue
            if (gestureConfig.gestureFor(GestureConfig.ACTION_TOGGLE_ERASER) !=
                GestureConfig.GESTURE_LONG_PRESS) continue

            val first = pressed.first()
            val startPosition = first.position
            val startTime = System.currentTimeMillis()
            var movedTooMuch = false
            var toggled = false

            while (true) {
                val ev = awaitPointerEvent(PointerEventPass.Initial)
                val current = ev.changes.firstOrNull { it.id == first.id }
                if (current == null || !current.pressed) break
                if ((current.position - startPosition).getDistance() > movePx) {
                    movedTooMuch = true
                }
                if (!movedTooMuch && !toggled && System.currentTimeMillis() - startTime >= longPressMs) {
                    ev.changes.forEach { it.consume() }
                    onToggleEraser()
                    toggled = true
                }
            }
        }
    }
}

@Composable
private fun EraserToggle(
    isErasing: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ink = MaterialTheme.colorScheme.onBackground
    Box(
        modifier = modifier
            .size(38.dp)
            .border(
                width = 1.dp,
                color = ink.copy(alpha = if (isErasing) 0.42f else 0.18f),
                shape = CircleShape,
            )
            .background(
                color = ink.copy(alpha = if (isErasing) 0.14f else 0.04f),
                shape = CircleShape,
            )
            .pointerInput(onToggle) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    onToggle()
                }
            },
    )
}

/**
 * Quadrado de confirmação — qualquer traço dentro dispara o avanço.
 * Fica no canto inferior direito da tela, ao lado da toolbar.
 */
@Composable
private fun EnterSquare(
    onAdvance: () -> Unit,
    onTripleTap: () -> Unit = {},
    onRegisterVisibilityChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val tapCount = remember { mutableIntStateOf(0) }
    val pendingTapJob = remember { mutableStateOf<Job?>(null) }

    Box(
        modifier = modifier
            .size(72.dp)
            .border(
                width = 1.5.dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.60f),
                shape = CircleShape,
            )
            .background(
                color = MaterialTheme.colorScheme.surface,
                shape = CircleShape,
            )
            .pointerInput(onAdvance, onTripleTap, onRegisterVisibilityChange) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val startAt = System.currentTimeMillis()
                    val holdMs = 430L
                    var registerShown = false

                    while (true) {
                        val event = awaitPointerEvent()
                        if (!registerShown && System.currentTimeMillis() - startAt >= holdMs) {
                            registerShown = true
                            event.changes.forEach { it.consume() }
                            onRegisterVisibilityChange(true)
                        }
                        if (event.changes.none { it.pressed }) {
                            if (registerShown) {
                                onRegisterVisibilityChange(false)
                            } else {
                                down.consume()
                                tapCount.intValue += 1
                                pendingTapJob.value?.cancel()
                                if (tapCount.intValue >= 3) {
                                    tapCount.intValue = 0
                                    onTripleTap()
                                } else {
                                    pendingTapJob.value = scope.launch {
                                        delay(320L)
                                        if (tapCount.intValue == 1) {
                                            onAdvance()
                                        }
                                        tapCount.intValue = 0
                                    }
                                }
                            }
                            break
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {}
}

@Composable
private fun SessionRegisterOverlay(
    correct: Int,
    total: Int,
    startedAtMs: Long,
    currentExercise: Int,
    modifier: Modifier = Modifier,
) {
    val now = remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(startedAtMs) {
        while (true) {
            now.longValue = System.currentTimeMillis()
            delay(1000)
        }
    }

    val elapsedSeconds = ((now.longValue - startedAtMs) / 1000).coerceAtLeast(0)
    val minutes = elapsedSeconds / 60
    val seconds = elapsedSeconds % 60
    val accuracy = if (total > 0) ((correct * 100) / total) else 0
    val ink = MaterialTheme.colorScheme.onBackground

    Column(
        modifier = modifier
            .background(
                color = MaterialTheme.colorScheme.background.copy(alpha = 0.92f),
                shape = RoundedCornerShape(8.dp),
            )
            .border(
                width = 1.dp,
                color = ink.copy(alpha = 0.12f),
                shape = RoundedCornerShape(8.dp),
            )
            .padding(horizontal = 12.dp, vertical = 9.dp),
    ) {
        Text(
            text = "$correct/$total · $accuracy%",
            fontSize = 13.sp,
            color = ink.copy(alpha = 0.55f),
        )
        Text(
            text = "${minutes}m ${seconds}s · ex $currentExercise",
            fontSize = 10.sp,
            color = ink.copy(alpha = 0.34f),
        )
    }
}
