import uuid

from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy import desc, select
from sqlalchemy.ext.asyncio import AsyncSession

from db import get_db
from engine.adaptive import build_folha_out, get_next_folha, get_skill_memory, update_skill_memory
from engine.correction import validate_answer
from engine.ocr import extract_answer
from engine.scoring import compute_score
from engine.vector import generate_vector
from models.attempt import ExerciseAttempt, PenEvent
from models.exercise import Exercise
from models.session import Session, SessionConfig
from models.vector import CognitiveVector
from schemas.submit import FieldResult, SubmitIn, SubmitOut, ThermometerOut

router = APIRouter()


def _count_pauses(events: list) -> int:
    ordered = sorted(events, key=lambda event: event.ts)
    return sum(1 for previous, current in zip(ordered, ordered[1:]) if current.ts - previous.ts > 2000)


def _average(values: list[float]) -> float | None:
    clean = [value for value in values if value is not None]
    if not clean:
        return None
    return sum(clean) / len(clean)


async def _recent_scores(db: AsyncSession, session_id: uuid.UUID, limit: int) -> list[float]:
    result = await db.execute(
        select(ExerciseAttempt.score)
        .where(ExerciseAttempt.session_id == session_id, ExerciseAttempt.score.is_not(None))
        .order_by(desc(ExerciseAttempt.created_at))
        .limit(limit)
    )
    scores = list(reversed(result.scalars().all()))
    return [(score or 0) / 100 for score in scores]


@router.post("/api/session/{session_id}/submit", response_model=SubmitOut)
async def submit_folha(
    session_id: uuid.UUID,
    body: SubmitIn,
    db: AsyncSession = Depends(get_db),
) -> SubmitOut:
    session = await db.get(Session, session_id)
    if session is None:
        raise HTTPException(status_code=404, detail="Session not found")
    if session.config_id is None:
        raise HTTPException(status_code=409, detail="Session has no config")

    config = await db.get(SessionConfig, session.config_id)
    if config is None:
        raise HTTPException(status_code=404, detail="Session config not found")

    results: list[FieldResult] = []
    page_vectors: list[dict] = []

    for field in body.fields:
        exercise = await db.get(Exercise, field.exercise_id)
        if exercise is None:
            raise HTTPException(status_code=404, detail=f"Exercise {field.exercise_id} not found")

        ocr_result = await extract_answer(field.image_base64)
        recognized_answer = ocr_result.get("answer_latex")
        correction = validate_answer(recognized_answer, exercise.expected_answer)
        score = compute_score(
            is_correct=correction["is_correct"],
            total_time_ms=field.total_time_ms,
            hesitation_ms=field.time_to_first_stroke_ms,
            difficulty=exercise.difficulty,
            estimated_time_ms=exercise.estimated_time_ms or 45000,
        )

        stroke_count = sum(1 for event in field.pen_events if event.event_type == "stroke_start")
        erase_count = sum(1 for event in field.pen_events if event.event_type == "erase")
        pause_count = _count_pauses(field.pen_events)

        attempt = ExerciseAttempt(
            folha_id=body.folha_id,
            session_id=session.id,
            student_id=session.student_id,
            exercise_id=exercise.id,
            field_index=field.field_index,
            recognized_answer=recognized_answer,
            expected_answer=exercise.expected_answer,
            is_correct=correction["is_correct"],
            score=score,
            total_time_ms=field.total_time_ms,
            time_to_first_stroke_ms=field.time_to_first_stroke_ms,
            stroke_count=stroke_count,
            erase_count=erase_count,
            pause_count=pause_count,
            average_pressure=_average([event.pressure for event in field.pen_events]),
            average_velocity=_average([event.velocity for event in field.pen_events]),
            error_type=correction["error_type"],
            ocr_confidence=ocr_result.get("confidence"),
        )
        db.add(attempt)
        await db.flush()

        db.add_all(
            PenEvent(
                attempt_id=attempt.id,
                ts=event.ts,
                x=event.x,
                y=event.y,
                pressure=event.pressure,
                tilt=event.tilt,
                velocity=event.velocity,
                event_type=event.event_type,
            )
            for event in field.pen_events
        )

        vector = generate_vector(attempt, field.pen_events, exercise)
        page_vectors.append(vector)
        db.add(
            CognitiveVector(
                student_id=session.student_id,
                attempt_id=attempt.id,
                correctness=vector["correctness"],
                speed_score=vector["speed_score"],
                hesitation_score=vector["hesitation_score"],
                fluency_score=vector["fluency_score"],
                pressure_stability=vector["pressure_stability"],
                erase_score=vector["erase_score"],
                difficulty_level=vector["difficulty_level"],
                skill_vector=vector["skill_vector"],
                fatigue_index=vector["fatigue_index"],
            )
        )

        if session.student_id is not None:
            await update_skill_memory(db, session.student_id, exercise.skill_tags, vector)

        results.append(
            FieldResult(
                field_index=field.field_index,
                recognized_answer=recognized_answer,
                expected_answer=exercise.expected_answer,
                is_correct=correction["is_correct"],
                score=score,
                error_type=correction["error_type"],
                vector=vector,
            )
        )

    session.exercise_count += len(body.fields)
    await db.flush()

    page_score = int(sum(result.score for result in results) / max(1, len(results)))
    avg_correctness = sum(vector["correctness"] for vector in page_vectors) / max(1, len(page_vectors))
    avg_speed = sum(vector["speed_score"] for vector in page_vectors) / max(1, len(page_vectors))
    avg_fluency = sum(vector["fluency_score"] for vector in page_vectors) / max(1, len(page_vectors))
    thermometer_value = round(min(1.0, max(0.0, 0.5 * avg_correctness + 0.3 * avg_speed + 0.2 * avg_fluency)), 2)

    session_status = session.status
    if config.duration_mode == "pages" and config.pages_limit is not None and session.page_count >= config.pages_limit:
        session.status = "finished"
        session_status = "finished"

    next_folha = None
    restart_triggered = False
    if session_status == "active":
        recent_scores = await _recent_scores(db, session.id, config.restart_window)
        skill_memory = await get_skill_memory(db, session.student_id) if session.student_id is not None else {}
        folha, exercises, restart_triggered = await get_next_folha(db, session, config, recent_scores, skill_memory)
        next_folha = build_folha_out(folha, exercises)

    await db.commit()

    return SubmitOut(
        results=results,
        page_score=page_score,
        thermometer=ThermometerOut(value=thermometer_value, trend="stable"),
        restart_triggered=restart_triggered,
        session_status=session.status,
        next_folha=next_folha,
    )
