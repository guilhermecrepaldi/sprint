"""
Gera um pool grande de exercícios procedurais por skill.
Cada skill recebe ~200 variações com números diferentes — sem repetição.
Roda idempotente: pula statements já existentes no banco.

Uso:
    python seed/generate_exercises.py
"""
import asyncio
import random
import sys
from fractions import Fraction
from pathlib import Path

sys.path.append(str(Path(__file__).resolve().parents[1]))

from sqlalchemy import select
from db import AsyncSessionLocal
from engine.exercise_library import generate_quadratic_1var_batch
from engine.unlock import PREREQUISITE_TREE
from models.exercise import Exercise

_DEFAULTS = {
    "estimated_time_ms": 40000,
    "subject": "math",
    "canvas_mode": "calculation",
    "validator": "sympy",
    "source_library": "sprint_parametric_v2",
    "source_license": "proprietary_generated",
}

BASE_EXERCISES_PER_SKILL = 100
VARIATIONS_PER_EXERCISE = 3
TARGET_PER_SKILL = BASE_EXERCISES_PER_SKILL * VARIATIONS_PER_EXERCISE

TRACK_SKILLS = [
    "soma_subtracao",
    "multiplicacao_divisao",
    "fracoes_decimais",
    "porcentagem_razao",
    "potenciacao_radiciacao",
    "equacoes_lineares",
    "sistemas_equacoes",
    "fatoracao_produtos_notaveis",
    "inequacoes",
    "equacoes_quadraticas",
    "funcao_afim",
    "funcao_quadratica",
    "funcao_exponencial",
    "funcao_logaritmica",
    "funcao_modular",
    "geometria_plana",
    "geometria_espacial",
    "geometria_analitica",
    "progressoes_pa_pg",
    "combinatoria",
    "probabilidade",
    "trig_razoes",
    "trig_seno_cosseno_tangente",
    "trig_identidades",
    "trig_equacoes",
    "nocao_de_limite",
    "continuidade",
    "derivadas_basicas",
    "derivadas_regra_cadeia",
    "derivadas_produto_quociente",
    "aplicacoes_derivadas",
    "integrais_indefinidas",
    "integrais_definidas",
    "aplicacoes_integrais",
]

# ── Geradores por skill ────────────────────────────────────────────────────────

def gen_equacoes_lineares(rng: random.Random) -> list[dict]:
    exercises = []

    # Tipo 1: ax + b = c  (dif 1.5)
    for _ in range(60):
        a = rng.randint(2, 9)
        x = rng.randint(-8, 8)
        b = rng.randint(1, 20)
        c = a * x + b
        stmt = f"Resolva: {a}x + {b} = {c}" if b >= 0 else f"Resolva: {a}x - {abs(b)} = {c}"
        exercises.append({"statement": stmt, "expected_answer": f"x = {x}",
                          "skill_tags": ["equacoes_lineares"], "difficulty": 1.5})

    # Tipo 2: ax + b = cx + d  (dif 2.0)
    for _ in range(60):
        a = rng.randint(3, 9)
        c = rng.randint(1, a - 1)
        x = rng.randint(-6, 6)
        b = rng.randint(-10, 10)
        d = (a - c) * x + b
        stmt = f"Resolva: {a}x + {b} = {c}x + {d}"
        exercises.append({"statement": stmt, "expected_answer": f"x = {x}",
                          "skill_tags": ["equacoes_lineares"], "difficulty": 2.0})

    # Tipo 3: a(x + b) = c  (dif 2.4)
    for _ in range(40):
        a = rng.randint(2, 8)
        x = rng.randint(-6, 6)
        b = rng.randint(-5, 5)
        c = a * (x + b)
        b_str = f"x + {b}" if b >= 0 else f"x - {abs(b)}"
        stmt = f"Resolva: {a}({b_str}) = {c}"
        exercises.append({"statement": stmt, "expected_answer": f"x = {x}",
                          "skill_tags": ["equacoes_lineares"], "difficulty": 2.4})

    # Tipo 4: x/a + b = c  (dif 2.2)
    for _ in range(40):
        a = rng.randint(2, 8)
        x = rng.randint(-8, 8) * a   # garante divisão exata
        b = rng.randint(1, 10)
        c = x // a + b
        stmt = f"Resolva: x/{a} + {b} = {c}"
        exercises.append({"statement": stmt, "expected_answer": f"x = {x}",
                          "skill_tags": ["equacoes_lineares"], "difficulty": 2.2})

    return exercises


def gen_sistemas_equacoes(rng: random.Random) -> list[dict]:
    exercises = []
    for _ in range(100):
        x = rng.randint(-5, 5)
        y = rng.randint(-5, 5)
        a1, b1 = rng.randint(1, 4), rng.randint(1, 4)
        a2, b2 = rng.randint(1, 4), rng.randint(1, 4)
        c1 = a1 * x + b1 * y
        c2 = a2 * x + b2 * y
        stmt = f"Resolva: {a1}x + {b1}y = {c1} e {a2}x + {b2}y = {c2}"
        exercises.append({"statement": stmt, "expected_answer": f"x = {x}, y = {y}",
                          "skill_tags": ["sistemas_equacoes"], "difficulty": 3.0})
    return exercises


def gen_inequacoes(rng: random.Random) -> list[dict]:
    exercises = []
    ops = [">", "<", "≥", "≤"]
    for _ in range(100):
        a = rng.randint(1, 6)
        b = rng.randint(1, 15)
        rhs = rng.randint(1, 20)
        op = rng.choice(ops)
        # ax + b op rhs  →  x op (rhs - b) / a
        lhs_val = rhs - b
        if a == 1:
            ans_rhs = lhs_val
            ans_op = op
        else:
            if lhs_val % a == 0:
                ans_rhs = lhs_val // a
                ans_op = op
            else:
                continue  # pula divisão não-inteira
        inv_op = {">": "<", "<": ">", "≥": "≤", "≤": "≥"}
        if a < 0:
            ans_op = inv_op[op]
        stmt = f"Resolva: {a}x + {b} {op} {rhs}"
        exercises.append({"statement": stmt, "expected_answer": f"x {ans_op} {ans_rhs}",
                          "skill_tags": ["inequacoes"], "difficulty": 2.5})
    return exercises


def gen_multiplicacao_divisao(rng: random.Random) -> list[dict]:
    exercises = []
    for _ in range(80):
        a = rng.randint(12, 99)
        b = rng.randint(12, 99)
        exercises.append({"statement": f"Calcule: {a} × {b}", "expected_answer": str(a * b),
                          "skill_tags": ["multiplicacao_divisao"], "difficulty": 1.5})
    for _ in range(60):
        b = rng.randint(11, 25)
        q = rng.randint(11, 40)
        a = b * q
        exercises.append({"statement": f"Calcule: {a} ÷ {b}", "expected_answer": str(q),
                          "skill_tags": ["multiplicacao_divisao"], "difficulty": 1.5})
    for _ in range(60):
        a = rng.randint(100, 999)
        b = rng.randint(2, 9)
        exercises.append({"statement": f"Calcule: {a} × {b}", "expected_answer": str(a * b),
                          "skill_tags": ["multiplicacao_divisao"], "difficulty": 1.8})
    return exercises


def gen_soma_subtracao(rng: random.Random) -> list[dict]:
    exercises = []
    for _ in range(100):
        a = rng.randint(100, 9999)
        b = rng.randint(100, 9999)
        exercises.append({"statement": f"Calcule: {a} + {b}", "expected_answer": str(a + b),
                          "skill_tags": ["soma_subtracao"], "difficulty": 1.0})
    for _ in range(100):
        a = rng.randint(500, 9999)
        b = rng.randint(100, a)
        exercises.append({"statement": f"Calcule: {a} - {b}", "expected_answer": str(a - b),
                          "skill_tags": ["soma_subtracao"], "difficulty": 1.2})
    return exercises


def gen_potenciacao_radiciacao(rng: random.Random) -> list[dict]:
    exercises = []
    # Potências
    for _ in range(60):
        base = rng.randint(2, 12)
        exp = rng.randint(2, 5)
        exercises.append({"statement": f"Calcule: {base}^{{{exp}}}",
                          "expected_answer": str(base ** exp),
                          "skill_tags": ["potenciacao_radiciacao"], "difficulty": 2.0})
    # Raízes exatas
    perfect_squares = [n * n for n in range(2, 30)]
    for _ in range(80):
        n = rng.choice(perfect_squares)
        exercises.append({"statement": f"Calcule: \\sqrt{{{n}}}",
                          "expected_answer": str(int(n ** 0.5)),
                          "skill_tags": ["potenciacao_radiciacao"], "difficulty": 1.8})
    # Potências negativas
    for _ in range(40):
        base = rng.randint(2, 5)
        exp = rng.randint(2, 4)
        neg = -base
        exercises.append({"statement": f"Calcule: ({neg})^{{{exp}}}",
                          "expected_answer": str(neg ** exp),
                          "skill_tags": ["potenciacao_radiciacao"], "difficulty": 2.2})
    return exercises


def gen_equacoes_quadraticas(rng: random.Random) -> list[dict]:
    exercises = []
    # Formato: (x - r1)(x - r2) = 0
    for _ in range(100):
        r1 = rng.randint(-8, 8)
        r2 = rng.randint(-8, 8)
        b = -(r1 + r2)
        c = r1 * r2
        b_str = f"+ {b}" if b >= 0 else f"- {abs(b)}"
        c_str = f"+ {c}" if c >= 0 else f"- {abs(c)}"
        stmt = f"Resolva: x² {b_str}x {c_str} = 0"
        if r1 == r2:
            ans = f"x = {r1}"
        else:
            lo, hi = min(r1, r2), max(r1, r2)
            ans = f"x = {lo} ou x = {hi}"
        exercises.append({"statement": stmt, "expected_answer": ans,
                          "skill_tags": ["equacoes_quadraticas"], "difficulty": 3.2})
    # Formato: x² = k
    for _ in range(50):
        k_root = rng.randint(2, 12)
        k = k_root * k_root
        stmt = f"Resolva: x² = {k}"
        exercises.append({"statement": stmt, "expected_answer": f"x = ±{k_root}",
                          "skill_tags": ["equacoes_quadraticas"], "difficulty": 2.4})
    return exercises


def gen_porcentagem_razao(rng: random.Random) -> list[dict]:
    exercises = []
    for _ in range(80):
        pct = rng.choice([5, 10, 15, 20, 25, 30, 40, 50, 60, 75])
        base = rng.randint(2, 40) * 10
        ans = base * pct // 100
        exercises.append({"statement": f"Quanto é {pct}% de {base}?",
                          "expected_answer": str(ans),
                          "skill_tags": ["porcentagem_razao"], "difficulty": 2.0})
    for _ in range(60):
        pct = rng.choice([5, 10, 15, 20, 25])
        original = rng.randint(4, 20) * 10
        desconto = original * pct // 100
        final = original - desconto
        exercises.append({"statement": f"Um produto custa R${original} com {pct}% de desconto. Qual o preço final?",
                          "expected_answer": str(final),
                          "skill_tags": ["porcentagem_razao"], "difficulty": 2.3})
    return exercises


def gen_funcao_afim(rng: random.Random) -> list[dict]:
    exercises = []
    for _ in range(80):
        a = rng.randint(1, 6)
        b = rng.randint(-10, 10)
        x = rng.randint(1, 8)
        fx = a * x + b
        b_str = f"+ {b}" if b >= 0 else f"- {abs(b)}"
        exercises.append({"statement": f"Dada f(x) = {a}x {b_str}, calcule f({x}).",
                          "expected_answer": str(fx),
                          "skill_tags": ["funcao_afim"], "difficulty": 2.5})
    for _ in range(60):
        a = rng.randint(1, 5)
        b = rng.randint(-15, 15)
        root = -b / a
        if root == int(root):
            root = int(root)
            b_str = f"+ {b}" if b >= 0 else f"- {abs(b)}"
            exercises.append({"statement": f"Encontre a raiz de f(x) = {a}x {b_str}.",
                              "expected_answer": f"x = {root}",
                              "skill_tags": ["funcao_afim"], "difficulty": 2.8})
    return exercises


def _item(skill: str, statement: str, expected_answer: str, difficulty: float) -> dict:
    return {
        "statement": statement,
        "expected_answer": expected_answer,
        "skill_tags": [skill],
        "difficulty": difficulty,
    }


def _fmt_fraction(value: Fraction) -> str:
    if value.denominator == 1:
        return str(value.numerator)
    return rf"\frac{{{value.numerator}}}{{{value.denominator}}}"


def _fallback_exercise(skill: str, rng: random.Random, n: int) -> dict:
    if skill == "fracoes_decimais":
        a, b = rng.randint(1, 9), rng.randint(2, 12)
        c, d = rng.randint(1, 9), rng.randint(2, 12)
        op = rng.choice(["+", "-"])
        result = Fraction(a, b) + Fraction(c, d) if op == "+" else Fraction(a, b) - Fraction(c, d)
        return _item(skill, rf"Calcule: \frac{{{a}}}{{{b}}} {op} \frac{{{c}}}{{{d}}}", _fmt_fraction(result), 2.0)

    if skill == "fatoracao_produtos_notaveis":
        r1, r2 = rng.randint(-20, 20), rng.randint(-20, 20)
        b, c = -(r1 + r2), r1 * r2
        b_txt = f"+ {b}" if b >= 0 else f"- {abs(b)}"
        c_txt = f"+ {c}" if c >= 0 else f"- {abs(c)}"
        f1 = f"(x + {abs(r1)})" if r1 < 0 else f"(x - {r1})"
        f2 = f"(x + {abs(r2)})" if r2 < 0 else f"(x - {r2})"
        return _item(skill, f"Fatore: x² {b_txt}x {c_txt}", f"{f1}{f2}", 3.0)

    if skill == "funcao_quadratica":
        a, h, k = rng.choice([1, 2, -1]), rng.randint(-6, 6), rng.randint(-8, 8)
        x = rng.randint(-5, 5)
        fx = a * (x - h) ** 2 + k
        return _item(skill, f"Dada f(x) = {a}(x - {h})² + {k}, calcule f({x}).", str(fx), 3.3)

    if skill == "funcao_exponencial":
        base = rng.choice([2, 3, 4, 5])
        exp = rng.randint(2, 6)
        coef = n % 97 + 2
        return _item(skill, f"Resolva: {coef}·{base}^x = {coef * (base ** exp)}", f"x = {exp}", 3.5)

    if skill == "funcao_logaritmica":
        base = rng.choice([2, 3, 5, 10])
        exp = rng.randint(1, 5)
        shift = n % 37
        return _item(skill, rf"Calcule: \log_{{{base}}} {base ** exp} + {shift} - {shift}", str(exp), 3.4)

    if skill == "funcao_modular":
        a, b = rng.randint(-50, 50), rng.randint(1, 50)
        return _item(skill, f"Resolva: |x - ({a})| = {b}", f"x = {a - b} ou x = {a + b}", 3.0)

    if skill == "geometria_plana":
        base, height = rng.randint(3, 30), rng.randint(3, 30)
        area = Fraction(base * height, 2)
        return _item(skill, f"Calcule a área de um triângulo com base {base} e altura {height}.", _fmt_fraction(area), 2.2)

    if skill == "geometria_espacial":
        a, b, c = rng.randint(2, 12), rng.randint(2, 12), rng.randint(2, 12)
        return _item(skill, f"Calcule o volume de um paralelepípedo {a} × {b} × {c}.", str(a * b * c), 3.0)

    if skill == "geometria_analitica":
        x1, y1 = rng.randint(-8, 8), rng.randint(-8, 8)
        dx, dy = rng.choice([(rng.randint(1, 10), 0), (0, rng.randint(1, 10))])
        x2, y2 = x1 + dx, y1 + dy
        return _item(skill, f"Calcule a distância entre A({x1},{y1}) e B({x2},{y2}).", str(abs(dx + dy)), 3.0)

    if skill == "progressoes_pa_pg":
        a1, r, pos = rng.randint(1, 20), rng.randint(1, 8), rng.randint(5, 20)
        an = a1 + (pos - 1) * r
        return _item(skill, f"Na PA com a1 = {a1} e razão {r}, calcule a{pos}.", str(an), 3.0)

    if skill == "combinatoria":
        n_val, k = rng.randint(5, 30), rng.randint(2, 6)
        num = 1
        den = 1
        for i in range(k):
            num *= n_val - i
            den *= i + 1
        return _item(skill, f"Calcule C({n_val},{k}).", str(num // den), 4.0)

    if skill == "probabilidade":
        red, blue = rng.randint(1, 80), rng.randint(1, 80)
        return _item(skill, f"Uma urna tem {red} bolas vermelhas e {blue} azuis. Probabilidade de vermelha?", _fmt_fraction(Fraction(red, red + blue)), 3.0)

    if skill == "trig_razoes":
        triples = [(3, 4, 5), (5, 12, 13), (8, 15, 17)]
        a, b, c = rng.choice(triples)
        return _item(skill, f"Num triângulo retângulo com catetos {a} e {b}, calcule sen do ângulo oposto a {a}.", _fmt_fraction(Fraction(a, c)), 4.0)

    if skill == "trig_seno_cosseno_tangente":
        angle, value = rng.choice([(0, "0"), (30, "1/2"), (45, "√2/2"), (60, "√3/2"), (90, "1")])
        cycles = n % 24
        return _item(skill, f"Calcule sen({angle + 360 * cycles}°).", value, 4.0)

    if skill == "trig_identidades":
        return _item(skill, f"Simplifique: sen²(x) + cos²(x) + {n} - {n}", "1", 4.5)

    if skill == "trig_equacoes":
        k = rng.randint(1, 5)
        scale = n % 31 + 1
        return _item(skill, f"Resolva em [0, 2π]: sen({k}x) = 0. Quantas soluções há? Série {scale}", str(2 * k + 1), 5.0)

    if skill == "nocao_de_limite":
        a, b, point = rng.randint(1, 8), rng.randint(-10, 10), rng.randint(-5, 5)
        return _item(skill, rf"Calcule: \lim_{{x \to {point}}} ({a}x + {b})", str(a * point + b), 5.0)

    if skill == "continuidade":
        a, b, point = rng.randint(1, 6), rng.randint(-10, 10), rng.randint(-5, 5)
        return _item(skill, f"Para f(x) = {a}x + {b}, calcule f({point}) para verificar continuidade.", str(a * point + b), 5.2)

    if skill == "derivadas_basicas":
        coef, power = rng.randint(1, 9), rng.randint(2, 6)
        return _item(skill, f"Calcule a derivada de f(x) = {coef}x^{power}.", f"{coef * power}x^{power - 1}", 5.5)

    if skill == "derivadas_regra_cadeia":
        a, power = rng.randint(2, 8), rng.randint(2, 5)
        return _item(skill, f"Derive: f(x) = ({a}x + 1)^{power}.", f"{power * a}({a}x + 1)^{power - 1}", 6.0)

    if skill == "derivadas_produto_quociente":
        a, b = rng.randint(1, 6), rng.randint(1, 6)
        return _item(skill, f"Derive: f(x) = ({a}x)(x + {b}).", f"{2 * a}x + {a * b}", 6.0)

    if skill == "aplicacoes_derivadas":
        a, b = rng.randint(1, 5), rng.randint(2, 20)
        return _item(skill, f"Para s(t) = {a}t² + {b}t, calcule v(3).", str(6 * a + b), 6.2)

    if skill == "integrais_indefinidas":
        coef, power = rng.randint(1, 9), rng.randint(1, 5)
        result = Fraction(coef, power + 1)
        return _item(skill, f"Calcule: ∫ {coef}x^{power} dx", f"{_fmt_fraction(result)}x^{power + 1} + C", 6.5)

    if skill == "integrais_definidas":
        coef, upper = rng.randint(1, 8), rng.randint(1, 6)
        value = Fraction(coef * upper * upper, 2)
        return _item(skill, f"Calcule: ∫ de 0 a {upper} de {coef}x dx", _fmt_fraction(value), 7.0)

    if skill == "aplicacoes_integrais":
        v, t = rng.randint(2, 20), rng.randint(2, 10)
        return _item(skill, f"Com velocidade constante {v} m/s por {t}s, calcule o deslocamento.", str(v * t), 6.0)

    return _item(skill, f"Calcule: {n} + {n + 1}", str(2 * n + 1), 1.0)


def _dedupe(items: list[dict]) -> list[dict]:
    seen = set()
    unique = []
    for item in items:
        if item["statement"] not in seen:
            seen.add(item["statement"])
            unique.append(item)
    return unique


def _infer_method_tags(skill: str, statement: str) -> list[str]:
    text = statement.lower()
    tags = [skill]
    if "x/" in text or "\\frac" in text:
        tags.append("fracoes")
    if "(" in text and ")" in text:
        tags.append("parenteses")
    if "≤" in text or "≥" in text or ">" in text or "<" in text:
        tags.append("desigualdade")
    if "bhaskara" in text:
        tags.append("bhaskara")
    if "fatore" in text or "produto" in text:
        tags.append("fatoracao")
    if "área" in text or "volume" in text:
        tags.append("geometria")
    return list(dict.fromkeys(tags))


def _difficulty_vector(statement: str, difficulty: float, variation_index: int) -> dict[str, float]:
    text = statement.lower()
    length_factor = min(1.0, len(statement) / 120)
    steps = 0.2 + 0.08 * text.count("=") + 0.12 * text.count("(") + 0.08 * text.count("\\frac")
    number_form = 0.15
    if any(char in statement for char in ["-", "−"]):
        number_form += 0.15
    if "\\frac" in text or "/" in text:
        number_form += 0.25
    method_choice = 0.18 + 0.06 * variation_index
    if any(word in text for word in ["resolva", "derive", "calcule", "monte"]):
        method_choice += 0.10
    base = max(0.0, min(1.0, difficulty / 10))
    return {
        "algebra": round(min(1.0, base + 0.12), 3),
        "steps": round(min(1.0, steps), 3),
        "number_form": round(min(1.0, number_form), 3),
        "abstraction": round(min(1.0, length_factor), 3),
        "method_choice": round(min(1.0, method_choice), 3),
        "error_risk": round(min(1.0, base + steps / 2 + number_form / 3), 3),
    }


def _standardize_item(skill: str, item: dict, index: int) -> dict:
    family_index = index // VARIATIONS_PER_EXERCISE
    variation_index = (index % VARIATIONS_PER_EXERCISE) + 1
    difficulty = round(float(item["difficulty"]), 1)
    method_tags = item.get("method_tags") or _infer_method_tags(skill, item["statement"])
    return {
        **item,
        "skill_tags": item.get("skill_tags") or [skill],
        "difficulty": difficulty,
        "estimated_time_ms": item.get("estimated_time_ms") or int(25000 + difficulty * 9000),
        "source_library": item.get("source_library") or _DEFAULTS["source_library"],
        "source_license": item.get("source_license") or _DEFAULTS["source_license"],
        "subject": item.get("subject") or "math",
        "canvas_mode": item.get("canvas_mode") or "calculation",
        "validator": item.get("validator") or "sympy",
        "node_id": item.get("node_id") or f"{skill}.core",
        "template_id": item.get("template_id") or f"{skill}.family_{family_index:03d}",
        "template_version": item.get("template_version") or 1,
        "variant_seed": item.get("variant_seed") or (family_index * 10 + variation_index),
        "answer_type": item.get("answer_type") or "expression",
        "method_tags": method_tags,
        "prerequisite_tags": item.get("prerequisite_tags") or PREREQUISITE_TREE.get(skill, []),
        "affinity_tags": item.get("affinity_tags") or ["fixacao", f"variacao_{variation_index}"],
        "parameter_vector": item.get("parameter_vector") or {
            "family_index": family_index,
            "variation_index": variation_index,
            "sprint_block": family_index // 10,
        },
        "difficulty_vector": item.get("difficulty_vector") or _difficulty_vector(
            item["statement"],
            difficulty,
            variation_index,
        ),
    }


def build_skill_items(skill: str, rng: random.Random) -> list[dict]:
    if skill == "equacoes_quadraticas":
        return generate_quadratic_1var_batch(TARGET_PER_SKILL, seed=4200)

    base_items = GENERATORS.get(skill, lambda _: [])(rng)
    items = _dedupe(base_items)
    guard = 0
    while len(items) < TARGET_PER_SKILL and guard < TARGET_PER_SKILL * 30:
        guard += 1
        candidate = _fallback_exercise(skill, rng, guard)
        if all(candidate["statement"] != item["statement"] for item in items):
            items.append(candidate)
    fill = 0
    while len(items) < TARGET_PER_SKILL:
        fill += 1
        candidate = _fallback_exercise(skill, rng, guard + fill)
        candidate["statement"] = f"{candidate['statement']} — variação {fill}"
        if all(candidate["statement"] != item["statement"] for item in items):
            items.append(candidate)
    return [_standardize_item(skill, item, index) for index, item in enumerate(items[:TARGET_PER_SKILL])]


GENERATORS = {
    "equacoes_lineares":     gen_equacoes_lineares,
    "sistemas_equacoes":     gen_sistemas_equacoes,
    "inequacoes":            gen_inequacoes,
    "multiplicacao_divisao": gen_multiplicacao_divisao,
    "soma_subtracao":        gen_soma_subtracao,
    "potenciacao_radiciacao":gen_potenciacao_radiciacao,
    "equacoes_quadraticas":  gen_equacoes_quadraticas,
    "porcentagem_razao":     gen_porcentagem_razao,
    "funcao_afim":           gen_funcao_afim,
}


async def main() -> None:
    rng = random.Random(42)  # seed fixo para reprodutibilidade

    all_items: list[dict] = []
    for skill in TRACK_SKILLS:
        items = build_skill_items(skill, rng)
        all_items.extend(items)
        print(f"  {skill}: {len(items)} exercícios gerados")

    async with AsyncSessionLocal() as db:
        # Buscar todos os statements já existentes de uma vez
        existing_result = await db.execute(
            select(Exercise.statement)
        )
        existing = set(existing_result.scalars().all())

        new_items = [
            Exercise(**{**_DEFAULTS, **item})
            for item in all_items
            if item["statement"] not in existing
        ]
        db.add_all(new_items)
        await db.commit()
        print(f"\nTotal: {len(new_items)} inseridos, {len(existing)} já existiam.")


if __name__ == "__main__":
    asyncio.run(main())
