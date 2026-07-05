"""
Análise de padrões de erro e fragilidade do aluno.

GET /api/student/{student_id}/error-analysis
  → Erros agrupados por tipo e skill, com frequências

GET /api/student/{student_id}/fragility-heatmap
  → Skills frágeis: combinando decay, accuracy baixa, erros frequentes

GET /api/student/{student_id}/review-plan
  → Plano de revisão sugerido: o que revisar, por quê, prioridade
"""

import uuid
from datetime import UTC, date, datetime, timedelta

from fastapi import APIRouter, Depends
from pydantic import BaseModel
from sqlalchemy import func, select, text
from sqlalchemy.ext.asyncio import AsyncSession

from db import get_db
from engine.adaptive import REVIEW_MIN_IDLE_DAYS, apply_decay, vector_to_memory_status
from engine.fsrs_scheduler import FSRSScheduler, get_scheduler
from engine.mastery import MASTERY_THRESHOLD
from models.attempt import ExerciseAttempt
from models.vector import StudentSkillMemory

router = APIRouter()


# ── Schemas ──────────────────────────────────────────────────────────────────

class ErrorStat(BaseModel):
    error_type: str         # "sinal", "fracao", "raiz_quadrada", "equacao_2_grau", "desconhecido"
    count: int
    pct_of_total: float     # 0.0–1.0


class SkillErrorAnalysis(BaseModel):
    skill: str
    total_attempts: int
    error_count: int
    accuracy: float         # 0.0–1.0
    errors: list[ErrorStat]
    dominant_error: str | None       # tipo de erro mais frequente
    dominant_error_pct: float | None # proporção do erro dominante
    last_practiced_days_ago: int | None


class FragileSkill(BaseModel):
    skill: str
    accuracy: float
    attempt_count: int
    days_idle: int
    decay_penalty: float        # quanto o domínio caiu devido ao decay
    effective_mastery: float    # accuracy ajustada pelo decay
    error_trend: str            # "worsening", "stable", "improving"
    priority: str               # "alta", "media", "baixa"
    suggestion: str             # por que revisar


class ReviewPlanItem(BaseModel):
    skill: str
    priority: int               # 1 = mais urgente
    reason: str
    suggested_exercises: int    # quantos exercícios sugeridos


# ── Endpoints ────────────────────────────────────────────────────────────────

@router.get("/api/student/{student_id}/error-analysis")
async def get_error_analysis(
    student_id: uuid.UUID,
    db: AsyncSession = Depends(get_db),
) -> list[SkillErrorAnalysis]:
    """
    Retorna análise de erros por skill: tipos de erro mais comuns,
    proporções, e dominância.
    """
    # Buscar tentativas com erro_type preenchido
    result = await db.execute(
        select(ExerciseAttempt)
        .where(
            ExerciseAttempt.student_id == student_id,
            ExerciseAttempt.is_correct == False,  # noqa: E712
        )
        .order_by(ExerciseAttempt.created_at.desc())
    )
    error_attempts = result.scalars().all()

    # Agrupar por skill
    from collections import defaultdict
    by_skill: dict[str, dict] = defaultdict(lambda: {"total": 0, "error_types": defaultdict(int)})

    for attempt in error_attempts:
        skill = attempt.skill or "unknown"
        etype = attempt.error_type or "desconhecido"
        by_skill[skill]["total"] += 1
        by_skill[skill]["error_types"][etype] += 1

    # Buscar totais por skill
    total_result = await db.execute(
        select(
            ExerciseAttempt.skill,
            func.count().label("cnt"),
        )
        .where(ExerciseAttempt.student_id == student_id)
        .group_by(ExerciseAttempt.skill)
    )
    total_by_skill = {row.skill: row.cnt for row in total_result}

    # Buscar last_practiced
    memory_result = await db.execute(
        select(StudentSkillMemory).where(StudentSkillMemory.student_id == student_id)
    )
    memories = {mem.skill: mem for mem in memory_result.scalars().all()}

    now = datetime.now(UTC)

    output = []
    for skill, data in by_skill.items():
        total = total_by_skill.get(skill, 0)
        error_count = data["total"]
        accuracy = (total - error_count) / max(total, 1)
        errors_list = []
        total_errors = sum(data["error_types"].values())

        for etype, ecount in sorted(data["error_types"].items(), key=lambda x: -x[1]):
            errors_list.append(ErrorStat(
                error_type=etype,
                count=ecount,
                pct_of_total=round(ecount / max(total_errors, 1), 3),
            ))

        dominant = max(data["error_types"].items(), key=lambda x: x[1]) if data["error_types"] else (None, 0)

        mem = memories.get(skill)
        last_days = None
        if mem and mem.last_practiced_at:
            last = mem.last_practiced_at
            if last.tzinfo is None:
                last = last.replace(tzinfo=UTC)
            last_days = (now - last).days

        output.append(SkillErrorAnalysis(
            skill=skill,
            total_attempts=total,
            error_count=error_count,
            accuracy=round(accuracy, 3),
            errors=errors_list,
            dominant_error=dominant[0],
            dominant_error_pct=round(dominant[1] / max(total_errors, 1), 3) if dominant[0] else None,
            last_practiced_days_ago=last_days,
        ))

    return sorted(output, key=lambda x: -x.error_count)


@router.get("/api/student/{student_id}/fragility-heatmap")
async def get_fragility_heatmap(
    student_id: uuid.UUID,
    db: AsyncSession = Depends(get_db),
) -> list[FragileSkill]:
    """
    Retorna skills frágeis ordenadas por prioridade de revisão.
    Combina: accuracy baixa, decay por dias sem prática, e tendência de erro.
    """
    result = await db.execute(
        select(StudentSkillMemory).where(StudentSkillMemory.student_id == student_id)
    )
    memories = list(result.scalars().all())

    now = datetime.now(UTC)
    fragile: list[FragileSkill] = []

    # Buscar tendência de erro recente por skill
    thirty_days_ago = now - timedelta(days=30)

    for mem in memories:
        if mem.attempt_count < 3:
            continue  # poucas tentativas, sem dados suficientes

        last = mem.last_practiced_at
        if last and last.tzinfo is None:
            last = last.replace(tzinfo=UTC)
        days_idle = (now - last).days if last else 999

        # Calcular decay
        mastery_before = mem.accuracy
        mastery_after = apply_decay(mastery_before, days_idle)
        decay_penalty = mastery_before - mastery_after

        # Efetiva = accuracy ajustada pelo decay
        effective = mastery_after

        # Tendência de erro: comparar primeiras vs últimas tentativas
        recent_errors = await db.execute(
            select(func.count())
            .select_from(ExerciseAttempt)
            .where(
                ExerciseAttempt.student_id == student_id,
                ExerciseAttempt.skill == mem.skill,
                ExerciseAttempt.created_at >= thirty_days_ago,
                ExerciseAttempt.is_correct == False,  # noqa: E712
            )
        )
        recent_error_count = recent_errors.scalar_one() or 0

        older_errors = await db.execute(
            select(func.count())
            .select_from(ExerciseAttempt)
            .where(
                ExerciseAttempt.student_id == student_id,
                ExerciseAttempt.skill == mem.skill,
                ExerciseAttempt.created_at < thirty_days_ago,
                ExerciseAttempt.is_correct == False,  # noqa: E712
            )
        )
        older_error_count = older_errors.scalar_one() or 0

        # Tendência
        if recent_error_count > older_error_count * 1.3 and mem.attempt_count >= 10:
            trend = "worsening"
        elif recent_error_count < older_error_count * 0.7:
            trend = "improving"
        else:
            trend = "stable"

        # Prioridade
        if effective < 0.5 and trend == "worsening":
            priority = "alta"
            suggestion = f"Domínio efetivo de {effective:.0%}. Erros recentes aumentando. Revisar urgente."
        elif effective < 0.7 or (days_idle or 0) > 7:
            priority = "media"
            suggestion = f"{'Sem prática há {days_idle} dias. ' if (days_idle or 0) > 7 else ''}Domínio efetivo: {effective:.0%}."
        else:
            priority = "baixa"
            suggestion = "Estável. Apenas monitorar."

        fragile.append(FragileSkill(
            skill=mem.skill,
            accuracy=round(mem.accuracy, 3),
            attempt_count=mem.attempt_count,
            days_idle=days_idle or 0,
            decay_penalty=round(decay_penalty, 4),
            effective_mastery=round(effective, 3),
            error_trend=trend,
            priority=priority,
            suggestion=suggestion,
        ))

    # Ordenar: alta prioridade primeiro, depois menor effective_mastery
    priority_order = {"alta": 0, "media": 1, "baixa": 2}
    fragile.sort(key=lambda x: (priority_order.get(x.priority, 3), x.effective_mastery))
    return fragile


@router.get("/api/student/{student_id}/review-plan")
async def get_review_plan(
    student_id: uuid.UUID,
    db: AsyncSession = Depends(get_db),
) -> list[ReviewPlanItem]:
    """
    Plano de revisão: top 5 skills mais urgentes para revisar,
    com motivo e quantidade sugerida de exercícios.
    """
    fragile = await get_fragility_heatmap(student_id, db)

    plan = []
    for i, skill in enumerate(fragile[:5]):
        base = 20 if skill.priority == "alta" else 10 if skill.priority == "media" else 5
        plan.append(ReviewPlanItem(
            skill=skill.skill,
            priority=i + 1,
            reason=skill.suggestion,
            suggested_exercises=base,
        ))

    return plan
