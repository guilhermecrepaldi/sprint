import random
import uuid
from datetime import UTC, datetime

from sqlalchemy import func, select
from sqlalchemy.ext.asyncio import AsyncSession

from engine.mastery import apply_decay, update_mastery
from engine.focus_expansion import ensure_exact_focus_pool
from engine.unlock import get_available_skills
from models.exercise import Exercise
from models.session import Folha, FolhaExercise, Session, SessionConfig
from models.vector import StudentSkillMemory
from schemas.session import FolhaField, FolhaOut

# ── Thresholds do motor adaptativo ────────────────────────────────────────────
REVIEW_INTERVAL_PAGES       = 3     # a cada N folhas, verificar revisão
REVIEW_MIN_IDLE_DAYS        = 2     # dias sem praticar para entrar em revisão
REVIEW_MIN_ATTEMPTS         = 5     # mínimo de tentativas para ser elegível a revisão
REVIEW_DIFFICULTY_FACTOR    = 0.80  # multiplicador de dificuldade dos exercícios de revisão


async def _session_exercise_ids(db: AsyncSession, session_id: uuid.UUID) -> list[uuid.UUID]:
    result = await db.execute(
        select(FolhaExercise.exercise_id)
        .join(Folha, Folha.id == FolhaExercise.folha_id)
        .where(Folha.session_id == session_id)
    )
    return list(result.scalars().all())


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


def _review_skills(skill_memory: dict[str, StudentSkillMemory], now: datetime) -> list[str]:
    """
    Skills elegíveis para revisão intercalada:
    - status instavel ou em_desenvolvimento (não automatizadas)
    - sem praticar por >= REVIEW_MIN_IDLE_DAYS dias
    - com pelo menos REVIEW_MIN_ATTEMPTS tentativas (não são skills novas)
    """
    result = []
    for skill, mem in skill_memory.items():
        if mem.status not in ("instavel", "em_desenvolvimento"):
            continue
        if mem.attempt_count < REVIEW_MIN_ATTEMPTS:
            continue
        if mem.last_practiced_at is None:
            continue
        last = mem.last_practiced_at
        if last.tzinfo is None:
            last = last.replace(tzinfo=UTC)
        if (now - last).days >= REVIEW_MIN_IDLE_DAYS:
            result.append(skill)
    return result


async def get_skill_memory(db: AsyncSession, student_id: uuid.UUID) -> dict[str, StudentSkillMemory]:
    result = await db.execute(select(StudentSkillMemory).where(StudentSkillMemory.student_id == student_id))
    return {memory.skill: memory for memory in result.scalars().all()}


async def update_skill_memory(
    db: AsyncSession,
    student_id: uuid.UUID,
    skill_tags: list[str],
    vector: dict,
    error_type: str | None = None,
    method_tags: list[str] | None = None,
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
        memory.velocity = 0.8 * memory.velocity + 0.2 * float(vector.get("speed_score") or 0.0)
        memory.stability = 0.8 * memory.stability + 0.2 * float(vector.get("erase_score") or 0.0)
        memory.fixation = 0.8 * memory.fixation + 0.2 * ((memory.accuracy + memory.fluency) / 2)
        memory.fatigue_avg = 0.8 * memory.fatigue_avg + 0.2 * fatigue
        memory.attempt_count += 1
        if correctness <= 0.5:
            memory.last_error_type = error_type
            memory.needs_training = _training_focus(error_type, method_tags)
            memory.suggestion_cooldown_count += 1
        else:
            memory.suggestion_cooldown_count = max(0, memory.suggestion_cooldown_count - 1)
        memory.last_practiced_at = datetime.now(UTC)
        memory.status = vector_to_memory_status(memory.accuracy)


def _training_focus(error_type: str | None, method_tags: list[str] | None) -> str | None:
    tags = set(method_tags or [])
    if error_type == "sinal" or "sinais" in tags:
        return "sinais"
    if "discriminante" in tags:
        return "discriminante"
    if "fatoracao" in tags:
        return "fatoracao"
    if "completar_quadrado" in tags:
        return "completar_quadrado"
    return error_type


def _build_range_filters(difficulty: float, subject: str) -> list:
    return [
        Exercise.subject == subject,
        Exercise.difficulty >= max(1.0, difficulty - 0.8),
        Exercise.difficulty <= min(10.0, difficulty + 0.5),
    ]


def _focus_difficulty(config: SessionConfig, exercise_count: int) -> float:
    block_size = max(1, config.difficulty_block_size or 30)
    block = exercise_count // block_size
    return round(min(10.0, max(1.0, config.difficulty_start + block * config.difficulty_step)), 1)


async def _select_exercises(
    db: AsyncSession,
    difficulty: float,
    limit: int,
    subject: str = "math",
    weak_skills: list[str] | None = None,
    exclude_ids: list | None = None,
    template_pin: str | None = None,
) -> list[Exercise]:
    excluded: list = list(exclude_ids) if exclude_ids else []
    exercises: list[Exercise] = []
    range_filters = _build_range_filters(difficulty, subject)

    # Pass 1: weak_skills dentro do range de dificuldade
    if template_pin:
        q = (
            select(Exercise)
            .where(Exercise.subject == subject, Exercise.template_id == template_pin)
            .order_by(func.abs(Exercise.difficulty - difficulty), func.random())
            .limit(limit)
        )
        if excluded:
            q = q.where(Exercise.id.not_in(excluded))
        result = await db.execute(q)
        exercises = list(result.scalars().all())
        excluded += [ex.id for ex in exercises]
        if len(exercises) >= limit:
            return exercises
        q = (
            select(Exercise)
            .where(Exercise.subject == subject, Exercise.template_id == template_pin)
            .order_by(func.abs(Exercise.difficulty - difficulty), func.random())
            .limit(limit - len(exercises))
        )
        result = await db.execute(q)
        exercises.extend(result.scalars().all())
        return exercises

    # Pass 2: weak_skills dentro do range de dificuldade
    if weak_skills and not template_pin:
        q = (
            select(Exercise)
            .where(*range_filters, Exercise.skill_tags.overlap(weak_skills))
            .order_by(func.abs(Exercise.difficulty - difficulty), func.random())
            .limit(limit)
        )
        if excluded:
            q = q.where(Exercise.id.not_in(excluded))
        result = await db.execute(q)
        exercises = list(result.scalars().all())
        excluded += [ex.id for ex in exercises]
        if len(exercises) >= limit:
            return exercises

    # Pass 3: qualquer exercício dentro do range
    if len(exercises) < limit:
        q = (
            select(Exercise)
            .where(*range_filters)
            .order_by(func.abs(Exercise.difficulty - difficulty), func.random())
            .limit(limit - len(exercises))
        )
        if excluded:
            q = q.where(Exercise.id.not_in(excluded))
        result = await db.execute(q)
        exercises.extend(result.scalars().all())
        excluded += [ex.id for ex in exercises]

    # Pass 4: broadening — qualquer dificuldade (último recurso)
    if len(exercises) < limit:
        q = (
            select(Exercise)
            .where(Exercise.subject == subject)
            .order_by(func.abs(Exercise.difficulty - difficulty), func.random())
            .limit(limit - len(exercises))
        )
        if excluded:
            q = q.where(Exercise.id.not_in(excluded))
        result = await db.execute(q)
        exercises.extend(result.scalars().all())

    # Pass 5: pool focado esgotado — permite repetição, mantendo skill/dificuldade/template.
    if len(exercises) < limit and excluded:
        q = (
            select(Exercise)
            .where(*range_filters)
            .order_by(func.abs(Exercise.difficulty - difficulty), func.random())
            .limit(limit - len(exercises))
        )
        if weak_skills:
            q = q.where(Exercise.skill_tags.overlap(weak_skills))
        if template_pin:
            q = q.where(Exercise.template_id == template_pin)
        result = await db.execute(q)
        exercises.extend(result.scalars().all())

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
                node_id=exercise.node_id,
                template_id=exercise.template_id,
                method_tags=exercise.method_tags,
                difficulty_vector=exercise.difficulty_vector,
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
    review_skills: list[str] | None = None,
    skill_pin: str | None = None,
    template_pin: str | None = None,
    focus_source_exercise_id: uuid.UUID | None = None,
    focus_target_count: int = 200,
) -> tuple[Folha, list[Exercise]]:
    folha = Folha(session_id=session.id, page_index=session.page_count, difficulty=difficulty)
    db.add(folha)
    await db.flush()
    exclude_ids = await _session_exercise_ids(db, session.id)

    if template_pin:
        await ensure_exact_focus_pool(
            db,
            source_exercise_id=focus_source_exercise_id,
            template_id=template_pin,
            target_count=focus_target_count,
        )
        exercises = await _select_exercises(
            db, difficulty, exercises_per_page, subject, weak_skills=[skill_pin] if skill_pin else None,
            exclude_ids=exclude_ids, template_pin=template_pin,
        )
    # Fixação mode: pin all slots to the selected skill, no review intercalation
    elif skill_pin:
        exercises = await _select_exercises(
            db, difficulty, exercises_per_page, subject, weak_skills=[skill_pin],
            exclude_ids=exclude_ids,
        )
    # Se há skills de revisão, reservar 1 slot para exercício de revisão
    elif review_skills:
        main_count = max(1, exercises_per_page - 1)
        review_difficulty = max(1.0, difficulty * REVIEW_DIFFICULTY_FACTOR)
        review_exs = await _select_exercises(db, review_difficulty, 1, subject, review_skills)
        main_exs = await _select_exercises(
            db, difficulty, main_count, subject, weak_skills,
            exclude_ids=exclude_ids + [ex.id for ex in review_exs],
        )
        # Inserir o exercício de revisão numa posição aleatória
        insert_pos = random.randint(0, len(main_exs))
        exercises = main_exs[:insert_pos] + review_exs + main_exs[insert_pos:]
    else:
        exercises = await _select_exercises(db, difficulty, exercises_per_page, subject, weak_skills, exclude_ids=exclude_ids)

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
    skill_pin: str | None = None,
    template_pin: str | None = None,
    config: SessionConfig | None = None,
) -> tuple[Folha, list[Exercise]]:
    memories = await get_skill_memory(db, student_id)
    weak_skills = [skill for skill, memory in memories.items() if memory.status in ("fraco", "instavel")]
    focus_difficulty = _focus_difficulty(config, session.exercise_count) if config and config.focus_mode else difficulty
    return await create_folha(
        db,
        session,
        focus_difficulty,
        exercises_per_page,
        subject,
        weak_skills,
        skill_pin=skill_pin,
        template_pin=template_pin,
        focus_source_exercise_id=config.focus_source_exercise_id if config else None,
        focus_target_count=config.focus_target_count if config else 200,
    )


async def get_next_folha(
    db: AsyncSession,
    session: Session,
    config: SessionConfig,
    recent_scores: list[float],
    skill_memory: dict[str, StudentSkillMemory],
    skill_pin: str | None = None,
    template_pin: str | None = None,
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

    if config.focus_mode and (skill_pin or template_pin):
        new_difficulty = _focus_difficulty(config, session.exercise_count)
    elif restart:
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
    adjusted_difficulty = new_difficulty if config.focus_mode and (skill_pin or template_pin) else adjust_difficulty_for_role(new_difficulty, role)

    # Fixação mode: skip adaptive selection entirely, use pinned skill only
    if skill_pin or template_pin:
        folha, exercises = await create_folha(
            db, session, adjusted_difficulty, config.exercises_per_page,
            config.subject, skill_pin=skill_pin, template_pin=template_pin,
            focus_source_exercise_id=config.focus_source_exercise_id,
            focus_target_count=config.focus_target_count,
        )
        return folha, exercises, restart

    # Erros consecutivos viram sinalizacao no cliente, nao mudanca automatica.
    # A engine pode sugerir recuperacao, mas a escolha de permanecer/ajustar e do aluno.
    skill_memory_dict = {
        skill: {"accuracy": mem.accuracy, "attempt_count": mem.attempt_count}
        for skill, mem in skill_memory.items()
    }
    available_skills = get_available_skills(skill_memory_dict)
    weak_skills = [
        skill for skill, mem in skill_memory.items()
        if mem.status in ("fraco", "instavel") and skill in available_skills
    ]

    # Revisão não é automática — o aluno escolhe via /review-suggestions.
    folha, exercises = await create_folha(
        db, session, adjusted_difficulty, config.exercises_per_page,
        config.subject, weak_skills if weak_skills else None,
    )
    return folha, exercises, restart
