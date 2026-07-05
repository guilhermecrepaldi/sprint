"""
Seed de exercícios com skill_tags canônicas do PREREQUISITE_TREE.
"""
import asyncio
import sys
from pathlib import Path

from sqlalchemy import select

sys.path.append(str(Path(__file__).resolve().parents[1]))

from db import AsyncSessionLocal
from models.exercise import Exercise

EXERCISES = [
    # ── soma_subtracao ────────────────────────────────────────────────────────
    {"statement": "Calcule: 348 + 197", "expected_answer": "545",
     "skill_tags": ["soma_subtracao"], "difficulty": 1.0},
    {"statement": "Calcule: 1002 - 487", "expected_answer": "515",
     "skill_tags": ["soma_subtracao"], "difficulty": 1.2},
    {"statement": "Calcule: 56 + 78 - 34", "expected_answer": "100",
     "skill_tags": ["soma_subtracao"], "difficulty": 1.3},

    # ── multiplicacao_divisao ─────────────────────────────────────────────────
    {"statement": "Calcule: 24 × 13", "expected_answer": "312",
     "skill_tags": ["multiplicacao_divisao"], "difficulty": 1.5},
    {"statement": "Calcule: 288 ÷ 12", "expected_answer": "24",
     "skill_tags": ["multiplicacao_divisao"], "difficulty": 1.5},
    {"statement": "Calcule: 15 × 8 ÷ 6", "expected_answer": "20",
     "skill_tags": ["multiplicacao_divisao"], "difficulty": 1.8},

    # ── fracoes_decimais ──────────────────────────────────────────────────────
    {"statement": r"Calcule: \frac{2}{3} + \frac{5}{6}", "expected_answer": r"\frac{3}{2}",
     "skill_tags": ["fracoes_decimais"], "difficulty": 2.0},
    {"statement": r"Calcule: \frac{7}{8} - \frac{1}{4}", "expected_answer": r"\frac{5}{8}",
     "skill_tags": ["fracoes_decimais"], "difficulty": 2.0},
    {"statement": r"Calcule: 0{,}25 + \frac{1}{2}", "expected_answer": r"\frac{3}{4}",
     "skill_tags": ["fracoes_decimais"], "difficulty": 2.2},
    {"statement": r"Simplifique: \frac{18}{24}", "expected_answer": r"\frac{3}{4}",
     "skill_tags": ["fracoes_decimais"], "difficulty": 1.8},

    # ── porcentagem_razao ─────────────────────────────────────────────────────
    {"statement": "Quanto é 30% de 250?", "expected_answer": "75",
     "skill_tags": ["porcentagem_razao"], "difficulty": 2.0},
    {"statement": "Um produto custa R$80 com 15% de desconto. Qual o preço final?",
     "expected_answer": "68", "skill_tags": ["porcentagem_razao"], "difficulty": 2.3},

    # ── potenciacao_radiciacao ────────────────────────────────────────────────
    {"statement": r"Calcule: 2^{10}", "expected_answer": "1024",
     "skill_tags": ["potenciacao_radiciacao"], "difficulty": 2.0},
    {"statement": r"Calcule: \sqrt{144}", "expected_answer": "12",
     "skill_tags": ["potenciacao_radiciacao"], "difficulty": 1.8},
    {"statement": r"Calcule: (-3)^4", "expected_answer": "81",
     "skill_tags": ["potenciacao_radiciacao"], "difficulty": 2.2},

    # ── equacoes_lineares ─────────────────────────────────────────────────────
    {"statement": "x + 3 = 0", "expected_answer": "x = -3",
     "skill_tags": ["equacoes_lineares"], "difficulty": 1.5},
    {"statement": "Resolva: 3x + 7 = 22", "expected_answer": "x = 5",
     "skill_tags": ["equacoes_lineares"], "difficulty": 2.0},
    {"statement": "Resolva: 5 - 2x = 17", "expected_answer": "x = -6",
     "skill_tags": ["equacoes_lineares"], "difficulty": 2.2},
    {"statement": "Resolva: 2(x - 3) = 4x + 2", "expected_answer": "x = -4",
     "skill_tags": ["equacoes_lineares"], "difficulty": 2.8},
    {"statement": r"Resolva: \frac{x-1}{2} = 3", "expected_answer": "x = 7",
     "skill_tags": ["equacoes_lineares"], "difficulty": 2.6},
    {"statement": r"Resolva: \frac{x}{3} + 2 = 6", "expected_answer": "x = 12",
     "skill_tags": ["equacoes_lineares"], "difficulty": 2.4},

    # ── sistemas_equacoes ─────────────────────────────────────────────────────
    {"statement": "Resolva: x + y = 10 e x - y = 4", "expected_answer": "x = 7, y = 3",
     "skill_tags": ["sistemas_equacoes"], "difficulty": 3.0},
    {"statement": "Resolva: 2x + y = 8 e x - y = 1", "expected_answer": "x = 3, y = 2",
     "skill_tags": ["sistemas_equacoes"], "difficulty": 3.2},

    # ── fatoracao_produtos_notaveis ───────────────────────────────────────────
    {"statement": "Fatore: x² + 5x + 6", "expected_answer": "(x+2)(x+3)",
     "skill_tags": ["fatoracao_produtos_notaveis"], "difficulty": 3.0},
    {"statement": "Fatore: x² - 16", "expected_answer": "(x-4)(x+4)",
     "skill_tags": ["fatoracao_produtos_notaveis"], "difficulty": 2.7},
    {"statement": "Expanda: (x + 3)²", "expected_answer": "x^2 + 6x + 9",
     "skill_tags": ["fatoracao_produtos_notaveis"], "difficulty": 2.8},
    {"statement": "Expanda: (x + 4)(x - 2)", "expected_answer": "x^2 + 2x - 8",
     "skill_tags": ["fatoracao_produtos_notaveis"], "difficulty": 3.0},

    # ── inequacoes ────────────────────────────────────────────────────────────
    {"statement": "Resolva: 2x - 3 > 7", "expected_answer": "x > 5",
     "skill_tags": ["inequacoes"], "difficulty": 2.5},
    {"statement": "Resolva: -3x + 1 ≤ 10", "expected_answer": r"x \geq -3",
     "skill_tags": ["inequacoes"], "difficulty": 2.8},

    # ── equacoes_quadraticas ──────────────────────────────────────────────────
    {"statement": "Resolva: x² - 9 = 0", "expected_answer": r"x = \pm 3",
     "skill_tags": ["equacoes_quadraticas"], "difficulty": 2.5},
    {"statement": "Resolva: x² - 5x + 6 = 0", "expected_answer": "x = 2 ou x = 3",
     "skill_tags": ["equacoes_quadraticas"], "difficulty": 3.2},
    {"statement": "Resolva: 2x² + 5x - 3 = 0", "expected_answer": r"x = \frac{1}{2} ou x = -3",
     "skill_tags": ["equacoes_quadraticas"], "difficulty": 3.5},
    {"statement": "Resolva: x² = 49", "expected_answer": r"x = \pm 7",
     "skill_tags": ["equacoes_quadraticas"], "difficulty": 2.4},

    # ── funcao_afim ───────────────────────────────────────────────────────────
    {"statement": "Dada f(x) = 3x - 2, calcule f(4).", "expected_answer": "10",
     "skill_tags": ["funcao_afim"], "difficulty": 2.5},
    {"statement": "Encontre a raiz de f(x) = 2x + 8.", "expected_answer": "x = -4",
     "skill_tags": ["funcao_afim"], "difficulty": 2.8},

    # ── funcao_quadratica ─────────────────────────────────────────────────────
    {"statement": "Dada f(x) = x² - 4x + 3, calcule o vértice.", "expected_answer": "(2, -1)",
     "skill_tags": ["funcao_quadratica"], "difficulty": 3.5},
    {"statement": "Qual o valor mínimo de f(x) = x² - 6x + 5?", "expected_answer": "-4",
     "skill_tags": ["funcao_quadratica"], "difficulty": 3.5},

    # ── funcao_exponencial ────────────────────────────────────────────────────
    {"statement": "Resolva: 2^x = 32", "expected_answer": "x = 5",
     "skill_tags": ["funcao_exponencial"], "difficulty": 3.5},
    {"statement": "Resolva: 3^(x+1) = 27", "expected_answer": "x = 2",
     "skill_tags": ["funcao_exponencial"], "difficulty": 3.8},

    # ── funcao_logaritmica ────────────────────────────────────────────────────
    {"statement": r"Calcule: \log_2 8", "expected_answer": "3",
     "skill_tags": ["funcao_logaritmica"], "difficulty": 3.5},
    {"statement": r"Resolva: \log_3 x = 4", "expected_answer": "x = 81",
     "skill_tags": ["funcao_logaritmica"], "difficulty": 3.8},
    {"statement": r"Calcule: \log_{10} 1000", "expected_answer": "3",
     "skill_tags": ["funcao_logaritmica"], "difficulty": 3.2},

    # ── geometria_plana ───────────────────────────────────────────────────────
    {"statement": "Calcule a área de um triângulo com base 8 e altura 5.", "expected_answer": "20",
     "skill_tags": ["geometria_plana"], "difficulty": 2.0},
    {"statement": "Calcule a área de um círculo com raio 7. Use π ≈ 3,14.", "expected_answer": "153,86",
     "skill_tags": ["geometria_plana"], "difficulty": 2.5},

    # ── nocao_de_limite ───────────────────────────────────────────────────────
    {"statement": r"Calcule: \lim_{x \to 2} (3x - 1)", "expected_answer": "5",
     "skill_tags": ["nocao_de_limite"], "difficulty": 5.0},

    # ── derivadas_basicas ─────────────────────────────────────────────────────
    {"statement": "Calcule a derivada de f(x) = x³ - 2x + 1.", "expected_answer": "3x^2 - 2",
     "skill_tags": ["derivadas_basicas"], "difficulty": 6.0},
    {"statement": "Calcule f'(x) para f(x) = 5x² + 3x.", "expected_answer": "10x + 3",
     "skill_tags": ["derivadas_basicas"], "difficulty": 5.5},

    # ── integrais_indefinidas ─────────────────────────────────────────────────
    {"statement": r"Calcule: \int (2x + 3)\,dx", "expected_answer": "x^2 + 3x + C",
     "skill_tags": ["integrais_indefinidas"], "difficulty": 7.0},
    {"statement": r"Calcule: \int x^3\,dx", "expected_answer": r"\frac{x^4}{4} + C",
     "skill_tags": ["integrais_indefinidas"], "difficulty": 6.5},
    {"statement": r"Calcule: \int \cos(x)\,dx", "expected_answer": r"\text{sen}(x) + C",
     "skill_tags": ["integrais_indefinidas"], "difficulty": 6.8},
    {"statement": r"Calcule: \int e^x\,dx", "expected_answer": "e^x + C",
     "skill_tags": ["integrais_indefinidas"], "difficulty": 6.5},

    # ── funcao_afim (expansão) ────────────────────────────────────────────────
    {"statement": "Uma função afim tem f(0) = 3 e f(2) = 7. Qual a expressão?",
     "expected_answer": "f(x) = 2x + 3", "skill_tags": ["funcao_afim"], "difficulty": 3.0},
    {"statement": "Dada f(x) = -2x + 6, qual o zero da função?",
     "expected_answer": "x = 3", "skill_tags": ["funcao_afim"], "difficulty": 2.6},
    {"statement": "Para f(x) = 4x - 8, calcule f(-1).",
     "expected_answer": "-12", "skill_tags": ["funcao_afim"], "difficulty": 2.4},

    # ── funcao_quadratica (expansão) ──────────────────────────────────────────
    {"statement": r"Qual o discriminante de f(x) = x^2 - 4x + 3?",
     "expected_answer": "4", "skill_tags": ["funcao_quadratica"], "difficulty": 3.0},
    {"statement": r"Quais as raízes de f(x) = x^2 - 4?",
     "expected_answer": "x = 2 ou x = -2", "skill_tags": ["funcao_quadratica"], "difficulty": 3.0},
    {"statement": r"Dada f(x) = 2x^2 - 8, calcule f(3).",
     "expected_answer": "10", "skill_tags": ["funcao_quadratica"], "difficulty": 3.2},

    # ── geometria_plana (expansão) ────────────────────────────────────────────
    {"statement": "Calcule o perímetro de um retângulo de lados 6 e 4.",
     "expected_answer": "20", "skill_tags": ["geometria_plana"], "difficulty": 1.8},
    {"statement": "Qual a área de um trapézio com bases 6 e 10 e altura 4?",
     "expected_answer": "32", "skill_tags": ["geometria_plana"], "difficulty": 2.2},
    {"statement": r"Calcule a diagonal de um quadrado de lado 5.",
     "expected_answer": r"5\sqrt{2}", "skill_tags": ["geometria_plana"], "difficulty": 2.8},

    # ── porcentagem_razao (expansão) ──────────────────────────────────────────
    {"statement": "Qual é 12,5% de 400?",
     "expected_answer": "50", "skill_tags": ["porcentagem_razao"], "difficulty": 2.1},
    {"statement": "Um produto de R$120 teve aumento de 25%. Qual o novo preço?",
     "expected_answer": "150", "skill_tags": ["porcentagem_razao"], "difficulty": 2.4},
    {"statement": "A razão entre 15 e 25 simplificada é?",
     "expected_answer": r"\frac{3}{5}", "skill_tags": ["porcentagem_razao"], "difficulty": 1.8},

    # ── sistemas_equacoes (expansão) ──────────────────────────────────────────
    {"statement": "Resolva: x + y = 5 e 2x - y = 1",
     "expected_answer": "x = 2, y = 3", "skill_tags": ["sistemas_equacoes"], "difficulty": 3.0},
    {"statement": "Resolva: 3x + 2y = 12 e x = 2",
     "expected_answer": "x = 2, y = 3", "skill_tags": ["sistemas_equacoes"], "difficulty": 2.8},
    {"statement": "Resolva: x - y = 4 e 2x + y = 8",
     "expected_answer": "x = 4, y = 0", "skill_tags": ["sistemas_equacoes"], "difficulty": 3.2},

    # ── inequacoes (expansão) ─────────────────────────────────────────────────
    {"statement": "Resolva: 5x + 2 > 12",
     "expected_answer": "x > 2", "skill_tags": ["inequacoes"], "difficulty": 2.5},
    {"statement": "Resolva: -2x < 8",
     "expected_answer": "x > -4", "skill_tags": ["inequacoes"], "difficulty": 2.6},
    {"statement": r"Resolva: \frac{x+1}{3} \geq 2",
     "expected_answer": r"x \geq 5", "skill_tags": ["inequacoes"], "difficulty": 2.9},

    # ── nocao_de_limite (expansão) ────────────────────────────────────────────
    {"statement": r"Calcule: \lim_{x \to 0} \frac{\text{sen}(x)}{x}",
     "expected_answer": "1", "skill_tags": ["nocao_de_limite"], "difficulty": 5.5},
    {"statement": r"Calcule: \lim_{x \to \infty} \frac{1}{x}",
     "expected_answer": "0", "skill_tags": ["nocao_de_limite"], "difficulty": 4.8},
    {"statement": r"Calcule: \lim_{x \to 1} \frac{x^2 - 1}{x - 1}",
     "expected_answer": "2", "skill_tags": ["nocao_de_limite"], "difficulty": 5.2},

    # ── derivadas_basicas (expansão) ──────────────────────────────────────────
    {"statement": r"Derive: f(x) = \text{sen}(x)",
     "expected_answer": r"\cos(x)", "skill_tags": ["derivadas_basicas"], "difficulty": 5.5},
    {"statement": r"Derive: f(x) = e^x",
     "expected_answer": "e^x", "skill_tags": ["derivadas_basicas"], "difficulty": 5.5},
    {"statement": r"Derive: f(x) = \ln(x)",
     "expected_answer": r"\frac{1}{x}", "skill_tags": ["derivadas_basicas"], "difficulty": 5.8},

    # ── continuidade ──────────────────────────────────────────────────────────
    {"statement": r"Calcule: \lim_{x \to 2} (x^2 - 4x + 4)",
     "expected_answer": "0", "skill_tags": ["continuidade"], "difficulty": 5.0},
    {"statement": r"Calcule: \lim_{x \to 0} \frac{x^2 + x}{x}",
     "expected_answer": "1", "skill_tags": ["continuidade"], "difficulty": 5.2},
    {"statement": r"Calcule: \lim_{x \to 3} \frac{x^2 - 9}{x - 3}",
     "expected_answer": "6", "skill_tags": ["continuidade"], "difficulty": 5.5},
    {"statement": r"A função f(x) = x^2 é contínua em x = 3? Calcule \lim_{x \to 3} f(x).",
     "expected_answer": "9", "skill_tags": ["continuidade"], "difficulty": 4.8},

    # ── derivadas_regra_cadeia ────────────────────────────────────────────────
    {"statement": r"Calcule a derivada de f(x) = (2x + 1)^3.",
     "expected_answer": r"6(2x+1)^2", "skill_tags": ["derivadas_regra_cadeia"], "difficulty": 6.5},
    {"statement": r"Derive: f(x) = \sqrt{x^2 + 1}",
     "expected_answer": r"\frac{x}{\sqrt{x^2+1}}", "skill_tags": ["derivadas_regra_cadeia"], "difficulty": 7.0},
    {"statement": r"Derive: f(x) = (x^2 + 3)^4.",
     "expected_answer": r"8x(x^2+3)^3", "skill_tags": ["derivadas_regra_cadeia"], "difficulty": 6.8},
    {"statement": r"Derive: f(x) = \text{sen}(3x).",
     "expected_answer": r"3\cos(3x)", "skill_tags": ["derivadas_regra_cadeia"], "difficulty": 6.2},

    # ── derivadas_produto_quociente ───────────────────────────────────────────
    {"statement": r"Derive: f(x) = x^2 \cdot \text{sen}(x).",
     "expected_answer": r"2x\,\text{sen}(x) + x^2\cos(x)", "skill_tags": ["derivadas_produto_quociente"], "difficulty": 7.0},
    {"statement": r"Derive: f(x) = \frac{x}{x+1}",
     "expected_answer": r"\frac{1}{(x+1)^2}", "skill_tags": ["derivadas_produto_quociente"], "difficulty": 6.8},
    {"statement": r"Derive: f(x) = x \cdot e^x.",
     "expected_answer": r"e^x(x+1)", "skill_tags": ["derivadas_produto_quociente"], "difficulty": 6.5},
    {"statement": r"Derive: f(x) = \frac{x^2}{x-1}",
     "expected_answer": r"\frac{x^2 - 2x}{(x-1)^2}", "skill_tags": ["derivadas_produto_quociente"], "difficulty": 7.2},

    # ── aplicacoes_derivadas ──────────────────────────────────────────────────
    {"statement": r"Encontre os pontos críticos de f(x) = x^3 - 3x.",
     "expected_answer": "x = 1 e x = -1", "skill_tags": ["aplicacoes_derivadas"], "difficulty": 7.0},
    {"statement": r"Qual o valor máximo de f(x) = -x^2 + 4x?",
     "expected_answer": "4", "skill_tags": ["aplicacoes_derivadas"], "difficulty": 6.8},
    {"statement": r"Determine os intervalos de crescimento de f(x) = x^3 - 6x^2.",
     "expected_answer": "x < 0 ou x > 4", "skill_tags": ["aplicacoes_derivadas"], "difficulty": 7.5},
    {"statement": r"Qual o mínimo local de f(x) = x^3 - 3x^2?",
     "expected_answer": "-4", "skill_tags": ["aplicacoes_derivadas"], "difficulty": 7.2},

    # ── integrais_definidas ───────────────────────────────────────────────────
    {"statement": r"Calcule: \int_0^2 x\,dx",
     "expected_answer": "2", "skill_tags": ["integrais_definidas"], "difficulty": 7.0},
    {"statement": r"Calcule: \int_1^3 2x\,dx",
     "expected_answer": "8", "skill_tags": ["integrais_definidas"], "difficulty": 7.2},
    {"statement": r"Calcule: \int_0^1 x^2\,dx",
     "expected_answer": r"\frac{1}{3}", "skill_tags": ["integrais_definidas"], "difficulty": 7.5},
    {"statement": r"Calcule: \int_0^\pi \text{sen}(x)\,dx",
     "expected_answer": "2", "skill_tags": ["integrais_definidas"], "difficulty": 8.0},

    # ── aplicacoes_integrais ──────────────────────────────────────────────────
    {"statement": r"Calcule a área entre f(x) = x e g(x) = x^2 de 0 a 1.",
     "expected_answer": r"\frac{1}{6}", "skill_tags": ["aplicacoes_integrais"], "difficulty": 8.5},
    {"statement": r"Calcule a área sob f(x) = x de 0 a 3.",
     "expected_answer": r"\frac{9}{2}", "skill_tags": ["aplicacoes_integrais"], "difficulty": 7.5},
    {"statement": "A velocidade é v(t) = 3t². Qual o deslocamento de t = 0 a t = 2?",
     "expected_answer": "8", "skill_tags": ["aplicacoes_integrais"], "difficulty": 8.0},
    {"statement": r"Calcule a área entre f(x) = x^2 e o eixo x de -1 a 1.",
     "expected_answer": r"\frac{2}{3}", "skill_tags": ["aplicacoes_integrais"], "difficulty": 8.2},

    # ── funcao_modular ────────────────────────────────────────────────────────
    {"statement": "Resolva: |x - 3| = 5",
     "expected_answer": "x = 8 ou x = -2", "skill_tags": ["funcao_modular"], "difficulty": 3.5},
    {"statement": "Resolva: |2x + 1| = 7",
     "expected_answer": "x = 3 ou x = -4", "skill_tags": ["funcao_modular"], "difficulty": 3.8},
    {"statement": "Calcule f(-4) para f(x) = |x| + 2.",
     "expected_answer": "6", "skill_tags": ["funcao_modular"], "difficulty": 2.8},
    {"statement": "Resolva: |x| < 4",
     "expected_answer": "-4 < x < 4", "skill_tags": ["funcao_modular"], "difficulty": 3.2},
    {"statement": "Resolva: |3x - 6| = 0",
     "expected_answer": "x = 2", "skill_tags": ["funcao_modular"], "difficulty": 3.0},

    # ── trig_razoes ───────────────────────────────────────────────────────────
    {"statement": r"Calcule: \text{sen}(30°)",
     "expected_answer": r"\frac{1}{2}", "skill_tags": ["trig_razoes"], "difficulty": 3.5},
    {"statement": r"Calcule: \cos(60°)",
     "expected_answer": r"\frac{1}{2}", "skill_tags": ["trig_razoes"], "difficulty": 3.5},
    {"statement": r"Calcule: \text{tg}(45°)",
     "expected_answer": "1", "skill_tags": ["trig_razoes"], "difficulty": 3.5},
    {"statement": "Em um triângulo retângulo, cateto oposto = 3 e hipotenusa = 5. Qual sen(θ)?",
     "expected_answer": r"\frac{3}{5}", "skill_tags": ["trig_razoes"], "difficulty": 4.0},
    {"statement": r"Calcule: \cos(30°)",
     "expected_answer": r"\frac{\sqrt{3}}{2}", "skill_tags": ["trig_razoes"], "difficulty": 3.8},

    # ── trig_seno_cosseno_tangente ────────────────────────────────────────────
    {"statement": r"Calcule: \text{sen}^2(30°) + \cos^2(30°)",
     "expected_answer": "1", "skill_tags": ["trig_seno_cosseno_tangente"], "difficulty": 4.0},
    {"statement": r"Calcule: \text{tg}(60°)",
     "expected_answer": r"\sqrt{3}", "skill_tags": ["trig_seno_cosseno_tangente"], "difficulty": 4.0},
    {"statement": r"Calcule: \text{sen}(45°) \cdot \sqrt{2}",
     "expected_answer": "1", "skill_tags": ["trig_seno_cosseno_tangente"], "difficulty": 4.2},
    {"statement": r"Calcule: \cos(0°) + \text{sen}(90°)",
     "expected_answer": "2", "skill_tags": ["trig_seno_cosseno_tangente"], "difficulty": 3.8},
    {"statement": r"Calcule: \text{tg}(30°) \cdot \sqrt{3}",
     "expected_answer": "1", "skill_tags": ["trig_seno_cosseno_tangente"], "difficulty": 4.5},

    # ── trig_identidades ──────────────────────────────────────────────────────
    {"statement": r"Simplifique: \frac{\text{sen}\,\theta}{\cos\theta}",
     "expected_answer": r"\text{tg}\,\theta", "skill_tags": ["trig_identidades"], "difficulty": 4.5},
    {"statement": r"Simplifique: \frac{1}{\cos^2\theta} - 1",
     "expected_answer": r"\text{tg}^2\theta", "skill_tags": ["trig_identidades"], "difficulty": 5.0},
    {"statement": r"Calcule: \text{sen}(\frac{\pi}{6}) + \cos(\frac{\pi}{3})",
     "expected_answer": "1", "skill_tags": ["trig_identidades"], "difficulty": 4.8},
    {"statement": r"Expanda: \text{sen}(A + B)",
     "expected_answer": r"\text{sen}A\cos B + \cos A\,\text{sen}B",
     "skill_tags": ["trig_identidades"], "difficulty": 5.5},

    # ── trig_equacoes ─────────────────────────────────────────────────────────
    {"statement": r"Resolva para $0 \leq x < 2\pi$: \text{sen}(x) = 1",
     "expected_answer": r"x = \frac{\pi}{2}", "skill_tags": ["trig_equacoes"], "difficulty": 5.0},
    {"statement": r"Resolva: \cos(x) = 0, \; 0 \leq x < 2\pi",
     "expected_answer": r"x = \frac{\pi}{2} \text{ ou } x = \frac{3\pi}{2}",
     "skill_tags": ["trig_equacoes"], "difficulty": 5.2},
    {"statement": r"Resolva: \text{tg}(x) = 1, \; 0 \leq x < \pi",
     "expected_answer": r"x = \frac{\pi}{4}", "skill_tags": ["trig_equacoes"], "difficulty": 5.0},
    {"statement": r"Resolva: 2\,\text{sen}(x) = 1, \; 0 \leq x < 2\pi",
     "expected_answer": r"x = \frac{\pi}{6} \text{ ou } x = \frac{5\pi}{6}",
     "skill_tags": ["trig_equacoes"], "difficulty": 5.5},

    # ── geometria_espacial ────────────────────────────────────────────────────
    {"statement": "Calcule o volume de um cubo com aresta 4 cm.",
     "expected_answer": "64", "skill_tags": ["geometria_espacial"], "difficulty": 3.0},
    {"statement": "Calcule a área total da superfície de um cubo com aresta 3 cm.",
     "expected_answer": "54", "skill_tags": ["geometria_espacial"], "difficulty": 3.2},
    {"statement": "Calcule o volume de um cilindro com raio 2 e altura 5. Use π ≈ 3,14.",
     "expected_answer": "62,8", "skill_tags": ["geometria_espacial"], "difficulty": 3.5},
    {"statement": r"Volume de uma esfera com raio 3. Use \pi \approx 3{,}14.",
     "expected_answer": "113,04", "skill_tags": ["geometria_espacial"], "difficulty": 3.8},
    {"statement": "Volume de um cone com raio 3 e altura 4. Use π ≈ 3,14.",
     "expected_answer": "37,68", "skill_tags": ["geometria_espacial"], "difficulty": 3.8},

    # ── geometria_analitica ───────────────────────────────────────────────────
    {"statement": "Calcule a distância entre os pontos A(1, 2) e B(4, 6).",
     "expected_answer": "5", "skill_tags": ["geometria_analitica"], "difficulty": 3.5},
    {"statement": "Calcule o ponto médio entre A(0, 0) e B(4, 6).",
     "expected_answer": "(2, 3)", "skill_tags": ["geometria_analitica"], "difficulty": 3.0},
    {"statement": "Qual a equação da reta que passa por (0, 3) com coeficiente angular 2?",
     "expected_answer": "y = 2x + 3", "skill_tags": ["geometria_analitica"], "difficulty": 3.5},
    {"statement": "Encontre a distância do ponto P(3, 4) à origem.",
     "expected_answer": "5", "skill_tags": ["geometria_analitica"], "difficulty": 3.2},
    {"statement": "Qual o coeficiente angular da reta que passa por (1, 2) e (3, 8)?",
     "expected_answer": "3", "skill_tags": ["geometria_analitica"], "difficulty": 3.8},

    # ── progressoes_pa_pg ─────────────────────────────────────────────────────
    {"statement": "Qual é o 10º termo da PA (2, 5, 8, ...)?",
     "expected_answer": "29", "skill_tags": ["progressoes_pa_pg"], "difficulty": 3.5},
    {"statement": "Calcule a soma dos 5 primeiros termos da PA (1, 3, 5, 7, 9).",
     "expected_answer": "25", "skill_tags": ["progressoes_pa_pg"], "difficulty": 3.5},
    {"statement": "Qual é o 5º termo da PG (2, 6, 18, ...)?",
     "expected_answer": "162", "skill_tags": ["progressoes_pa_pg"], "difficulty": 4.0},
    {"statement": "Calcule a soma dos primeiros 4 termos da PG (1, 2, 4, 8).",
     "expected_answer": "15", "skill_tags": ["progressoes_pa_pg"], "difficulty": 3.8},
    {"statement": "Em uma PA com a₁ = 3 e r = 4, qual o 6º termo?",
     "expected_answer": "23", "skill_tags": ["progressoes_pa_pg"], "difficulty": 3.6},

    # ── combinatoria ──────────────────────────────────────────────────────────
    {"statement": r"Calcule: C(5, 2)",
     "expected_answer": "10", "skill_tags": ["combinatoria"], "difficulty": 4.0},
    {"statement": "De quantas formas podem-se organizar 4 pessoas em fila?",
     "expected_answer": "24", "skill_tags": ["combinatoria"], "difficulty": 3.8},
    {"statement": r"Calcule: A(5, 2)",
     "expected_answer": "20", "skill_tags": ["combinatoria"], "difficulty": 4.0},
    {"statement": r"Calcule: C(6, 3)",
     "expected_answer": "20", "skill_tags": ["combinatoria"], "difficulty": 4.2},
    {"statement": "Quantos subconjuntos de 3 elementos existem em {a, b, c, d, e}?",
     "expected_answer": "10", "skill_tags": ["combinatoria"], "difficulty": 4.0},

    # ── probabilidade ─────────────────────────────────────────────────────────
    {"statement": "Um dado é lançado. Qual a probabilidade de sair número par?",
     "expected_answer": r"\frac{1}{2}", "skill_tags": ["probabilidade"], "difficulty": 3.5},
    {"statement": "De um baralho de 52 cartas, qual a probabilidade de tirar um ás?",
     "expected_answer": r"\frac{1}{13}", "skill_tags": ["probabilidade"], "difficulty": 4.0},
    {"statement": "Uma moeda é lançada 2 vezes. Qual a probabilidade de sair 2 caras?",
     "expected_answer": r"\frac{1}{4}", "skill_tags": ["probabilidade"], "difficulty": 3.8},
    {"statement": "Uma urna tem 3 bolas vermelhas e 2 azuis. Qual a probabilidade de sortear vermelha?",
     "expected_answer": r"\frac{3}{5}", "skill_tags": ["probabilidade"], "difficulty": 3.5},
    {"statement": "Dois dados são lançados. Qual a probabilidade de sair soma 7?",
     "expected_answer": r"\frac{1}{6}", "skill_tags": ["probabilidade"], "difficulty": 4.5},
]

# Campos padrão para todos os exercícios
_DEFAULTS = {
    "estimated_time_ms": 45000,
    "subject": "math",
    "canvas_mode": "calculation",
    "validator": "sympy",
    "source_library": "seed_v2",
}


async def main() -> None:
    async with AsyncSessionLocal() as db:
        statements = [item["statement"] for item in EXERCISES]
        existing = await db.execute(
            select(Exercise.statement).where(Exercise.statement.in_(statements))
        )
        existing_set = set(existing.scalars().all())

        new_exercises = [
            Exercise(**{**_DEFAULTS, **item})
            for item in EXERCISES
            if item["statement"] not in existing_set
        ]
        db.add_all(new_exercises)
        await db.commit()
        print(f"Seed v2: {len(new_exercises)} exercícios inseridos, {len(existing_set)} já existiam.")


if __name__ == "__main__":
    asyncio.run(main())
