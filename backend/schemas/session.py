import uuid
from typing import Literal

from pydantic import BaseModel, Field


class SessionConfigIn(BaseModel):
    show_thermometer: bool = True
    background: Literal["white", "dark"] = "white"
    pen_color: str = Field(default="#1a1a1a", pattern=r"^#[0-9a-fA-F]{6}$")
    duration_mode: Literal["unlimited", "timed", "pages"] = "unlimited"
    duration_limit_ms: int | None = None
    pages_limit: int | None = None
    difficulty_progression: Literal["arithmetic", "geometric"] = "arithmetic"
    difficulty_start: float = Field(default=2.0, ge=1.0, le=10.0)
    difficulty_step: float = Field(default=0.5, gt=0)
    difficulty_ratio: float = Field(default=1.15, gt=1.0)
    restart_on_avg: float | None = Field(default=None, ge=0.0, le=10.0)
    restart_window: int = Field(default=10, ge=1)
    exercises_per_page: int = Field(default=5, ge=1, le=20)


class FolhaField(BaseModel):
    field_index: int
    exercise_id: uuid.UUID
    statement: str
    skill_tags: list[str]
    estimated_time_ms: int | None = None


class FolhaOut(BaseModel):
    folha_id: uuid.UUID
    page_index: int
    difficulty: float
    fields: list[FolhaField]


class SessionStartIn(BaseModel):
    student_id: uuid.UUID
    config: SessionConfigIn = Field(default_factory=SessionConfigIn)


class SessionStartOut(BaseModel):
    session_id: uuid.UUID
    config_id: uuid.UUID
    first_folha: FolhaOut
