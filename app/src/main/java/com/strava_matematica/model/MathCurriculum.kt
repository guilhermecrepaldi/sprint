package com.strava_matematica.model

data class CurriculumNode(
    val id: String,
    val name: String,
    val children: List<CurriculumNode> = emptyList(),
    val proceduralTag: String? = null
)

object MathCurriculum {
    val tree = listOf(
        CurriculumNode(
            id = "fnd",
            name = "1. Domínio: Fundamentos e Lógica (A Raiz)",
            children = listOf(
                CurriculumNode(
                    id = "fnd_log",
                    name = "Lógica Matemática",
                    children = listOf(
                        CurriculumNode(id = "fnd_log_prop", name = "Lógica Proposicional (Conectivos, Tabelas-Verdade)"),
                        CurriculumNode(id = "fnd_log_pred", name = "Lógica de Predicados (Quantificadores ∀, ∃)"),
                        CurriculumNode(id = "fnd_log_fo", name = "Lógica de Primeira Ordem e Sistemas Formais")
                    )
                ),
                CurriculumNode(
                    id = "fnd_set",
                    name = "Teoria dos Conjuntos",
                    children = listOf(
                        CurriculumNode(id = "fnd_set_op", name = "Operações Básicas (União, Interseção, Complementar)"),
                        CurriculumNode(id = "fnd_set_card", name = "Cardinalidade (Conjuntos Finitos e Infinitos)"),
                        CurriculumNode(id = "fnd_set_zfc", name = "Axiomática de Zermelo-Fraenkel (ZFC) e Paradoxo de Russell")
                    )
                ),
                CurriculumNode(
                    id = "fnd_num",
                    name = "Sistemas Numéricos",
                    children = listOf(
                        CurriculumNode(id = "fnd_num_nat", name = "Números Naturais (Axiomas de Peano e Indução)"),
                        CurriculumNode(id = "fnd_num_int", name = "Números Inteiros e Racionais", proceduralTag = "fracoes_decimais"),
                        CurriculumNode(id = "fnd_num_real", name = "Números Reais (Cortes de Dedekind e Supremia)"),
                        CurriculumNode(id = "fnd_num_comp", name = "Números Complexos (Forma Algébrica e Polar)")
                    )
                ),
                CurriculumNode(
                    id = "fnd_graph",
                    name = "Teoria dos Grafos",
                    children = listOf(
                        CurriculumNode(id = "fnd_graph_vert", name = "Vértices, Arestas e Conectividade"),
                        CurriculumNode(id = "fnd_graph_path", name = "Caminhos, Ciclos e Árvores"),
                        CurriculumNode(id = "fnd_graph_col", name = "Coloração de Grafos e Matrizes de Adjacência")
                    )
                )
            )
        ),
        CurriculumNode(
            id = "alg",
            name = "2. Domínio: Álgebra (As Estruturas)",
            children = listOf(
                CurriculumNode(
                    id = "alg_elem",
                    name = "Álgebra Elementar",
                    children = listOf(
                        CurriculumNode(id = "alg_elem_poly", name = "Polinômios, Fatoração e Expressões Algébricas", proceduralTag = "fatoracao_produtos_notaveis"),
                        CurriculumNode(id = "alg_elem_eq", name = "Equações e Inequações (Linear, Quadrática)", proceduralTag = "equacoes_lineares"),
                        CurriculumNode(id = "alg_elem_sys", name = "Sistemas de Equações Lineares Básicos", proceduralTag = "sistemas_equacoes")
                    )
                ),
                CurriculumNode(
                    id = "alg_lin",
                    name = "Álgebra Linear",
                    children = listOf(
                        CurriculumNode(id = "alg_lin_vec", name = "Vetores, Matrizes e Operações Matriciais", proceduralTag = "soma_produto_matrizes"),
                        CurriculumNode(id = "alg_lin_spc", name = "Espaços Vetoriais e Subespaços"),
                        CurriculumNode(id = "alg_lin_trans", name = "Transformações Lineares e Mudança de Base"),
                        CurriculumNode(id = "alg_lin_eig", name = "Autovalores, Autovetores e Decomposição (SVD, PCA)")
                    )
                ),
                CurriculumNode(
                    id = "alg_abs",
                    name = "Álgebra Abstrata / Moderna",
                    children = listOf(
                        CurriculumNode(id = "alg_abs_grp", name = "Teoria de Grupos (Simetrias, Permutações)"),
                        CurriculumNode(id = "alg_abs_ring", name = "Anéis e Corpos (Fields)"),
                        CurriculumNode(id = "alg_abs_gal", name = "Teoria de Galois e Criptografia")
                    )
                ),
                CurriculumNode(
                    id = "alg_num",
                    name = "Teoria dos Números",
                    children = listOf(
                        CurriculumNode(id = "alg_num_div", name = "Divisibilidade e Algoritmo de Euclides"),
                        CurriculumNode(id = "alg_num_prim", name = "Números Primos e Teorema Fundamental da Aritmética"),
                        CurriculumNode(id = "alg_num_mod", name = "Aritmética Modular e Congruências"),
                        CurriculumNode(id = "alg_num_dio", name = "Equações Diofantinas")
                    )
                )
            )
        ),
        CurriculumNode(
            id = "geo",
            name = "3. Domínio: Geometria e Topologia (O Espaço)",
            children = listOf(
                CurriculumNode(
                    id = "geo_euc",
                    name = "Geometria Euclidiana",
                    children = listOf(
                        CurriculumNode(id = "geo_euc_plan", name = "Geometria Plana (Triângulos, Polígonos, Áreas)", proceduralTag = "geometria_plana"),
                        CurriculumNode(id = "geo_euc_spc", name = "Geometria Espacial (Sólidos, Volumes, Projeções)", proceduralTag = "geometria_espacial"),
                        CurriculumNode(id = "geo_euc_non", name = "Axiomas de Euclides e Geometrias Não-Euclidianas")
                    )
                ),
                CurriculumNode(
                    id = "geo_ana",
                    name = "Geometria Analítica",
                    children = listOf(
                        CurriculumNode(id = "geo_ana_cart", name = "Sistema de Coordenadas Cartesianas e Vetores"),
                        CurriculumNode(id = "geo_ana_eq", name = "Equações da Reta e do Plano"),
                        CurriculumNode(id = "geo_ana_con", name = "Seções Cônicas (Elipse, Parábola, Hipérbole)")
                    )
                ),
                CurriculumNode(
                    id = "geo_diff",
                    name = "Geometria Diferencial",
                    children = listOf(
                        CurriculumNode(id = "geo_diff_crv", name = "Curvas e Superfícies no Espaço"),
                        CurriculumNode(id = "geo_diff_man", name = "Variedades Diferenciáveis e Tensores"),
                        CurriculumNode(id = "geo_diff_riem", name = "Curvatura e Geometria Riemanniana")
                    )
                ),
                CurriculumNode(
                    id = "geo_top",
                    name = "Topologia",
                    children = listOf(
                        CurriculumNode(id = "geo_top_spc", name = "Espaços Tópicos e Vizinhança"),
                        CurriculumNode(id = "geo_top_cont", name = "Continuidade e Homeomorfismos"),
                        CurriculumNode(id = "geo_top_comp", name = "Compacidade e Conexidade"),
                        CurriculumNode(id = "geo_top_alg", name = "Topologia Algébrica (Invariantes)")
                    )
                )
            )
        ),
        CurriculumNode(
            id = "calc",
            name = "4. Domínio: Análise e Cálculo (A Mudança)",
            children = listOf(
                CurriculumNode(
                    id = "calc_pre",
                    name = "Pré-Cálculo",
                    children = listOf(
                        CurriculumNode(id = "calc_pre_func", name = "Funções Reais (Injetora, Sobrejetora, Bijetora)", proceduralTag = "funcao_quadratica"),
                        CurriculumNode(id = "calc_pre_elem", name = "Funções Elementares (Exponencial, Logarítmica, Trigonométrica)", proceduralTag = "funcao_logaritmica"),
                        CurriculumNode(id = "calc_pre_seq", name = "Sequências e Séries Numéricas (Convergência)")
                    )
                ),
                CurriculumNode(
                    id = "calc_dif",
                    name = "Cálculo Diferencial e Integral",
                    children = listOf(
                        CurriculumNode(id = "calc_dif_lim", name = "Limites e Continuidade", proceduralTag = "calc_dif_lim"),
                        CurriculumNode(id = "calc_dif_der", name = "Derivadas, Regra da Cadeia e Otimização", proceduralTag = "derivadas_regra_cadeia"),
                        CurriculumNode(id = "calc_dif_int", name = "Integrais (Indefinidas, Definidas e TFC)", proceduralTag = "calc_dif_int"),
                        CurriculumNode(id = "calc_dif_mul", name = "Cálculo Multivariável (Gradiente, Rotacional, Integrais Múltiplas)")
                    )
                ),
                CurriculumNode(
                    id = "calc_eq",
                    name = "Equações Diferenciais",
                    children = listOf(
                        CurriculumNode(id = "calc_eq_ode", name = "EDOs (Equações Diferenciais Ordinárias) de 1ª e 2ª Ordem"),
                        CurriculumNode(id = "calc_eq_pde", name = "EDPs (Equações Diferenciais Parciais)"),
                        CurriculumNode(id = "calc_eq_trans", name = "Transformadas de Laplace e Fourier")
                    )
                ),
                CurriculumNode(
                    id = "calc_real",
                    name = "Análise Real e Complexa",
                    children = listOf(
                        CurriculumNode(id = "calc_real_lim", name = "Formalização Rigorosa de Limites (Épsilon-Delta)"),
                        CurriculumNode(id = "calc_real_met", name = "Espaços Métricos e Sequências de Cauchy"),
                        CurriculumNode(id = "calc_real_comp", name = "Funções de Variável Complexa (Holomorfas e Integrais)")
                    )
                )
            )
        ),
        CurriculumNode(
            id = "stat",
            name = "5. Domínio: Probabilidade e Estatística (A Incerteza)",
            children = listOf(
                CurriculumNode(
                    id = "stat_comb",
                    name = "Análise Combinatória",
                    children = listOf(
                        CurriculumNode(id = "stat_comb_fund", name = "Princípio Fundamental da Contagem"),
                        CurriculumNode(id = "stat_comb_perm", name = "Permutações, Arranjos e Combinações", proceduralTag = "stat_comb_perm"),
                        CurriculumNode(id = "stat_comb_bin", name = "Binômio de Newton e Coeficientes Binomiais")
                    )
                ),
                CurriculumNode(
                    id = "stat_prob",
                    name = "Teoria da Probabilidade",
                    children = listOf(
                        CurriculumNode(id = "stat_prob_spc", name = "Espaço Amostral, Eventos e Axiomas de Kolmogorov"),
                        CurriculumNode(id = "stat_prob_cond", name = "Probabilidade Condicional e Teorema de Bayes"),
                        CurriculumNode(id = "stat_prob_var", name = "Variáveis Aleatórias e Expectância (Média, Variância)")
                    )
                ),
                CurriculumNode(
                    id = "stat_dist",
                    name = "Distribuições de Probabilidade",
                    children = listOf(
                        CurriculumNode(id = "stat_dist_disc", name = "Distribuições Discretas (Binomial, Poisson)"),
                        CurriculumNode(id = "stat_dist_cont", name = "Distribuições Contínuas (Normal/Gaussiana, t-Student, Qui-Quadrado)"),
                        CurriculumNode(id = "stat_dist_clt", name = "Teorema Central do Limite")
                    )
                ),
                CurriculumNode(
                    id = "stat_inf",
                    name = "Estatística Inferencial",
                    children = listOf(
                        CurriculumNode(id = "stat_inf_samp", name = "Amostragem e Estimação de Parâmetros"),
                        CurriculumNode(id = "stat_inf_conf", name = "Intervalos de Confiança"),
                        CurriculumNode(id = "stat_inf_test", name = "Testes de Hipóteses (p-valor, Erros Tipo I e II)"),
                        CurriculumNode(id = "stat_inf_reg", name = "Regressão Linear, Correlação e Análise de Dados")
                    )
                )
            )
        ),
        CurriculumNode(
            id = "comp",
            name = "6. Domínio: Matemática Computacional (A Execução)",
            children = listOf(
                CurriculumNode(
                    id = "comp_num",
                    name = "Análise Numérica",
                    children = listOf(
                        CurriculumNode(id = "comp_num_err", name = "Algoritmos de Aproximação e Propagação de Erros"),
                        CurriculumNode(id = "comp_num_int", name = "Interpolação e Ajuste de Curvas"),
                        CurriculumNode(id = "comp_num_diff", name = "Integração e Diferenciação Numérica"),
                        CurriculumNode(id = "comp_num_sys", name = "Resolução Numérica de Sistemas Lineares e EDOs")
                    )
                ),
                CurriculumNode(
                    id = "comp_opt",
                    name = "Otimização e Pesquisa Operacional",
                    children = listOf(
                        CurriculumNode(id = "comp_opt_lin", name = "Programação Linear (Algoritmo Simplex)"),
                        CurriculumNode(id = "comp_opt_non", name = "Otimização Não-Linear e Convexa (Descida de Gradiente)"),
                        CurriculumNode(id = "comp_opt_int", name = "Programação Inteira e Teoria dos Jogos")
                    )
                ),
                CurriculumNode(
                    id = "comp_dyn",
                    name = "Sistemas Dinâmicos e Teoria do Caos",
                    children = listOf(
                        CurriculumNode(id = "comp_dyn_sys", name = "Sistemas Lineares e Não-Lineares no Tempo"),
                        CurriculumNode(id = "comp_dyn_attr", name = "Atratores, Bifurcações e Estabilidade de Lyapunov"),
                        CurriculumNode(id = "comp_dyn_chaos", name = "Teoria do Caos (Sensibilidade às Condições Iniciais)")
                    )
                ),
                CurriculumNode(
                    id = "comp_th",
                    name = "Teoria da Computação",
                    children = listOf(
                        CurriculumNode(id = "comp_th_aut", name = "Autômatos Finitos e Linguagens Formais"),
                        CurriculumNode(id = "comp_th_comp", name = "Computabilidade (Máquinas de Turing, Problema da Parada)"),
                        CurriculumNode(id = "comp_th_comp_ana", name = "Análise de Complexidade de Algoritmos (P vs NP)")
                    )
                )
            )
        )
    )
}
