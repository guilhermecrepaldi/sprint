import uuid

from sqlalchemy import CheckConstraint, Float, Integer, String, Text
from sqlalchemy.dialects.postgresql import ARRAY, JSONB, UUID
from sqlalchemy.orm import Mapped, mapped_column

from db import Base


class Exercise(Base):
    __tablename__ = "exercises"
    __table_args__ = (
        CheckConstraint("difficulty BETWEEN 1.0 AND 10.0", name="ck_exercises_difficulty_range"),
    )

    id: Mapped[uuid.UUID] = mapped_column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    statement: Mapped[str] = mapped_column(Text, nullable=False)
    expected_answer: Mapped[str] = mapped_column(Text, nullable=False)
    skill_tags: Mapped[list[str]] = mapped_column(ARRAY(Text), nullable=False)
    difficulty: Mapped[float] = mapped_column(Float, nullable=False)
    estimated_time_ms: Mapped[int | None] = mapped_column(Integer)
    source_library: Mapped[str | None] = mapped_column(String(100))
    source_license: Mapped[str | None] = mapped_column(String(80))
    subject: Mapped[str] = mapped_column(String(32), default="math", server_default="math", index=True)
    canvas_mode: Mapped[str] = mapped_column(String(32), default="calculation", server_default="calculation")
    validator: Mapped[str] = mapped_column(String(32), default="sympy", server_default="sympy")
    node_id: Mapped[str | None] = mapped_column(String(100), index=True)
    template_id: Mapped[str | None] = mapped_column(String(140), index=True)
    template_version: Mapped[int] = mapped_column(Integer, default=1, server_default="1")
    variant_seed: Mapped[int | None] = mapped_column(Integer)
    answer_type: Mapped[str] = mapped_column(String(40), default="expression", server_default="expression")
    method_tags: Mapped[list[str] | None] = mapped_column(ARRAY(Text))
    prerequisite_tags: Mapped[list[str] | None] = mapped_column(ARRAY(Text))
    affinity_tags: Mapped[list[str] | None] = mapped_column(ARRAY(Text))
    parameter_vector: Mapped[dict | None] = mapped_column(JSONB)
    difficulty_vector: Mapped[dict | None] = mapped_column(JSONB)
    # Calibração por contributors: quantos acertaram e tempo médio real
    verified_count: Mapped[int] = mapped_column(Integer, default=0, server_default="0")
    expert_avg_time_ms: Mapped[int | None] = mapped_column(Integer)
    calibrated_difficulty: Mapped[float | None] = mapped_column(Float)
