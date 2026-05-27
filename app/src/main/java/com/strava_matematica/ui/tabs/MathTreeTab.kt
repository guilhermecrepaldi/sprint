package com.strava_matematica.ui.tabs

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Skill tree como timeline vertical scrollável.
 * Linha tracejada à esquerda · nó preenchido = skill ativa · nó vazio = disponível.
 * Cores: verde >85% accuracy · vermelho <60% · neutro entre os dois.
 */
@Composable
fun MathTreeTab(
    currentSkill: String,
    skillStatuses: Map<String, String>,
    skillAttempts: Map<String, Int> = emptyMap(),
    skillAvailable: Map<String, Int> = emptyMap(),
    skillAccuracy: Map<String, Float> = emptyMap(),
    onSkillSelect: (String) -> Unit,
    onGoToSprint: () -> Unit,
) {
    val totalSkills = SkillTree.groups.sumOf { it.skills.size }
    var skillCounter = 0

    SettingsTabScaffold(title = "ÁRVORE", onGoToSprint = onGoToSprint) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 48.dp),
        ) {
            SkillTree.groups.forEachIndexed { groupIndex, group ->

                // ── Marcador de grupo (como "ano" na timeline) ──────────────
                item(key = "hdr_$groupIndex") {
                    TimelineGroupHeader(
                        label = group.label,
                        topPad = if (groupIndex == 0) 20.dp else 28.dp,
                    )
                }

                // ── Nós de skill ────────────────────────────────────────────
                itemsIndexed(
                    items = group.skills,
                    key = { _, s -> s.tag },
                ) { skillIndex, skill ->
                    skillCounter++
                    val isFirst = skillCounter == 1
                    val isLast  = skillCounter == totalSkills
                    SkillTimelineRow(
                        skill      = skill,
                        attempts   = skillAttempts[skill.tag] ?: 0,
                        available  = skillAvailable[skill.tag] ?: 0,
                        accuracy   = skillAccuracy[skill.tag],
                        isActive   = skill.tag == currentSkill,
                        isFirst    = isFirst,
                        isLast     = isLast,
                        onClick    = { onSkillSelect(skill.tag); onGoToSprint() },
                    )
                }
            }
        }
    }
}

// ── Timeline: marcador de grupo ────────────────────────────────────────────────

@Composable
private fun TimelineGroupHeader(
    label: String,
    topPad: androidx.compose.ui.unit.Dp,
) {
    val ink  = MaterialTheme.colorScheme.onBackground
    val lineX = 20.dp

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = topPad, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Segmento da linha tracejada no espaço do grupo-header
        Canvas(
            modifier = Modifier
                .width(40.dp)
                .height(18.dp),
        ) {
            drawLine(
                color = ink.copy(alpha = 0.12f),
                start = Offset(lineX.toPx(), 0f),
                end   = Offset(lineX.toPx(), size.height),
                strokeWidth = 1.dp.toPx(),
                pathEffect  = PathEffect.dashPathEffect(floatArrayOf(2f, 5f), 0f),
            )
        }
        Text(
            text          = label,
            fontSize      = 9.sp,
            letterSpacing = 1.5.sp,
            color         = ink.copy(alpha = 0.28f),
            modifier      = Modifier.padding(start = 4.dp),
        )
    }
}

// ── Timeline: linha de skill ───────────────────────────────────────────────────

@Composable
private fun SkillTimelineRow(
    skill    : SkillEntry,
    attempts : Int,
    available: Int,
    accuracy : Float?,
    isActive : Boolean,
    isFirst  : Boolean,
    isLast   : Boolean,
    onClick  : () -> Unit,
) {
    val ink = MaterialTheme.colorScheme.onBackground

    val nodeColor: Color = when {
        isActive                                         -> ink.copy(alpha = 0.75f)
        accuracy != null && attempts > 0 && accuracy > 0.85f -> Color(0xFF388E3C)
        accuracy != null && attempts > 0 && accuracy < 0.60f -> Color(0xFFD32F2F)
        else                                             -> ink.copy(alpha = 0.22f)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // ── Coluna da timeline (linha + nó) ──────────────────────────────────
        val rowHeight = 52.dp
        Canvas(
            modifier = Modifier
                .width(40.dp)
                .height(rowHeight),
        ) {
            val lx   = 20.dp.toPx()
            val cy   = size.height / 2f
            val r    = if (isActive) 5.dp.toPx() else 3.5.dp.toPx()
            val dash = PathEffect.dashPathEffect(floatArrayOf(2f, 5f), 0f)
            val lc   = ink.copy(alpha = 0.14f)
            val lw   = 1.dp.toPx()

            // Linha acima do nó
            if (!isFirst) {
                drawLine(
                    color       = lc,
                    start       = Offset(lx, 0f),
                    end         = Offset(lx, cy - r - 2f),
                    strokeWidth = lw,
                    pathEffect  = dash,
                )
            }
            // Linha abaixo do nó
            if (!isLast) {
                drawLine(
                    color       = lc,
                    start       = Offset(lx, cy + r + 2f),
                    end         = Offset(lx, size.height),
                    strokeWidth = lw,
                    pathEffect  = dash,
                )
            }
            // Nó: preenchido se ativo, vazio se disponível
            if (isActive) {
                drawCircle(
                    color  = nodeColor,
                    radius = r,
                    center = Offset(lx, cy),
                )
            } else {
                drawCircle(
                    color  = nodeColor,
                    radius = r,
                    center = Offset(lx, cy),
                    style  = Stroke(width = 1.2.dp.toPx()),
                )
            }
        }

        // ── Conteúdo: nome + progresso ───────────────────────────────────────
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 20.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text       = skill.label.replace("\n", " "),
                fontSize   = if (isActive) 14.sp else 13.sp,
                fontWeight = if (isActive) FontWeight.Medium else FontWeight.Normal,
                color      = if (isActive) ink.copy(alpha = 0.80f) else ink.copy(alpha = 0.50f),
                maxLines   = 1,
            )
            Text(
                text     = progressLabel(attempts, available, accuracy),
                fontSize = 10.sp,
                color    = ink.copy(alpha = if (isActive) 0.42f else 0.22f),
            )
        }

        // ── Ponto colorido de accuracy à direita (like year on right) ────────
        if (accuracy != null && attempts > 0) {
            val accColor = when {
                accuracy > 0.85f -> Color(0xFF388E3C)
                accuracy < 0.60f -> Color(0xFFD32F2F)
                else             -> ink.copy(alpha = 0.20f)
            }
            Canvas(
                modifier = Modifier
                    .width(28.dp)
                    .height(52.dp),
            ) {
                drawCircle(
                    color  = accColor,
                    radius = 3.dp.toPx(),
                    center = Offset(size.width / 2f, size.height / 2f),
                )
            }
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun progressLabel(attempts: Int, available: Int, accuracy: Float?): String {
    val pool = if (available > 0) available.toString() else "—"
    val acc  = accuracy?.takeIf { attempts > 0 }?.let { " · ${(it * 100).toInt()}%" } ?: ""
    return "$attempts/$pool$acc"
}

// ── Dados da árvore ───────────────────────────────────────────────────────────

private data class SkillEntry(val tag: String, val label: String)
private data class SkillGroup(val label: String, val skills: List<SkillEntry>)

private object SkillTree {
    val groups = listOf(
        SkillGroup("FUNDAMENTOS", listOf(
            SkillEntry("soma_subtracao",              "Soma / Subtração"),
            SkillEntry("multiplicacao_divisao",       "Multiplicação / Divisão"),
            SkillEntry("fracoes_decimais",            "Frações / Decimais"),
            SkillEntry("porcentagem_razao",           "Porcentagem e Razão"),
            SkillEntry("potenciacao_radiciacao",      "Potência / Raiz"),
        )),
        SkillGroup("ÁLGEBRA", listOf(
            SkillEntry("equacoes_lineares",           "Equações Lineares"),
            SkillEntry("sistemas_equacoes",           "Sistemas de Equações"),
            SkillEntry("fatoracao_produtos_notaveis", "Fatoração / Produtos Notáveis"),
            SkillEntry("inequacoes",                  "Inequações"),
            SkillEntry("equacoes_quadraticas",        "Equações Quadráticas"),
        )),
        SkillGroup("FUNÇÕES", listOf(
            SkillEntry("funcao_afim",                 "Função Afim"),
            SkillEntry("funcao_quadratica",           "Função Quadrática"),
            SkillEntry("funcao_exponencial",          "Função Exponencial"),
            SkillEntry("funcao_logaritmica",          "Função Logarítmica"),
            SkillEntry("funcao_modular",              "Função Modular"),
        )),
        SkillGroup("GEOMETRIA", listOf(
            SkillEntry("geometria_plana",             "Geometria Plana"),
            SkillEntry("geometria_espacial",          "Geometria Espacial"),
            SkillEntry("geometria_analitica",         "Geometria Analítica"),
        )),
        SkillGroup("COMBINATÓRIA", listOf(
            SkillEntry("progressoes_pa_pg",           "Progressões PA / PG"),
            SkillEntry("combinatoria",                "Combinatória"),
            SkillEntry("probabilidade",               "Probabilidade"),
        )),
        SkillGroup("TRIGONOMETRIA", listOf(
            SkillEntry("trig_razoes",                 "Razões Trigonométricas"),
            SkillEntry("trig_seno_cosseno_tangente",  "Seno / Cosseno / Tg"),
            SkillEntry("trig_identidades",            "Identidades Trigonométricas"),
            SkillEntry("trig_equacoes",               "Equações Trigonométricas"),
        )),
        SkillGroup("CÁLCULO", listOf(
            SkillEntry("nocao_de_limite",             "Noção de Limite"),
            SkillEntry("continuidade",                "Continuidade"),
            SkillEntry("derivadas_basicas",           "Derivadas Básicas"),
            SkillEntry("derivadas_regra_cadeia",      "Regra da Cadeia"),
            SkillEntry("derivadas_produto_quociente", "Derivadas Produto / Quociente"),
            SkillEntry("aplicacoes_derivadas",        "Aplicações de Derivadas"),
            SkillEntry("integrais_indefinidas",       "Integrais Indefinidas"),
            SkillEntry("integrais_definidas",         "Integrais Definidas"),
            SkillEntry("aplicacoes_integrais",        "Aplicações de Integrais"),
        )),
    )
}
