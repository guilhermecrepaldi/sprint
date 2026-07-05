"""initial

Revision ID: 0001_initial
Revises:
Create Date: 2026-05-23 23:00:00
"""
from collections.abc import Sequence

from alembic import op
import sqlalchemy as sa
from sqlalchemy.dialects import postgresql

revision: str = "0001_initial"
down_revision: str | None = None
branch_labels: str | Sequence[str] | None = None
depends_on: str | Sequence[str] | None = None


def upgrade() -> None:
    op.execute("CREATE EXTENSION IF NOT EXISTS pgcrypto")

    op.create_table(
        "students",
        sa.Column("id", postgresql.UUID(as_uuid=True), primary_key=True, server_default=sa.text("gen_random_uuid()")),
        sa.Column("name", sa.String(length=255), nullable=False),
        sa.Column("age", sa.Integer()),
        sa.Column("created_at", sa.DateTime(timezone=True), server_default=sa.func.now()),
    )
    op.create_table(
        "exercises",
        sa.Column("id", postgresql.UUID(as_uuid=True), primary_key=True, server_default=sa.text("gen_random_uuid()")),
        sa.Column("statement", sa.Text(), nullable=False),
        sa.Column("expected_answer", sa.Text(), nullable=False),
        sa.Column("skill_tags", postgresql.ARRAY(sa.Text()), nullable=False),
        sa.Column("difficulty", sa.Float(), nullable=False),
        sa.Column("estimated_time_ms", sa.Integer()),
        sa.Column("source_library", sa.String(length=100)),
        sa.CheckConstraint("difficulty BETWEEN 1.0 AND 10.0", name="ck_exercises_difficulty_range"),
    )
    op.create_table(
        "session_configs",
        sa.Column("id", postgresql.UUID(as_uuid=True), primary_key=True, server_default=sa.text("gen_random_uuid()")),
        sa.Column("student_id", postgresql.UUID(as_uuid=True), sa.ForeignKey("students.id")),
        sa.Column("show_thermometer", sa.Boolean(), server_default=sa.text("true")),
        sa.Column("background", sa.String(length=10), server_default=sa.text("'white'")),
        sa.Column("pen_color", sa.String(length=7), server_default=sa.text("'#1a1a1a'")),
        sa.Column("duration_mode", sa.String(length=20), server_default=sa.text("'unlimited'")),
        sa.Column("duration_limit_ms", sa.Integer()),
        sa.Column("pages_limit", sa.Integer()),
        sa.Column("difficulty_progression", sa.String(length=20), server_default=sa.text("'arithmetic'")),
        sa.Column("difficulty_start", sa.Float(), server_default=sa.text("2.0")),
        sa.Column("difficulty_step", sa.Float(), server_default=sa.text("0.5")),
        sa.Column("difficulty_ratio", sa.Float(), server_default=sa.text("1.15")),
        sa.Column("restart_on_avg", sa.Float()),
        sa.Column("restart_window", sa.Integer(), server_default=sa.text("10")),
        sa.Column("exercises_per_page", sa.Integer(), server_default=sa.text("5")),
        sa.Column("created_at", sa.DateTime(timezone=True), server_default=sa.func.now()),
    )
    op.create_table(
        "sessions",
        sa.Column("id", postgresql.UUID(as_uuid=True), primary_key=True, server_default=sa.text("gen_random_uuid()")),
        sa.Column("student_id", postgresql.UUID(as_uuid=True), sa.ForeignKey("students.id")),
        sa.Column("config_id", postgresql.UUID(as_uuid=True), sa.ForeignKey("session_configs.id")),
        sa.Column("started_at", sa.DateTime(timezone=True), server_default=sa.func.now()),
        sa.Column("ended_at", sa.DateTime(timezone=True)),
        sa.Column("current_difficulty", sa.Float(), nullable=False),
        sa.Column("page_count", sa.Integer(), server_default=sa.text("0")),
        sa.Column("exercise_count", sa.Integer(), server_default=sa.text("0")),
        sa.Column("restart_count", sa.Integer(), server_default=sa.text("0")),
        sa.Column("status", sa.String(length=20), server_default=sa.text("'active'")),
    )
    op.create_table(
        "folhas",
        sa.Column("id", postgresql.UUID(as_uuid=True), primary_key=True, server_default=sa.text("gen_random_uuid()")),
        sa.Column("session_id", postgresql.UUID(as_uuid=True), sa.ForeignKey("sessions.id")),
        sa.Column("page_index", sa.Integer(), nullable=False),
        sa.Column("difficulty", sa.Float(), nullable=False),
        sa.Column("created_at", sa.DateTime(timezone=True), server_default=sa.func.now()),
    )
    op.create_table(
        "exercise_attempts",
        sa.Column("id", postgresql.UUID(as_uuid=True), primary_key=True, server_default=sa.text("gen_random_uuid()")),
        sa.Column("folha_id", postgresql.UUID(as_uuid=True), sa.ForeignKey("folhas.id")),
        sa.Column("session_id", postgresql.UUID(as_uuid=True), sa.ForeignKey("sessions.id")),
        sa.Column("student_id", postgresql.UUID(as_uuid=True), sa.ForeignKey("students.id")),
        sa.Column("exercise_id", postgresql.UUID(as_uuid=True), sa.ForeignKey("exercises.id")),
        sa.Column("field_index", sa.Integer(), nullable=False),
        sa.Column("recognized_answer", sa.Text()),
        sa.Column("expected_answer", sa.Text(), nullable=False),
        sa.Column("is_correct", sa.Boolean()),
        sa.Column("score", sa.Integer()),
        sa.Column("total_time_ms", sa.Integer()),
        sa.Column("time_to_first_stroke_ms", sa.Integer()),
        sa.Column("stroke_count", sa.Integer(), server_default=sa.text("0")),
        sa.Column("erase_count", sa.Integer(), server_default=sa.text("0")),
        sa.Column("pause_count", sa.Integer(), server_default=sa.text("0")),
        sa.Column("average_pressure", sa.Float()),
        sa.Column("average_velocity", sa.Float()),
        sa.Column("error_type", sa.String(length=50)),
        sa.Column("ocr_confidence", sa.Float()),
        sa.Column("created_at", sa.DateTime(timezone=True), server_default=sa.func.now()),
    )
    op.create_table(
        "pen_events",
        sa.Column("id", sa.BigInteger(), primary_key=True, autoincrement=True),
        sa.Column("attempt_id", postgresql.UUID(as_uuid=True), sa.ForeignKey("exercise_attempts.id")),
        sa.Column("ts", sa.BigInteger(), nullable=False),
        sa.Column("x", sa.Float(), nullable=False),
        sa.Column("y", sa.Float(), nullable=False),
        sa.Column("pressure", sa.Float()),
        sa.Column("tilt", sa.Float()),
        sa.Column("velocity", sa.Float()),
        sa.Column("event_type", sa.String(length=20), nullable=False),
    )
    op.create_table(
        "cognitive_vectors",
        sa.Column("id", postgresql.UUID(as_uuid=True), primary_key=True, server_default=sa.text("gen_random_uuid()")),
        sa.Column("student_id", postgresql.UUID(as_uuid=True), sa.ForeignKey("students.id")),
        sa.Column("attempt_id", postgresql.UUID(as_uuid=True), sa.ForeignKey("exercise_attempts.id")),
        sa.Column("correctness", sa.Float()),
        sa.Column("speed_score", sa.Float()),
        sa.Column("hesitation_score", sa.Float()),
        sa.Column("fluency_score", sa.Float()),
        sa.Column("pressure_stability", sa.Float()),
        sa.Column("erase_score", sa.Float()),
        sa.Column("difficulty_level", sa.Float()),
        sa.Column("skill_vector", postgresql.JSONB()),
        sa.Column("fatigue_index", sa.Float()),
        sa.Column("created_at", sa.DateTime(timezone=True), server_default=sa.func.now()),
    )
    op.create_table(
        "student_skill_memory",
        sa.Column("student_id", postgresql.UUID(as_uuid=True), sa.ForeignKey("students.id"), nullable=False),
        sa.Column("skill", sa.String(length=100), nullable=False),
        sa.Column("accuracy", sa.Float(), server_default=sa.text("0.5")),
        sa.Column("fluency", sa.Float(), server_default=sa.text("0.5")),
        sa.Column("retention", sa.Float(), server_default=sa.text("0.5")),
        sa.Column("fatigue_avg", sa.Float(), server_default=sa.text("0.0")),
        sa.Column("status", sa.String(length=30), server_default=sa.text("'novo'")),
        sa.Column("last_updated", sa.DateTime(timezone=True), server_default=sa.func.now()),
        sa.PrimaryKeyConstraint("student_id", "skill", name="pk_student_skill_memory"),
    )


def downgrade() -> None:
    op.drop_table("student_skill_memory")
    op.drop_table("cognitive_vectors")
    op.drop_table("pen_events")
    op.drop_table("exercise_attempts")
    op.drop_table("folhas")
    op.drop_table("sessions")
    op.drop_table("session_configs")
    op.drop_table("exercises")
    op.drop_table("students")
