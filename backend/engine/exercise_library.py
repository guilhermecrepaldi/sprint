from __future__ import annotations

import random
from dataclasses import dataclass, field
from fractions import Fraction
from typing import Any


@dataclass(frozen=True)
class ExerciseNode:
    id: str
    subject: str
    title: str
    track: str
    prerequisites: list[str]
    target_hours: float
    target_attempts: int = 300


@dataclass(frozen=True)
class ExerciseTemplate:
    id: str
    node_id: str
    level: int
    method_tags: list[str]
    answer_type: str
    difficulty: float
    estimated_time_ms: int
    difficulty_vector: dict[str, float]
    affinity_tags: list[str] = field(default_factory=list)
    version: int = 1


@dataclass(frozen=True)
class GeneratedExercise:
    statement: str
    expected_answer: str
    skill_tags: list[str]
    difficulty: float
    estimated_time_ms: int
    source_library: str
    source_license: str
    subject: str
    canvas_mode: str
    validator: str
    node_id: str
    template_id: str
    template_version: int
    variant_seed: int
    answer_type: str
    method_tags: list[str]
    prerequisite_tags: list[str]
    affinity_tags: list[str]
    parameter_vector: dict[str, Any]
    difficulty_vector: dict[str, float]

    def to_dict(self) -> dict[str, Any]:
        return {
            "statement": self.statement,
            "expected_answer": self.expected_answer,
            "skill_tags": self.skill_tags,
            "difficulty": self.difficulty,
            "estimated_time_ms": self.estimated_time_ms,
            "source_library": self.source_library,
            "source_license": self.source_license,
            "subject": self.subject,
            "canvas_mode": self.canvas_mode,
            "validator": self.validator,
            "node_id": self.node_id,
            "template_id": self.template_id,
            "template_version": self.template_version,
            "variant_seed": self.variant_seed,
            "answer_type": self.answer_type,
            "method_tags": self.method_tags,
            "prerequisite_tags": self.prerequisite_tags,
            "affinity_tags": self.affinity_tags,
            "parameter_vector": self.parameter_vector,
            "difficulty_vector": self.difficulty_vector,
        }


QUADRATIC_1VAR_NODE = ExerciseNode(
    id="equacoes_quadraticas.1var",
    subject="math",
    title="Equacoes quadraticas com uma variavel",
    track="algebra",
    prerequisites=[
        "soma_subtracao",
        "multiplicacao_divisao",
        "fracoes_decimais",
        "potenciacao_radiciacao",
        "fatoracao_produtos_notaveis",
    ],
    target_hours=8.0,
)

VARIATIONS_PER_FAMILY = 3


QUADRATIC_1VAR_TEMPLATES: list[ExerciseTemplate] = [
    ExerciseTemplate(
        id="quadratic_1var_l01_square_root_exact",
        node_id=QUADRATIC_1VAR_NODE.id,
        level=1,
        method_tags=["raiz_quadrada"],
        answer_type="set",
        difficulty=2.4,
        estimated_time_ms=30000,
        difficulty_vector={
            "algebra": 0.25,
            "steps": 0.20,
            "number_form": 0.20,
            "abstraction": 0.10,
            "method_choice": 0.10,
            "error_risk": 0.20,
        },
        affinity_tags=["aquecimento", "raizes_simples"],
    ),
    ExerciseTemplate(
        id="quadratic_1var_l02_monic_positive_roots",
        node_id=QUADRATIC_1VAR_NODE.id,
        level=2,
        method_tags=["fatoracao", "vieta"],
        answer_type="set",
        difficulty=2.8,
        estimated_time_ms=40000,
        difficulty_vector={"algebra": 0.35, "steps": 0.30, "number_form": 0.25, "abstraction": 0.15, "method_choice": 0.25, "error_risk": 0.25},
    ),
    ExerciseTemplate(
        id="quadratic_1var_l03_monic_signed_roots",
        node_id=QUADRATIC_1VAR_NODE.id,
        level=3,
        method_tags=["fatoracao", "sinais"],
        answer_type="set",
        difficulty=3.2,
        estimated_time_ms=45000,
        difficulty_vector={"algebra": 0.45, "steps": 0.35, "number_form": 0.35, "abstraction": 0.20, "method_choice": 0.30, "error_risk": 0.45},
    ),
    ExerciseTemplate(
        id="quadratic_1var_l04_double_root",
        node_id=QUADRATIC_1VAR_NODE.id,
        level=4,
        method_tags=["fatoracao", "raiz_dupla"],
        answer_type="set",
        difficulty=3.4,
        estimated_time_ms=45000,
        difficulty_vector={"algebra": 0.45, "steps": 0.35, "number_form": 0.30, "abstraction": 0.35, "method_choice": 0.35, "error_risk": 0.35},
    ),
    ExerciseTemplate(
        id="quadratic_1var_l05_leading_coefficient",
        node_id=QUADRATIC_1VAR_NODE.id,
        level=5,
        method_tags=["fatoracao", "coeficiente_a"],
        answer_type="set",
        difficulty=3.8,
        estimated_time_ms=55000,
        difficulty_vector={"algebra": 0.55, "steps": 0.45, "number_form": 0.40, "abstraction": 0.30, "method_choice": 0.45, "error_risk": 0.50},
    ),
    ExerciseTemplate(
        id="quadratic_1var_l06_bhaskara_rational",
        node_id=QUADRATIC_1VAR_NODE.id,
        level=6,
        method_tags=["bhaskara", "discriminante"],
        answer_type="set",
        difficulty=4.2,
        estimated_time_ms=70000,
        difficulty_vector={"algebra": 0.60, "steps": 0.65, "number_form": 0.45, "abstraction": 0.35, "method_choice": 0.55, "error_risk": 0.60},
    ),
    ExerciseTemplate(
        id="quadratic_1var_l07_no_real_roots",
        node_id=QUADRATIC_1VAR_NODE.id,
        level=7,
        method_tags=["bhaskara", "discriminante_negativo"],
        answer_type="classification",
        difficulty=4.5,
        estimated_time_ms=65000,
        difficulty_vector={"algebra": 0.55, "steps": 0.60, "number_form": 0.45, "abstraction": 0.55, "method_choice": 0.60, "error_risk": 0.55},
        affinity_tags=["conceitual"],
    ),
    ExerciseTemplate(
        id="quadratic_1var_l08_complete_square",
        node_id=QUADRATIC_1VAR_NODE.id,
        level=8,
        method_tags=["completar_quadrado"],
        answer_type="set",
        difficulty=4.8,
        estimated_time_ms=80000,
        difficulty_vector={"algebra": 0.70, "steps": 0.75, "number_form": 0.55, "abstraction": 0.60, "method_choice": 0.70, "error_risk": 0.65},
    ),
    ExerciseTemplate(
        id="quadratic_1var_l09_vieta_inverse",
        node_id=QUADRATIC_1VAR_NODE.id,
        level=9,
        method_tags=["vieta", "modelagem"],
        answer_type="equation",
        difficulty=5.0,
        estimated_time_ms=70000,
        difficulty_vector={"algebra": 0.65, "steps": 0.55, "number_form": 0.45, "abstraction": 0.75, "method_choice": 0.75, "error_risk": 0.60},
    ),
    ExerciseTemplate(
        id="quadratic_1var_l10_contextual_model",
        node_id=QUADRATIC_1VAR_NODE.id,
        level=10,
        method_tags=["modelagem", "fatoracao"],
        answer_type="number",
        difficulty=5.4,
        estimated_time_ms=90000,
        difficulty_vector={"algebra": 0.70, "steps": 0.75, "number_form": 0.45, "abstraction": 0.85, "method_choice": 0.80, "error_risk": 0.70},
        affinity_tags=["leitura"],
    ),
]


def generate_quadratic_1var(level: int, seed: int) -> GeneratedExercise:
    template = QUADRATIC_1VAR_TEMPLATES[(level - 1) % len(QUADRATIC_1VAR_TEMPLATES)]
    rng = random.Random(seed)

    if template.level == 1:
        root = rng.randint(2, 120)
        k = root * root
        statement = f"Resolva: x² = {k}"
        expected = f"x = -{root} ou x = {root}"
        params = {"k": k, "roots": [-root, root]}
    elif template.level == 2:
        r1, r2 = sorted(rng.sample(range(1, 40), 2))
        b = -(r1 + r2)
        c = r1 * r2
        statement = f"Resolva: x² {_signed_term(b, 'x')} {_signed_number(c)} = 0"
        expected = f"x = {r1} ou x = {r2}"
        params = {"a": 1, "b": b, "c": c, "roots": [r1, r2]}
    elif template.level == 3:
        r1 = rng.randint(-30, -1)
        r2 = rng.randint(1, 30)
        lo, hi = sorted([r1, r2])
        b = -(r1 + r2)
        c = r1 * r2
        statement = f"Resolva: x² {_signed_term(b, 'x')} {_signed_number(c)} = 0"
        expected = f"x = {lo} ou x = {hi}"
        params = {"a": 1, "b": b, "c": c, "roots": [lo, hi]}
    elif template.level == 4:
        root = rng.randint(-60, 60)
        b = -2 * root
        c = root * root
        statement = f"Resolva: x² {_signed_term(b, 'x')} {_signed_number(c)} = 0"
        expected = f"x = {root}"
        params = {"a": 1, "b": b, "c": c, "roots": [root], "multiplicity": 2}
    elif template.level == 5:
        a = rng.randint(2, 5)
        r1, r2 = sorted(rng.sample(range(-8, 9), 2))
        b = -a * (r1 + r2)
        c = a * r1 * r2
        statement = f"Resolva: {a}x² {_signed_term(b, 'x')} {_signed_number(c)} = 0"
        expected = f"x = {r1} ou x = {r2}"
        params = {"a": a, "b": b, "c": c, "roots": [r1, r2]}
    elif template.level == 6:
        a = rng.randint(2, 6)
        r1_num = rng.randint(-8, -1)
        r2_num = a * rng.randint(1, 8)
        r1 = Fraction(r1_num, a)
        r2 = Fraction(r2_num, a)
        b = -a * (r1 + r2)
        c = a * r1 * r2
        statement = f"Resolva por Bhaskara: {a}x² {_signed_term(int(b), 'x')} {_signed_number(int(c))} = 0"
        expected = f"x = {_fmt_fraction(r1)} ou x = {_fmt_fraction(r2)}"
        params = {"a": a, "b": int(b), "c": int(c), "roots": [_fmt_fraction(r1), _fmt_fraction(r2)]}
    elif template.level == 7:
        a = rng.randint(1, 5)
        h = rng.randint(-6, 6)
        k = rng.randint(1, 12)
        b = -2 * a * h
        c = a * h * h + k
        statement = f"Resolva nos reais: {a}x² {_signed_term(b, 'x')} {_signed_number(c)} = 0"
        expected = "sem raízes reais"
        params = {"a": a, "b": b, "c": c, "discriminant": b * b - 4 * a * c}
    elif template.level == 8:
        h = rng.randint(-40, 40)
        radius = rng.randint(2, 30)
        statement = f"Resolva completando quadrado: (x {_signed_number(-h)})² = {radius * radius}"
        roots = sorted([h - radius, h + radius])
        expected = f"x = {roots[0]} ou x = {roots[1]}"
        params = {"center": h, "radius": radius, "roots": roots}
    elif template.level == 9:
        r1, r2 = sorted(rng.sample(range(-40, 41), 2))
        s = r1 + r2
        p = r1 * r2
        statement = f"Monte a equação monica de 2º grau cujas raízes têm soma {s} e produto {p}."
        expected = f"x² {_signed_term(-s, 'x')} {_signed_number(p)} = 0"
        params = {"root_sum": s, "root_product": p, "roots": [r1, r2]}
    else:
        width = rng.randint(3, 60)
        height = rng.randint(3, 60)
        area = width * height
        perimeter_half = width + height
        statement = (
            f"Um retângulo tem área {area} e lados x e {perimeter_half} - x. "
            "Encontre o menor lado."
        )
        expected = str(min(width, height))
        params = {"area": area, "sum_of_sides": perimeter_half, "roots": sorted([width, height])}

    return GeneratedExercise(
        statement=statement,
        expected_answer=expected,
        skill_tags=["equacoes_quadraticas"],
        difficulty=template.difficulty,
        estimated_time_ms=template.estimated_time_ms,
        source_library="sprint_parametric_v2",
        source_license="proprietary_generated",
        subject="math",
        canvas_mode="calculation",
        validator="sympy",
        node_id=template.node_id,
        template_id=template.id,
        template_version=template.version,
        variant_seed=seed,
        answer_type=template.answer_type,
        method_tags=template.method_tags,
        prerequisite_tags=QUADRATIC_1VAR_NODE.prerequisites,
        affinity_tags=template.affinity_tags,
        parameter_vector=params | {"level": template.level},
        difficulty_vector=template.difficulty_vector,
    )


def generate_quadratic_1var_batch(count: int = 300, seed: int = 42) -> list[dict[str, Any]]:
    items: list[dict[str, Any]] = []
    seen: set[str] = set()
    family_index = 0
    while len(items) < count:
        level = (family_index % len(QUADRATIC_1VAR_TEMPLATES)) + 1
        variation_index = 1
        collision_offset = 0
        while variation_index <= VARIATIONS_PER_FAMILY and len(items) < count:
            candidate_seed = seed + family_index * 97 + variation_index + collision_offset * 100_000
            candidate = generate_quadratic_1var(level=level, seed=candidate_seed).to_dict()
            if candidate["statement"] in seen:
                collision_offset += 1
                continue
            candidate["parameter_vector"] = {
                **candidate.get("parameter_vector", {}),
                "family_index": family_index,
                "variation_index": variation_index,
                "sprint_block": family_index // 10,
            }
            candidate["affinity_tags"] = list(dict.fromkeys(
                candidate.get("affinity_tags", []) + ["fixacao", f"variacao_{variation_index}"]
            ))
            seen.add(candidate["statement"])
            items.append(candidate)
            variation_index += 1
        family_index += 1
    return items


def build_attempt_features(total_time_ms: int | None, exercise: Any, erase_count: int, pause_count: int) -> dict[str, Any]:
    expected = getattr(exercise, "estimated_time_ms", None) or 45000
    elapsed = total_time_ms or expected
    return {
        "time_ratio": round(elapsed / max(expected, 1), 3),
        "erase_count": erase_count,
        "pause_count": pause_count,
        "slow": elapsed > expected * 1.8,
        "high_friction": erase_count >= 3 or pause_count >= 3,
    }


def light_intervention_signal(
    *,
    attempt_count: int,
    is_correct: bool,
    error_type: str | None,
    method_tags: list[str] | None,
    features: dict[str, Any],
) -> str | None:
    if is_correct:
        return None
    if attempt_count < 5:
        return None
    if features.get("time_ratio", 0.0) < 1.8 and not features.get("high_friction"):
        return None

    tags = set(method_tags or [])
    if "sinais" in tags or error_type == "sinal":
        return "observe o sinal antes de isolar a resposta"
    if "discriminante" in tags:
        return "calcule o discriminante com calma antes das raizes"
    if "fatoracao" in tags:
        return "procure dois numeros com soma e produto corretos"
    if "completar_quadrado" in tags:
        return "mantenha o mesmo valor nos dois lados ao completar quadrado"
    return "volte um passo e refaca a transformacao principal"


def _fmt_fraction(value: Fraction) -> str:
    value = Fraction(value)
    if value.denominator == 1:
        return str(value.numerator)
    return rf"\frac{{{value.numerator}}}{{{value.denominator}}}"


def _signed_number(value: int) -> str:
    return f"+ {value}" if value >= 0 else f"- {abs(value)}"


def _signed_term(value: int, symbol: str) -> str:
    if value == 0:
        return "+ 0" + symbol
    if value == 1:
        return f"+ {symbol}"
    if value == -1:
        return f"- {symbol}"
    return f"+ {value}{symbol}" if value > 0 else f"- {abs(value)}{symbol}"
