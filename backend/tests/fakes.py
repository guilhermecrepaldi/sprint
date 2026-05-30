import uuid
from datetime import UTC, datetime
from typing import Any

from models.attempt import ExerciseAttempt, PenEvent
from models.exercise import Exercise
from models.session import Folha, FolhaExercise, Session, SessionConfig
from models.student import Student
from models.vector import CognitiveVector, StudentSkillMemory


class FakeScalarResult:
    def __init__(self, items: list[Any]) -> None:
        self._items = items

    def all(self) -> list[Any]:
        return self._items


class FakeResult:
    def __init__(self, items: list[Any] | None = None, scalar: Any | None = None) -> None:
        self._items = items or []
        self._scalar = scalar

    def scalars(self) -> FakeScalarResult:
        return FakeScalarResult(self._items)

    def scalar_one(self) -> Any:
        return self._scalar

    def scalar_one_or_none(self) -> Any:
        return self._scalar

    def all(self) -> list[Any]:
        return self._items


class FakeAsyncSession:
    def __init__(self) -> None:
        self._stores: dict[type, dict[Any, Any]] = {
            CognitiveVector: {},
            Exercise: {},
            ExerciseAttempt: {},
            Folha: {},
            FolhaExercise: {},
            PenEvent: {},
            Session: {},
            SessionConfig: {},
            Student: {},
            StudentSkillMemory: {},
        }
        self._next_int_id = 1
        self.commits = 0
        self.rollbacks = 0

    def seed_exercises(self, count: int = 64) -> list[Exercise]:
        exercises = []
        for index in range(count):
            exercise = Exercise(
                id=uuid.uuid4(),
                statement=f"Resolva: x + {index} = {index + 5}",
                expected_answer="x = 5",
                skill_tags=["equacao_1_grau"],
                difficulty=1.0 + (index % 90) * 0.1,
                estimated_time_ms=30000,
                source_library="test",
                subject="math",
                canvas_mode="calculation",
                validator="sympy",
            )
            self.add(exercise)
            exercises.append(exercise)
        physics = Exercise(
            id=uuid.uuid4(),
            statement="Um corpo percorre 20 m em 4 s. Calcule a velocidade média.",
            expected_answer="x = 5",
            skill_tags=["cinematica"],
            difficulty=2.0,
            estimated_time_ms=30000,
            source_library="test",
            subject="physics",
            canvas_mode="calculation",
            validator="sympy",
        )
        self.add(physics)
        exercises.append(physics)
        return exercises

    def add(self, obj: Any) -> None:
        self._apply_defaults(obj)
        self._stores[type(obj)][self._key_for(obj)] = obj

    def add_all(self, objects: Any) -> None:
        for obj in objects:
            self.add(obj)

    async def flush(self) -> None:
        for store in self._stores.values():
            for obj in list(store.values()):
                self._apply_defaults(obj)

    async def commit(self) -> None:
        self.commits += 1

    async def rollback(self) -> None:
        self.rollbacks += 1

    async def get(self, model: type, key: Any) -> Any:
        if model is StudentSkillMemory and isinstance(key, dict):
            return self._stores[model].get((key["student_id"], key["skill"]))
        return self._stores[model].get(key)

    async def execute(self, stmt: Any) -> FakeResult:
        compiled = stmt.compile()
        params = compiled.params
        descriptions = stmt.column_descriptions
        entity = descriptions[0].get("entity") if descriptions else None
        sql = str(stmt)

        if "count(*)" in sql and "FROM exercise_attempts" in sql:
            session_id = params.get("session_id_1")
            folha_id = params.get("folha_id_1")
            count = len(
                [
                    attempt
                    for attempt in self._stores[ExerciseAttempt].values()
                    if attempt.session_id == session_id and attempt.folha_id == folha_id
                ]
            )
            return FakeResult(scalar=count)

        if "count(*)" in sql and "FROM exercises" in sql:
            template_id = params.get("template_id_1")
            count = len(
                [
                    exercise
                    for exercise in self._stores[Exercise].values()
                    if template_id is None or exercise.template_id == template_id
                ]
            )
            return FakeResult(scalar=count)

        if entity is FolhaExercise:
            folha_id = params.get("folha_id_1")
            session_id = params.get("session_id_1")
            if session_id is not None:
                folha_ids = {
                    folha.id
                    for folha in self._stores[Folha].values()
                    if folha.session_id == session_id
                }
                items = [
                    item
                    for item in self._stores[FolhaExercise].values()
                    if item.folha_id in folha_ids
                ]
            else:
                items = [
                    item
                    for item in self._stores[FolhaExercise].values()
                    if item.folha_id == folha_id
                ]
            if descriptions[0]["name"] == "exercise_id":
                return FakeResult(items=[item.exercise_id for item in items])
            return FakeResult(items=sorted(items, key=lambda item: item.field_index))

        if entity is StudentSkillMemory:
            student_id = params.get("student_id_1")
            items = [
                item
                for item in self._stores[StudentSkillMemory].values()
                if item.student_id == student_id
            ]
            return FakeResult(items=items)

        if entity is Session:
            student_id = params.get("student_id_1") or params.get("student_id_2")
            sessions = [
                session
                for session in self._stores[Session].values()
                if student_id is None or session.student_id == student_id
            ]
            sessions.sort(key=lambda session: session.started_at, reverse=True)
            if len(descriptions) > 1 and descriptions[1].get("entity") is SessionConfig:
                rows = [
                    (session, self._stores[SessionConfig].get(session.config_id))
                    for session in sessions
                ]
                return FakeResult(items=rows)
            return FakeResult(items=sessions)

        if entity is Exercise:
            if "WHERE exercises.id IN" in sql:
                ids = set()
                for value in params.values():
                    if isinstance(value, list):
                        ids.update(value)
                items = [
                    exercise
                    for exercise in self._stores[Exercise].values()
                    if exercise.id in ids
                ]
                return FakeResult(items=items)
            has_difficulty_range = "exercises.difficulty >=" in sql and "exercises.difficulty <=" in sql
            lower = params.get("difficulty_1", 1.0) if has_difficulty_range else 1.0
            upper = params.get("difficulty_2", 10.0) if has_difficulty_range else 10.0
            subject = params.get("subject_1")
            template_id = params.get("template_id_1")
            skill_tags = params.get("skill_tags_1")
            limit = params.get("param_1")
            excluded = set()
            for key, value in params.items():
                if key.startswith("id_"):
                    excluded.update(value if isinstance(value, list) else [value])
            items = [
                exercise
                for exercise in self._stores[Exercise].values()
                if lower <= exercise.difficulty <= upper and exercise.id not in excluded
                and (subject is None or exercise.subject == subject)
                and (template_id is None or exercise.template_id == template_id)
                and (not skill_tags or set(exercise.skill_tags or []).intersection(skill_tags))
            ]
            items.sort(key=lambda exercise: (abs(exercise.difficulty - ((lower + upper) / 2)), str(exercise.id)))
            return FakeResult(items=items[:limit])

        if entity is ExerciseAttempt:
            session_id = params.get("session_id_1")
            limit = params.get("param_1")
            if isinstance(session_id, list):
                session_ids = set(session_id)
                session_id = None
            else:
                session_ids = set()
            if descriptions[0]["name"] == "score":
                scores = [
                    attempt.score
                    for attempt in self._stores[ExerciseAttempt].values()
                    if attempt.session_id == session_id and attempt.score is not None
                ]
                return FakeResult(items=scores[-limit:] if limit else scores)
            for value in params.values():
                if isinstance(value, list):
                    session_ids.update(value)
            items = [
                attempt
                for attempt in self._stores[ExerciseAttempt].values()
                if (session_id is None or attempt.session_id == session_id)
                and (not session_ids or attempt.session_id in session_ids)
            ]
            items.sort(key=lambda attempt: attempt.created_at)
            return FakeResult(items=items)

        if entity is CognitiveVector:
            attempt_ids = set()
            for value in params.values():
                if isinstance(value, list):
                    attempt_ids.update(value)
            items = [
                vector
                for vector in self._stores[CognitiveVector].values()
                if not attempt_ids or vector.attempt_id in attempt_ids
            ]
            return FakeResult(items=items)

        if entity is PenEvent:
            attempt_ids = set()
            for value in params.values():
                if isinstance(value, list):
                    attempt_ids.update(value)
            items = [
                event
                for event in self._stores[PenEvent].values()
                if not attempt_ids or event.attempt_id in attempt_ids
            ]
            return FakeResult(items=items)

        raise AssertionError(f"FakeAsyncSession does not support query: {stmt}")

    def all_of(self, model: type) -> list[Any]:
        return list(self._stores[model].values())

    def _key_for(self, obj: Any) -> Any:
        self._apply_defaults(obj)
        if isinstance(obj, StudentSkillMemory):
            return (obj.student_id, obj.skill)
        return obj.id

    def _apply_defaults(self, obj: Any) -> None:
        if hasattr(obj, "id") and obj.id is None:
            obj.id = self._next_int_id if isinstance(obj, PenEvent) else uuid.uuid4()
            if isinstance(obj, PenEvent):
                self._next_int_id += 1

        if isinstance(obj, SessionConfig):
            obj.template_pin = obj.template_pin if hasattr(obj, "template_pin") else None
            obj.focus_source_exercise_id = obj.focus_source_exercise_id if hasattr(obj, "focus_source_exercise_id") else None
            obj.focus_mode = obj.focus_mode if obj.focus_mode is not None else False
            obj.difficulty_block_size = obj.difficulty_block_size or 30
            obj.focus_target_count = obj.focus_target_count or 300
            obj.fixation_density = getattr(obj, "fixation_density", None) or "fixa"
            obj.ranked_mode = getattr(obj, "ranked_mode", None) or False
            obj.arena_seed = getattr(obj, "arena_seed", None)
            obj.rules_version = getattr(obj, "rules_version", None) or ("arena_v1" if obj.ranked_mode else "free_v1")

        if isinstance(obj, Session):
            obj.started_at = obj.started_at or datetime.now(UTC)
            obj.page_count = obj.page_count or 0
            obj.exercise_count = obj.exercise_count or 0
            obj.restart_count = obj.restart_count or 0
            obj.status = obj.status or "active"
            obj.competitive_score = getattr(obj, "competitive_score", None) or 0
            obj.competitive_valid = getattr(obj, "competitive_valid", None)
            if obj.competitive_valid is None:
                obj.competitive_valid = True
            obj.audit_flags = getattr(obj, "audit_flags", None)

        if isinstance(obj, ExerciseAttempt):
            obj.stroke_count = obj.stroke_count or 0
            obj.erase_count = obj.erase_count or 0
            obj.pause_count = obj.pause_count or 0
            obj.recognized_latex = obj.recognized_latex or obj.recognized_answer
            obj.recognition_engine = obj.recognition_engine or "test"
            obj.analysis_reliable = obj.analysis_reliable if obj.analysis_reliable is not None else True
            obj.analysis_notes = obj.analysis_notes or "ok"
            obj.competitive_score = getattr(obj, "competitive_score", None) or 0
            obj.competitive_valid = getattr(obj, "competitive_valid", None)
            if obj.competitive_valid is None:
                obj.competitive_valid = True
            obj.audit_flags = getattr(obj, "audit_flags", None)
            obj.created_at = obj.created_at or datetime.now(UTC)

        if isinstance(obj, Exercise):
            obj.subject = obj.subject or "math"
            obj.canvas_mode = obj.canvas_mode or "calculation"
            obj.validator = obj.validator or "sympy"
            obj.source_license = obj.source_license or "proprietary_generated"
            obj.node_id = obj.node_id or ((obj.skill_tags or ["skill"])[0] + ".core")
            obj.template_id = obj.template_id or ((obj.skill_tags or ["skill"])[0] + ".family_000")
            obj.template_version = obj.template_version or 1
            obj.answer_type = obj.answer_type or "expression"

        if isinstance(obj, StudentSkillMemory):
            obj.accuracy = obj.accuracy if obj.accuracy is not None else 0.5
            obj.fluency = obj.fluency if obj.fluency is not None else 0.5
            obj.retention = obj.retention if obj.retention is not None else 0.5
            obj.velocity = obj.velocity if obj.velocity is not None else 0.5
            obj.stability = obj.stability if obj.stability is not None else 0.5
            obj.fixation = obj.fixation if obj.fixation is not None else 0.5
            obj.fatigue_avg = obj.fatigue_avg if obj.fatigue_avg is not None else 0.0
            obj.suggestion_cooldown_count = obj.suggestion_cooldown_count or 0
            obj.status = obj.status or "novo"
