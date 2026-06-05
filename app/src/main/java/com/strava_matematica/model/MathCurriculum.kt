package com.strava_matematica.model

data class CurriculumNode(
    val id: String,
    val name: String,
    val children: List<CurriculumNode> = emptyList(),
    val proceduralTag: String? = null,
    val youtubeChannel: String? = null,
    val youtubeUrl: String? = null
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
                        CurriculumNode(id = "fnd_log_prop", name = "Lógica Proposicional (Conectivos, Tabelas-Verdade)", youtubeChannel = "UNIVESP", youtubeUrl = "https://www.youtube.com/playlist?list=PLxI8Can9yAHf78v4scZ8Yvs_MvOonpU3v"),
                        CurriculumNode(id = "fnd_log_pred", name = "Lógica de Predicados (Quantificadores ∀, ∃)", youtubeChannel = "UNIVESP", youtubeUrl = "https://www.youtube.com/watch?v=Ff6_YhXw_xM"),
                        CurriculumNode(id = "fnd_log_fo", name = "Lógica de Primeira Ordem e Sistemas Formais", youtubeChannel = "Instituto de Computação UFF", youtubeUrl = "https://www.youtube.com/playlist?list=PLg478N778FmY7O91oI384G7rJ6D77P1kP")
                    )
                ),
                CurriculumNode(
                    id = "fnd_set",
                    name = "Teoria dos Conjuntos",
                    children = listOf(
                        CurriculumNode(id = "fnd_set_op", name = "Operações Básicas (União, Interseção, Complementar)", youtubeChannel = "Ferretto Matemática", youtubeUrl = "https://www.youtube.com/watch?v=8mB7N1L_L4M"),
                        CurriculumNode(id = "fnd_set_card", name = "Cardinalidade (Conjuntos Finitos e Infinitos)", youtubeChannel = "Universo Narrado", youtubeUrl = "https://www.youtube.com/watch?v=A_D-mFWhh0M"),
                        CurriculumNode(id = "fnd_set_zfc", name = "Axiomática de Zermelo-Fraenkel (ZFC) e Paradoxo de Russell", youtubeChannel = "Ciência Todo Dia", youtubeUrl = "https://www.youtube.com/watch?v=uD9B_p-k6eI")
                    )
                ),
                CurriculumNode(
                    id = "fnd_num",
                    name = "Sistemas Numéricos",
                    children = listOf(
                        CurriculumNode(id = "fnd_num_nat", name = "Números Naturais (Axiomas de Peano e Indução)", youtubeChannel = "Canal do Ledo", youtubeUrl = "https://www.youtube.com/watch?v=hGqit_5C34A"),
                        CurriculumNode(id = "fnd_num_int", name = "Números Inteiros e Racionais", youtubeChannel = "Canal do Ledo", youtubeUrl = "https://www.youtube.com/watch?v=1F3gGvSjN5Q", proceduralTag = "fracoes_decimais"),
                        CurriculumNode(id = "fnd_num_real", name = "Números Reais (Cortes de Dedekind e Supremia)", youtubeChannel = "IMPA", youtubeUrl = "https://www.youtube.com/playlist?list=PLo4jXE851knR-H_A6G38H_Xm3ePZzK5R9"),
                        CurriculumNode(id = "fnd_num_comp", name = "Números Complexos (Forma Algébrica e Polar)", youtubeChannel = "Universo Narrado", youtubeUrl = "https://www.youtube.com/playlist?list=PLz7s6_nZ8fFp6q7Z9C3Mms0zG67-iRWhV")
                    )
                ),
                CurriculumNode(
                    id = "fnd_graph",
                    name = "Teoria dos Grafos",
                    children = listOf(
                        CurriculumNode(id = "fnd_graph_vert", name = "Vértices, Arestas e Conectividade", youtubeChannel = "Curso em Vídeo", youtubeUrl = "https://www.youtube.com/playlist?list=PLHz_AreHm4dm7A_iIL_19fCcVdh9vUWhO"),
                        CurriculumNode(id = "fnd_graph_path", name = "Caminhos, Ciclos e Árvores", youtubeChannel = "UNIVESP", youtubeUrl = "https://www.youtube.com/watch?v=l_Z486A7jK4"),
                        CurriculumNode(id = "fnd_graph_col", name = "Coloração de Grafos e Matrizes de Adjacência", youtubeChannel = "Professor Aquino - UFAL", youtubeUrl = "https://www.youtube.com/watch?v=H7mJ93vC81s")
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
                        CurriculumNode(id = "alg_elem_poly", name = "Polinômios, Fatoração e Expressões Algébricas", youtubeChannel = "Ferretto Matemática", youtubeUrl = "https://www.youtube.com/playlist?list=PLxI8Can9yAHfS7O70vN0HhO2S6rG7mN6v", proceduralTag = "fatoracao_produtos_notaveis"),
                        CurriculumNode(id = "alg_elem_eq", name = "Equações e Inequações (Linear, Quadrática)", youtubeChannel = "Me Salva!", youtubeUrl = "https://www.youtube.com/playlist?list=PLf1lowbdbFIBw8e3M_FwG68qH3wS9I4P9", proceduralTag = "equacoes_lineares"),
                        CurriculumNode(id = "alg_elem_sys", name = "Sistemas de Equações Lineares Básicos", youtubeChannel = "Toda a Matemática", youtubeUrl = "https://www.youtube.com/watch?v=CqW4bK4YI9k", proceduralTag = "sistemas_equacoes")
                    )
                ),
                CurriculumNode(
                    id = "alg_lin",
                    name = "Álgebra Linear",
                    children = listOf(
                        CurriculumNode(id = "alg_lin_vec", name = "Vetores, Matrizes e Operações Matriciais", youtubeChannel = "3Blue1Brown em Português", youtubeUrl = "https://www.youtube.com/playlist?list=PLvG4g6XFz_w3BofwD_U7v4Yv5H3UbycKz", proceduralTag = "soma_produto_matrizes"),
                        CurriculumNode(id = "alg_lin_spc", name = "Espaços Vetoriais e Subespaços", youtubeChannel = "UNIVESP", youtubeUrl = "https://www.youtube.com/playlist?list=PLxI8Can9yAHe_j0b69YwU7Sg83w0D3iSm"),
                        CurriculumNode(id = "alg_lin_trans", name = "Transformações Lineares e Mudança de Base", youtubeChannel = "IMPA", youtubeUrl = "https://www.youtube.com/watch?v=C95e0Z4r2b8"),
                        CurriculumNode(id = "alg_lin_eig", name = "Autovalores, Autovetores e Decomposição (SVD, PCA)", youtubeChannel = "3Blue1Brown em Português", youtubeUrl = "https://www.youtube.com/watch?v=8F8bV6Ait_Y")
                    )
                ),
                CurriculumNode(
                    id = "alg_abs",
                    name = "Álgebra Abstrata / Moderna",
                    children = listOf(
                        CurriculumNode(id = "alg_abs_grp", name = "Teoria de Grupos (Simetrias, Permutações)", youtubeChannel = "Canal do Ledo", youtubeUrl = "https://www.youtube.com/playlist?list=PLv_6G17fT6g9R6Lp7m16Z-0xU84jZ891f"),
                        CurriculumNode(id = "alg_abs_ring", name = "Anéis e Corpos (Fields)", youtubeChannel = "Canal do Ledo", youtubeUrl = "https://www.youtube.com/playlist?list=PLv_6G17fT6g_X98aC7M-pA11zQZpxO4X4"),
                        CurriculumNode(id = "alg_abs_gal", name = "Teoria de Galois e Criptografia", youtubeChannel = "IMPA", youtubeUrl = "https://www.youtube.com/watch?v=F_f75W_WlP0")
                    )
                ),
                CurriculumNode(
                    id = "alg_num",
                    name = "Teoria dos Números",
                    children = listOf(
                        CurriculumNode(id = "alg_num_div", name = "Divisibilidade e Algoritmo de Euclides", youtubeChannel = "Canal do Ledo", youtubeUrl = "https://www.youtube.com/playlist?list=PLv_6G17fT6g92rS0C76xK7n8O2w9_5XmS"),
                        CurriculumNode(id = "alg_num_prim", name = "Números Primos e Teorema Fundamental da Aritmética", youtubeChannel = "Universo Narrado", youtubeUrl = "https://www.youtube.com/watch?v=e_wK7Vq687k"),
                        CurriculumNode(id = "alg_num_mod", name = "Aritmética Modular e Congruências", youtubeChannel = "PROFMAT", youtubeUrl = "https://www.youtube.com/playlist?list=PLo4jXE851knS7_pU4m_qM4Yv8g-G6w5gC"),
                        CurriculumNode(id = "alg_num_dio", name = "Equações Diofantinas", youtubeChannel = "IMPA", youtubeUrl = "https://www.youtube.com/watch?v=B91-RbywN5A")
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
                        CurriculumNode(id = "geo_euc_plan", name = "Geometria Plana (Triângulos, Polígonos, Áreas)", youtubeChannel = "Ferretto Matemática", youtubeUrl = "https://www.youtube.com/playlist?list=PLxI8Can9yAHecm6E_eQpGjXyL3A_Cq-M5", proceduralTag = "geometria_plana"),
                        CurriculumNode(id = "geo_euc_spc", name = "Geometria Espacial (Sólidos, Volumes, Projeções)", youtubeChannel = "Ferretto Matemática", youtubeUrl = "https://www.youtube.com/playlist?list=PLxI8Can9yAHeGfG88y6GvD6fJ6C3gWj8W", proceduralTag = "geometria_espacial"),
                        CurriculumNode(id = "geo_euc_non", name = "Axiomas de Euclides e Geometrias Não-Euclidianas", youtubeChannel = "Universo Narrado", youtubeUrl = "https://www.youtube.com/watch?v=fXvU7qI72-4")
                    )
                ),
                CurriculumNode(
                    id = "geo_ana",
                    name = "Geometria Analítica",
                    children = listOf(
                        CurriculumNode(id = "geo_ana_cart", name = "Sistema de Coordenadas Cartesianas e Vetores", youtubeChannel = "Professor Possani (USP)", youtubeUrl = "https://www.youtube.com/playlist?list=PL8XwR9f5InVfUq_A8uXhT5p8K_fXy7X_N"),
                        CurriculumNode(id = "geo_ana_eq", name = "Equações da Reta e do Plano", youtubeChannel = "UNIVESP", youtubeUrl = "https://www.youtube.com/watch?v=x71A6wK9z-Q"),
                        CurriculumNode(id = "geo_ana_con", name = "Seções Cônicas (Elipse, Parábola, Hipérbole)", youtubeChannel = "Professor Aquino - UFAL", youtubeUrl = "https://www.youtube.com/playlist?list=PLg478N778FmY7hL-pY_k8rFmC7R4lS2W0")
                    )
                ),
                CurriculumNode(
                    id = "geo_diff",
                    name = "Geometria Diferencial",
                    children = listOf(
                        CurriculumNode(id = "geo_diff_crv", name = "Curvas e Superfícies no Espaço", youtubeChannel = "IMPA", youtubeUrl = "https://www.youtube.com/playlist?list=PLo4jXE851knR-U_F-fLIDMv7jZ1w-H2u9"),
                        CurriculumNode(id = "geo_diff_man", name = "Variedades Diferenciáveis e Tensores", youtubeChannel = "Ciência Todo Dia", youtubeUrl = "https://www.youtube.com/watch?v=A8wE9UoNmsQ"),
                        CurriculumNode(id = "geo_diff_riem", name = "Curvatura e Geometria Riemanniana", youtubeChannel = "IMPA", youtubeUrl = "https://www.youtube.com/playlist?list=PLo4jXE851knTk4pSuh38n6x2iLp7Zz2Y7")
                    )
                ),
                CurriculumNode(
                    id = "geo_top",
                    name = "Topologia",
                    children = listOf(
                        CurriculumNode(id = "geo_top_spc", name = "Espaços Tópicos e Vizinhança", youtubeChannel = "IMPA", youtubeUrl = "https://www.youtube.com/playlist?list=PLo4jXE851knQ2C-N-V7oGvK0A_0-Oq8i6"),
                        CurriculumNode(id = "geo_top_cont", name = "Continuidade e Homeomorfismos", youtubeChannel = "IMPA", youtubeUrl = "https://www.youtube.com/watch?v=ZfXn9L-Wn40"),
                        CurriculumNode(id = "geo_top_comp", name = "Compacidade e Conexidade", youtubeChannel = "IMPA", youtubeUrl = "https://www.youtube.com/watch?v=Y8k82C_1zXg"),
                        CurriculumNode(id = "geo_top_alg", name = "Topologia Algébrica (Invariantes)", youtubeChannel = "IMPA", youtubeUrl = "https://www.youtube.com/playlist?list=PLo4jXE851knTx4fU3qP3B5mJpXQ6hG6Z9")
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
                        CurriculumNode(id = "calc_pre_func", name = "Funções Reais (Injetora, Sobrejetora, Bijetora)", youtubeChannel = "Ferretto Matemática", youtubeUrl = "https://www.youtube.com/playlist?list=PLxI8Can9yAHfs69S9qS8oA8x_qA9bX5w4", proceduralTag = "funcao_quadratica"),
                        CurriculumNode(id = "calc_pre_elem", name = "Funções Elementares (Exponencial, Logarítmica, Trigonométrica)", youtubeChannel = "Ferretto Matemática", youtubeUrl = "https://www.youtube.com/playlist?list=PLxI8Can9yAHff7Fj9o92m6_7Pz8cTqA6D", proceduralTag = "funcao_logaritmica"),
                        CurriculumNode(id = "calc_pre_seq", name = "Sequências e Séries Numéricas (Convergência)", youtubeChannel = "UNIVESP", youtubeUrl = "https://www.youtube.com/playlist?list=PLxI8Can9yAHdf8O-uGvV62xOepS9w9W2J")
                    )
                ),
                CurriculumNode(
                    id = "calc_dif",
                    name = "Cálculo Diferencial e Integral",
                    children = listOf(
                        CurriculumNode(id = "calc_dif_lim", name = "Limites e Continuidade", youtubeChannel = "Possani (USP)", youtubeUrl = "https://www.youtube.com/playlist?list=PL8XwR9f5InVfU-E5O7A6q1cO9gH_Kz7C1", proceduralTag = "calc_dif_lim"),
                        CurriculumNode(id = "calc_dif_der", name = "Derivadas, Regra da Cadeia e Otimização", proceduralTag = "derivadas_regra_cadeia"),
                        CurriculumNode(id = "calc_dif_int", name = "Integrais (Indefinidas, Definidas e TFC)", proceduralTag = "calc_dif_int"),
                        CurriculumNode(id = "calc_dif_mul", name = "Cálculo Multivariável (Gradiente, Rotacional, Integrais Múltiplas)")
                    )
                ),
                CurriculumNode(
                    id = "calc_eq",
                    name = "Equações Diferenciais",
                    children = listOf(
                        CurriculumNode(id = "calc_eq_ode", name = "EDOs (Equações Diferenciais Ordinárias) de 1ª e 2ª Ordem", youtubeChannel = "Possani (USP)", youtubeUrl = "https://www.youtube.com/playlist?list=PL8XwR9f5InVdfcO-yVscYw6UclGvF0F2A"),
                        CurriculumNode(id = "calc_eq_pde", name = "EDPs (Equações Diferenciais Parciais)", youtubeChannel = "IMPA", youtubeUrl = "https://www.youtube.com/playlist?list=PLo4jXE851knS8E3uVb_z0h4S2PZ6v6U5M"),
                        CurriculumNode(id = "calc_eq_trans", name = "Transformadas de Laplace e Fourier", youtubeChannel = "3Blue1Brown em Português", youtubeUrl = "https://www.youtube.com/watch?v=spUNpyF58BY")
                    )
                ),
                CurriculumNode(
                    id = "calc_real",
                    name = "Análise Real e Complexa",
                    children = listOf(
                        CurriculumNode(id = "calc_real_lim", name = "Formalização Rigorosa de Limites (Épsilon-Delta)"),
                        CurriculumNode(id = "calc_real_met", name = "Espaços Métricos e Sequências de Cauchy", youtubeChannel = "IMPA", youtubeUrl = "https://www.youtube.com/playlist?list=PLo4jXE851knReO6GfS2w57pYwVf7YnK5S"),
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
                        CurriculumNode(id = "stat_comb_bin", name = "Binômio de Newton e Coeficientes Binomiais", youtubeChannel = "Toda a Matemática", youtubeUrl = "https://www.youtube.com/watch?v=Fj0X7oM8rXw")
                    )
                ),
                CurriculumNode(
                    id = "stat_prob",
                    name = "Teoria da Probabilidade",
                    children = listOf(
                        CurriculumNode(id = "stat_prob_spc", name = "Espaço Amostral, Eventos e Axiomas de Kolmogorov", youtubeChannel = "UNIVESP", youtubeUrl = "https://www.youtube.com/playlist?list=PLxI8Can9yAHdO0M6v8-CIn6p_fO3gWj8W"),
                        CurriculumNode(id = "stat_prob_cond", name = "Probabilidade Condicional e Teorema de Bayes", youtubeChannel = "3Blue1Brown em Português", youtubeUrl = "https://www.youtube.com/watch?v=HZGCoVF3YvM"),
                        CurriculumNode(id = "stat_prob_var", name = "Variáveis Aleatórias e Expectância (Média, Variância)")
                    )
                ),
                CurriculumNode(
                    id = "stat_dist",
                    name = "Distribuições de Probabilidade",
                    children = listOf(
                        CurriculumNode(id = "stat_dist_disc", name = "Distribuições Discretas (Binomial, Poisson)", youtubeChannel = "Me Salva!", youtubeUrl = "https://www.youtube.com/playlist?list=PLf1lowbdbFIAwSgX_A9_TqK8IqK6v8K3_"),
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
                        CurriculumNode(id = "stat_inf_test", name = "Testes de Hipóteses (p-valor, Erros Tipo I e II)", youtubeChannel = "Estatística de Forma Simples", youtubeUrl = "https://www.youtube.com/watch?v=9_FpXGf2_qU"),
                        CurriculumNode(id = "stat_inf_reg", name = "Regressão Linear, Correlação e Análise de Dados", youtubeChannel = "Didática Tech", youtubeUrl = "https://www.youtube.com/watch?v=fSgV_l7B2-A")
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
                        CurriculumNode(id = "comp_opt_lin", name = "Programação Linear (Algoritmo Simplex)", youtubeChannel = "UNIVESP", youtubeUrl = "https://www.youtube.com/playlist?list=PLxI8Can9yAHe_j0U9uK7v3qO_S4Z_wGvU"),
                        CurriculumNode(id = "comp_opt_non", name = "Otimização Não-Linear e Convexa (Descida de Gradiente)", youtubeChannel = "3Blue1Brown em Português", youtubeUrl = "https://www.youtube.com/watch?v=tIeHLnjs5U8"),
                        CurriculumNode(id = "comp_opt_int", name = "Programação Inteira e Teoria dos Jogos", youtubeChannel = "Ciência Todo Dia", youtubeUrl = "https://www.youtube.com/watch?v=7_hE4uYvFyw")
                    )
                ),
                CurriculumNode(
                    id = "comp_dyn",
                    name = "Sistemas Dinâmicos e Teoria do Caos",
                    children = listOf(
                        CurriculumNode(id = "comp_dyn_sys", name = "Sistemas Lineares e Não-Lineares no Tempo"),
                        CurriculumNode(id = "comp_dyn_attr", name = "Atratores, Bifurcações e Estabilidade de Lyapunov"),
                        CurriculumNode(id = "comp_dyn_chaos", name = "Teoria do Caos (Sensibilidade às Condições Iniciais)", youtubeChannel = "Universo Narrado", youtubeUrl = "https://www.youtube.com/watch?v=wXoU3uC9kI0")
                    )
                ),
                CurriculumNode(
                    id = "comp_th",
                    name = "Teoria da Computação",
                    children = listOf(
                        CurriculumNode(id = "comp_th_aut", name = "Autômatos Finitos e Linguagens Formais", youtubeChannel = "Instituto de Computação UFF", youtubeUrl = "https://www.youtube.com/playlist?list=PLg478N778FmYT82_o8-C9KkUCl_V99C3K"),
                        CurriculumNode(id = "comp_th_comp", name = "Computabilidade (Máquinas de Turing, Problema da Parada)", youtubeChannel = "Ciência Todo Dia", youtubeUrl = "https://www.youtube.com/watch?v=vV_X2U_3_oY"),
                        CurriculumNode(id = "comp_th_comp_ana", name = "Análise de Complexidade de Algoritmos (P vs NP)")
                    )
                )
            )
        )
    )
}
