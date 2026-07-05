package com.sprint.ui.folha

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.border
import androidx.compose.ui.window.Dialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.layout.Spacer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sprint.design.FocusColors
import com.sprint.design.Spacing
import com.sprint.model.BackgroundMode
import com.sprint.model.FolhaField
import com.sprint.model.PenEvent


@Composable
fun ExerciseField(
    field: FolhaField,
    isActive: Boolean,
    backgroundMode: BackgroundMode,
    penColor: String,
    penWidth: Float = 2.2f,
    modifier: Modifier = Modifier,
    isKPlus: Boolean = false,
    isCompact: Boolean = false,
    exercisesPerPage: Int = 1,
    isBlindMode: Boolean = false,
    showFormatHint: Boolean = false,
    // Split-canvas params
    initialScratchStrokes: List<List<Offset>> = emptyList(),
    initialAnswerStrokes: List<List<Offset>> = emptyList(),
    initialScratchRedoStack: List<List<Offset>> = emptyList(),
    initialAnswerRedoStack: List<List<Offset>> = emptyList(),
    typedAnswer: String = "",
    clearSignal: Int = 0,
    undoSignal: Int = 0,
    redoSignal: Int = 0,
    onClick: () -> Unit = {},
    onSyncScratch: (List<List<Offset>>, List<List<Offset>>) -> Unit = { _, _ -> },
    onSyncAnswer: (List<List<Offset>>, List<List<Offset>>) -> Unit = { _, _ -> },
    onTypedAnswerChange: (String) -> Unit = {},
    // Legacy alias: callers that still pass onSyncStrokes are wired to the answer canvas
    onSyncStrokes: (List<List<Offset>>, List<List<Offset>>) -> Unit = { _, _ -> },
    onPenEvent: (PenEvent) -> Unit = {},
    onConfirmStroke: (List<List<Offset>>) -> Unit = {},
    isErasing: Boolean = false,
    userGuideMode: String = "nenhuma",
) {
    val isDark = backgroundMode == BackgroundMode.DARK
    val fieldColor = MaterialTheme.colorScheme.background
    val surfaceColor = MaterialTheme.colorScheme.background
    // Single ink color — everything reads from here, varying only alpha
    val ink = if (isDark) FocusColors.DarkTextPrimary else FocusColors.WhiteTextPrimary
    val isFullPage = field.canvasMode == "full_page"
    val isLined = field.canvasMode == "lined"
    val context = LocalContext.current
    val defaultScratchRatio = if (isLined) 0.80f else if (field.statement.length > 90) 0.66f else 0.75f
    val scratchRatio = remember(field.fieldIndex) {
        mutableFloatStateOf(SplitRatioPrefs.get(context, field.fieldIndex, defaultScratchRatio))
    }
    // Map config.guideMode → InkCanvas guideMode values. "nenhuma" defers to field.canvasMode logic.
    val mappedGuideMode: String? = when (userGuideMode) {
        "horizontal", "grade" -> "lined"
        "dots" -> "dots"
        else -> null   // "nenhuma" — use field-based default
    }

    // Merge legacy onSyncStrokes into onSyncAnswer so old callers still work.
    val hasAnswerStroke = remember(initialAnswerStrokes) { mutableStateOf(initialAnswerStrokes.isNotEmpty()) }
    val answerPadVisible = remember(field.fieldIndex) { mutableStateOf(false) }
    val answerSync: (List<List<Offset>>, List<List<Offset>>) -> Unit = { s, r ->
        if (s.isNotEmpty()) hasAnswerStroke.value = true
        onSyncAnswer(s, r)
        onSyncStrokes(s, r)
    }

    val isKPlusMode = isKPlus && (field.statement.contains("+") || field.statement.contains("-") || field.statement.contains("times") || field.statement.contains("x") || field.statement.contains("*"))
    val regex = Regex("""^(\d+)\s*([\+\-\*x]|\\times)\s*(\d+)$""")
    val match = regex.find(field.statement.trim().replace("$", ""))
    
    val verticalStackData = if (isKPlusMode && match != null) {
        val term1 = match.groupValues[1]
        val rawOp = match.groupValues[2]
        val term2 = match.groupValues[3]
        val op = when (rawOp) {
            "\\times", "*", "x" -> "×"
            else -> rawOp
        }
        Triple(term1, op, term2)
    } else {
        null
    }

    val statementText = field.statement
    val mediaTagRegex = remember(statementText) { Regex("""\[(?:fig|vid|aud|img):.*?\]""") }
    val mediaMatch = remember(statementText) { mediaTagRegex.find(statementText) }
    val mediaSpec = remember(statementText) { mediaMatch?.value }
    val cleanStatement = remember(statementText) { statementText.replace(mediaTagRegex, "").trim() }
    val formatHintText = if (showFormatHint) field.answerFormatHint else null

    

    var localClearSignal by remember { mutableStateOf(0) }
    var localUndoSignal by remember { mutableStateOf(0) }
    var localRedoSignal by remember { mutableStateOf(0) }

    // Converte undo/redo globais para locais — cada ExerciseField tem seu próprio
    // Assim desfazer em um campo NÃO afeta outro. LIFO é preservado por canvas.
    LaunchedEffect(undoSignal) {
        if (undoSignal > 0) localUndoSignal++
    }
    LaunchedEffect(redoSignal) {
        if (redoSignal > 0) localRedoSignal++
    }

    // ── Test Hook para E2E (UI Invisible) ──────────────────────────────────
    BasicTextField(
        value = typedAnswer,
        onValueChange = onTypedAnswerChange,
        modifier = Modifier
            .size(1.dp)
            .alpha(0f)
            .testTag("AnswerInput_${field.fieldIndex}")
    )

    if (isCompact) {
        val isCorrect = typedAnswer.trim() == field.expectedAnswer?.trim()
        val hasAnswer = typedAnswer.isNotBlank()
        val boxBgColor = when {
            !hasAnswer -> MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f)
            isBlindMode && hasAnswer -> Color(0xFFF5F5F5) // Neutral color for answered in blind mode
            isCorrect -> Color(0xFFE8F5E9)  // verde claro pastel
            else -> Color(0xFFFFEBEE)       // vermelho claro pastel
        }
        val boxBorderColor = when {
            !hasAnswer -> ink.copy(alpha = 0.25f)
            isBlindMode && hasAnswer -> Color(0xFF9E9E9E) // Neutral border for answered in blind mode
            isCorrect -> Color(0xFF2E7D32)  // verde esmeralda
            else -> Color(0xFFC62828)       // vermelho coral
        }

        // Novo Layout Compacto de Linha para a Grade Superior
        Row(
            modifier = modifier
                .fillMaxWidth()
                .shadow(elevation = 2.dp, shape = RoundedCornerShape(16.dp), spotColor = ink.copy(alpha = 0.1f))
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
                .padding(vertical = 16.dp, horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Esquerda: Enunciado
            Row(
                modifier = Modifier.weight(1f, fill = false),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Number Indicator Bubble
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(44.dp)
                        .background(ink.copy(alpha = 0.05f), CircleShape)
                ) {
                    Text(
                        text = "${field.fieldIndex + 1}",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = ink.copy(alpha = 0.6f)
                    )
                }
                
                // Subtle divider
                Box(modifier = Modifier.height(32.dp).width(2.dp).background(ink.copy(alpha = 0.08f)))


                if (verticalStackData != null) {
                    VerticalArithmeticStack(
                        term1 = verticalStackData.first,
                        op = verticalStackData.second,
                        term2 = verticalStackData.third,
                        ink = ink,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                } else {
                    Text(
                        text = renderLatex(cleanStatement),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ink
                    )
                }
                
                // Se houver figura (muito raro em compact), exibe pequena
                if (mediaSpec != null) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .padding(start = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (mediaSpec.startsWith("[fig:")) {
                            GeometricDiagram(spec = mediaSpec, ink = ink)
                        } else {
                            // Placeholder futuro para img/svg
                            Text("Media", color = ink.copy(alpha = 0.5f), fontSize = 10.sp)
                        }
                    }
                }
            }

            // Direita: Caixa de Resposta (Canvas Isolado)
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .height(110.dp)
                    .width(260.dp)
                    .background(boxBgColor, RoundedCornerShape(8.dp))
                    .border(2.dp, boxBorderColor, RoundedCornerShape(8.dp))
            ) {
                InkCanvas(
                    modifier = Modifier.matchParentSize().padding(4.dp),
                    penColor = penColor,
                    penWidth = penWidth,
                    enabled = isActive,
                    isErasing = isErasing,
                    clearSignal = localClearSignal, // Resposta escuta SOMENTE o clear local
                    undoSignal = localUndoSignal, 
                    redoSignal = localRedoSignal,
                    initialStrokes = initialAnswerStrokes,
                    initialRedoStack = initialAnswerRedoStack,
                    guideMode = "single",
                    onSyncStrokes = answerSync,
                    onPenEvent = onPenEvent,
                    onTap = { answerPadVisible.value = true }
                )
                if (typedAnswer.isNotBlank()) {
                    MathAnswerTemplate(
                        expectedAnswer = field.expectedAnswer,
                        studentInput = typedAnswer,
                        isCorrect = isCorrect,
                        ink = ink
                    )
                }
                
                // Botão de Limpar Local no topo-direito
                androidx.compose.material3.IconButton(
                    onClick = { localClearSignal++ },
                    modifier = Modifier.align(Alignment.TopEnd).size(24.dp).padding(4.dp)
                ) {
                    androidx.compose.material3.Icon(
                        imageVector = androidx.compose.material.icons.Icons.Outlined.Clear,
                        contentDescription = "Limpar Campo",
                        tint = ink.copy(alpha = 0.5f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        if (answerPadVisible.value) {
            Dialog(
                onDismissRequest = { answerPadVisible.value = false }
            ) {
                AnswerPad(
                    ink = ink,
                    onKey = { key ->
                        when (key) {
                            "ok" -> answerPadVisible.value = false
                            "del" -> onTypedAnswerChange(typedAnswer.dropLast(1))
                            "clr" -> onTypedAnswerChange("")
                            else -> onTypedAnswerChange(typedAnswer + key)
                        }
                    }
                )
            }
        }
    } else {
        // Layout Normal Z-Index Overlap
        val isCorrect = typedAnswer.trim() == field.expectedAnswer?.trim()
        val hasAnswer = typedAnswer.isNotBlank()
        val boxBgColor = when {
            !hasAnswer -> MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f)
            isBlindMode && hasAnswer -> Color(0xFFF5F5F5) // Neutral color
            isCorrect -> Color(0xFFE8F5E9)
            else -> Color(0xFFFFEBEE)
        }
        val boxBorderColor = when {
            !hasAnswer -> ink.copy(alpha = 0.25f)
            isBlindMode && hasAnswer -> Color(0xFF9E9E9E) // Neutral border
            isCorrect -> Color(0xFF2E7D32)
            else -> Color(0xFFC62828)
        }

        Box(
            modifier = modifier
                .fillMaxWidth()
                .background(surfaceColor, RoundedCornerShape(8.dp))
        ) {
            // Rascunho cobrindo TUDO
            InkCanvas(
                modifier = Modifier.matchParentSize().padding(Spacing.xs),
                penColor = penColor,
                penWidth = penWidth,
                enabled = isActive,
                isErasing = isErasing,
                clearSignal = clearSignal,
                undoSignal = localUndoSignal,
                redoSignal = localRedoSignal,
                initialStrokes = initialScratchStrokes,
                initialRedoStack = initialScratchRedoStack,
                guideMode = mappedGuideMode ?: if (isLined) "lined" else "single",
                onSyncStrokes = onSyncScratch,
                onPenEvent = onPenEvent,
            )

            // Enunciado e Quadrado flutuante
            Column(
                modifier = Modifier
                    .matchParentSize()
                    .padding(Spacing.md),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Topo: Statement
                if (verticalStackData != null) {
                    VerticalArithmeticStack(
                        term1 = verticalStackData.first,
                        op = verticalStackData.second,
                        term2 = verticalStackData.third,
                        ink = ink,
                        modifier = Modifier.padding(vertical = Spacing.xs)
                    )
                } else {
                    Text(
                        text = "${field.fieldIndex + 1}. " + renderLatex(cleanStatement),
                        fontSize = if (isActive) 32.sp else 28.sp,
                        lineHeight = if (isActive) 40.sp else 36.sp,
                        fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
                        color = ink,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                if (mediaSpec != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .padding(bottom = Spacing.md),
                        contentAlignment = Alignment.Center
                    ) {
                        if (mediaSpec.startsWith("[fig:")) {
                            GeometricDiagram(spec = mediaSpec, ink = ink)
                        } else {
                            // Placeholder futuro para img/svg
                            Text("Media area", color = ink.copy(alpha = 0.5f))
                        }
                    }
                }

                Spacer(Modifier.weight(1f))

                // Rodapé Inferior Direito: Quadrado de Resposta + Botao Verde
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .height(90.dp)
                            .width(180.dp)
                            .background(boxBgColor, RoundedCornerShape(8.dp))
                            .border(2.dp, boxBorderColor, RoundedCornerShape(8.dp))
                    ) {
                        InkCanvas(
                            modifier = Modifier.matchParentSize().padding(Spacing.xs),
                            penColor = penColor,
                            penWidth = penWidth,
                            enabled = isActive,
                            isErasing = isErasing,
                            clearSignal = localClearSignal,
                            undoSignal = localUndoSignal,
                            redoSignal = localRedoSignal,
                            initialStrokes = initialAnswerStrokes,
                            initialRedoStack = initialAnswerRedoStack,
                            guideMode = mappedGuideMode ?: if (isFullPage || isLined) "lined" else "single",
                            onSyncStrokes = answerSync,
                            onPenEvent = onPenEvent,
                            onTap = {
                                answerPadVisible.value = true
                            },
                        )
                        if (typedAnswer.isNotBlank()) {
                            MathAnswerTemplate(
                                expectedAnswer = field.expectedAnswer,
                                studentInput = typedAnswer,
                                isCorrect = isCorrect,
                                ink = ink
                            )
                        }

                        // Botao x no canto superior direito (limpa canvas)
                        if (initialAnswerStrokes.isNotEmpty()) {
                            androidx.compose.material3.IconButton(
                                onClick = { localClearSignal++ },
                                modifier = Modifier.align(Alignment.TopEnd).size(28.dp).padding(4.dp)
                            ) {
                                androidx.compose.material3.Icon(
                                    imageVector = androidx.compose.material.icons.Icons.Outlined.Clear,
                                    contentDescription = "Limpar",
                                    tint = ink.copy(alpha = 0.4f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                    
                    // Botao verde de confirmacao — ao lado direito do campo
                    if (isActive) {
                        androidx.compose.material3.IconButton(
                            onClick = {
                                localClearSignal++
                                onConfirmStroke(initialAnswerStrokes)
                            },
                            enabled = initialAnswerStrokes.isNotEmpty(),
                            modifier = Modifier
                                .size(64.dp)
                                .padding(start = 6.dp),
                        ) {
                            androidx.compose.foundation.layout.Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .background(
                                        if (initialAnswerStrokes.isNotEmpty()) Color(0xFF2E7D32) else Color(0xFF2E7D32).copy(alpha = 0.25f),
                                        androidx.compose.foundation.shape.RoundedCornerShape(32.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                androidx.compose.material3.Text(
                                    "OK",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                )
                            }
                        }
                    }
                }
            }

            if (answerPadVisible.value) {
                Dialog(
                    onDismissRequest = { answerPadVisible.value = false }
                ) {
                    AnswerPad(
                        ink = ink,
                        onKey = { key ->
                            when (key) {
                                "ok" -> answerPadVisible.value = false
                                "del" -> onTypedAnswerChange(typedAnswer.dropLast(1))
                                "clr" -> onTypedAnswerChange("")
                                else -> onTypedAnswerChange(typedAnswer + key)
                            }
                        }
                    )
                }
            }
        }
    }
}

// AnswerPad, SplitHeightHandle, InkCanvas e helpers extraidos para arquivos separados (mesmo package).

@Composable
fun VerticalArithmeticStack(
    term1: String,
    op: String,
    term2: String,
    ink: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.width(140.dp),
            horizontalAlignment = Alignment.End
        ) {
            // Termo 1 (alinhado à direita)
            Text(
                text = term1,
                fontSize = 32.sp,
                fontWeight = FontWeight.SemiBold,
                color = ink,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
            )
            // Operador e Termo 2
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = op,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ink
                )
                Text(
                    text = term2,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ink,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
            }
            Spacer(Modifier.height(4.dp))
            // Barra de conta cinza
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(ink.copy(alpha = 0.40f))
            )
        }
    }
}
