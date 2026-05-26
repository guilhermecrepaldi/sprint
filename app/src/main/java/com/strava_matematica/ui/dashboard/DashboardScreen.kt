package com.strava_matematica.ui.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.strava_matematica.design.Spacing
import com.strava_matematica.ui.folha.InkCanvas
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ── Trilha de habilidades em ordem pedagógica ────────────────────────────────

val SKILL_ORDER = listOf(
    "soma_subtracao", "multiplicacao_divisao", "fracoes_decimais",
    "porcentagem_razao", "potenciacao_radiciacao", "equacoes_lineares",
    "sistemas_equacoes", "fatoracao_produtos_notaveis", "inequacoes",
    "equacoes_quadraticas", "funcao_afim", "funcao_quadratica",
    "funcao_exponencial", "funcao_logaritmica", "funcao_modular",
    "geometria_plana", "geometria_espacial", "geometria_analitica",
    "progressoes_pa_pg", "combinatoria", "probabilidade",
    "trig_razoes", "trig_seno_cosseno_tangente", "trig_identidades",
    "trig_equacoes", "nocao_de_limite", "continuidade",
    "derivadas_basicas", "derivadas_regra_cadeia", "derivadas_produto_quociente",
    "aplicacoes_derivadas", "integrais_indefinidas", "integrais_definidas",
    "aplicacoes_integrais",
)

val SKILL_NAMES = mapOf(
    "soma_subtracao" to "Soma e Sub.", "multiplicacao_divisao" to "Multiplicação",
    "fracoes_decimais" to "Frações", "porcentagem_razao" to "Porcentagem",
    "potenciacao_radiciacao" to "Potenciação", "equacoes_lineares" to "Eq. Lineares",
    "sistemas_equacoes" to "Sistemas", "fatoracao_produtos_notaveis" to "Fatoração",
    "inequacoes" to "Inequações", "equacoes_quadraticas" to "Eq. Quadráticas",
    "funcao_afim" to "Função Afim", "funcao_quadratica" to "Função Quadrática",
    "funcao_exponencial" to "Exponencial", "funcao_logaritmica" to "Logaritmo",
    "funcao_modular" to "Modular", "geometria_plana" to "Geo. Plana",
    "geometria_espacial" to "Geo. Espacial", "geometria_analitica" to "Geo. Analítica",
    "progressoes_pa_pg" to "PA e PG", "combinatoria" to "Combinatória",
    "probabilidade" to "Probabilidade", "trig_razoes" to "Trig. Razões",
    "trig_seno_cosseno_tangente" to "Sen, Cos, Tan", "trig_identidades" to "Identidades",
    "trig_equacoes" to "Eq. Trig.", "nocao_de_limite" to "Limite",
    "continuidade" to "Continuidade", "derivadas_basicas" to "Derivadas",
    "derivadas_regra_cadeia" to "Regra da Cadeia",
    "derivadas_produto_quociente" to "Prod. e Quot.",
    "aplicacoes_derivadas" to "Aplic. Derivadas",
    "integrais_indefinidas" to "Integrais Indef.",
    "integrais_definidas" to "Integrais Def.",
    "aplicacoes_integrais" to "Aplic. Integrais",
)

// ── Modos de estudo: mapeiam para density no ViewModel ───────────────────────
// "high" = Simulado  |  "medium" = Fixação  |  "low" = Atenção
private val MODES = listOf(
    "high"   to "Simulado",
    "medium" to "Fixação",
    "low"    to "Atenção",
)

// ── Heatmap ──────────────────────────────────────────────────────────────────

@Composable
fun ActivityHeatmap(
    activityByDate: Map<String, Int> = emptyMap(),
    modifier: Modifier = Modifier,
) {
    val weeks = 18
    val today = java.time.LocalDate.now()

    fun cellColor(count: Int): Color = when {
        count == 0 -> Color(0xFFE8E8E8)
        count < 5  -> Color(0xFF9BE9A8)
        count < 15 -> Color(0xFF40C463)
        count < 30 -> Color(0xFF30A14E)
        else       -> Color(0xFF216E39)
    }

    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        repeat(weeks) { weekOffset ->
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                repeat(7) { dayOffset ->
                    val date = today.minusDays(
                        ((weeks - 1 - weekOffset) * 7 + (6 - dayOffset)).toLong()
                    )
                    val count = activityByDate[date.toString()] ?: 0
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(cellColor(count))
                    )
                }
            }
        }
    }
}

// ── DashboardScreen ──────────────────────────────────────────────────────────

@Composable
fun DashboardScreen(
    streak: Int = 0,
    totalExercises: Int = 0,
    activityByDate: Map<String, Int> = emptyMap(),
    selectedSkillTag: String = "soma_subtracao",
    densityLevel: String = "medium",          // high=Simulado, medium=Fixação, low=Atenção
    onSkillSelect: (String) -> Unit = {},
    onDensitySelect: (String) -> Unit = {},
    onStrokesChanged: (List<List<Offset>>) -> Unit = {},
    onStart: () -> Unit = {},
    onStartDrill: () -> Unit = {},
    onShowRanking: () -> Unit = {},
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var canvasStrokes by remember { mutableStateOf<List<List<Offset>>>(emptyList()) }
    var clearSignal by remember { mutableIntStateOf(0) }

    // Sub-panel abre na 1ª seleção de skill; fecha se skill mudar externamente (OCR)
    var subPanelOpen by remember { mutableStateOf(false) }

    // Centra o scroll no tópico selecionado (OCR ou manual)
    val selectedIndex = SKILL_ORDER.indexOf(selectedSkillTag).coerceAtLeast(0)
    LaunchedEffect(selectedSkillTag) {
        scope.launch {
            listState.animateScrollToItem((selectedIndex - 1).coerceAtLeast(0))
        }
        // OCR mudou o skill → abre sub-panel automaticamente
        subPanelOpen = true
    }

    // Debounce 900ms → dispara OCR de identificação
    LaunchedEffect(canvasStrokes) {
        if (canvasStrokes.isNotEmpty()) {
            delay(900)
            onStrokesChanged(canvasStrokes)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .twoFingerTapDetector(onStart),
    ) {

        // ── Header / Perfil ───────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.lg, vertical = Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "G",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            }
            Column {
                Text(
                    "LOVE CLASS",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                if (totalExercises > 0) {
                    Text(
                        "$totalExercises exercícios",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            if (streak > 0) {
                Text("🔥 $streak dias", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.width(Spacing.sm))
            }
            Text(
                text = "🏆",
                fontSize = 22.sp,
                modifier = androidx.compose.ui.Modifier.clickable { onShowRanking() },
            )
        }

        // ── Heatmap de atividade ──────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.lg),
        ) {
            Text(
                "Rotina",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f),
            )
            Spacer(Modifier.height(4.dp))
            ActivityHeatmap(
                activityByDate = activityByDate,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Spacer(Modifier.weight(1f))

        // ── Interaction Square ────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .padding(horizontal = Spacing.md)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(
                    1.dp,
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                    RoundedCornerShape(16.dp),
                ),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {

                // ── Row 1: Skill scroll ───────────────────────────────────────
                // 1º clique → abre sub-panel
                // 2º clique no mesmo skill → onStart()
                LazyRow(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        horizontal = Spacing.sm
                    ),
                ) {
                    itemsIndexed(SKILL_ORDER) { _, tag ->
                        val isSelected = tag == selectedSkillTag
                        val scale by animateFloatAsState(
                            targetValue = if (isSelected) 1.22f else 1f,
                            animationSpec = tween(durationMillis = 180),
                            label = "skillScale",
                        )
                        val alpha by animateFloatAsState(
                            targetValue = if (isSelected) 1f else 0.45f,
                            animationSpec = tween(durationMillis = 180),
                            label = "skillAlpha",
                        )
                        Text(
                            text = SKILL_NAMES[tag] ?: tag,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            modifier = Modifier
                                .wrapContentWidth()
                                .graphicsLayer(scaleX = scale, scaleY = scale, alpha = alpha)
                                .clickable {
                                    if (tag == selectedSkillTag && subPanelOpen) {
                                        onStart() // 2º clique = enter
                                    } else {
                                        onSkillSelect(tag)
                                        subPanelOpen = true
                                    }
                                }
                                .padding(horizontal = 6.dp, vertical = 4.dp),
                        )
                    }
                }

                // ── Row 2: Modo de estudo (AnimatedVisibility) ────────────────
                // Aparece após 1º clique no skill
                AnimatedVisibility(
                    visible = subPanelOpen,
                    enter = expandVertically(animationSpec = tween(200)),
                    exit = shrinkVertically(animationSpec = tween(150)),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(36.dp)
                            .background(
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.55f)
                            ),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        MODES.forEach { (level, label) ->
                            val isModeSelected = level == densityLevel
                            val modeAlpha by animateFloatAsState(
                                targetValue = if (isModeSelected) 1f else 0.42f,
                                animationSpec = tween(160),
                                label = "modeAlpha",
                            )
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clickable { onDensitySelect(level) }
                                    .background(
                                        if (isModeSelected)
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                                        else Color.Transparent
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    fontWeight = if (isModeSelected) FontWeight.SemiBold
                                                 else FontWeight.Normal,
                                    color = if (isModeSelected) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.graphicsLayer(alpha = modeAlpha),
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                    }
                }

                // ── Canvas: largura total (sem coluna de densidade) ───────────
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(Spacing.xs),
                ) {
                    InkCanvas(
                        modifier = Modifier.matchParentSize(),
                        penColor = "#1a1a1a",
                        enabled = true,
                        clearSignal = clearSignal,
                        guideMode = "single",
                        onSyncStrokes = { strokes, _ -> canvasStrokes = strokes },
                    )
                    if (canvasStrokes.isEmpty()) {
                        Text(
                            text = "escreva uma equação...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f),
                            modifier = Modifier.align(Alignment.Center),
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(Spacing.md))

        // ── Botões de ação ────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.md),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Button(
                onClick = onStart,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text("Iniciar", style = MaterialTheme.typography.titleMedium)
            }
            Button(
                onClick = onStartDrill,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text("Maratona", style = MaterialTheme.typography.titleMedium)
            }
        }

        Text(
            text = "ou dois dedos em qualquer lugar",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.28f),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = Spacing.lg, top = Spacing.xs),
            textAlign = TextAlign.Center,
        )
    }
}

// ── Detector de 2 dedos simultâneos ─────────────────────────────────────────

internal fun Modifier.twoFingerTapDetector(onTwoFinger: () -> Unit): Modifier =
    pointerInput(onTwoFinger) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                val newPresses = event.changes.count { it.pressed && !it.previousPressed }
                if (newPresses >= 2) {
                    event.changes.forEach { it.consume() }
                    onTwoFinger()
                }
            }
        }
    }
