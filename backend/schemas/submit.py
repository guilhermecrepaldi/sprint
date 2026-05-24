import uuid
from typing import Any

from pydantic import BaseModel, Field

from schemas.session import FolhaOut


class PenEventIn(BaseModel):
    ts: int
    x: float
    y: float
    pressure: float | None = None
    tilt: float | None = None
    velocity: float | None = None
    event_type: str


class FieldSubmit(BaseModel):
    field_index: int
    exercise_id: uuid.UUID
    image_base64: str
    total_time_ms: int = Field(ge=0)
    time_to_first_stroke_ms: int = Field(default=0, ge=0)
    pen_events: list[PenEventIn] = Field(default_factory=list)


class SubmitIn(BaseModel):
    folha_id: uuid.UUID
    submitted_at_ms: int | None = None
    fields: list[FieldSubmit]


class FieldResult(BaseModel):
    field_index: int
    recognized_answer: str | None
    expected_answer: str
    is_correct: bool
    score: int
    error_type: str | None
    vector: dict[str, Any]


class ThermometerOut(BaseModel):
    value: float
    trend: str


class SubmitOut(BaseModel):
    results: list[FieldResult]
    page_score: int
    thermometer: ThermometerOut
    restart_triggered: bool
    session_status: str
    next_folha: FolhaOut | None = None
