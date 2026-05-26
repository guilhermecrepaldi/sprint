import uuid
from datetime import UTC, datetime

from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy import desc, func, select
from sqlalchemy.ext.asyncio import AsyncSession

from agents.scoring_agent import ScoringAgent
from db import get_db
from engine.adaptive import build_folha_out, get_next_folha, get_skill_memory, update_skill_memory
from engine.batch_ocr_validate import batch_ocr_validate
from engine.exercise_library import build_attempt_features, light_intervention_signal
from engine.text_validate import text_matches
from engine.vector import generate_vector
from models.attempt import ExerciseAttempt, PenEvent
from models.exercise import Exercise
from models.session import Folha, FolhaExercise, Session, SessionConfig
from models.student import Student
from models.vector import CognitiveVector, StudentSkillMemory
from schemas.submit import FieldResult, SubmitIn, SubmitOut, ThermometerOut

router = APIRouter()
scoring_agent = ScoringAgent()

_CONTRIBUTOR_MIN_ACCURACY = 0.90  # só calibra se sessão tiver >=90% de acerto


async def extract_answer(image_base64: str) -> dict:
    """
    Compatibilidade para testes e fallback local com payloads textuais.
    Imagens reais continuam no batch OCR; entradas latex:/text: evitam chamada externa.
    """
    if image_base64.startswith(("latex:", "text:")):
        return {"answer_latex": image_base64.split(":", 1)[1].strip(), "confidence": 1.0}
    return {"answer_latex": None, "confidence": 0.0}


def _compute_xp(is_correct: bool, difficulty: float, total_time_ms: int, estimated_time_ms: int | None) -> int:
    """XP por exercício: base × dificuldade × bônus de velocidade."""
    if not is_correct:
        return 0
    base = 10
    diff_mult = max(1.0, difficulty / 2.0)
    ref_ms = estimated_time_ms or 45_000
    ratio = total_time_ms / max(ref_ms, 1)
    time_bonus = 2.0 if ratio < 0.3 else 1.5 if ratio < 0.6 else 1.0 if ratio < 1.2 else 0.7
    return max(1, round(base * diff_mult * time_bonus))


async def _calibrate_exercise(db: AsyncSession, exercise: Exercise, time_ms: int, is_correct: bool) -> None:
    """
    Atualiza métricas de calibração do exercício com dados de um contributor.
    - verified_count: quantos contributors acertaram (valida o exercício)
    - expert_avg_time_ms: média móvel do tempo real (referência para dificuldade)
    - calibrated_difficulty: ajuste fino baseado no tempo do expert vs. estimado
    """
    if not is_correct:
        return

    prev_count = exercise.verified_count or 0
    prev_avg = exercise.expert_avg_time_ms or time_ms

    # Média móvel: incrementa suavemente sem re-processar histórico
    new_count = prev_count + 1
    new_avg = round((prev_avg * prev_count + time_ms) / new_count)

    exercise.verified_count = new_count
    exercise.expert_avg_time_ms = new_avg

    # Recalibra dificuldade: se expert demora 3× mais que o esperado, é mais difícil
    if exercise.estimated_time_ms and exercise.estimated_time_ms > 0:
        ratio = new_avg / exercise.estimated_time_ms
        # ratio 1.0 = dificuldade correta; >1 = mais difícil; <1 = mais fácil
        raw = exercise.difficulty * ratio
        exercise.calibrated_difficulty = round(max(1.0, min(10.0, raw)), 2)


def _count_pauses(events: list) -> int:
    ordered = sorted(events, key=lambda event: event.ts)
    return sum(1 for previous, current in zip(ordered, ordered[1:]) if current.ts - previous.ts > 2000)


def _average(values: list[float]) -> float | None:
    clean = [value for value in values if value is not None]
    if not clean:
        return None
    return sum(clean) / len(clean)


def _writing_analysis(
    *,
    recognized: str | None,
    confidence: float,
    engine: str | None,
    stroke_count: int,
    attempt_features: dict,
) -> tuple[bool, str]:
    notes = []
    if not recognized:
        notes.append("sem_leitura")
    if confidence < 0.72:
        notes.append("baixa_confianca")
    if stroke_count <= 0:
        notes.append("sem_tracos")
    if attempt_features.get("time_ratio", 1.0) < 0.08:
        notes.append("rapido_demais")
    if engine in (None, "unknown"):
        notes.append("motor_desconhecido")
    reliable = not notes
    return reliable, ",".join(notes) if notes else "ok"


async def _recent_scores(db: AsyncSession, session_id: uuid.UUID, limit: int) -> list[float]:
    result = await db.execute(
        select(ExerciseAttempt.score)
        .where(ExerciseAttempt.session_id == session_id, ExerciseAttempt.score.is_not(None))
        .order_by(desc(ExerciseAttempt.created_at))
        .limit(limit)
    )
    scores = list(reversed(result.scalars().all()))
    return [(score or 0) / 100 for score in scores]


def _should_finish_session(session: Session, config: SessionConfig) -> bool:
    if config.duration_mode == "pages" and config.pages_limit is not None:
        return session.page_count >= config.pages_limit

    if config.duration_mode == "timed" and config.duration_limit_ms is not None and session.started_at is not None:
        started_at = session.started_at
        if started_at.tzinfo is None:
            started_at = started_at.replace(tzinfo=UTC)
        elapsed_ms = int((datetime.now(UTC) - started_at).total_seconds() * 1000)
        return elapsed_ms >= config.duration_limit_ms

    return False


@router.post("/api/session/{session_id}/submit", response_model=SubmitOut)
async def submit_folha(
    session_id: uuid.UUID,
    body: SubmitIn,
    db: AsyncSession = Depends(get_db),
) -> SubmitOut:
    session = await db.get(Session, session_id)
    if session is None:
        raise HTTPException(status_code=404, detail="Session not found")
    if session.status != "active":
        raise HTTPException(status_code=409, detail=f"Session is {session.status}")
    if session.config_id is None:
        raise HTTPException(status_code=409, detail="Session has no config")

    config = await db.get(SessionConfig, session.config_id)
    if config is None:
        raise HTTPException(status_code=404, detail="Session config not found")

    folha = await db.get(Folha, body.folha_id)
    if folha is None or folha.session_id != session.id:
        raise HTTPException(status_code=404, detail="Folha not found for this session")

    assignment_result = await db.execute(
        select(FolhaExercise).where(FolhaExercise.folha_id == body.folha_id)
    )
    assignments = {assignment.field_index: assignment for assignment in assignment_result.scalars().all()}
    if not assignments:
        raise HTTPException(status_code=409, detail="Folha has no exercise assignments")

    submitted_indexes = {field.field_index for field in body.fields}
    expected_indexes = set(assignments)
    if submitted_indexes != expected_indexes:
        missing = sorted(expected_indexes - submitted_indexes)
        extra = sorted(submitted_indexes - expected_indexes)
        raise HTTPException(
            status_code=400,
            detail={"message": "Submit must include exactly all folha fields", "missing": missing, "extra": extra},
        )

    existing_attempts = await db.execute(
        select(func.count())
        .select_from(ExerciseAttempt)
        .where(ExerciseAttempt.session_id == session.id, ExerciseAttempt.folha_id == body.folha_id)
    )
    if existing_attempts.scalar_one() > 0:
        raise HTTPException(status_code=409, detail="Folha already submitted")

    # ── Valida campos e carrega exercícios ───────────────────────────────────
    ordered_fields = sorted(body.fields, key=lambda f: f.field_index)
    field_exercises: list[tuple] = []  # (field, assignment, exercise)
    for field in ordered_fields:
        assignment = assignments.get(field.field_index)
        if assignment is None:
            raise HTTPException(status_code=400, detail=f"Field {field.field_index} does not belong to this folha")
        if assignment.exercise_id != field.exercise_id:
            raise HTTPException(status_code=400, detail=f"Exercise mismatch for field {field.field_index}")
        exercise = await db.get(Exercise, assignment.exercise_id)
        if exercise is None:
            raise HTTPException(status_code=404, detail=f"Exercise {field.exercise_id} not found")
        field_exercises.append((field, assignment, exercise))

    # ── OCR / validação: ML Kit local ou Claude (fallback) ───────────────────
    #
    # Se o campo chegou com recognized_text (ML Kit ou iink no dispositivo),
    # valida diretamente com sympy — zero chamada Claude para esse campo.
    # Campos sem recognized_text vão para batch_ocr_validate (1 chamada Claude).
    #
    local_results: dict[int, dict] = {}   # field_index → correction dict
    claude_fields: list[tuple] = []       # (field, assignment, exercise) sem recognized_text

    for field, assignment, exercise in field_exercises:
        if field.recognized_text:
            is_correct = text_matches(field.recognized_text, exercise.expected_answer)
            local_results[field.field_index] = {
                "recognized_answer": field.recognized_text,
                "is_correct": is_correct,
                "error_type": None if is_correct else "wrong_answer",
                "confidence": field.recognition_confidence if field.recognition_confidence is not None else 0.9,
                "recognition_engine": field.recognition_engine or "device_digital_ink",
            }
        elif field.image_base64.startswith(("latex:", "text:")):
            extracted = await extract_answer(field.image_base64)
            recognized = extracted.get("answer_latex")
            is_correct = text_matches(recognized, exercise.expected_answer)
            local_results[field.field_index] = {
                "recognized_answer": recognized,
                "is_correct": is_correct,
                "error_type": None if is_correct else "wrong_answer",
                "confidence": float(extracted.get("confidence") or 0.0),
                "recognition_engine": "local_text_fallback",
            }
        else:
            claude_fields.append((field, assignment, exercise))

    claude_corrections: list[dict] = []
    if claude_fields:
        claude_batch = [
            {
                "statement": exercise.statement,
                "expected_answer": exercise.expected_answer,
                "image_base64": field.image_base64,
            }
            for field, _, exercise in claude_fields
        ]
        claude_corrections = await batch_ocr_validate(claude_batch)

    # Merge: preserva a ordem original de field_exercises
    claude_idx = 0
    batch_results: list[dict] = []
    for field, _, _ in field_exercises:
        if field.field_index in local_results:
            batch_results.append(local_results[field.field_index])
        else:
            correction = claude_corrections[claude_idx]
            correction.setdefault("recognition_engine", "cloud_batch_ocr")
            batch_results.append(correction)
            claude_idx += 1

    # ── Persiste tentativas e vetores ─────────────────────────────────────────
    results: list[FieldResult] = []
    page_vectors: list[dict] = []
    xp_before = session.xp or 0

    for (field, _, exercise), correction in zip(field_exercises, batch_results):
        recognized_answer = correction["recognized_answer"]
        is_correct = correction["is_correct"]
        error_type = correction["error_type"]
        confidence = correction["confidence"]
        recognition_engine = correction.get("recognition_engine") or "unknown"

        score = scoring_agent.compute(
            is_correct=is_correct,
            total_time_ms=field.total_time_ms,
            hesitation_ms=field.time_to_first_stroke_ms,
            difficulty=exercise.difficulty,
            estimated_time_ms=exercise.estimated_time_ms,
        )

        stroke_count = sum(1 for event in field.pen_events if event.event_type == "stroke_start")
        erase_count = sum(1 for event in field.pen_events if event.event_type == "erase")
        pause_count = _count_pauses(field.pen_events)
        attempt_features = build_attempt_features(field.total_time_ms, exercise, erase_count, pause_count)
        analysis_reliable, analysis_notes = _writing_analysis(
            recognized=recognized_answer,
            confidence=confidence,
            engine=recognition_engine,
            stroke_count=stroke_count,
            attempt_features=attempt_features,
        )
        attempt_features["writing_analysis"] = {
            "engine": recognition_engine,
            "confidence": round(confidence, 3),
            "reliable": analysis_reliable,
            "notes": analysis_notes,
        }
        primary_skill = exercise.skill_tags[0] if exercise.skill_tags else None
        skill_memory = (
            await db.get(StudentSkillMemory, {"student_id": session.student_id, "skill": primary_skill})
            if session.student_id is not None and primary_skill is not None
            else None
        )
        intervention_signal = light_intervention_signal(
            attempt_count=skill_memory.attempt_count if skill_memory is not None else 0,
            is_correct=is_correct,
            error_type=error_type,
            method_tags=exercise.method_tags,
            features=attempt_features,
        )

        attempt = ExerciseAttempt(
            folha_id=body.folha_id,
            session_id=session.id,
            student_id=session.student_id,
            exercise_id=exercise.id,
            field_index=field.field_index,
            recognized_answer=recognized_answer,
            expected_answer=exercise.expected_answer,
            is_correct=is_correct,
            score=score,
            total_time_ms=field.total_time_ms,
            time_to_first_stroke_ms=field.time_to_first_stroke_ms,
            stroke_count=stroke_count,
            erase_count=erase_count,
            pause_count=pause_count,
            average_pressure=_average([event.pressure for event in field.pen_events]),
            average_velocity=_average([event.velocity for event in field.pen_events]),
            error_type=error_type,
            ocr_confidence=confidence,
            recognized_latex=recognized_answer,
            recognition_engine=recognition_engine,
            analysis_reliable=analysis_reliable,
            analysis_notes=analysis_notes,
            node_id=exercise.node_id,
            template_id=exercise.template_id,
            method_tags=exercise.method_tags,
            difficulty_vector=exercise.difficulty_vector,
            attempt_features=attempt_features,
            intervention_signal=intervention_signal,
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
            await update_skill_memory(
                db,
                session.student_id,
                exercise.skill_tags,
                vector,
                error_type=error_type,
                method_tags=exercise.method_tags,
            )

        # XP: acumula na sessão
        xp_earned = _compute_xp(is_correct, exercise.difficulty, field.total_time_ms, exercise.estimated_time_ms)
        session.xp = (session.xp or 0) + xp_earned

        # Calibração: só em modo contributor
        if config.contributor_mode and is_correct:
            await _calibrate_exercise(db, exercise, field.total_time_ms, is_correct)

        results.append(
            FieldResult(
                field_index=field.field_index,
                recognized_answer=recognized_answer,
                expected_answer=exercise.expected_answer,
                is_correct=is_correct,
                score=score,
                error_type=error_type,
                vector=vector,
                feedback=intervention_signal or ("" if is_correct else "resposta diferente do esperado"),
                intervention_signal=intervention_signal,
                recognition_engine=recognition_engine,
                recognition_confidence=confidence,
                analysis_reliable=analysis_reliable,
            )
        )

    session.exercise_count += len(body.fields)
    if session.started_at is not None:
        started_at = session.started_at
        if started_at.tzinfo is None:
            started_at = started_at.replace(tzinfo=UTC)
        session.duration_ms = int((datetime.now(UTC) - started_at).total_seconds() * 1000)
    total_attempts = (session.exercise_count or 0)
    previous_correct = round((session.session_accuracy or 0.0) * max(0, total_attempts - len(body.fields)))
    current_correct = sum(1 for result in results if result.is_correct)
    session.session_accuracy = (previous_correct + current_correct) / max(1, total_attempts)
    await db.flush()

    xp_earned = (session.xp or 0) - xp_before  # XP ganho nesta folha

    page_score = int(sum(result.score for result in results) / max(1, len(results)))
    avg_correctness = sum(vector["correctness"] for vector in page_vectors) / max(1, len(page_vectors))
    avg_speed = sum(vector["speed_score"] for vector in page_vectors) / max(1, len(page_vectors))
    avg_fluency = sum(vector["fluency_score"] for vector in page_vectors) / max(1, len(page_vectors))
    thermometer_value = round(min(1.0, max(0.0, 0.5 * avg_correctness + 0.3 * avg_speed + 0.2 * avg_fluency)), 2)

    if _should_finish_session(session, config):
        session.status = "finished"
        session.ended_at = datetime.now(UTC)

    # Propaga XP para o student (acumulado total)
    student_xp_total = 0
    if session.student_id is not None:
        student = await db.get(Student, session.student_id)
        if student is not None:
            student.xp_total = (student.xp_total or 0) + xp_earned
            student_xp_total = student.xp_total

    next_folha = None
    restart_triggered = False
    if session.status == "active":
        recent_scores = await _recent_scores(db, session.id, config.restart_window)
        skill_memory = await get_skill_memory(db, session.student_id) if session.student_id is not None else {}
        folha, exercises, restart_triggered = await get_next_folha(
            db,
            session,
            config,
            recent_scores,
            skill_memory,
            skill_pin=config.skill_pin,
            template_pin=config.template_pin,
        )
        next_folha = build_folha_out(folha, exercises)

    await db.commit()

    return SubmitOut(
        results=results,
        page_score=page_score,
        thermometer=ThermometerOut(value=thermometer_value, trend="stable"),
        restart_triggered=restart_triggered,
        session_status=session.status,
        xp_earned=xp_earned,
        xp_total=student_xp_total,
        next_folha=next_folha,
    )
