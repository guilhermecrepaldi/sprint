import uuid
from datetime import datetime

from sqlalchemy import Boolean, DateTime, Float, ForeignKey, Index, Integer, String, UniqueConstraint, func
from sqlalchemy.dialects.postgresql import ARRAY, UUID
from sqlalchemy.orm import Mapped, mapped_column

from db import Base


class SessionConfig(Base):
    __tablename__ = "session_configs"

    id: Mapped[uuid.UUID] = mapped_column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    student_id: Mapped[uuid.UUID | None] = mapped_column(UUID(as_uuid=True), ForeignKey("students.id"))
    contributor_mode: Mapped[bool] = mapped_column(default=False, server_default="false")
    show_thermometer: Mapped[bool] = mapped_column(default=True, server_default="true")
    background: Mapped[str] = mapped_column(String(10), default="white", server_default="white")
    pen_color: Mapped[str] = mapped_column(String(7), default="#1a1a1a", server_default="#1a1a1a")
    duration_mode: Mapped[str] = mapped_column(String(20), default="unlimited", server_default="unlimited")
    duration_limit_ms: Mapped[int | None] = mapped_column(Integer)
    pages_limit: Mapped[int | None] = mapped_column(Integer)
    difficulty_progression: Mapped[str] = mapped_column(String(20), default="arithmetic", server_default="arithmetic")
    difficulty_start: Mapped[float] = mapped_column(Float, default=2.0, server_default="2.0")
    difficulty_step: Mapped[float] = mapped_column(Float, default=0.5, server_default="0.5")
    difficulty_ratio: Mapped[float] = mapped_column(Float, default=1.15, server_default="1.15")
    restart_on_avg: Mapped[float | None] = mapped_column(Float)
    restart_window: Mapped[int] = mapped_column(Integer, default=10, server_default="10")
    exercises_per_page: Mapped[int] = mapped_column(Integer, default=5, server_default="5")
    subject: Mapped[str] = mapped_column(String(32), default="math", server_default="math")
    skill_pin: Mapped[str | None] = mapped_column(String(80), nullable=True)  # Fixação mode
    template_pin: Mapped[str | None] = mapped_column(String(140), nullable=True)
    focus_source_exercise_id: Mapped[uuid.UUID | None] = mapped_column(UUID(as_uuid=True), nullable=True)
    focus_mode: Mapped[bool] = mapped_column(default=False, server_default="false")
    difficulty_block_size: Mapped[int] = mapped_column(Integer, default=30, server_default="30")
    focus_target_count: Mapped[int] = mapped_column(Integer, default=300, server_default="300")
    fixation_density: Mapped[str] = mapped_column(String(20), default="fixa", server_default="fixa")
    ranked_mode: Mapped[bool] = mapped_column(Boolean, default=False, server_default="false")
    arena_seed: Mapped[str | None] = mapped_column(String(80), nullable=True)
    rules_version: Mapped[str] = mapped_column(String(40), default="free_v1", server_default="free_v1")
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), server_default=func.now())


class Session(Base):
    __tablename__ = "sessions"

    id: Mapped[uuid.UUID] = mapped_column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    student_id: Mapped[uuid.UUID | None] = mapped_column(UUID(as_uuid=True), ForeignKey("students.id"))
    config_id: Mapped[uuid.UUID | None] = mapped_column(UUID(as_uuid=True), ForeignKey("session_configs.id"))
    started_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), server_default=func.now())
    ended_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True))
    current_difficulty: Mapped[float] = mapped_column(Float, nullable=False)
    page_count: Mapped[int] = mapped_column(Integer, default=0, server_default="0")
    exercise_count: Mapped[int] = mapped_column(Integer, default=0, server_default="0")
    restart_count: Mapped[int] = mapped_column(Integer, default=0, server_default="0")
    status: Mapped[str] = mapped_column(String(20), default="active", server_default="active")
    duration_ms: Mapped[int] = mapped_column(Integer, default=0, server_default="0")
    session_accuracy: Mapped[float] = mapped_column(Float, default=0.0, server_default="0.0")
    xp: Mapped[int] = mapped_column(Integer, default=0, server_default="0")
    competitive_score: Mapped[int] = mapped_column(Integer, default=0, server_default="0")
    competitive_valid: Mapped[bool] = mapped_column(Boolean, default=True, server_default="true")
    audit_flags: Mapped[list[str] | None] = mapped_column(ARRAY(String))


class Folha(Base):
    __tablename__ = "folhas"

    id: Mapped[uuid.UUID] = mapped_column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    session_id: Mapped[uuid.UUID | None] = mapped_column(UUID(as_uuid=True), ForeignKey("sessions.id"))
    page_index: Mapped[int] = mapped_column(Integer, nullable=False)
    difficulty: Mapped[float] = mapped_column(Float, nullable=False)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), server_default=func.now())


class FolhaExercise(Base):
    __tablename__ = "folha_exercises"
    __table_args__ = (
        UniqueConstraint("folha_id", "field_index", name="uq_folha_exercises_field"),
        Index("ix_folha_exercises_folha", "folha_id"),
    )

    id: Mapped[uuid.UUID] = mapped_column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    folha_id: Mapped[uuid.UUID] = mapped_column(UUID(as_uuid=True), ForeignKey("folhas.id"), nullable=False)
    exercise_id: Mapped[uuid.UUID] = mapped_column(UUID(as_uuid=True), ForeignKey("exercises.id"), nullable=False)
    field_index: Mapped[int] = mapped_column(Integer, nullable=False)
