"""
GET /api/session/{session_id}/export

Exporta uma sessão completa com todos os dados de telemetria:
traços, tempos, vetores cognitivos, acertos por exercício.

Usado para:
  - Comparação em campeonatos ("no exercício 3 você demorou 2× mais")
  - Replay de sessão traço a traço
  - Anti-trapaça (padrão de escrita humana)
  - Dataset de treinamento para OCR futuro
"""
import uuid

from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from db import get_db
from models.attempt import ExerciseAttempt, PenEvent
from models.exercise import Exercise
from models.session import Folha, FolhaExercise, Session, SessionConfig
from models.vector import CognitiveVector

router = APIRouter()


@router.get("/api/session/{session_id}/export")
async def export_session(
    session_id: uuid.UUID,
    db: AsyncSession = Depends(get_db),
) -> dict:
    """
    Retorna snapshot completo da sessão.
    Inclui: config, exercícios, tentativas, pen_events e vetores cognitivos.
    """
    session = await db.get(Session, session_id)
    if session is None:
        raise HTTPException(status_code=404, detail="Session not found")

    config = await db.get(SessionConfig, session.config_id) if session.config_id else None

    # Tentativas desta sessão
    attempts_result = await db.execute(
        select(ExerciseAttempt).where(ExerciseAttempt.session_id == session_id)
    )
    attempts = attempts_result.scalars().all()
    attempt_ids = [a.id for a in attempts]

    # Pen events de todas as tentativas
    pen_events_result = await db.execute(
        select(PenEvent).where(PenEvent.attempt_id.in_(attempt_ids))
    )
    pen_events_by_attempt: dict[uuid.UUID, list] = {}
    for ev in pen_events_result.scalars().all():
        pen_events_by_attempt.setdefault(ev.attempt_id, []).append(ev)

    # Vetores cognitivos
    vectors_result = await db.execute(
        select(CognitiveVector).where(CognitiveVector.attempt_id.in_(attempt_ids))
    )
    vectors_by_attempt = {v.attempt_id: v for v in vectors_result.scalars().all()}

    # Exercícios
    exercise_ids = list({a.exercise_id for a in attempts if a.exercise_id})
    exercises_result = await db.execute(
        select(Exercise).where(Exercise.id.in_(exercise_ids))
    )
    exercises_by_id = {e.id: e for e in exercises_result.scalars().all()}

    # Monta snapshot
    exercises_out = []
    for attempt in sorted(attempts, key=lambda a: a.created_at):
        exercise = exercises_by_id.get(attempt.exercise_id)
        vector = vectors_by_attempt.get(attempt.id)
        events = sorted(
            pen_events_by_attempt.get(attempt.id, []),
            key=lambda e: e.ts,
        )

        exercises_out.append({
            "attempt_id": str(attempt.id),
            "exercise_id": str(attempt.exercise_id),
            "field_index": attempt.field_index,
            "skill_tags": exercise.skill_tags if exercise else [],
            "difficulty": exercise.difficulty if exercise else None,
            "statement": exercise.statement if exercise else None,
            "expected_answer": exercise.expected_answer if exercise else None,
            "recognized_answer": attempt.recognized_answer,
            "recognized_latex": attempt.recognized_latex,
            "recognition_engine": attempt.recognition_engine,
            "recognition_confidence": attempt.ocr_confidence,
            "analysis_reliable": attempt.analysis_reliable,
            "analysis_notes": attempt.analysis_notes,
            "is_correct": attempt.is_correct,
            "score": attempt.score,
            "competitive_score": attempt.competitive_score,
            "competitive_valid": attempt.competitive_valid,
            "audit_flags": attempt.audit_flags or [],
            "total_time_ms": attempt.total_time_ms,
            "time_to_first_stroke_ms": attempt.time_to_first_stroke_ms,
            "stroke_count": attempt.stroke_count,
            "erase_count": attempt.erase_count,
            "pause_count": attempt.pause_count,
            "average_pressure": attempt.average_pressure,
            "average_velocity": attempt.average_velocity,
            "attempt_features": attempt.attempt_features,
            "intervention_signal": attempt.intervention_signal,
            "cognitive_vector": {
                "correctness": vector.correctness,
                "speed_score": vector.speed_score,
                "hesitation_score": vector.hesitation_score,
                "fluency_score": vector.fluency_score,
                "pressure_stability": vector.pressure_stability,
                "erase_score": vector.erase_score,
                "fatigue_index": vector.fatigue_index,
            } if vector else None,
            # Traços para replay (posição + timing + pressão)
            "strokes": [
                {
                    "ts": ev.ts,
                    "x": ev.x,
                    "y": ev.y,
                    "pressure": ev.pressure,
                    "velocity": ev.velocity,
                    "type": ev.event_type,
                }
                for ev in events
            ],
        })

    return {
        "session_id": str(session_id),
        "student_id": str(session.student_id) if session.student_id else None,
        "status": session.status,
        "started_at": session.started_at.isoformat() if session.started_at else None,
        "ended_at": session.ended_at.isoformat() if session.ended_at else None,
        "duration_ms": session.duration_ms,
        "exercise_count": session.exercise_count,
        "session_accuracy": session.session_accuracy,
        "xp": session.xp,
        "competitive_score": session.competitive_score,
        "competitive_valid": session.competitive_valid,
        "audit_flags": session.audit_flags or [],
        "contributor_mode": config.contributor_mode if config else False,
        "ranked_mode": config.ranked_mode if config else False,
        "arena_seed": config.arena_seed if config else None,
        "rules_version": config.rules_version if config else None,
        "exercises_per_page": config.exercises_per_page if config else None,
        "exercises": exercises_out,
    }
