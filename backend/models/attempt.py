import uuid
from datetime import datetime

from sqlalchemy import BigInteger, Boolean, DateTime, Float, ForeignKey, Index, Integer, String, Text, UniqueConstraint, func
from sqlalchemy.dialects.postgresql import ARRAY, JSONB, UUID
from sqlalchemy.orm import Mapped, mapped_column

from db import Base


class ExerciseAttempt(Base):
    __tablename__ = "exercise_attempts"
    __table_args__ = (
        UniqueConstraint("session_id", "folha_id", "field_index", name="uq_attempt_session_folha_field"),
        Index("ix_exercise_attempts_session_created", "session_id", "created_at"),
    )

    id: Mapped[uuid.UUID] = mapped_column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    folha_id: Mapped[uuid.UUID | None] = mapped_column(UUID(as_uuid=True), ForeignKey("folhas.id"))
    session_id: Mapped[uuid.UUID | None] = mapped_column(UUID(as_uuid=True), ForeignKey("sessions.id"))
    student_id: Mapped[uuid.UUID | None] = mapped_column(UUID(as_uuid=True), ForeignKey("students.id"))
    exercise_id: Mapped[uuid.UUID | None] = mapped_column(UUID(as_uuid=True), ForeignKey("exercises.id"))
    field_index: Mapped[int] = mapped_column(Integer, nullable=False)
    recognized_answer: Mapped[str | None] = mapped_column(Text)
    expected_answer: Mapped[str] = mapped_column(Text, nullable=False)
    is_correct: Mapped[bool | None] = mapped_column(Boolean)
    score: Mapped[int | None] = mapped_column(Integer)
    total_time_ms: Mapped[int | None] = mapped_column(Integer)
    time_to_first_stroke_ms: Mapped[int | None] = mapped_column(Integer)
    stroke_count: Mapped[int] = mapped_column(Integer, default=0, server_default="0")
    erase_count: Mapped[int] = mapped_column(Integer, default=0, server_default="0")
    pause_count: Mapped[int] = mapped_column(Integer, default=0, server_default="0")
    average_pressure: Mapped[float | None] = mapped_column(Float)
    average_velocity: Mapped[float | None] = mapped_column(Float)
    error_type: Mapped[str | None] = mapped_column(String(50))
    ocr_confidence: Mapped[float | None] = mapped_column(Float)
    recognized_latex: Mapped[str | None] = mapped_column(Text)
    recognition_engine: Mapped[str | None] = mapped_column(String(60))
    analysis_reliable: Mapped[bool | None] = mapped_column(Boolean)
    analysis_notes: Mapped[str | None] = mapped_column(Text)
    node_id: Mapped[str | None] = mapped_column(String(100))
    template_id: Mapped[str | None] = mapped_column(String(140))
    method_tags: Mapped[list[str] | None] = mapped_column(ARRAY(Text))
    difficulty_vector: Mapped[dict | None] = mapped_column(JSONB)
    attempt_features: Mapped[dict | None] = mapped_column(JSONB)
    intervention_signal: Mapped[str | None] = mapped_column(Text)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), server_default=func.now())


class PenEvent(Base):
    __tablename__ = "pen_events"

    id: Mapped[int] = mapped_column(BigInteger, primary_key=True, autoincrement=True)
    attempt_id: Mapped[uuid.UUID | None] = mapped_column(UUID(as_uuid=True), ForeignKey("exercise_attempts.id"))
    ts: Mapped[int] = mapped_column(BigInteger, nullable=False)
    x: Mapped[float] = mapped_column(Float, nullable=False)
    y: Mapped[float] = mapped_column(Float, nullable=False)
    pressure: Mapped[float | None] = mapped_column(Float)
    tilt: Mapped[float | None] = mapped_column(Float)
    velocity: Mapped[float | None] = mapped_column(Float)
    event_type: Mapped[str] = mapped_column(String(20), nullable=False)
