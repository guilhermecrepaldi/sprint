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

    def seed_exercises(self, count: int = 8) -> list[Exercise]:
        exercises = []
        for index in range(count):
            exercise = Exercise(
                id=uuid.uuid4(),
                statement=f"Resolva: x + {index} = {index + 5}",
                expected_answer="x = 5",
                skill_tags=["equacao_1_grau"],
                difficulty=2.0 + min(index, 4) * 0.2,
                estimated_time_ms=30000,
                source_library="test",
            )
            self.add(exercise)
            exercises.append(exercise)
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

        if entity is FolhaExercise:
            folha_id = params.get("folha_id_1")
            items = [
                item
                for item in self._stores[FolhaExercise].values()
                if item.folha_id == folha_id
            ]
            return FakeResult(items=sorted(items, key=lambda item: item.field_index))

        if entity is StudentSkillMemory:
            student_id = params.get("student_id_1")
            items = [
                item
                for item in self._stores[StudentSkillMemory].values()
                if item.student_id == student_id
            ]
            return FakeResult(items=items)

        if entity is Exercise:
            lower = params.get("difficulty_1", 1.0)
            upper = params.get("difficulty_2", 10.0)
            limit = params.get("param_1")
            excluded = set()
            for key, value in params.items():
                if key.startswith("id_"):
                    excluded.update(value if isinstance(value, list) else [value])
            items = [
                exercise
                for exercise in self._stores[Exercise].values()
                if lower <= exercise.difficulty <= upper and exercise.id not in excluded
            ]
            items.sort(key=lambda exercise: (abs(exercise.difficulty - ((lower + upper) / 2)), str(exercise.id)))
            return FakeResult(items=items[:limit])

        if entity is ExerciseAttempt:
            session_id = params.get("session_id_1")
            limit = params.get("param_1")
            if descriptions[0]["name"] == "score":
                scores = [
                    attempt.score
                    for attempt in self._stores[ExerciseAttempt].values()
                    if attempt.session_id == session_id and attempt.score is not None
                ]
                return FakeResult(items=scores[-limit:] if limit else scores)

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

        if isinstance(obj, Session):
            obj.started_at = obj.started_at or datetime.now(UTC)
            obj.page_count = obj.page_count or 0
            obj.exercise_count = obj.exercise_count or 0
            obj.restart_count = obj.restart_count or 0
            obj.status = obj.status or "active"

        if isinstance(obj, ExerciseAttempt):
            obj.stroke_count = obj.stroke_count or 0
            obj.erase_count = obj.erase_count or 0
            obj.pause_count = obj.pause_count or 0
            obj.created_at = obj.created_at or datetime.now(UTC)

        if isinstance(obj, StudentSkillMemory):
            obj.accuracy = obj.accuracy if obj.accuracy is not None else 0.5
            obj.fluency = obj.fluency if obj.fluency is not None else 0.5
            obj.retention = obj.retention if obj.retention is not None else 0.5
            obj.fatigue_avg = obj.fatigue_avg if obj.fatigue_avg is not None else 0.0
            obj.status = obj.status or "novo"
