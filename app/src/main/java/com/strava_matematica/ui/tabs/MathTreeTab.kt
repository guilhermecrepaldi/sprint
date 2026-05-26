package com.strava_matematica.ui.tabs

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Skill tree — visualização da árvore de matemática.
 * Toque num nó = seleciona skill para próxima sessão.
 * PlatformMap (canvas API) será integrado aqui em fase 2.
 * Por ora: grid de skills com status de domínio.
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
    val ink = MaterialTheme.colorScheme.onBackground

    SettingsTabScaffold(title = "ÁRVORE", onGoToSprint = onGoToSprint) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SkillTree.groups.forEach { group ->
                Text(
                    text = group.label,
                    fontSize = 9.sp,
                    letterSpacing = 1.5.sp,
                    color = ink.copy(alpha = 0.28f),
                    modifier = Modifier.padding(top = 8.dp, bottom = 2.dp),
                )
                group.skills.chunked(3).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        row.forEach { skill ->
                            val status = skillStatuses[skill.tag] ?: "novo"
                            val isActive = skill.tag == currentSkill
                            SkillNode(
                                label = skill.label,
                                status = status,
                                attempts = skillAttempts[skill.tag] ?: 0,
                                available = skillAvailable[skill.tag] ?: 0,
                                accuracy = skillAccuracy[skill.tag],
                                isActive = isActive,
                                onClick = {
                                    onSkillSelect(skill.tag)
                                    onGoToSprint()
                                },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        repeat(3 - row.size) {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                }
            }
        }
    }
}

@Composable
private fun SkillNode(
    label: String,
    status: String,
    attempts: Int,
    available: Int,
    accuracy: Float?,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ink = MaterialTheme.colorScheme.onBackground
    val fillAlpha = when (status) {
        "automatizado"      -> 0.85f
        "em_desenvolvimento" -> 0.45f
        "instavel"          -> 0.22f
        else                -> 0.08f
    }
    Column(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(
                    if (isActive) ink.copy(alpha = 0.70f)
                    else ink.copy(alpha = fillAlpha),
                    CircleShape,
                ),
        )
        Text(
            text = label,
            fontSize = 9.sp,
            color = if (isActive) ink.copy(alpha = 0.80f) else ink.copy(alpha = 0.45f),
            textAlign = TextAlign.Center,
            lineHeight = 11.sp,
            fontWeight = if (isActive) FontWeight.Medium else FontWeight.Normal,
        )
        Text(
            text = progressLabel(attempts, available, accuracy),
            fontSize = 8.sp,
            color = if (isActive) ink.copy(alpha = 0.52f) else ink.copy(alpha = 0.26f),
            textAlign = TextAlign.Center,
            lineHeight = 9.sp,
        )
    }
}

private fun progressLabel(attempts: Int, available: Int, accuracy: Float?): String {
    val pool = if (available > 0) available.toString() else "0"
    val acc = accuracy?.takeIf { attempts > 0 }?.let { " · ${(it * 100).toInt()}%" } ?: ""
    return "$attempts/$pool$acc"
}

// ── Skill tree data ────────────────────────────────────────────────────────────

private data class SkillEntry(val tag: String, val label: String)
private data class SkillGroup(val label: String, val skills: List<SkillEntry>)

private object SkillTree {
    val groups = listOf(
        SkillGroup("FUNDAMENTOS", listOf(
            SkillEntry("soma_subtracao",       "Soma /\nSubtração"),
            SkillEntry("multiplicacao_divisao", "Multi /\nDivisão"),
            SkillEntry("fracoes_decimais",     "Frações /\nDecimais"),
            SkillEntry("porcentagem_razao",    "% e\nRazão"),
            SkillEntry("potenciacao_radiciacao","Potência /\nRaiz"),
        )),
        SkillGroup("ÁLGEBRA", listOf(
            SkillEntry("equacoes_lineares",    "Eq.\nLinear"),
            SkillEntry("sistemas_equacoes",    "Sistemas"),
            SkillEntry("fatoracao_produtos_notaveis", "Fatoração /\nProdutos"),
            SkillEntry("inequacoes",           "Inequação"),
            SkillEntry("equacoes_quadraticas", "Eq.\nQuadrática"),
        )),
        SkillGroup("FUNÇÕES", listOf(
            SkillEntry("funcao_afim",          "Função\nAfim"),
            SkillEntry("funcao_quadratica",    "Função\nQuadrática"),
            SkillEntry("funcao_exponencial",   "Função\nExponencial"),
            SkillEntry("funcao_logaritmica",   "Função\nLog"),
            SkillEntry("funcao_modular",       "Função\nModular"),
        )),
        SkillGroup("GEOMETRIA", listOf(
            SkillEntry("geometria_plana",      "Geo.\nPlana"),
            SkillEntry("geometria_espacial",   "Geo.\nEspacial"),
            SkillEntry("geometria_analitica",  "Geo.\nAnalítica"),
        )),
        SkillGroup("COMBINATÓRIA", listOf(
            SkillEntry("progressoes_pa_pg",    "PA /\nPG"),
            SkillEntry("combinatoria",         "Combinatória"),
            SkillEntry("probabilidade",        "Probabilidade"),
        )),
        SkillGroup("TRIGONOMETRIA", listOf(
            SkillEntry("trig_razoes",          "Razões\nTrig."),
            SkillEntry("trig_seno_cosseno_tangente", "Seno /\nCosseno"),
            SkillEntry("trig_identidades",     "Identidades"),
            SkillEntry("trig_equacoes",        "Eq.\nTrig."),
        )),
        SkillGroup("CÁLCULO", listOf(
            SkillEntry("nocao_de_limite",      "Noção de\nLimite"),
            SkillEntry("continuidade",         "Continuidade"),
            SkillEntry("derivadas_basicas",    "Derivadas\nBásicas"),
            SkillEntry("derivadas_regra_cadeia", "Regra da\nCadeia"),
            SkillEntry("derivadas_produto_quociente", "Produto /\nQuociente"),
            SkillEntry("aplicacoes_derivadas", "Aplic.\nDerivadas"),
            SkillEntry("integrais_indefinidas","Integrais\nIndef."),
            SkillEntry("integrais_definidas",  "Integrais\nDef."),
            SkillEntry("aplicacoes_integrais", "Aplic.\nIntegrais"),
        )),
    )
}
