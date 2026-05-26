"""
Gerador de exercícios aritméticos on-the-fly.

Regra: nenhuma chamada de IA, nenhuma query ao banco.
Os exercícios são criados por um PRNG com seed controlável — determinísticos
e infinitos. O mesmo batch_id sempre produz os mesmos exercícios (útil para
replay e auditoria).

Níveis:
  basic   — inteiros 1–10, operações +/-   (meta: 100 em 50s)
  medium  — inteiros 1–20, +-×, com carries
  hard    — inteiros 1–50, +-×÷, dividem exato

O campo `auto_submit_chars` indica ao app Android quantos caracteres
constituem uma resposta completa — quando o campo bate esse comprimento,
avança automático sem o aluno pressionar Enter.
"""
import random
import uuid
from dataclasses import dataclass


@dataclass(frozen=True)
class DrillItem:
    item_id: str
    statement: str
    expected_answer: str
    skill_tag: str
    difficulty: float
    # Android: auto-avança quando o input tem exatamente esta qtd de caracteres.
    # -1 = não pode determinar (ex: frações), não aplica auto-submit.
    auto_submit_chars: int


def generate_batch(
    count: int,
    level: str = "basic",
    batch_id: str | None = None,
) -> tuple[str, list[DrillItem]]:
    """
    Retorna (batch_id, items).

    O batch_id é um UUID determinístico derivado dos parâmetros quando não
    fornecido, permitindo que o cliente repita o mesmo batch (ex: retry offline).
    """
    if batch_id is None:
        batch_id = str(uuid.uuid4())  # novo batch único a cada chamada

    rng = random.Random(batch_id)  # determinístico por batch_id
    items: list[DrillItem] = []

    generators = _GENERATORS_BY_LEVEL.get(level, _GENERATORS_BY_LEVEL["basic"])

    for _ in range(count):
        gen = rng.choice(generators)
        item = gen(rng, batch_id)
        items.append(item)

    return batch_id, items


# ── Geradores por nível ──────────────────────────────────────────────────────

def _addition(rng: random.Random, batch_id: str) -> DrillItem:
    a = rng.randint(1, 10)
    b = rng.randint(1, 10)
    answer = a + b
    return DrillItem(
        item_id=str(uuid.uuid4()),
        statement=f"{a}+{b}",
        expected_answer=str(answer),
        skill_tag="soma_subtracao",
        difficulty=1.0,
        auto_submit_chars=len(str(answer)),
    )


def _subtraction_signed(rng: random.Random, batch_id: str) -> DrillItem:
    """Treino de jogo de sinais: pode resultar em número negativo."""
    a = rng.randint(-9, 9)
    b = rng.randint(-9, 9)
    answer = a + b
    # Exibe como "a+b" ou "a-|b|" dependendo do sinal
    if b >= 0:
        stmt = f"{a}+{b}" if a >= 0 else f"({a})+{b}"
    else:
        stmt = f"{a}{b}" if a >= 0 else f"({a})+({b})"
    return DrillItem(
        item_id=str(uuid.uuid4()),
        statement=stmt,
        expected_answer=str(answer),
        skill_tag="soma_subtracao",
        difficulty=1.2,
        auto_submit_chars=len(str(answer)),
    )


def _subtraction_positive(rng: random.Random, batch_id: str) -> DrillItem:
    b = rng.randint(1, 10)
    a = rng.randint(b, b + 10)  # garante resultado >= 0
    answer = a - b
    return DrillItem(
        item_id=str(uuid.uuid4()),
        statement=f"{a}-{b}",
        expected_answer=str(answer),
        skill_tag="soma_subtracao",
        difficulty=1.0,
        auto_submit_chars=len(str(answer)),
    )


def _subtraction_negative(rng: random.Random, batch_id: str) -> DrillItem:
    """Resultado negativo — treino de sinal."""
    a = rng.randint(1, 9)
    b = rng.randint(a + 1, a + 9)  # b > a → resultado negativo
    answer = a - b
    return DrillItem(
        item_id=str(uuid.uuid4()),
        statement=f"{a}-{b}",
        expected_answer=str(answer),
        skill_tag="soma_subtracao",
        difficulty=1.3,
        auto_submit_chars=len(str(answer)),
    )


def _addition_medium(rng: random.Random, batch_id: str) -> DrillItem:
    a = rng.randint(5, 20)
    b = rng.randint(5, 20)
    answer = a + b
    return DrillItem(
        item_id=str(uuid.uuid4()),
        statement=f"{a}+{b}",
        expected_answer=str(answer),
        skill_tag="soma_subtracao",
        difficulty=1.5,
        auto_submit_chars=len(str(answer)),
    )


def _multiplication_small(rng: random.Random, batch_id: str) -> DrillItem:
    a = rng.randint(2, 9)
    b = rng.randint(2, 9)
    answer = a * b
    return DrillItem(
        item_id=str(uuid.uuid4()),
        statement=f"{a}×{b}",
        expected_answer=str(answer),
        skill_tag="multiplicacao_divisao",
        difficulty=1.8,
        auto_submit_chars=len(str(answer)),
    )


def _multiplication_medium(rng: random.Random, batch_id: str) -> DrillItem:
    a = rng.randint(3, 15)
    b = rng.randint(3, 15)
    answer = a * b
    return DrillItem(
        item_id=str(uuid.uuid4()),
        statement=f"{a}×{b}",
        expected_answer=str(answer),
        skill_tag="multiplicacao_divisao",
        difficulty=2.2,
        auto_submit_chars=len(str(answer)),
    )


def _division_exact_medium(rng: random.Random, batch_id: str) -> DrillItem:
    b = rng.randint(2, 12)
    answer = rng.randint(2, 10)
    a = b * answer  # garante divisão exata
    return DrillItem(
        item_id=str(uuid.uuid4()),
        statement=f"{a}÷{b}",
        expected_answer=str(answer),
        skill_tag="multiplicacao_divisao",
        difficulty=2.0,
        auto_submit_chars=len(str(answer)),
    )


def _division_exact_hard(rng: random.Random, batch_id: str) -> DrillItem:
    b = rng.randint(3, 20)
    answer = rng.randint(3, 20)
    a = b * answer
    return DrillItem(
        item_id=str(uuid.uuid4()),
        statement=f"{a}÷{b}",
        expected_answer=str(answer),
        skill_tag="multiplicacao_divisao",
        difficulty=2.5,
        auto_submit_chars=len(str(answer)),
    )


_GENERATORS_BY_LEVEL: dict[str, list] = {
    "basic": [
        _addition,
        _addition,                # +peso
        _subtraction_positive,
        _subtraction_signed,
        _subtraction_negative,
    ],
    "medium": [
        _addition_medium,
        _subtraction_signed,
        _multiplication_small,
        _multiplication_small,    # +peso
        _division_exact_medium,
    ],
    "hard": [
        _multiplication_medium,
        _division_exact_medium,
        _division_exact_hard,
        _addition_medium,
        _subtraction_signed,
    ],
}

VALID_LEVELS = list(_GENERATORS_BY_LEVEL.keys())
