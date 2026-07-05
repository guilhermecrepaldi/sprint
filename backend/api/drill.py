"""
GET  /api/drill/arithmetic          — baixa um batch de exercícios aritméticos
POST /api/drill/flush               — envia todos os resultados de uma vez

Marathon Mode: o app baixa o lote inteiro, resolve localmente sem round-trips,
e faz um único POST no final. Latência zero entre exercícios.
"""
import uuid
from datetime import UTC, datetime
from typing import Literal

from fastapi import APIRouter, Depends, Query
from pydantic import BaseModel, Field
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from db import get_db
from engine.arithmetic_drill import DrillItem, VALID_LEVELS, generate_batch
from models.attempt import ExerciseAttempt
from models.session import Session, SessionConfig
from models.student import Student

router = APIRouter()

# ── Schemas ──────────────────────────────────────────────────────────────────

class DrillItemOut(BaseModel):
    item_id: str
    statement: str
    expected_answer: str
    skill_tag: str
    difficulty: float
    auto_submit_chars: int


class DrillBatchOut(BaseModel):
    batch_id: str
    level: str
    count: int
    items: list[DrillItemOut]
    generated_at: str


class ItemResult(BaseModel):
    item_id: str
    written_answer: str
    is_correct: bool
    time_ms: int = Field(ge=0)


class DrillFlushIn(BaseModel):
    student_id: uuid.UUID
    batch_id: str
    level: str
    started_at_ms: int
    results: list[ItemResult] = Field(min_length=1)


class DrillFlushOut(BaseModel):
    session_id: str
    total: int
    correct: int
    accuracy: float            # 0.0–1.0
    total_time_ms: int
    avg_time_ms: int           # ms por exercício
    xp_earned: int


# ── Endpoints ─────────────────────────────────────────────────────────────────

@router.get("/api/drill/arithmetic", response_model=DrillBatchOut)
async def get_arithmetic_batch(
    count: int = Query(default=20, ge=1, le=200),
    level: str = Query(default="basic"),
    batch_id: str | None = Query(default=None),
) -> DrillBatchOut:
    """
    Retorna um lote de exercícios aritméticos gerados algoritmicamente.

    - Nenhuma chamada de IA ou banco de dados.
    - O mesmo batch_id sempre retorna os mesmos exercícios (determinístico).
    - O app armazena localmente e resolve sem round-trips.
    """
    if level not in VALID_LEVELS:
        from fastapi import HTTPException
        raise HTTPException(status_code=400, detail=f"level deve ser um de: {VALID_LEVELS}")

    resolved_batch_id, items = generate_batch(count=count, level=level, batch_id=batch_id)

    return DrillBatchOut(
        batch_id=resolved_batch_id,
        level=level,
        count=len(items),
        items=[
            DrillItemOut(
                item_id=item.item_id,
                statement=item.statement,
                expected_answer=item.expected_answer,
                skill_tag=item.skill_tag,
                difficulty=item.difficulty,
                auto_submit_chars=item.auto_submit_chars,
            )
            for item in items
        ],
        generated_at=datetime.now(UTC).isoformat(),
    )


@router.post("/api/drill/flush", response_model=DrillFlushOut)
async def flush_drill(
    body: DrillFlushIn,
    db: AsyncSession = Depends(get_db),
) -> DrillFlushOut:
    """
    Recebe todos os resultados de um batch concluído.

    Cria uma sessão retroativa e persiste as tentativas. Atualiza o XP do aluno.
    Chamado UMA vez no final do treino — não há round-trips durante o drill.
    """
    # Garante que o aluno existe
    student = await db.get(Student, body.student_id)
    if student is None:
        student = Student(id=body.student_id, name="Aluno")
        db.add(student)
        await db.flush()

    # Recria os exercícios do batch (determinístico — mesmo batch_id)
    _, items = generate_batch(
        count=len(body.results),
        level=body.level,
        batch_id=body.batch_id,
    )
    items_by_id: dict[str, DrillItem] = {item.item_id: item for item in items}

    # Sessão retroativa de drill
    started_at = datetime.fromtimestamp(body.started_at_ms / 1000, tz=UTC)
    total_time_ms = sum(r.time_ms for r in body.results)
    correct_count = sum(1 for r in body.results if r.is_correct)
    accuracy = correct_count / len(body.results)

    config = SessionConfig(
        student_id=body.student_id,
        exercises_per_page=len(body.results),
        subject="math",
    )
    db.add(config)
    await db.flush()

    session = Session(
        student_id=body.student_id,
        config_id=config.id,
        started_at=started_at,
        ended_at=datetime.now(UTC),
        duration_ms=total_time_ms,
        exercise_count=len(body.results),
        session_accuracy=accuracy,
        status="finished",
    )
    db.add(session)
    await db.flush()

    # XP: 1 ponto por acerto correto em drill básico
    xp_earned = correct_count

    for idx, result in enumerate(body.results):
        drill_item = items_by_id.get(result.item_id)
        attempt = ExerciseAttempt(
            session_id=session.id,
            student_id=body.student_id,
            field_index=idx,
            recognized_answer=result.written_answer,
            expected_answer=drill_item.expected_answer if drill_item else "",
            is_correct=result.is_correct,
            score=100 if result.is_correct else 0,
            total_time_ms=result.time_ms,
            stroke_count=0,   # drill: input numérico, sem stylus
            erase_count=0,
            pause_count=0,
        )
        db.add(attempt)

    # Propaga XP
    session.xp = xp_earned
    student.xp_total = (student.xp_total or 0) + xp_earned

    await db.commit()

    return DrillFlushOut(
        session_id=str(session.id),
        total=len(body.results),
        correct=correct_count,
        accuracy=round(accuracy, 3),
        total_time_ms=total_time_ms,
        avg_time_ms=total_time_ms // max(len(body.results), 1),
        xp_earned=xp_earned,
    )
