package com.sprint.ui.canvas

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sprint.design.CanvasColors

// ── Skill prerequisite tree (mirrors backend engine/unlock.py) ────────────────

private val PREREQ_TREE: Map<String, List<String>> = mapOf(
    "multiplicacao_divisao"       to listOf("soma_subtracao"),
    "fracoes_decimais"            to listOf("multiplicacao_divisao"),
    "porcentagem_razao"           to listOf("fracoes_decimais"),
    "potenciacao_radiciacao"      to listOf("multiplicacao_divisao"),
    "equacoes_lineares"           to listOf("fracoes_decimais", "potenciacao_radiciacao"),
    "sistemas_equacoes"           to listOf("equacoes_lineares"),
    "fatoracao_produtos_notaveis" to listOf("equacoes_lineares"),
    "equacoes_quadraticas"        to listOf("equacoes_lineares"),
    "inequacoes"                  to listOf("equacoes_lineares"),
    "funcao_afim"                 to listOf("equacoes_lineares"),
    "funcao_quadratica"           to listOf("equacoes_quadraticas", "funcao_afim"),
    "funcao_exponencial"          to listOf("potenciacao_radiciacao", "funcao_afim"),
    "funcao_logaritmica"          to listOf("funcao_exponencial"),
    "funcao_modular"              to listOf("inequacoes"),
    "trig_razoes"                 to listOf("funcao_quadratica"),
    "trig_seno_cosseno_tangente"  to listOf("trig_razoes"),
    "trig_identidades"            to listOf("trig_seno_cosseno_tangente"),
    "trig_equacoes"               to listOf("trig_identidades"),
    "geometria_plana"             to listOf("fracoes_decimais"),
    "geometria_espacial"          to listOf("geometria_plana"),
    "geometria_analitica"         to listOf("funcao_afim", "geometria_plana"),
    "progressoes_pa_pg"           to listOf("funcao_afim", "funcao_quadratica"),
    "combinatoria"                to listOf("fracoes_decimais"),
    "probabilidade"               to listOf("combinatoria"),
    "nocao_de_limite"             to listOf("funcao_logaritmica", "trig_identidades"),
    "continuidade"                to listOf("nocao_de_limite"),
    "derivadas_basicas"           to listOf("nocao_de_limite"),
    "derivadas_regra_cadeia"      to listOf("derivadas_basicas"),
    "derivadas_produto_quociente" to listOf("derivadas_basicas"),
    "aplicacoes_derivadas"        to listOf("derivadas_regra_cadeia", "derivadas_produto_quociente"),
    "integrais_indefinidas"       to listOf("aplicacoes_derivadas"),
    "integrais_definidas"         to listOf("integrais_indefinidas"),
    "aplicacoes_integrais"        to listOf("integrais_definidas"),
)

private val MODES = listOf(
    "medium" to "Fixação",
    "high" to "Simulado",
    "low" to "Maratona",
)

// Skills unlocked by a given skill (reverse lookup)
private fun unlockedBy(skill: String): List<String> =
    PREREQ_TREE.entries.filter { skill in it.value }.map { it.key }

// Short display label from snake_case tag
private fun label(tag: String): String =
    tag.split("_").joinToString(" ") { it.replaceFirstChar(Char::uppercase) }
        .let { if (it.length > 14) it.take(13) + "…" else it }

private const val NODE_RADIUS_DP  = 28f
private const val SPACING_X_DP   = 210f
private const val SPACING_Y_DP   = 150f
private const val STROKE_DP      = 1.2f
private const val REVIEW_STROKE_DP = 2f
private const val EDGE_STROKE_DP = 0.8f

@Composable
fun PlatformMap(
    currentSkill: String,
    skillStatuses: Map<String, String>,       // skill → "fraco"|"instavel"|"em_desenvolvimento"|"automatizado"
    reviewSkills: List<String>,
    onSkillSelect: (String) -> Unit,
    onModeSelect: (String) -> Unit,
) {
    val density = LocalDensity.current
    val measurer = rememberTextMeasurer()

    val nodeRadiusPx  = with(density) { NODE_RADIUS_DP.dp.toPx() }
    val spacingXPx    = with(density) { SPACING_X_DP.dp.toPx() }
    val spacingYPx    = with(density) { SPACING_Y_DP.dp.toPx() }
    val strokePx      = with(density) { STROKE_DP.dp.toPx() }
    val reviewStrokePx = with(density) { REVIEW_STROKE_DP.dp.toPx() }
    val edgeStrokePx  = with(density) { EDGE_STROKE_DP.dp.toPx() }

    // Build node list (positions relative to canvas center, computed in draw)
    val prereqs  = PREREQ_TREE[currentSkill].orEmpty()
    val unlocked = unlockedBy(currentSkill)

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(currentSkill, onSkillSelect, onModeSelect) {
                detectTapGestures { tap ->
                    val cx = size.width / 2f
                    val cy = size.height / 2f

                    // Current skill node (center)
                    // No action needed — already active

                    // Prereq nodes (below center)
                    prereqs.forEachIndexed { i, tag ->
                        val count = prereqs.size
                        val x = cx + (i - (count - 1) / 2f) * spacingXPx
                        val y = cy + spacingYPx
                        if ((tap - Offset(x, y)).getDistance() < nodeRadiusPx * 1.5f) {
                            onSkillSelect(tag)
                            return@detectTapGestures
                        }
                    }

                    // Unlocked nodes (right of center, spread vertically)
                    unlocked.take(3).forEachIndexed { i, tag ->
                        val x = cx + spacingXPx
                        val y = cy + (i - (unlocked.take(3).size - 1) / 2f) * (spacingYPx * 0.7f)
                        if ((tap - Offset(x, y)).getDistance() < nodeRadiusPx * 1.5f) {
                            onSkillSelect(tag)
                            return@detectTapGestures
                        }
                    }

                    // Review nodes (above center)
                    reviewSkills.take(3).forEachIndexed { i, tag ->
                        val count = reviewSkills.take(3).size
                        val x = cx + (i - (count - 1) / 2f) * spacingXPx
                        val y = cy - spacingYPx
                        if ((tap - Offset(x, y)).getDistance() < nodeRadiusPx * 1.5f) {
                            onSkillSelect(tag)
                            return@detectTapGestures
                        }
                    }

                    // Mode labels (top strip)
                    MODES.forEachIndexed { i, mode ->
                        val x = cx + (i - 1) * spacingXPx
                        val y = cy - spacingYPx * 2.2f
                        if ((tap - Offset(x, y)).getDistance() < nodeRadiusPx * 2f) {
                            onModeSelect(mode.first)
                            return@detectTapGestures
                        }
                    }
                }
            },
    ) {
        val cx = size.width / 2f
        val cy = size.height / 2f

        // ── Compute positions ─────────────────────────────────────────────────
        val currentPos = Offset(cx, cy)

        val prereqPositions = prereqs.mapIndexed { i, tag ->
            val count = prereqs.size
            val x = cx + (i - (count - 1) / 2f) * spacingXPx
            val y = cy + spacingYPx
            tag to Offset(x, y)
        }

        val unlockedPositions = unlocked.take(3).mapIndexed { i, tag ->
            val x = cx + spacingXPx
            val y = cy + (i - (unlocked.take(3).size - 1) / 2f) * (spacingYPx * 0.7f)
            tag to Offset(x, y)
        }

        val reviewPositions = reviewSkills.take(3).mapIndexed { i, tag ->
            val count = reviewSkills.take(3).size
            val x = cx + (i - (count - 1) / 2f) * spacingXPx
            val y = cy - spacingYPx
            tag to Offset(x, y)
        }

        // ── Draw edges ────────────────────────────────────────────────────────
        prereqPositions.forEach { (_, pos) ->
            drawLine(CanvasColors.Edge, currentPos, pos, strokeWidth = edgeStrokePx)
        }
        unlockedPositions.forEach { (_, pos) ->
            drawLine(CanvasColors.Edge, currentPos, pos, strokeWidth = edgeStrokePx)
        }

        // ── Draw mode labels (text only, no circles) ──────────────────────────
        MODES.forEachIndexed { i, mode ->
            val x = cx + (i - 1) * spacingXPx
            val y = cy - spacingYPx * 2.2f
            drawMapLabel(measurer, mode.second, Offset(x, y), CanvasColors.TextSecondary, 13.sp)
        }

        // ── Draw review nodes ─────────────────────────────────────────────────
        if (reviewPositions.isNotEmpty()) {
            reviewPositions.forEach { (tag, pos) ->
                drawNode(
                    pos, nodeRadiusPx,
                    fill = null,
                    border = CanvasColors.NodeReview,
                    strokePx = reviewStrokePx,
                )
                drawMapLabel(measurer, label(tag), pos + Offset(0f, nodeRadiusPx + 10f), CanvasColors.TextSecondary)
            }
        }

        // ── Draw prerequisite nodes ───────────────────────────────────────────
        prereqPositions.forEach { (tag, pos) ->
            val status = skillStatuses[tag]
            val (fill, border) = nodeColorsForStatus(status)
            drawNode(pos, nodeRadiusPx, fill, border, strokePx)
            val text = if (status == "automatizado") "🏆 " + label(tag) else label(tag)
            drawMapLabel(measurer, text, pos + Offset(0f, nodeRadiusPx + 10f), CanvasColors.TextSecondary)
        }

        // ── Draw unlocked nodes ───────────────────────────────────────────────
        unlockedPositions.forEach { (tag, pos) ->
            val status = skillStatuses[tag]
            val locked = PREREQ_TREE[tag]?.any { p ->
                val s = skillStatuses[p]
                s == null || s == "fraco"
            } ?: false
            val (fill, border) = if (locked) Pair(null, CanvasColors.NodeLocked)
                                 else nodeColorsForStatus(status)
            val alpha = if (locked) 0.4f else 1f
            drawNode(pos, nodeRadiusPx, fill, border, strokePx, alpha)
            val text = if (status == "automatizado") "🏆 " + label(tag) else label(tag)
            drawMapLabel(
                measurer, text,
                pos + Offset(0f, nodeRadiusPx + 10f),
                CanvasColors.TextSecondary.copy(alpha = alpha),
            )
        }

        // ── Draw current skill (center, filled) ───────────────────────────────
        drawNode(currentPos, nodeRadiusPx, fill = CanvasColors.NodeActive, border = CanvasColors.NodeActive, strokePx = strokePx)
        drawMapLabel(measurer, label(currentSkill), currentPos + Offset(0f, nodeRadiusPx + 10f), CanvasColors.TextPrimary)
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun nodeColorsForStatus(status: String?): Pair<Color?, Color> = when (status) {
    "automatizado"      -> Pair(CanvasColors.NodeFill, CanvasColors.NodeBorder)
    "em_desenvolvimento"-> Pair(null, CanvasColors.NodeBorder)
    "instavel"          -> Pair(null, CanvasColors.NodeBorder)
    "fraco"             -> Pair(null, CanvasColors.NodeLocked)
    else                -> Pair(null, CanvasColors.NodeBorder)   // available, not yet practiced
}

private fun DrawScope.drawNode(
    center: Offset,
    radius: Float,
    fill: Color?,
    border: Color,
    strokePx: Float,
    alpha: Float = 1f,
) {
    if (fill != null) {
        drawCircle(fill.copy(alpha = alpha), radius = radius, center = center)
    }
    drawCircle(border.copy(alpha = alpha), radius = radius, center = center, style = Stroke(width = strokePx))
}

private fun DrawScope.drawMapLabel(
    measurer: TextMeasurer,
    text: String,
    topCenter: Offset,
    color: Color,
    fontSize: TextUnit = 11.sp,
) {
    val result = measurer.measure(
        text,
        style = TextStyle(
            color = color,
            fontSize = fontSize,
            fontWeight = FontWeight.Light,
        ),
    )
    drawText(
        textLayoutResult = result,
        topLeft = Offset(topCenter.x - result.size.width / 2f, topCenter.y),
    )
}
