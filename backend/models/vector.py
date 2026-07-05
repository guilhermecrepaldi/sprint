import uuid
from datetime import datetime

from sqlalchemy import DateTime, Float, ForeignKey, Index, Integer, PrimaryKeyConstraint, String, func
from sqlalchemy.dialects.postgresql import JSONB, UUID
from sqlalchemy.orm import Mapped, mapped_column

from db import Base


class CognitiveVector(Base):
    __tablename__ = "cognitive_vectors"

    id: Mapped[uuid.UUID] = mapped_column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    student_id: Mapped[uuid.UUID | None] = mapped_column(UUID(as_uuid=True), ForeignKey("students.id"))
    attempt_id: Mapped[uuid.UUID | None] = mapped_column(UUID(as_uuid=True), ForeignKey("exercise_attempts.id"))
    correctness: Mapped[float | None] = mapped_column(Float)
    speed_score: Mapped[float | None] = mapped_column(Float)
    hesitation_score: Mapped[float | None] = mapped_column(Float)
    fluency_score: Mapped[float | None] = mapped_column(Float)
    pressure_stability: Mapped[float | None] = mapped_column(Float)
    erase_score: Mapped[float | None] = mapped_column(Float)
    difficulty_level: Mapped[float | None] = mapped_column(Float)
    skill_vector: Mapped[dict | None] = mapped_column(JSONB)
    fatigue_index: Mapped[float | None] = mapped_column(Float)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), server_default=func.now())


class StudentSkillMemory(Base):
    __tablename__ = "student_skill_memory"
    __table_args__ = (
        PrimaryKeyConstraint("student_id", "skill", name="pk_student_skill_memory"),
        Index("ix_student_skill_memory_student_status", "student_id", "status"),
    )

    student_id: Mapped[uuid.UUID] = mapped_column(UUID(as_uuid=True), ForeignKey("students.id"))
    skill: Mapped[str] = mapped_column(String(100))
    accuracy: Mapped[float] = mapped_column(Float, default=0.5, server_default="0.5")
    fluency: Mapped[float] = mapped_column(Float, default=0.5, server_default="0.5")
    retention: Mapped[float] = mapped_column(Float, default=0.5, server_default="0.5")
    velocity: Mapped[float] = mapped_column(Float, default=0.5, server_default="0.5")
    stability: Mapped[float] = mapped_column(Float, default=0.5, server_default="0.5")
    fixation: Mapped[float] = mapped_column(Float, default=0.5, server_default="0.5")
    fatigue_avg: Mapped[float] = mapped_column(Float, default=0.0, server_default="0.0")
    attempt_count: Mapped[int] = mapped_column(Integer, default=0, server_default="0")
    status: Mapped[str] = mapped_column(String(30), default="novo", server_default="novo")
    needs_training: Mapped[str | None] = mapped_column(String(100))
    last_error_type: Mapped[str | None] = mapped_column(String(50))
    suggestion_cooldown_count: Mapped[int] = mapped_column(Integer, default=0, server_default="0")
    last_practiced_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), server_default=func.now())
    last_updated: Mapped[datetime] = mapped_column(DateTime(timezone=True), server_default=func.now(), onupdate=func.now())
