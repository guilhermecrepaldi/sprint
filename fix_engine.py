import os

engine_path = r"D:\LOVE CLASS\app\src\main\java\com\strava_matematica\domain\procedural\ProceduralEngine.kt"

with open(engine_path, "r", encoding="utf-8") as f:
    content = f.read()

# We need to replace the entire `fun generate(...)` with our new version.
# First, let's extract everything from `fun generate` down to `private fun generatePlaceholder`
import re

# Split before `fun generate`
parts = content.split("fun generate(skillTag: String, mmr: Int, config: SessionConfig = SessionConfig()): ProceduralExercise {")
if len(parts) != 2:
    print("Failed to find generate function")
    exit(1)

head = parts[0]
tail_parts = parts[1].split("private fun generatePlaceholder")
if len(tail_parts) != 2:
    print("Failed to find generatePlaceholder")
    exit(1)

# Now we construct the new body
new_body = """
    private val statementHistory = java.util.LinkedList<String>()
    private const val HISTORY_SIZE = 30

    fun generate(skillTag: String, mmr: Int, config: SessionConfig = SessionConfig()): ProceduralExercise {
        var exercise: ProceduralExercise
        var attempts = 0
        do {
            exercise = generateInternal(skillTag, mmr, config)
            attempts++
        } while (statementHistory.contains(exercise.statement) && attempts < 10)
        
        statementHistory.addLast(exercise.statement)
        if (statementHistory.size > HISTORY_SIZE) {
            statementHistory.removeFirst()
        }
        return exercise
    }

    private fun generateInternal(skillTag: String, mmr: Int, config: SessionConfig): ProceduralExercise {
        // Grandezas e Lógica
        if (skillTag in listOf("rule_of_3", "percentage", "interest")) return ProceduralProportions.generate(skillTag, mmr)
        if (skillTag in listOf("sets_venn", "propositional")) return ProceduralLogic.generate(skillTag, mmr)
        
        // Aritmética Avançada
        if (skillTag in listOf("mmc_mdc", "divisibility_primes", "dizimas", "scientific_notation")) return ProceduralArithmetic.generate(skillTag, mmr)
        
        // Funções Reais
        if (skillTag in listOf("function_eval", "function_domain", "function_composition", "function_exp_log")) return ProceduralFunctions.generate(skillTag, mmr)
        
        return when (skillTag) {
            "soma_subtracao" -> generateSomaSubtracao(mmr, config.digitsCount, config.valuesCount, config.numberSet)
            "multiplicacao_divisao" -> generateMultiplicacaoDivisao(mmr, config.digitsCount)

            // Algebra
            "equacoes_quadraticas", "equacao_2_grau",
            "fatoracao_produtos_notaveis", "polinomios", "alg_elem_poly",
            "fracoes_decimais", "porcentagem_razao", "potenciacao_radiciacao",
            "equacoes_lineares", "sistemas_equacoes", "inequacoes", "funcao_afim",
            "funcao_quadratica", "funcao_exponencial", "funcao_logaritmica", "funcao_modular" ->
                ProceduralAlgebra.generate(skillTag, mmr)

            // Geometry
            "geo_euc_plan", "geo_euc_spc", "geo_euc_non",
            "geo_ana_cart", "geo_ana_eq", "geo_ana_con",
            "geo_diff_crv", "geo_diff_man", "geo_diff_riem",
            "geo_top_spc", "geo_top_cont", "geo_top_comp", "geo_top_alg",
            "geometria_plana", "geometria_espacial", "geometria_analitica",
            "progressoes_pa_pg", "trig_razoes", "trig_seno_cosseno_tangente", "trig_identidades", "trig_equacoes" ->
                ProceduralGeometry.generate(skillTag, mmr)

            // Calculus
            "calc_pre_func", "calc_pre_elem", "calc_pre_seq",
            "calc_dif_lim", "calc_dif_der", "calc_dif_int", "calc_dif_mul",
            "calc_eq_ode", "calc_eq_pde", "calc_eq_trans",
            "calc_real_lim", "calc_real_met", "calc_real_comp",
            "nocao_de_limite", "continuidade", "derivadas_basicas",
            "derivadas_regra_cadeia", "derivadas_produto_quociente",
            "aplicacoes_derivadas", "integrais_indefinidas",
            "integrais_definidas", "aplicacoes_integrais" ->
                ProceduralCalculus.generate(skillTag, mmr)

            // Statistics & Combinatorics
            "stat_comb_fund", "stat_comb_perm", "stat_comb_bin",
            "stat_prob_cond", "stat_prob_var", "stat_prob_dist", "stat_prob",
            "combinatoria", "probabilidade" ->
                ProceduralStats.generate(skillTag, mmr)

            // Linear Algebra
            "soma_produto_matrizes", "determinantes", "operacoes_vetoriais" ->
                ProceduralLinearAlgebra.generate(skillTag, mmr)

            else -> generatePlaceholder(skillTag, mmr)
        }
    }

    private fun generatePlaceholder"""

final_content = head + new_body.lstrip('\n') + tail_parts[1]

with open(engine_path, "w", encoding="utf-8") as f:
    f.write(final_content)

print("ProceduralEngine successfully updated with history and routers!")
