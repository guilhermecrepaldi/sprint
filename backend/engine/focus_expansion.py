from __future__ import annotations

import re
import uuid
from copy import deepcopy
from typing import Any

from sqlalchemy import func, select
from sqlalchemy.ext.asyncio import AsyncSession

from models.exercise import Exercise


DEFAULT_EXACT_FOCUS_TARGET = 200


async def ensure_exact_focus_pool(
    db: AsyncSession,
    *,
    source_exercise_id: uuid.UUID | None,
    template_id: str | None,
    target_count: int = DEFAULT_EXACT_FOCUS_TARGET,
) -> None:
    if not source_exercise_id or not template_id:
        return

    existing_count = await _template_count(db, template_id)
    if existing_count >= target_count:
        return

    source = await db.get(Exercise, source_exercise_id)
    if source is None:
        return

    generated = _generate_similar_exercises(source, target_count - existing_count)
    if not generated:
        return

    db.add_all(generated)
    await db.flush()


async def _template_count(db: AsyncSession, template_id: str) -> int:
    result = await db.execute(select(func.count()).select_from(Exercise).where(Exercise.template_id == template_id))
    return int(result.scalar_one())


def _generate_similar_exercises(source: Exercise, count: int) -> list[Exercise]:
    skill = (source.skill_tags or [""])[0]
    if skill == "potenciacao_radiciacao":
        return _generate_power_family(source, count)
    if skill == "equacoes_lineares":
        return _generate_linear_family(source, count)
    return []


def _clone_metadata(source: Exercise, *, statement: str, expected_answer: str, index: int) -> Exercise:
    parameter_vector = deepcopy(source.parameter_vector or {})
    parameter_vector.update({
        "exact_focus_index": index,
        "focus_source_exercise_id": str(source.id),
    })
    affinity_tags = list(dict.fromkeys((source.affinity_tags or []) + ["fixacao_exata", "extremamente_semelhante"]))
    return Exercise(
        statement=statement,
        expected_answer=expected_answer,
        skill_tags=list(source.skill_tags or []),
        difficulty=source.difficulty,
        estimated_time_ms=source.estimated_time_ms,
        source_library="sprint_exact_focus_v1",
        source_license=source.source_license or "proprietary_generated",
        subject=source.subject,
        canvas_mode=source.canvas_mode,
        validator=source.validator,
        node_id=source.node_id,
        template_id=source.template_id,
        template_version=source.template_version,
        variant_seed=(source.variant_seed or 0) * 1000 + index,
        answer_type=source.answer_type,
        method_tags=list(source.method_tags or []),
        prerequisite_tags=list(source.prerequisite_tags or []),
        affinity_tags=affinity_tags,
        parameter_vector=parameter_vector,
        difficulty_vector=deepcopy(source.difficulty_vector or {}),
    )


def _generate_power_family(source: Exercise, count: int) -> list[Exercise]:
    statement = source.statement
    sqrt_match = re.search(r"\\sqrt\{(\d+)\}", statement)
    power_match = re.search(r"Calcule:\s*\(?(-?\d+)\)?\^\{(\d+)\}", statement)
    items: list[Exercise] = []

    if sqrt_match:
        root = int(int(sqrt_match.group(1)) ** 0.5)
        for index in range(1, count + 1):
            new_root = max(2, root + index)
            radicand = new_root * new_root
            items.append(_clone_metadata(
                source,
                statement=rf"Calcule: \sqrt{{{radicand}}}",
                expected_answer=str(new_root),
                index=index,
            ))
        return items

    if power_match:
        base = int(power_match.group(1))
        exp = int(power_match.group(2))
        for index in range(1, count + 1):
            magnitude = max(2, abs(base) + index)
            new_base = -magnitude if base < 0 else magnitude
            new_exp = max(2, min(6, exp + _small_offset(index)))
            base_text = f"({new_base})" if new_base < 0 else str(new_base)
            items.append(_clone_metadata(
                source,
                statement=f"Calcule: {base_text}^{{{new_exp}}}",
                expected_answer=str(new_base ** new_exp),
                index=index,
            ))
    return items


def _generate_linear_family(source: Exercise, count: int) -> list[Exercise]:
    statement = source.statement
    match = re.search(r"Resolva:\s*(\d+)x\s*([+-])\s*(\d+)\s*=\s*(-?\d+)", statement)
    if not match:
        return []

    a = int(match.group(1))
    sign = 1 if match.group(2) == "+" else -1
    b = sign * int(match.group(3))
    c = int(match.group(4))
    x0 = int(round((c - b) / a))
    items: list[Exercise] = []
    for index in range(1, count + 1):
        new_a = max(2, a + _small_offset(index))
        new_x = x0 + _signed_step(index)
        new_b = b + _signed_step(index + 7)
        new_c = new_a * new_x + new_b
        b_text = f"+ {new_b}" if new_b >= 0 else f"- {abs(new_b)}"
        items.append(_clone_metadata(
            source,
            statement=f"Resolva: {new_a}x {b_text} = {new_c}",
            expected_answer=f"x = {new_x}",
            index=index,
        ))
    return items


def _small_offset(index: int) -> int:
    return (index % 5) - 2


def _signed_step(index: int) -> int:
    magnitude = (index + 1) // 2
    return -magnitude if index % 2 else magnitude
