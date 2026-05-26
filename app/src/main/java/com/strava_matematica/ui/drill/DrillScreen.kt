package com.strava_matematica.ui.drill

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.strava_matematica.design.Spacing
import com.strava_matematica.model.DrillFlushResult
import com.strava_matematica.viewmodel.DrillPhase
import com.strava_matematica.viewmodel.DrillViewModel

private val BgColor   = Color(0xFF0E0E0E)
private val TextColor = Color(0xFFEEEEEE)
private val DimColor  = Color(0xFF555555)
private val GreenDot  = Color(0xFF4CAF50)
private val RedDot    = Color(0xFFE53935)
private val InputBg   = Color(0xFF1A1A1A)

@Composable
fun DrillScreen(
    studentId: String,
    count: Int = 30,
    level: String = "basic",
    onDone: (DrillFlushResult) -> Unit,
    onBack: () -> Unit,
    vm: DrillViewModel = viewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()

    // Inicia o batch ao entrar na tela
    LaunchedEffect(Unit) {
        vm.setStudentId(studentId)
        vm.loadBatch(count, level)
    }

    // Navega pro resultado quando concluído
    LaunchedEffect(state.flushResult) {
        state.flushResult?.let { onDone(it) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgColor),
    ) {
        when (state.phase) {
            DrillPhase.LOADING -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = DimColor,
                )
            }

            DrillPhase.FLUSHING -> {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Spacing.md),
                ) {
                    CircularProgressIndicator(color = GreenDot)
                    Text("Salvando...", color = DimColor, style = MaterialTheme.typography.bodyMedium)
                }
            }

            DrillPhase.DONE, DrillPhase.ERROR -> {
                // handled by LaunchedEffect above
            }

            DrillPhase.ACTIVE -> {
                val item = state.currentItem
                if (item != null) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = Spacing.lg, vertical = Spacing.xl),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        // ── Barra de progresso ────────────────────────────────
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "${state.currentIndex + 1} / ${state.total}",
                                color = DimColor,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Text(
                                text = formatTime(state.elapsedSeconds),
                                color = DimColor,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }

                        // Progress bar fina
                        val progress = (state.currentIndex.toFloat() / state.total.coerceAtLeast(1))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = Spacing.sm)
                                .height(2.dp)
                                .background(Color(0xFF2A2A2A), RoundedCornerShape(1.dp)),
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(progress)
                                    .height(2.dp)
                                    .background(GreenDot, RoundedCornerShape(1.dp)),
                            )
                        }

                        Spacer(Modifier.weight(1f))

                        // ── Statement ─────────────────────────────────────────
                        AnimatedContent(
                            targetState = item.statement,
                            transitionSpec = {
                                (slideInVertically { -it } + fadeIn()) togetherWith
                                    (slideOutVertically { it } + fadeOut())
                            },
                            label = "statement",
                        ) { stmt ->
                            Text(
                                text = stmt,
                                color = TextColor,
                                fontSize = 64.sp,
                                fontWeight = FontWeight.Light,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }

                        Spacer(Modifier.height(Spacing.xl))

                        // ── Input ─────────────────────────────────────────────
                        val focusRequester = remember { FocusRequester() }

                        LaunchedEffect(state.currentIndex) {
                            // Re-foca a cada novo exercício
                            focusRequester.requestFocus()
                        }

                        BasicTextField(
                            value = state.currentInput,
                            onValueChange = vm::onInputChange,
                            modifier = Modifier
                                .focusRequester(focusRequester)
                                .width(160.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(InputBg)
                                .padding(horizontal = Spacing.lg, vertical = Spacing.md),
                            textStyle = TextStyle(
                                color = TextColor,
                                fontSize = 48.sp,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center,
                            ),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Done,
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = { vm.submitCurrent() },
                            ),
                            cursorBrush = SolidColor(TextColor),
                            singleLine = true,
                        )

                        Spacer(Modifier.height(Spacing.xl))

                        // ── Dots de momentum (últimos 10 resultados) ──────────
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            state.recentCorrect.forEach { correct ->
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(if (correct) GreenDot else RedDot),
                                )
                            }
                            // Placeholders para os slots ainda não preenchidos
                            repeat(10 - state.recentCorrect.size) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(DimColor.copy(alpha = 0.3f)),
                                )
                            }
                        }

                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

private fun formatTime(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return "%d:%02d".format(m, s)
}

// ── Tela de resultado do drill ────────────────────────────────────────────────

@Composable
fun DrillResultScreen(
    result: DrillFlushResult,
    onNewDrill: () -> Unit,
    onBack: () -> Unit,
) {
    val accuracy = (result.accuracy * 100).toInt()
    val avgSec = result.avgTimeMs / 1000f

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgColor)
            .padding(Spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "$accuracy%",
            color = if (accuracy >= 80) GreenDot else TextColor,
            fontSize = 80.sp,
            fontWeight = FontWeight.Bold,
        )

        Spacer(Modifier.height(Spacing.sm))

        Text(
            text = "${result.correct} / ${result.total}",
            color = DimColor,
            style = MaterialTheme.typography.titleLarge,
        )

        Spacer(Modifier.height(Spacing.xl))

        StatRow("Tempo médio por exercício", "%.1fs".format(avgSec))
        StatRow("XP ganho", "+${result.xpEarned}")
        StatRow("Tempo total", formatTime(result.totalTimeMs / 1000))

        Spacer(Modifier.height(48.dp))

        Text(
            text = "Novamente",
            color = TextColor,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(InputBg)
                .clickable { onNewDrill() }
                .padding(horizontal = Spacing.xl, vertical = Spacing.md),
        )

        Spacer(Modifier.height(Spacing.lg))

        Text(
            text = "Voltar",
            color = DimColor,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .clickable { onBack() }
                .padding(vertical = Spacing.sm),
        )
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, color = DimColor, style = MaterialTheme.typography.bodyMedium)
        Text(value, color = TextColor, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}
