"""
GET /api/student/{student_id}/activity
Retorna contagem de exercícios por dia nos últimos N dias.
Usado pelo heatmap de atividade no dashboard (estilo GitHub).

GET /api/student/{student_id}/review-suggestions
Retorna skills que o aluno pode revisar: instáveis ou em desenvolvimento,
sem praticar há pelo menos 2 dias. O aluno decide quando revisar.

GET /api/student/{student_id}/sessions
Retorna histórico de sessões para o dashboard.
"""

import uuid
from datetime import UTC, date, datetime, timedelta

from fastapi import APIRouter, Depends, Query
from pydantic import BaseModel
from sqlalchemy import func, select
from sqlalchemy.ext.asyncio import AsyncSession

from db import get_db
from engine.adaptive import REVIEW_MIN_IDLE_DAYS, REVIEW_MIN_ATTEMPTS, _review_skills
from models.attempt import ExerciseAttempt, PenEvent
from models.exercise import Exercise
from models.session import Session, SessionConfig
from models.vector import CognitiveVector, StudentSkillMemory

router = APIRouter()


@router.get("/api/student/{student_id}/activity")
async def get_activity(
    student_id: str,
    days: int = 365,
    db: AsyncSession = Depends(get_db),
) -> dict:
    """
    Retorna {'days': [{'date': 'YYYY-MM-DD', 'count': N}, ...]}
    para os últimos `days` dias, incluindo zeros.
    """
    since = date.today() - timedelta(days=days - 1)

    result = await db.execute(
        select(
            func.date(ExerciseAttempt.created_at).label("day"),
            func.count().label("count"),
        )
        .where(ExerciseAttempt.student_id == student_id)
        .where(func.date(ExerciseAttempt.created_at) >= since)
        .group_by(func.date(ExerciseAttempt.created_at))
        .order_by(func.date(ExerciseAttempt.created_at))
    )
    rows = result.all()
    counts = {str(row.day): row.count for row in rows}

    # Preencher todos os dias com zero onde não houve atividade
    all_days = []
    for i in range(days):
        d = since + timedelta(days=i)
        all_days.append({"date": str(d), "count": counts.get(str(d), 0)})

    return {"days": all_days}


class ReviewSuggestion(BaseModel):
    skill: str
    status: str           # "instavel" | "em_desenvolvimento"
    accuracy: float
    days_idle: int        # dias sem praticar
    attempt_count: int


class SkillProgressItem(BaseModel):
    skill: str
    status: str
    accuracy: float
    fluency: float
    retention: float
    velocity: float
    stability: float
    fixation: float
    attempt_count: int
    available_count: int
    needs_training: str | None = None


@router.get("/api/student/{student_id}/skill-progress")
async def get_skill_progress(
    student_id: uuid.UUID,
    db: AsyncSession = Depends(get_db),
) -> list[SkillProgressItem]:
    """
    Retorna progresso e cobertura de exercícios por skill.
    A tela Sprint permanece limpa; Painel/Árvore usam esse resumo.
    """
    memory_result = await db.execute(
        select(StudentSkillMemory).where(StudentSkillMemory.student_id == student_id)
    )
    memories = {mem.skill: mem for mem in memory_result.scalars().all()}

    exercise_result = await db.execute(select(Exercise.skill_tags))
    available_counts: dict[str, int] = {}
    for tags in exercise_result.scalars().all():
        for skill in tags or []:
            available_counts[skill] = available_counts.get(skill, 0) + 1

    skills = sorted(set(available_counts) | set(memories))
    return [
        SkillProgressItem(
            skill=skill,
            status=memories[skill].status if skill in memories else "novo",
            accuracy=round(memories[skill].accuracy, 3) if skill in memories else 0.0,
            fluency=round(memories[skill].fluency, 3) if skill in memories else 0.0,
            retention=round(memories[skill].retention, 3) if skill in memories else 0.0,
            velocity=round(memories[skill].velocity, 3) if skill in memories else 0.0,
            stability=round(memories[skill].stability, 3) if skill in memories else 0.0,
            fixation=round(memories[skill].fixation, 3) if skill in memories else 0.0,
            attempt_count=memories[skill].attempt_count if skill in memories else 0,
            available_count=available_counts.get(skill, 0),
            needs_training=memories[skill].needs_training if skill in memories else None,
        )
        for skill in skills
    ]


@router.get("/api/student/{student_id}/review-suggestions")
async def get_review_suggestions(
    student_id: uuid.UUID,
    db: AsyncSession = Depends(get_db),
) -> list[ReviewSuggestion]:
    """
    Retorna skills elegíveis para revisão voluntária.
    O aluno pode usar essa lista para decidir o que revisar no zoom-out da plataforma.
    """
    result = await db.execute(
        select(StudentSkillMemory).where(StudentSkillMemory.student_id == student_id)
    )
    memories = {mem.skill: mem for mem in result.scalars().all()}

    now = datetime.now(UTC)
    eligible = _review_skills(memories, now)

    suggestions = []
    for skill in eligible:
        mem = memories[skill]
        last = mem.last_practiced_at
        if last.tzinfo is None:
            last = last.replace(tzinfo=UTC)
        suggestions.append(
            ReviewSuggestion(
                skill=skill,
                status=mem.status,
                accuracy=round(mem.accuracy, 3),
                days_idle=(now - last).days,
                attempt_count=mem.attempt_count,
            )
        )

    # Ordena: mais urgente primeiro (mais dias idle + pior accuracy)
    suggestions.sort(key=lambda s: (-s.days_idle, s.accuracy))
    return suggestions


# ── Sprint history ─────────────────────────────────────────────────────────────

class SprintHistoryItem(BaseModel):
    session_id: str
    skill: str          # skill_pin or "variado"
    density: str
    template: str | None = None
    exercises_done: int
    accuracy: int       # 0-100
    duration_min: int
    started_at: str     # ISO-8601
    is_active: bool     # True = sessão ainda em andamento


@router.get("/api/student/{student_id}/sessions")
async def get_session_history(
    student_id: uuid.UUID,
    limit: int = Query(default=20, le=50),
    db: AsyncSession = Depends(get_db),
) -> list[SprintHistoryItem]:
    """
    Retorna as últimas `limit` sessões do aluno em ordem decrescente de data.
    """
    result = await db.execute(
        select(Session, SessionConfig)
        .join(SessionConfig, Session.config_id == SessionConfig.id, isouter=True)
        .where(Session.student_id == student_id)
        .order_by(Session.started_at.desc())
        .limit(limit)
    )
    rows = result.all()

    items = []
    for session, config in rows:
        skill = (config.skill_pin if config and config.skill_pin else "variado")
        duration_min = max(1, (session.duration_ms or 0) // 60_000)
        accuracy = round((session.session_accuracy or 0.0) * 100)
        items.append(
            SprintHistoryItem(
                session_id=str(session.id),
                skill=skill,
                density=(config.fixation_density if config else "fixa"),
                template=(config.template_pin if config else None),
                exercises_done=session.exercise_count or 0,
                accuracy=accuracy,
                duration_min=duration_min,
                started_at=session.started_at.isoformat(),
                is_active=(session.status == "active"),
            )
        )
    return items


@router.get("/api/student/{student_id}/timeline")
async def get_student_timeline(
    student_id: uuid.UUID,
    days: int = Query(default=30, ge=1, le=3650),
    include_strokes: bool = Query(default=False),
    db: AsyncSession = Depends(get_db),
) -> dict:
    """
    Histórico privado completo: dias -> sessões -> tentativas.
    `include_strokes=true` devolve a cópia traço-a-traço para auditoria/replay.
    """
    since = datetime.now(UTC) - timedelta(days=days)

    session_rows = await db.execute(
        select(Session, SessionConfig)
        .join(SessionConfig, Session.config_id == SessionConfig.id, isouter=True)
        .where(Session.student_id == student_id, Session.started_at >= since)
        .order_by(Session.started_at.desc())
    )
    sessions = session_rows.all()
    session_ids = [session.id for session, _ in sessions]

    attempts_by_session: dict[uuid.UUID, list[ExerciseAttempt]] = {sid: [] for sid in session_ids}
    exercises_by_id: dict[uuid.UUID, Exercise] = {}
    vectors_by_attempt: dict[uuid.UUID, CognitiveVector] = {}
    pen_events_by_attempt: dict[uuid.UUID, list[PenEvent]] = {}

    if session_ids:
        attempt_rows = await db.execute(
            select(ExerciseAttempt)
            .where(ExerciseAttempt.session_id.in_(session_ids))
            .order_by(ExerciseAttempt.created_at.asc())
        )
        attempts = attempt_rows.scalars().all()
        for attempt in attempts:
            attempts_by_session.setdefault(attempt.session_id, []).append(attempt)

        attempt_ids = [attempt.id for attempt in attempts]
        exercise_ids = list({attempt.exercise_id for attempt in attempts if attempt.exercise_id})

        if exercise_ids:
            exercise_rows = await db.execute(select(Exercise).where(Exercise.id.in_(exercise_ids)))
            exercises_by_id = {exercise.id: exercise for exercise in exercise_rows.scalars().all()}

        if attempt_ids:
            vector_rows = await db.execute(select(CognitiveVector).where(CognitiveVector.attempt_id.in_(attempt_ids)))
            vectors_by_attempt = {vector.attempt_id: vector for vector in vector_rows.scalars().all()}

            if include_strokes:
                event_rows = await db.execute(select(PenEvent).where(PenEvent.attempt_id.in_(attempt_ids)))
                for event in event_rows.scalars().all():
                    pen_events_by_attempt.setdefault(event.attempt_id, []).append(event)

    days_out: dict[str, dict] = {}
    for session, config in sessions:
        day_key = session.started_at.date().isoformat()
        day = days_out.setdefault(day_key, {"date": day_key, "exercise_count": 0, "correct_count": 0, "sessions": []})
        session_attempts = attempts_by_session.get(session.id, [])
        correct_count = sum(1 for attempt in session_attempts if attempt.is_correct)
        day["exercise_count"] += len(session_attempts)
        day["correct_count"] += correct_count
        day["sessions"].append({
            "session_id": str(session.id),
            "status": session.status,
            "skill": config.skill_pin if config and config.skill_pin else "variado",
            "started_at": session.started_at.isoformat() if session.started_at else None,
            "ended_at": session.ended_at.isoformat() if session.ended_at else None,
            "duration_ms": session.duration_ms,
            "exercise_count": len(session_attempts),
            "correct_count": correct_count,
            "accuracy": round(correct_count / len(session_attempts), 3) if session_attempts else 0.0,
            "attempts": [
                _timeline_attempt(attempt, exercises_by_id.get(attempt.exercise_id), vectors_by_attempt.get(attempt.id), pen_events_by_attempt.get(attempt.id, []))
                for attempt in session_attempts
            ],
        })

    return {
        "student_id": str(student_id),
        "days_requested": days,
        "include_strokes": include_strokes,
        "days": sorted(days_out.values(), key=lambda item: item["date"], reverse=True),
    }


def _timeline_attempt(
    attempt: ExerciseAttempt,
    exercise: Exercise | None,
    vector: CognitiveVector | None,
    events: list[PenEvent],
) -> dict:
    speed_score = vector.speed_score if vector else None
    return {
        "attempt_id": str(attempt.id),
        "exercise_id": str(attempt.exercise_id) if attempt.exercise_id else None,
        "created_at": attempt.created_at.isoformat() if attempt.created_at else None,
        "field_index": attempt.field_index,
        "statement": exercise.statement if exercise else None,
        "expected_answer": attempt.expected_answer,
        "recognized_answer": attempt.recognized_answer,
        "recognized_latex": attempt.recognized_latex,
        "is_correct": attempt.is_correct,
        "score": attempt.score,
        "error_type": attempt.error_type,
        "skill_tags": exercise.skill_tags if exercise else [],
        "node_id": attempt.node_id,
        "template_id": attempt.template_id,
        "method_tags": attempt.method_tags,
        "timing": {
            "total_time_ms": attempt.total_time_ms,
            "time_to_first_stroke_ms": attempt.time_to_first_stroke_ms,
            "stroke_count": attempt.stroke_count,
            "erase_count": attempt.erase_count,
            "pause_count": attempt.pause_count,
            "average_velocity": attempt.average_velocity,
        },
        "writing_analysis": {
            "engine": attempt.recognition_engine,
            "confidence": attempt.ocr_confidence,
            "reliable": attempt.analysis_reliable,
            "notes": attempt.analysis_notes,
            "speed_score": speed_score,
            "attempt_features": attempt.attempt_features,
        },
        "cognitive_vector": {
            "correctness": vector.correctness,
            "speed_score": vector.speed_score,
            "hesitation_score": vector.hesitation_score,
            "fluency_score": vector.fluency_score,
            "pressure_stability": vector.pressure_stability,
            "erase_score": vector.erase_score,
            "fatigue_index": vector.fatigue_index,
        } if vector else None,
        "strokes": [
            {
                "ts": event.ts,
                "x": event.x,
                "y": event.y,
                "pressure": event.pressure,
                "velocity": event.velocity,
                "type": event.event_type,
            }
            for event in sorted(events, key=lambda ev: ev.ts)
        ] if events else None,
    }
