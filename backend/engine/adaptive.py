import uuid
from datetime import UTC, datetime

from sqlalchemy import func, select
from sqlalchemy.ext.asyncio import AsyncSession

from engine.mastery import apply_decay, update_mastery
from engine.unlock import get_available_skills
from models.exercise import Exercise
from models.session import Folha, FolhaExercise, Session, SessionConfig
from models.vector import StudentSkillMemory
from schemas.session import FolhaField, FolhaOut


def _score_window(recent_scores: list[float], window_size: int) -> list[float]:
    return recent_scores[-window_size:] if window_size > 0 else []


def check_restart(recent_scores: list[float], config: SessionConfig) -> bool:
    if config.restart_on_avg is None:
        return False
    window = _score_window(recent_scores, config.restart_window)
    if len(window) < config.restart_window:
        return False
    return sum(window) / len(window) >= config.restart_on_avg


def vector_to_memory_status(accuracy: float) -> str:
    if accuracy >= 0.85:
        return "automatizado"
    if accuracy >= 0.70:
        return "em_desenvolvimento"
    if accuracy >= 0.50:
        return "instavel"
    return "fraco"


async def get_skill_memory(db: AsyncSession, student_id: uuid.UUID) -> dict[str, StudentSkillMemory]:
    result = await db.execute(select(StudentSkillMemory).where(StudentSkillMemory.student_id == student_id))
    return {memory.skill: memory for memory in result.scalars().all()}


async def update_skill_memory(
    db: AsyncSession,
    student_id: uuid.UUID,
    skill_tags: list[str],
    vector: dict,
) -> None:
    for skill in skill_tags:
        memory = await db.get(StudentSkillMemory, {"student_id": student_id, "skill": skill})
        if memory is None:
            memory = StudentSkillMemory(student_id=student_id, skill=skill, attempt_count=0)
            db.add(memory)
            await db.flush()

        correctness = float(vector.get("correctness") or 0.0)
        fluency = float(vector.get("fluency_score") or 0.0)
        fatigue = float(vector.get("fatigue_index") or 0.0)

        memory.accuracy = update_mastery(memory.accuracy, correctness > 0.5)
        memory.fluency = 0.8 * memory.fluency + 0.2 * fluency
        memory.fatigue_avg = 0.8 * memory.fatigue_avg + 0.2 * fatigue
        memory.attempt_count += 1
        memory.last_practiced_at = datetime.now(UTC)
        memory.status = vector_to_memory_status(memory.accuracy)


async def _select_exercises(
    db: AsyncSession,
    difficulty: float,
    limit: int,
    subject: str = "math",
    weak_skills: list[str] | None = None,
) -> list[Exercise]:
    upper_bound = min(10.0, difficulty + 0.5)
    lower_bound = max(1.0, difficulty - 0.8)

    base_filters = [
        Exercise.subject == subject,
        Exercise.difficulty >= lower_bound,
        Exercise.difficulty <= upper_bound,
    ]

    if weak_skills:
        result = await db.execute(
            select(Exercise)
            .where(*base_filters, Exercise.skill_tags.overlap(weak_skills))
            .order_by(func.abs(Exercise.difficulty - difficulty), func.random())
            .limit(limit)
        )
        exercises = list(result.scalars().all())
        if len(exercises) >= limit:
            return exercises
    else:
        exercises = []

    selected_ids = [exercise.id for exercise in exercises]
    fallback_filters = list(base_filters)
    if selected_ids:
        fallback_filters.append(Exercise.id.not_in(selected_ids))

    fallback = await db.execute(
        select(Exercise)
        .where(*fallback_filters)
        .order_by(func.abs(Exercise.difficulty - difficulty), func.random())
        .limit(limit - len(exercises))
    )
    exercises.extend(fallback.scalars().all())

    if len(exercises) < limit:
        broader_filters = [Exercise.subject == subject]
        selected_ids = [exercise.id for exercise in exercises]
        if selected_ids:
            broader_filters.append(Exercise.id.not_in(selected_ids))
        broader = await db.execute(
            select(Exercise)
            .where(*broader_filters)
            .order_by(func.abs(Exercise.difficulty - difficulty), func.random())
            .limit(limit - len(exercises))
        )
        exercises.extend(broader.scalars().all())

    return exercises


def get_exercise_role(exercise_num_in_session: int, recent_errors_last_3: int) -> str:
    """
    Distribui papéis dentro de uma rodada de 10 exercícios.
    exercise_num_in_session é 0-based.
    """
    pos = exercise_num_in_session % 10
    if pos < 3:
        return "warmup"
    if pos == 7 and recent_errors_last_3 >= 2:
        return "breathing"
    if pos >= 8:
        return "sprint"
    return "flow"


def adjust_difficulty_for_role(base_difficulty: float, role: str) -> float:
    adjustments = {
        "warmup": 0.90,
        "breathing": 0.80,
        "flow": 1.00,
        "sprint": 1.00,  # dificuldade + offset fixo abaixo
    }
    result = base_difficulty * adjustments[role]
    if role == "sprint":
        result = base_difficulty + 0.5
    return round(min(10.0, max(1.0, result)), 1)


def build_folha_out(folha: Folha, exercises: list[Exercise]) -> FolhaOut:
    return FolhaOut(
        folha_id=folha.id,
        page_index=folha.page_index,
        difficulty=folha.difficulty,
        fields=[
            FolhaField(
                field_index=index,
                exercise_id=exercise.id,
                subject=exercise.subject,
                canvas_mode=exercise.canvas_mode,
                statement=exercise.statement,
                skill_tags=exercise.skill_tags,
                estimated_time_ms=exercise.estimated_time_ms,
            )
            for index, exercise in enumerate(exercises)
        ],
    )


async def create_folha(
    db: AsyncSession,
    session: Session,
    difficulty: float,
    exercises_per_page: int,
    subject: str = "math",
    weak_skills: list[str] | None = None,
) -> tuple[Folha, list[Exercise]]:
    folha = Folha(session_id=session.id, page_index=session.page_count, difficulty=difficulty)
    db.add(folha)
    await db.flush()

    exercises = await _select_exercises(db, difficulty, exercises_per_page, subject, weak_skills)
    db.add_all(
        FolhaExercise(folha_id=folha.id, exercise_id=exercise.id, field_index=index)
        for index, exercise in enumerate(exercises)
    )
    session.page_count += 1
    await db.flush()
    return folha, exercises


async def get_first_folha(
    db: AsyncSession,
    session: Session,
    difficulty: float,
    exercises_per_page: int,
    student_id: uuid.UUID,
    subject: str = "math",
) -> tuple[Folha, list[Exercise]]:
    memories = await get_skill_memory(db, student_id)
    weak_skills = [skill for skill, memory in memories.items() if memory.status in ("fraco", "instavel")]
    return await create_folha(db, session, difficulty, exercises_per_page, subject, weak_skills)


async def get_next_folha(
    db: AsyncSession,
    session: Session,
    config: SessionConfig,
    recent_scores: list[float],
    skill_memory: dict[str, StudentSkillMemory],
) -> tuple[Folha, list[Exercise], bool]:
    # Apply mastery decay for each skill based on days since last practice
    now = datetime.now(UTC)
    for mem in skill_memory.values():
        last = mem.last_practiced_at
        if last is not None:
            if last.tzinfo is None:
                last = last.replace(tzinfo=UTC)
            days = (now - last).days
            mem.accuracy = apply_decay(mem.accuracy, days)
            mem.status = vector_to_memory_status(mem.accuracy)

    restart = check_restart(recent_scores, config)

    if restart:
        new_difficulty = min(10.0, config.difficulty_start + 0.5 * (session.restart_count + 1))
        session.restart_count += 1
    elif config.difficulty_progression == "geometric":
        new_difficulty = session.current_difficulty * config.difficulty_ratio
    else:
        new_difficulty = session.current_difficulty + config.difficulty_step

    new_difficulty = min(10.0, max(1.0, new_difficulty))
    session.current_difficulty = new_difficulty

    # Apply rhythmic difficulty adjustment based on position in session
    recent_errors = sum(1 for s in recent_scores[-3:] if s < 0.5)
    role = get_exercise_role(session.exercise_count, recent_errors)
    adjusted_difficulty = adjust_difficulty_for_role(new_difficulty, role)

    skill_memory_dict = {
        skill: {"accuracy": mem.accuracy, "attempt_count": mem.attempt_count}
        for skill, mem in skill_memory.items()
    }
    available_skills = get_available_skills(skill_memory_dict)
    weak_skills = [
        skill for skill, mem in skill_memory.items()
        if mem.status in ("fraco", "instavel") and skill in available_skills
    ]
    folha, exercises = await create_folha(
        db,
        session,
        adjusted_difficulty,
        config.exercises_per_page,
        config.subject,
        weak_skills if weak_skills else None,
    )
    return folha, exercises, restart
