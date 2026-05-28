"""
Seed aditivo para ampliar funcao modular e trigonometria.

Os exercicios sao gerados localmente por familias parametrizadas, sem copiar
enunciados de bancos externos. As referencias abertas servem como mapa de
topicos: valor absoluto, razoes no triangulo retangulo, ciclo trigonometrico,
identidades e equacoes trigonometricas.

Uso:
    python seed/expand_modular_trig.py
    python seed/expand_modular_trig.py --count 400
    python seed/expand_modular_trig.py --dry-run
"""

from __future__ import annotations

import argparse
import asyncio
import math
import random
import sys
from fractions import Fraction
from pathlib import Path

sys.path.append(str(Path(__file__).resolve().parents[1]))

from sqlalchemy import select

from db import AsyncSessionLocal
from engine.unlock import PREREQUISITE_TREE
from models.exercise import Exercise

SOURCE_LIBRARY = "sprint_parametric_modular_trig_v1"
SOURCE_LICENSE = "generated_from_open_curriculum_map"
DEFAULT_COUNT_PER_SKILL = 250
SKILLS = [
    "funcao_modular",
    "trig_razoes",
    "trig_seno_cosseno_tangente",
    "trig_identidades",
    "trig_equacoes",
]


def frac(value: Fraction) -> str:
    if value.denominator == 1:
        return str(value.numerator)
    return rf"\frac{{{value.numerator}}}{{{value.denominator}}}"


def signed(n: int) -> str:
    return f"+ {n}" if n >= 0 else f"- {abs(n)}"


def abs_linear_text(a: int, b: int) -> str:
    if a == 1:
        core = "x"
    elif a == -1:
        core = "-x"
    else:
        core = f"{a}x"
    if b == 0:
        return core
    return f"{core} {signed(b)}"


def item(
    skill: str,
    statement: str,
    expected_answer: str,
    difficulty: float,
    family: str,
    index: int,
    answer_type: str = "expression",
    method_tags: list[str] | None = None,
    params: dict | None = None,
) -> dict:
    block = index // 10
    variation = index % 10
    tags = method_tags or [skill, family]
    difficulty = round(difficulty, 1)
    return {
        "statement": statement,
        "expected_answer": expected_answer,
        "skill_tags": [skill],
        "difficulty": difficulty,
        "estimated_time_ms": int(24000 + difficulty * 8500),
        "source_library": SOURCE_LIBRARY,
        "source_license": SOURCE_LICENSE,
        "subject": "math",
        "canvas_mode": "calculation",
        "validator": "sympy",
        "node_id": f"{skill}.{family}",
        "template_id": f"{skill}.{family}",
        "template_version": 1,
        "variant_seed": index + 1,
        "answer_type": answer_type,
        "method_tags": tags,
        "prerequisite_tags": PREREQUISITE_TREE.get(skill, []),
        "affinity_tags": ["fixacao", family, f"bloco_{block}", f"variacao_{variation}"],
        "parameter_vector": {"family": family, "index": index, **(params or {})},
        "difficulty_vector": {
            "algebra": min(1.0, difficulty / 10 + 0.18),
            "steps": min(1.0, 0.25 + difficulty / 16),
            "number_form": min(1.0, 0.20 + difficulty / 18),
            "abstraction": min(1.0, 0.20 + difficulty / 14),
            "method_choice": min(1.0, 0.22 + difficulty / 15),
            "error_risk": min(1.0, 0.26 + difficulty / 13),
        },
    }


def dedupe(items: list[dict]) -> list[dict]:
    out: list[dict] = []
    seen: set[str] = set()
    for ex in items:
        if ex["statement"] in seen:
            continue
        seen.add(ex["statement"])
        out.append(ex)
    return out


def gen_funcao_modular(count: int, rng: random.Random) -> list[dict]:
    items: list[dict] = []
    i = 0

    for _ in range(count * 2):
        h = rng.randint(-30, 30)
        r = rng.randint(1, 30)
        items.append(item(
            "funcao_modular",
            f"Resolva: |x {signed(-h)}| = {r}",
            f"x = {h - r} ou x = {h + r}",
            3.0 + min(1.2, abs(h) / 40),
            "abs_eq_shift",
            i,
            method_tags=["funcao_modular", "valor_absoluto", "duas_solucoes"],
            params={"h": h, "r": r},
        ))
        i += 1

        a = rng.choice([2, 3, 4, 5, -2, -3, -4])
        root = rng.randint(-10, 10)
        b = rng.randint(-25, 25)
        c = abs(a * root + b)
        if c == 0:
            continue
        left = abs_linear_text(a, b)
        sol1 = Fraction(c - b, a)
        sol2 = Fraction(-c - b, a)
        ans = sorted({frac(sol1), frac(sol2)})
        items.append(item(
            "funcao_modular",
            f"Resolva: |{left}| = {c}",
            "x = " + " ou x = ".join(ans),
            3.6 + min(1.0, abs(a) / 8),
            "abs_eq_linear",
            i,
            method_tags=["funcao_modular", "valor_absoluto", "linear"],
            params={"a": a, "b": b, "c": c},
        ))
        i += 1

        h2 = rng.randint(-12, 12)
        r2 = rng.randint(1, 14)
        op = rng.choice(["<", "<=", ">", ">="])
        if op in ("<", "<="):
            ans2 = f"{h2 - r2} {op} x {op} {h2 + r2}"
        elif op == ">":
            ans2 = f"x > {h2 + r2} ou x < {h2 - r2}"
        else:
            ans2 = f"x >= {h2 + r2} ou x <= {h2 - r2}"
        items.append(item(
            "funcao_modular",
            f"Resolva: |x {signed(-h2)}| {op} {r2}",
            ans2,
            4.0 if op in ("<", "<=") else 4.4,
            "abs_ineq_interval",
            i,
            answer_type="interval",
            method_tags=["funcao_modular", "inequacao_modular"],
            params={"h": h2, "r": r2, "op": op},
        ))
        i += 1

        a3 = rng.choice([1, 2, -1, -2])
        h3 = rng.randint(-8, 8)
        k3 = rng.randint(-10, 10)
        x0 = rng.randint(-10, 10)
        fx = abs(a3 * (x0 - h3)) + k3
        body = f"{a3}(x {signed(-h3)})" if a3 != 1 else f"x {signed(-h3)}"
        items.append(item(
            "funcao_modular",
            f"Dada f(x) = |{body}| {signed(k3)}, calcule f({x0}).",
            str(fx),
            2.8,
            "abs_eval",
            i,
            method_tags=["funcao_modular", "avaliacao"],
            params={"a": a3, "h": h3, "k": k3, "x": x0},
        ))
        i += 1

        if len(dedupe(items)) >= count:
            break

    return dedupe(items)[:count]


def gen_trig_razoes(count: int, rng: random.Random) -> list[dict]:
    triples = [(3, 4, 5), (5, 12, 13), (8, 15, 17), (7, 24, 25), (9, 40, 41)]
    notable = [30, 45, 60]
    items: list[dict] = []
    i = 0

    for scale in range(1, 80):
        for a, b, c in triples:
            aa, bb, cc = a * scale, b * scale, c * scale
            for opposite, adjacent in [(aa, bb), (bb, aa)]:
                items.append(item(
                    "trig_razoes",
                    f"Em um triangulo retangulo com catetos {opposite} e {adjacent} e hipotenusa {cc}, calcule sen do angulo oposto ao cateto {opposite}.",
                    frac(Fraction(opposite, cc)),
                    3.2 + min(1.2, scale / 40),
                    "right_triangle_sine",
                    i,
                    method_tags=["trig_razoes", "seno", "triangulo_retangulo"],
                    params={"oposto": opposite, "adjacente": adjacent, "hipotenusa": cc},
                ))
                i += 1
                items.append(item(
                    "trig_razoes",
                    f"Em um triangulo retangulo com catetos {opposite} e {adjacent} e hipotenusa {cc}, calcule cos do angulo oposto ao cateto {opposite}.",
                    frac(Fraction(adjacent, cc)),
                    3.3 + min(1.2, scale / 40),
                    "right_triangle_cosine",
                    i,
                    method_tags=["trig_razoes", "cosseno", "triangulo_retangulo"],
                    params={"oposto": opposite, "adjacente": adjacent, "hipotenusa": cc},
                ))
                i += 1
                items.append(item(
                    "trig_razoes",
                    f"No triangulo retangulo, o cateto oposto mede {opposite} e o adjacente mede {adjacent}. Calcule tg do angulo.",
                    frac(Fraction(opposite, adjacent)),
                    3.5 + min(1.2, scale / 40),
                    "right_triangle_tangent",
                    i,
                    method_tags=["trig_razoes", "tangente", "triangulo_retangulo"],
                    params={"oposto": opposite, "adjacente": adjacent},
                ))
                i += 1
            if len(dedupe(items)) >= count:
                return dedupe(items)[:count]

    for hyp in range(4, 220, 2):
        for angle in notable:
            if angle == 30:
                ans = frac(Fraction(hyp, 2))
            elif angle == 45:
                ans = rf"{hyp}\sqrt{{2}}/2"
            else:
                ans = rf"{hyp}\sqrt{{3}}/2"
            items.append(item(
                "trig_razoes",
                f"A hipotenusa mede {hyp} e o angulo e {angle} graus. Calcule o cateto oposto.",
                ans,
                4.0,
                "notable_angle_side",
                i,
                method_tags=["trig_razoes", "angulo_notavel", "lado_desconhecido"],
                params={"angulo": angle, "hipotenusa": hyp},
            ))
            i += 1
            if len(dedupe(items)) >= count:
                return dedupe(items)[:count]

    return dedupe(items)[:count]


TRIG_VALUES = {
    0: ("0", "1", "0"),
    30: (r"\frac{1}{2}", r"\frac{\sqrt{3}}{2}", r"\frac{\sqrt{3}}{3}"),
    45: (r"\frac{\sqrt{2}}{2}", r"\frac{\sqrt{2}}{2}", "1"),
    60: (r"\frac{\sqrt{3}}{2}", r"\frac{1}{2}", r"\sqrt{3}"),
    90: ("1", "0", "indefinida"),
    120: (r"\frac{\sqrt{3}}{2}", r"-\frac{1}{2}", r"-\sqrt{3}"),
    135: (r"\frac{\sqrt{2}}{2}", r"-\frac{\sqrt{2}}{2}", "-1"),
    150: (r"\frac{1}{2}", r"-\frac{\sqrt{3}}{2}", r"-\frac{\sqrt{3}}{3}"),
    180: ("0", "-1", "0"),
    210: (r"-\frac{1}{2}", r"-\frac{\sqrt{3}}{2}", r"\frac{\sqrt{3}}{3}"),
    225: (r"-\frac{\sqrt{2}}{2}", r"-\frac{\sqrt{2}}{2}", "1"),
    240: (r"-\frac{\sqrt{3}}{2}", r"-\frac{1}{2}", r"\sqrt{3}"),
    270: ("-1", "0", "indefinida"),
    300: (r"-\frac{\sqrt{3}}{2}", r"\frac{1}{2}", r"-\sqrt{3}"),
    315: (r"-\frac{\sqrt{2}}{2}", r"\frac{\sqrt{2}}{2}", "-1"),
    330: (r"-\frac{1}{2}", r"\frac{\sqrt{3}}{2}", r"-\frac{\sqrt{3}}{3}"),
    360: ("0", "1", "0"),
}


def gen_trig_sct(count: int, rng: random.Random) -> list[dict]:
    items: list[dict] = []
    angles = list(TRIG_VALUES)
    funcs = [("sen", 0), ("cos", 1), ("tg", 2)]
    i = 0
    for _ in range(count * 3):
        angle = rng.choice(angles)
        cycles = rng.randint(0, 5)
        fn, idx = rng.choice(funcs)
        shown = angle + 360 * cycles
        items.append(item(
            "trig_seno_cosseno_tangente",
            f"Calcule {fn}({shown} graus).",
            TRIG_VALUES[angle][idx],
            3.7 + min(1.2, cycles * 0.15),
            "unit_circle_value",
            i,
            method_tags=["trig_seno_cosseno_tangente", fn, "ciclo_trigonometrico"],
            params={"funcao": fn, "angulo": shown},
        ))
        i += 1

        q_angle = rng.choice([35, 80, 110, 160, 200, 250, 290, 340])
        if q_angle < 90:
            signs = "sen positivo, cos positivo"
        elif q_angle < 180:
            signs = "sen positivo, cos negativo"
        elif q_angle < 270:
            signs = "sen negativo, cos negativo"
        else:
            signs = "sen negativo, cos positivo"
        items.append(item(
            "trig_seno_cosseno_tangente",
            f"Determine os sinais de sen({q_angle} graus) e cos({q_angle} graus).",
            signs,
            4.1,
            "quadrant_sign",
            i,
            answer_type="text",
            method_tags=["trig_seno_cosseno_tangente", "quadrantes", "sinais"],
            params={"angulo": q_angle},
        ))
        i += 1

        if len(dedupe(items)) >= count:
            break
    return dedupe(items)[:count]


def gen_trig_identidades(count: int, rng: random.Random) -> list[dict]:
    templates = [
        ("Simplifique: sen^2(x) + cos^2(x)", "1", 4.2, "pythagorean"),
        ("Simplifique: 1 - sen^2(x)", "cos^2(x)", 4.4, "pythagorean"),
        ("Simplifique: 1 - cos^2(x)", "sen^2(x)", 4.4, "pythagorean"),
        ("Simplifique: sen(x)/cos(x)", "tg(x)", 4.5, "quotient"),
        ("Simplifique: 1 + tg^2(x)", "sec^2(x)", 5.1, "tangent_identity"),
        ("Simplifique: sec^2(x) - tg^2(x)", "1", 5.2, "tangent_identity"),
        ("Simplifique: cos(x)tg(x)", "sen(x)", 4.8, "quotient"),
        ("Simplifique: sen(x)cotg(x)", "cos(x)", 5.0, "quotient"),
    ]
    items: list[dict] = []
    i = 0
    for _ in range(count * 2):
        stmt, ans, difficulty, family = rng.choice(templates)
        n = rng.randint(1, 999)
        noisy_stmt = f"{stmt} + {n} - {n}"
        items.append(item(
            "trig_identidades",
            noisy_stmt,
            ans,
            difficulty,
            family,
            i,
            method_tags=["trig_identidades", family, "simplificacao"],
            params={"offset": n},
        ))
        i += 1

        a = rng.randint(2, 9)
        items.append(item(
            "trig_identidades",
            f"Simplifique: {a}sen^2(x) + {a}cos^2(x)",
            str(a),
            4.6,
            "scaled_pythagorean",
            i,
            method_tags=["trig_identidades", "pitagorica", "coeficiente"],
            params={"coef": a},
        ))
        i += 1

        if len(dedupe(items)) >= count:
            break
    return dedupe(items)[:count]


def gen_trig_equacoes(count: int, rng: random.Random) -> list[dict]:
    items: list[dict] = []
    sin_solutions = {
        "0": "x = 0, pi",
        r"\frac{1}{2}": r"x = \frac{\pi}{6}, \frac{5\pi}{6}",
        r"\frac{\sqrt{2}}{2}": r"x = \frac{\pi}{4}, \frac{3\pi}{4}",
        r"\frac{\sqrt{3}}{2}": r"x = \frac{\pi}{3}, \frac{2\pi}{3}",
        "1": r"x = \frac{\pi}{2}",
        "-1": r"x = \frac{3\pi}{2}",
    }
    cos_solutions = {
        "0": r"x = \frac{\pi}{2}, \frac{3\pi}{2}",
        r"\frac{1}{2}": r"x = \frac{\pi}{3}, \frac{5\pi}{3}",
        r"\frac{\sqrt{2}}{2}": r"x = \frac{\pi}{4}, \frac{7\pi}{4}",
        r"\frac{\sqrt{3}}{2}": r"x = \frac{\pi}{6}, \frac{11\pi}{6}",
        "1": "x = 0",
        "-1": "x = pi",
    }
    tan_solutions = {
        "0": "x = 0, pi",
        "1": r"x = \frac{\pi}{4}, \frac{5\pi}{4}",
        "-1": r"x = \frac{3\pi}{4}, \frac{7\pi}{4}",
        r"\sqrt{3}": r"x = \frac{\pi}{3}, \frac{4\pi}{3}",
        r"-\sqrt{3}": r"x = \frac{2\pi}{3}, \frac{5\pi}{3}",
    }
    i = 0
    for _ in range(count * 3):
        value, ans = rng.choice(list(sin_solutions.items()))
        items.append(item(
            "trig_equacoes",
            f"Resolva em [0, 2pi): sen(x) = {value}.",
            ans,
            4.8,
            "sin_basic_interval",
            i,
            answer_type="set",
            method_tags=["trig_equacoes", "seno", "intervalo_0_2pi"],
            params={"valor": value},
        ))
        i += 1

        value, ans = rng.choice(list(cos_solutions.items()))
        items.append(item(
            "trig_equacoes",
            f"Resolva em [0, 2pi): cos(x) = {value}.",
            ans,
            4.9,
            "cos_basic_interval",
            i,
            answer_type="set",
            method_tags=["trig_equacoes", "cosseno", "intervalo_0_2pi"],
            params={"valor": value},
        ))
        i += 1

        value, ans = rng.choice(list(tan_solutions.items()))
        items.append(item(
            "trig_equacoes",
            f"Resolva em [0, 2pi): tg(x) = {value}.",
            ans,
            5.2,
            "tan_basic_interval",
            i,
            answer_type="set",
            method_tags=["trig_equacoes", "tangente", "intervalo_0_2pi"],
            params={"valor": value},
        ))
        i += 1

        k = rng.randint(2, 5)
        items.append(item(
            "trig_equacoes",
            f"Em [0, 2pi), quantas solucoes tem sen({k}x) = 0?",
            str(2 * k),
            5.6,
            "sin_kx_count",
            i,
            answer_type="integer",
            method_tags=["trig_equacoes", "periodicidade", "contagem"],
            params={"k": k},
        ))
        i += 1

        if len(dedupe(items)) >= count:
            break

    for interval, multiplier in [("[0, 2pi)", 2), ("[0, pi)", 1), ("[0, 4pi)", 4)]:
        for k in range(1, 120):
            for fn in ["sen", "cos", "tg"]:
                items.append(item(
                    "trig_equacoes",
                    f"Em {interval}, quantas solucoes tem {fn}({k}x) = 0?",
                    str(multiplier * k),
                    5.0 + min(1.5, k / 90),
                    f"{fn}_kx_count",
                    i,
                    answer_type="integer",
                    method_tags=["trig_equacoes", fn, "periodicidade", "contagem"],
                    params={"k": k, "intervalo": interval},
                ))
                i += 1
                if len(dedupe(items)) >= count:
                    return dedupe(items)[:count]
    return dedupe(items)[:count]


GENERATORS = {
    "funcao_modular": gen_funcao_modular,
    "trig_razoes": gen_trig_razoes,
    "trig_seno_cosseno_tangente": gen_trig_sct,
    "trig_identidades": gen_trig_identidades,
    "trig_equacoes": gen_trig_equacoes,
}


async def insert_items(items: list[dict], dry_run: bool) -> int:
    async with AsyncSessionLocal() as db:
        statements = [it["statement"] for it in items]
        existing = await db.execute(select(Exercise.statement).where(Exercise.statement.in_(statements)))
        existing_set = set(existing.scalars().all())
        new_items = [Exercise(**it) for it in items if it["statement"] not in existing_set]
        if dry_run:
            for sample in new_items[:12]:
                print(f"- [{sample.skill_tags[0]}] {sample.statement} -> {sample.expected_answer}")
            print(f"Dry-run: {len(new_items)} novos, {len(existing_set)} ja existentes.")
            return len(new_items)
        db.add_all(new_items)
        await db.commit()
        return len(new_items)


async def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--count", type=int, default=DEFAULT_COUNT_PER_SKILL)
    parser.add_argument("--dry-run", action="store_true")
    parser.add_argument("--skills", default=",".join(SKILLS))
    args = parser.parse_args()

    rng = random.Random(20260527)
    selected = [skill.strip() for skill in args.skills.split(",") if skill.strip()]
    all_items: list[dict] = []
    for skill in selected:
        if skill not in GENERATORS:
            raise SystemExit(f"Skill sem gerador: {skill}")
        generated = GENERATORS[skill](args.count, rng)
        all_items.extend(generated)
        print(f"{skill}: {len(generated)} gerados")

    inserted = await insert_items(all_items, args.dry_run)
    action = "novos em dry-run" if args.dry_run else "inseridos"
    print(f"Total: {inserted} {action}.")


if __name__ == "__main__":
    asyncio.run(main())
