package com.sprint.domain

import com.sprint.domain.procedural.ProceduralEngine
import com.sprint.model.SessionConfig
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNotEquals
import org.junit.Test

class ProceduralEngineAuditTest {

    private val allSkills = listOf(
        // Base Antiga
        "soma_subtracao", "multiplicacao_divisao", "fracoes_decimais",
        "porcentagem_razao", "potenciacao_radiciacao",
        "equacoes_lineares", "sistemas_equacoes", "fatoracao_produtos_notaveis",
        "inequacoes", "equacoes_quadraticas",
        "funcao_afim", "funcao_quadratica", "funcao_exponencial",
        "funcao_logaritmica", "funcao_modular",
        "geometria_plana", "geometria_espacial", "geometria_analitica",
        "progressoes_pa_pg", "combinatoria", "probabilidade",
        "trig_razoes", "trig_seno_cosseno_tangente", "trig_identidades", "trig_equacoes",
        "nocao_de_limite", "continuidade", "derivadas_basicas",
        "derivadas_regra_cadeia", "derivadas_produto_quociente",
        "aplicacoes_derivadas", "integrais_indefinidas",
        "integrais_definidas", "aplicacoes_integrais",
        // Novas 100% Cobertura
        "fnd_log_prop", "fnd_set_op", "fnd_num_int", "fnd_graph_dir",
        "alg_lin_vec", "alg_abs_grp", "alg_elem_poly", "alg_num_mod",
        "geo_euc_plan", "geo_diff_crv", "geo_top_spc",
        "calc_pre_func", "calc_dif_lim", "calc_eq_ode", "calc_real_lim",
        "stat_comb_perm", "stat_dist_disc", "stat_inf_samp",
        "comp_num_err", "comp_opt_lin", "comp_dyn_sys", "comp_th_aut",
        // Extra tags
        "alg_elem_eq", "alg_elem_sys", "comp_dyn", "comp_num", "comp_opt", "comp_th",
        "stat_dist_clt", "stat_dist_cont", "stat_inf_conf", "stat_inf_reg", "stat_inf_test",
        "stat_prob_spc"
    )

    @Test
    fun testAllSkillsHaveUniqueProceduralGenerators() {
        val missingSkills = mutableListOf<String>()

        for (skill in allSkills) {
            // Usa as arvores (n), casas decimais (n), quantidade de termos (n)
            val config = SessionConfig(
                digitsCount = 2,
                valuesCount = 2,
                numberSet = "inteiros"
            )

            try {
                val exercise = ProceduralEngine.generate(skill, mmr = 500, config = config)
                
                // Se a skill nao for soma_subtracao nativamente, ela NUNCA deve retornar o template de soma_subtracao
                if (skill != "soma_subtracao") {
                    if (exercise.primarySkill == "soma_subtracao") {
                        missingSkills.add(skill)
                    }
                }
                // O fallback nunca deve ser retornado (template = placeholder)
                if (exercise.templateId == "placeholder") {
                    missingSkills.add("$skill (Placeholder)")
                }
            } catch (e: Exception) {
                missingSkills.add("$skill (Erro: ${e.message})")
            }
        }

        assertTrue(
            "Faltam motores procedurais ou cairam no fallback para as seguintes habilidades: \n${missingSkills.joinToString("\n")}",
            missingSkills.isEmpty()
        )
    }

    @Test
    fun testCombinatorialCombinations() {
        // Teste de probabilidade nxnxnxn
        val configs = listOf(
            SessionConfig(digitsCount = 1, valuesCount = 2, numberSet = "inteiros"),
            SessionConfig(digitsCount = 2, valuesCount = 3, numberSet = "decimais"),
            SessionConfig(digitsCount = 1, valuesCount = 4, numberSet = "racionais")
        )

        for (skill in allSkills) {
            for (config in configs) {
                val exercise = ProceduralEngine.generate(skill, mmr = 600, config = config)
                assertTrue(exercise.statement.isNotBlank())
            }
        }
    }
}
