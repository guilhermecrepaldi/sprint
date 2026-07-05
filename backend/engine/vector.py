import statistics
from typing import Any

from models.attempt import ExerciseAttempt
from models.exercise import Exercise


def _clamp(value: float, lower: float = 0.0, upper: float = 1.0) -> float:
    return min(upper, max(lower, value))


def _event_value(event: Any, name: str, default=None):
    if isinstance(event, dict):
        return event.get(name, default)
    return getattr(event, name, default)


def generate_vector(attempt: ExerciseAttempt, events: list[Any], exercise: Exercise) -> dict:
    estimated_time_ms = exercise.estimated_time_ms or 45000
    total_time_ms = attempt.total_time_ms or estimated_time_ms
    stroke_count = max(1, attempt.stroke_count)
    pause_count = attempt.pause_count
    erase_count = attempt.erase_count

    pressures = [float(value) for event in events if (value := _event_value(event, "pressure")) is not None]
    if len(pressures) > 1 and statistics.mean(pressures) > 0:
        pressure_stability = 1 - statistics.pstdev(pressures) / statistics.mean(pressures)
    else:
        pressure_stability = 1.0 if pressures else 0.5

    fatigue_index = _clamp((attempt.field_index or 0) / 20)

    return {
        "correctness": 1.0 if attempt.is_correct else 0.0,
        "speed_score": _clamp(1 - (total_time_ms / max(1, estimated_time_ms))),
        "hesitation_score": _clamp((attempt.time_to_first_stroke_ms or 0) / 5000),
        "fluency_score": _clamp(1 - (pause_count / stroke_count)),
        "pressure_stability": _clamp(pressure_stability),
        "erase_score": _clamp(1 - min(1, erase_count / 3)),
        "difficulty_level": exercise.difficulty,
        "skill_vector": {skill: 1.0 for skill in exercise.skill_tags},
        "fatigue_index": fatigue_index,
    }
