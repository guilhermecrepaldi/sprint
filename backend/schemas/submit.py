import uuid
from typing import Any, Literal

from pydantic import BaseModel, Field, model_validator

from schemas.session import FolhaOut


class PenEventIn(BaseModel):
    ts: int = Field(ge=0)
    x: float
    y: float
    pressure: float | None = None
    tilt: float | None = None
    velocity: float | None = None
    event_type: Literal["stroke_start", "stroke_move", "stroke_end", "erase", "clear", "undo", "redo"]


class FieldSubmit(BaseModel):
    field_index: int = Field(ge=0)
    exercise_id: uuid.UUID
    image_base64: str = Field(min_length=1)
    total_time_ms: int = Field(ge=0)
    time_to_first_stroke_ms: int = Field(default=0, ge=0)
    pen_events: list[PenEventIn] = Field(default_factory=list)
    # On-device recognition result (ML Kit or MyScript iink).
    # When present, validated locally with sympy — no Claude call for this field.
    recognized_text: str | None = None
    recognition_engine: str | None = None
    recognition_confidence: float | None = Field(default=None, ge=0.0, le=1.0)


class SubmitIn(BaseModel):
    folha_id: uuid.UUID
    submitted_at_ms: int | None = None
    fields: list[FieldSubmit] = Field(min_length=1)

    @model_validator(mode="after")
    def validate_unique_fields(self) -> "SubmitIn":
        field_indexes = [field.field_index for field in self.fields]
        if len(field_indexes) != len(set(field_indexes)):
            raise ValueError("field_index values must be unique within a submit")
        return self


class FieldResult(BaseModel):
    field_index: int
    recognized_answer: str | None
    expected_answer: str
    is_correct: bool
    score: int
    error_type: str | None
    vector: dict[str, Any]
    feedback: str = ""
    intervention_signal: str | None = None
    recognition_engine: str | None = None
    recognition_confidence: float | None = None
    analysis_reliable: bool | None = None


class ThermometerOut(BaseModel):
    value: float
    trend: str


class SubmitOut(BaseModel):
    results: list[FieldResult]
    page_score: int
    thermometer: ThermometerOut
    restart_triggered: bool
    session_status: str
    xp_earned: int = 0        # XP desta folha
    xp_total: int = 0         # XP acumulado do student
    next_folha: FolhaOut | None = None
