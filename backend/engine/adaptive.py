import uuid

from sqlalchemy import func, select
from sqlalchemy.ext.asyncio import AsyncSession

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
            memory = StudentSkillMemory(student_id=student_id, skill=skill)
            db.add(memory)
            await db.flush()

        correctness = float(vector.get("correctness") or 0.0)
        fluency = float(vector.get("fluency_score") or 0.0)
        fatigue = float(vector.get("fatigue_index") or 0.0)

        memory.accuracy = 0.8 * memory.accuracy + 0.2 * correctness
        memory.fluency = 0.8 * memory.fluency + 0.2 * fluency
        memory.fatigue_avg = 0.8 * memory.fatigue_avg + 0.2 * fatigue
        memory.status = vector_to_memory_status(memory.accuracy)


async def _select_exercises(
    db: AsyncSession,
    difficulty: float,
    limit: int,
    weak_skills: list[str] | None = None,
) -> list[Exercise]:
    upper_bound = min(10.0, difficulty + 0.5)
    lower_bound = max(1.0, difficulty - 0.8)

    base_filters = [
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
        broader_filters = []
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


def build_folha_out(folha: Folha, exercises: list[Exercise]) -> FolhaOut:
    return FolhaOut(
        folha_id=folha.id,
        page_index=folha.page_index,
        difficulty=folha.difficulty,
        fields=[
            FolhaField(
                field_index=index,
                exercise_id=exercise.id,
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
    weak_skills: list[str] | None = None,
) -> tuple[Folha, list[Exercise]]:
    folha = Folha(session_id=session.id, page_index=session.page_count, difficulty=difficulty)
    db.add(folha)
    await db.flush()

    exercises = await _select_exercises(db, difficulty, exercises_per_page, weak_skills)
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
) -> tuple[Folha, list[Exercise]]:
    memories = await get_skill_memory(db, student_id)
    weak_skills = [skill for skill, memory in memories.items() if memory.status in ("fraco", "instavel")]
    return await create_folha(db, session, difficulty, exercises_per_page, weak_skills)


async def get_next_folha(
    db: AsyncSession,
    session: Session,
    config: SessionConfig,
    recent_scores: list[float],
    skill_memory: dict[str, StudentSkillMemory],
) -> tuple[Folha, list[Exercise], bool]:
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

    weak_skills = [skill for skill, memory in skill_memory.items() if memory.status in ("fraco", "instavel")]
    folha, exercises = await create_folha(db, session, new_difficulty, config.exercises_per_page, weak_skills)
    return folha, exercises, restart
