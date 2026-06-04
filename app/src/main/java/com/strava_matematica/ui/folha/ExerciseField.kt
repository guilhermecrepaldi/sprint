package com.strava_matematica.ui.folha

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.strava_matematica.design.FocusColors
import com.strava_matematica.design.Spacing
import com.strava_matematica.model.BackgroundMode
import com.strava_matematica.model.FolhaField
import com.strava_matematica.model.PenEvent
import kotlin.math.hypot


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
    val mediaTagRegex = remember(statementText) { Regex("""\[(fig|img|svg):[^\]]+\]""") }
    val mediaMatch = remember(statementText) { mediaTagRegex.find(statementText) }
    val mediaSpec = remember(statementText) { mediaMatch?.value }
    val cleanStatement = remember(statementText) { statementText.replace(mediaTagRegex, "").trim() }

    var localClearSignal by remember { mutableStateOf(0) }
    var localUndoSignal by remember { mutableStateOf(0) }

    if (isCompact) {
        val isCorrect = typedAnswer.trim() == field.expectedAnswer?.trim()
        val hasAnswer = typedAnswer.isNotBlank()
        val boxBgColor = when {
            !hasAnswer -> MaterialTheme.colorScheme.onBackground.copy(alpha = 0.02f)
            isBlindMode && hasAnswer -> Color(0xFFF5F5F5) // Neutral color for answered in blind mode
            isCorrect -> Color(0xFFE8F5E9)  // verde claro pastel
            else -> Color(0xFFFFEBEE)       // vermelho claro pastel
        }
        val boxBorderColor = when {
            !hasAnswer -> ink.copy(alpha = 0.08f)
            isBlindMode && hasAnswer -> Color(0xFF9E9E9E) // Neutral border for answered in blind mode
            isCorrect -> Color(0xFF2E7D32)  // verde esmeralda
            else -> Color(0xFFC62828)       // vermelho coral
        }

        // Novo Layout Compacto de Linha para a Grade Superior
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Esquerda: Enunciado
            Row(
                modifier = Modifier.weight(1f, fill = false),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "${field.fieldIndex + 1}.",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = ink.copy(alpha = 0.35f)
                )

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
                    .height(90.dp)
                    .width(195.dp)
                    .background(boxBgColor, RoundedCornerShape(8.dp))
                    .border(2.dp, boxBorderColor, RoundedCornerShape(8.dp))
                    .clickable { answerPadVisible.value = true }
            ) {
                InkCanvas(
                    modifier = Modifier.matchParentSize().padding(4.dp),
                    penColor = penColor,
                    penWidth = penWidth,
                    enabled = isActive,
                    isErasing = isErasing,
                    clearSignal = localClearSignal, // Resposta escuta SOMENTE o clear local
                    undoSignal = undoSignal, 
                    redoSignal = redoSignal,
                    initialStrokes = initialAnswerStrokes,
                    initialRedoStack = initialAnswerRedoStack,
                    guideMode = "single",
                    onSyncStrokes = answerSync,
                    onPenEvent = onPenEvent
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
            !hasAnswer -> MaterialTheme.colorScheme.onBackground.copy(alpha = 0.02f)
            isBlindMode && hasAnswer -> Color(0xFFF5F5F5) // Neutral color
            isCorrect -> Color(0xFFE8F5E9)
            else -> Color(0xFFFFEBEE)
        }
        val boxBorderColor = when {
            !hasAnswer -> ink.copy(alpha = 0.08f)
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
                undoSignal = undoSignal,
                redoSignal = redoSignal,
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

                // Rodapé Inferior Direito: Quadrado de Resposta
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.BottomEnd
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .height(90.dp)
                            .width(180.dp)
                            .background(boxBgColor, RoundedCornerShape(8.dp))
                            .border(2.dp, boxBorderColor, RoundedCornerShape(8.dp))
                            .clickable { answerPadVisible.value = true }
                    ) {
                        InkCanvas(
                            modifier = Modifier.matchParentSize().padding(Spacing.xs),
                            penColor = penColor,
                            penWidth = penWidth,
                            enabled = isActive,
                            isErasing = isErasing,
                            clearSignal = localClearSignal, // Independente do global
                            undoSignal = if (isFullPage) undoSignal else 0,
                            redoSignal = if (isFullPage) redoSignal else 0,
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

                        androidx.compose.material3.IconButton(
                            onClick = { localClearSignal++ },
                            modifier = Modifier.align(Alignment.TopEnd).size(32.dp).padding(6.dp)
                        ) {
                            androidx.compose.material3.Icon(
                                imageVector = androidx.compose.material.icons.Icons.Outlined.Clear,
                                contentDescription = "Limpar",
                                tint = ink.copy(alpha = 0.5f),
                                modifier = Modifier.size(20.dp)
                            )
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

@Composable
private fun AnswerPad(
    ink: Color,
    onKey: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val keys = listOf("7", "8", "9", "-", "4", "5", "6", ".", "1", "2", "3", "/", "0", "x", "=", "del", "clr", "ok")
    Row(
        modifier = modifier
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.94f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 7.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        keys.forEach { key ->
            Box(
                modifier = Modifier
                    .size(width = if (key.length > 1) 38.dp else 30.dp, height = 30.dp)
                    .background(ink.copy(alpha = 0.045f), RoundedCornerShape(5.dp))
                    .clickable { onKey(key) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = key,
                    fontSize = 12.sp,
                    color = ink.copy(alpha = if (key == "ok") 0.62f else 0.46f),
                )
            }
        }
    }
}

@Composable
private fun SplitHeightHandle(
    ink: Color,
    ratio: Float,
    onRatioChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(30.dp)
            .drawBehind {
                val y = size.height / 2f
                drawLine(
                    color = ink.copy(alpha = 0.14f),
                    start = Offset(30.dp.toPx(), y),
                    end = Offset(size.width, y),
                    strokeWidth = 1.dp.toPx(),
                )
            },
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .size(28.dp)
                .pointerInput(ratio) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val delta = dragAmount.y / 420f
                        onRatioChange((ratio + delta).coerceIn(0.42f, 0.88f))
                    }
                }
                .drawBehind {
                    drawCircle(
                        color = ink.copy(alpha = 0.24f),
                        radius = 8.dp.toPx(),
                        center = Offset(size.width / 2f, size.height / 2f),
                    )
                },
        )
    }
}

@Composable
fun InkCanvas(
    modifier: Modifier = Modifier,
    penColor: String = "#1a1a1a",
    penWidth: Float = 2.2f,
    enabled: Boolean = true,
    isErasing: Boolean = false,
    clearSignal: Int = 0,
    undoSignal: Int = 0,
    redoSignal: Int = 0,
    initialStrokes: List<List<Offset>> = emptyList(),
    initialRedoStack: List<List<Offset>> = emptyList(),
    guideMode: String = "single",
    onSyncStrokes: (List<List<Offset>>, List<List<Offset>>) -> Unit = { _, _ -> },
    onPenEvent: (PenEvent) -> Unit = {},
    onTap: (() -> Unit)? = null,
) {
    val strokes = remember { mutableStateListOf(*initialStrokes.toTypedArray()) }
    val redoStack = remember { mutableStateListOf(*initialRedoStack.toTypedArray()) }
    
    LaunchedEffect(initialStrokes) {
        if (initialStrokes != strokes.toList()) {
            strokes.clear()
            strokes.addAll(initialStrokes)
        }
    }
    
    LaunchedEffect(initialRedoStack) {
        if (initialRedoStack != redoStack.toList()) {
            redoStack.clear()
            redoStack.addAll(initialRedoStack)
        }
    }
    
    val inkColor = remember(penColor) { parseHexColor(penColor) }
    var currentStroke = remember { mutableListOf<Offset>() }
    var strokeStartedAt = remember { 0L }
    var previousPoint = remember { Offset.Zero }
    var previousUptime = remember { 0L }

    LaunchedEffect(clearSignal) {
        if (clearSignal > 0) {
            strokes.clear()
            redoStack.clear()
            onSyncStrokes(strokes.toList(), redoStack.toList())
            onPenEvent(Offset.Zero.toPenEvent(System.currentTimeMillis(), 0f, "clear"))
        }
    }

    LaunchedEffect(undoSignal) {
        if (undoSignal > 0 && strokes.isNotEmpty()) {
            redoStack.add(strokes.removeAt(strokes.lastIndex))
            onSyncStrokes(strokes.toList(), redoStack.toList())
            onPenEvent(Offset.Zero.toPenEvent(System.currentTimeMillis(), 0f, "undo"))
        }
    }

    LaunchedEffect(redoSignal) {
        if (redoSignal > 0 && redoStack.isNotEmpty()) {
            strokes.add(redoStack.removeAt(redoStack.lastIndex))
            onSyncStrokes(strokes.toList(), redoStack.toList())
            onPenEvent(Offset.Zero.toPenEvent(System.currentTimeMillis(), 0f, "redo"))
        }
    }

    Canvas(
        modifier = modifier.pointerInput(enabled, penColor, penWidth, isErasing) {
            if (!enabled) return@pointerInput
            val eraserRadiusPx = 24.dp.toPx()
            awaitEachGesture {
                // Aguarda o primeiro toque na fase INITIAL (antes dos pais como ZoomableCanvas interceptarem)
                val event = awaitPointerEvent(androidx.compose.ui.input.pointer.PointerEventPass.Initial)
                val down = event.changes.firstOrNull { it.pressed } ?: return@awaitEachGesture
                
                // Temporariamente permitindo TODOS os toques (dedo, mouse) para testar o MVP
                val type = down.type
                val isPen = type == androidx.compose.ui.input.pointer.PointerType.Stylus || 
                            type == androidx.compose.ui.input.pointer.PointerType.Eraser ||
                            type == androidx.compose.ui.input.pointer.PointerType.Mouse ||
                            type == androidx.compose.ui.input.pointer.PointerType.Touch

                if (!isPen) return@awaitEachGesture // Deixa o dedo passar para o Pan/Zoom

                down.consume() // Consome o evento para garantir que ninguem mais use

                val motionEvent = try {
                    val field = event.javaClass.getDeclaredField("motionEvent")
                    field.isAccessible = true
                    field.get(event) as? android.view.MotionEvent
                } catch (e: Exception) {
                    null
                }
                val isStylusButton = motionEvent?.let {
                    (it.buttonState and android.view.MotionEvent.BUTTON_STYLUS_PRIMARY != 0) ||
                    (it.buttonState and android.view.MotionEvent.BUTTON_SECONDARY != 0)
                } ?: false

                val activeErasing = isErasing || 
                                    type == androidx.compose.ui.input.pointer.PointerType.Eraser ||
                                    isStylusButton

                if (activeErasing) {
                    eraseNear(down.position, strokes, eraserRadiusPx)
                } else {
                    strokeStartedAt = System.currentTimeMillis()
                    previousUptime = strokeStartedAt
                    previousPoint = down.position
                    currentStroke = mutableListOf(down.position)
                    redoStack.clear()
                    strokes.add(currentStroke.toList())
                    onPenEvent(down.position.toPenEvent(strokeStartedAt, 0f, "stroke_start", down.pressure))
                }

                while (true) {
                    val loopEvent = awaitPointerEvent(androidx.compose.ui.input.pointer.PointerEventPass.Initial)
                    val change = loopEvent.changes.firstOrNull { it.id == down.id } ?: break

                    val motionEventLoop = try {
                        val field = loopEvent.javaClass.getDeclaredField("motionEvent")
                        field.isAccessible = true
                        field.get(loopEvent) as? android.view.MotionEvent
                    } catch (e: Exception) {
                        null
                    }
                    val isStylusButtonLoop = motionEventLoop?.let {
                        (it.buttonState and android.view.MotionEvent.BUTTON_STYLUS_PRIMARY != 0) ||
                        (it.buttonState and android.view.MotionEvent.BUTTON_SECONDARY != 0)
                    } ?: false

                    val activeErasingLoop = isErasing || 
                                            type == androidx.compose.ui.input.pointer.PointerType.Eraser ||
                                            isStylusButtonLoop

                    if (activeErasingLoop) {
                        if (change.pressed) {
                            eraseNear(change.position, strokes, eraserRadiusPx)
                            change.consume()
                        } else {
                            onSyncStrokes(strokes.toList(), redoStack.toList())
                            break
                        }
                        continue
                    }

                    if (change.position != previousPoint) {
                        val now = System.currentTimeMillis()
                        val offset = change.position
                        val elapsed = (now - previousUptime).coerceAtLeast(1L)
                        val distance = hypot(offset.x - previousPoint.x, offset.y - previousPoint.y)
                        val velocity = distance / elapsed
                        currentStroke.add(offset)
                        if (strokes.isNotEmpty()) {
                            strokes[strokes.lastIndex] = currentStroke.toList()
                        }
                        previousPoint = offset
                        previousUptime = now
                        onPenEvent(offset.toPenEvent(strokeStartedAt, velocity, "stroke_move", change.pressure))
                    }
                    change.consume()

                    if (!change.pressed) {
                        if (currentStroke.isNotEmpty()) {
                            onPenEvent(currentStroke.last().toPenEvent(strokeStartedAt, 0f, "stroke_end"))
                        }
                        val duration = System.currentTimeMillis() - strokeStartedAt
                        val dist = if (currentStroke.isNotEmpty()) {
                            hypot(currentStroke.last().x - currentStroke.first().x, currentStroke.last().y - currentStroke.first().y)
                        } else {
                            0f
                        }
                        val isTap = duration < 320L && dist < 12.dp.toPx()
                        if (isTap && onTap != null) {
                            if (strokes.isNotEmpty()) {
                                strokes.removeAt(strokes.lastIndex)
                            }
                            onSyncStrokes(strokes.toList(), redoStack.toList())
                            onTap()
                        } else {
                            onSyncStrokes(strokes.toList(), redoStack.toList())
                        }
                        break
                    }
                }
            }
        },
    ) {
        when (guideMode) {
            "lined" -> {
                val gap = 32.dp.toPx()
                var y = gap
                while (y < size.height) {
                    drawLine(
                        color = Color.Gray.copy(alpha = 0.22f),
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1.dp.toPx(),
                    )
                    y += gap
                }
            }
            "dots" -> {
                val gap = 28.dp.toPx()
                var x = gap / 2
                while (x < size.width) {
                    var y = gap / 2
                    while (y < size.height) {
                        drawCircle(
                            color = inkColor.copy(alpha = 0.12f),
                            radius = 1.5f,
                            center = Offset(x, y),
                        )
                        y += gap
                    }
                    x += gap
                }
            }
            else -> {
                val y = size.height * 0.72f
                drawLine(
                    color = Color.Gray.copy(alpha = 0.35f),
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1.dp.toPx(),
                )
            }
        }
        strokes.forEach { stroke ->
            if (stroke.size == 1) {
                drawCircle(color = inkColor, radius = penWidth.dp.toPx() / 2f, center = stroke.first())
            } else if (stroke.size > 1) {
                val path = Path().apply {
                    moveTo(stroke.first().x, stroke.first().y)
                    stroke.drop(1).forEach { lineTo(it.x, it.y) }
                }
                drawPath(
                    path = path,
                    color = inkColor,
                    style = Stroke(width = penWidth.dp.toPx(), cap = StrokeCap.Round),
                )
            }
        }
    }
}

private fun Offset.toPenEvent(
    strokeStartedAt: Long,
    velocity: Float,
    eventType: String,
    pressure: Float? = null,
): PenEvent {
    return PenEvent(
        ts = (System.currentTimeMillis() - strokeStartedAt).coerceAtLeast(0L),
        x = x,
        y = y,
        pressure = pressure,
        tilt = null,
        velocity = velocity,
        eventType = eventType,
    )
}

private fun eraseNear(point: Offset, strokes: MutableList<List<Offset>>, radiusPx: Float) {
    val r2 = radiusPx * radiusPx
    strokes.removeAll { stroke ->
        stroke.any { pt ->
            val dx = pt.x - point.x
            val dy = pt.y - point.y
            dx * dx + dy * dy <= r2
        }
    }
}

private fun parseHexColor(hex: String): Color {
    return runCatching { Color(android.graphics.Color.parseColor(hex)) }
        .getOrDefault(Color(0xFF1A1A1A))
}

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
