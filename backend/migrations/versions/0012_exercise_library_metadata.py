"""exercise library metadata

Revision ID: 0012_exercise_library_metadata
Revises: 0011_add_skill_pin
Create Date: 2026-05-26
"""
from collections.abc import Sequence

import sqlalchemy as sa
from alembic import op
from sqlalchemy.dialects import postgresql

revision: str = "0012_exercise_library_metadata"
down_revision: str | None = "0011_add_skill_pin"
branch_labels: str | Sequence[str] | None = None
depends_on: str | Sequence[str] | None = None


def upgrade() -> None:
    op.add_column("exercises", sa.Column("source_license", sa.String(length=80), nullable=True))
    op.add_column("exercises", sa.Column("node_id", sa.String(length=100), nullable=True))
    op.add_column("exercises", sa.Column("template_id", sa.String(length=140), nullable=True))
    op.add_column("exercises", sa.Column("template_version", sa.Integer(), server_default="1", nullable=False))
    op.add_column("exercises", sa.Column("variant_seed", sa.Integer(), nullable=True))
    op.add_column("exercises", sa.Column("answer_type", sa.String(length=40), server_default="expression", nullable=False))
    op.add_column("exercises", sa.Column("method_tags", postgresql.ARRAY(sa.Text()), nullable=True))
    op.add_column("exercises", sa.Column("prerequisite_tags", postgresql.ARRAY(sa.Text()), nullable=True))
    op.add_column("exercises", sa.Column("affinity_tags", postgresql.ARRAY(sa.Text()), nullable=True))
    op.add_column("exercises", sa.Column("parameter_vector", postgresql.JSONB(astext_type=sa.Text()), nullable=True))
    op.add_column("exercises", sa.Column("difficulty_vector", postgresql.JSONB(astext_type=sa.Text()), nullable=True))
    op.create_index(op.f("ix_exercises_node_id"), "exercises", ["node_id"], unique=False)
    op.create_index(op.f("ix_exercises_template_id"), "exercises", ["template_id"], unique=False)

    op.add_column("exercise_attempts", sa.Column("node_id", sa.String(length=100), nullable=True))
    op.add_column("exercise_attempts", sa.Column("template_id", sa.String(length=140), nullable=True))
    op.add_column("exercise_attempts", sa.Column("method_tags", postgresql.ARRAY(sa.Text()), nullable=True))
    op.add_column("exercise_attempts", sa.Column("difficulty_vector", postgresql.JSONB(astext_type=sa.Text()), nullable=True))
    op.add_column("exercise_attempts", sa.Column("attempt_features", postgresql.JSONB(astext_type=sa.Text()), nullable=True))
    op.add_column("exercise_attempts", sa.Column("intervention_signal", sa.Text(), nullable=True))

    op.add_column("student_skill_memory", sa.Column("velocity", sa.Float(), server_default="0.5", nullable=False))
    op.add_column("student_skill_memory", sa.Column("stability", sa.Float(), server_default="0.5", nullable=False))
    op.add_column("student_skill_memory", sa.Column("fixation", sa.Float(), server_default="0.5", nullable=False))
    op.add_column("student_skill_memory", sa.Column("needs_training", sa.String(length=100), nullable=True))
    op.add_column("student_skill_memory", sa.Column("last_error_type", sa.String(length=50), nullable=True))
    op.add_column("student_skill_memory", sa.Column("suggestion_cooldown_count", sa.Integer(), server_default="0", nullable=False))


def downgrade() -> None:
    op.drop_column("student_skill_memory", "suggestion_cooldown_count")
    op.drop_column("student_skill_memory", "last_error_type")
    op.drop_column("student_skill_memory", "needs_training")
    op.drop_column("student_skill_memory", "fixation")
    op.drop_column("student_skill_memory", "stability")
    op.drop_column("student_skill_memory", "velocity")

    op.drop_column("exercise_attempts", "intervention_signal")
    op.drop_column("exercise_attempts", "attempt_features")
    op.drop_column("exercise_attempts", "difficulty_vector")
    op.drop_column("exercise_attempts", "method_tags")
    op.drop_column("exercise_attempts", "template_id")
    op.drop_column("exercise_attempts", "node_id")

    op.drop_index(op.f("ix_exercises_template_id"), table_name="exercises")
    op.drop_index(op.f("ix_exercises_node_id"), table_name="exercises")
    op.drop_column("exercises", "difficulty_vector")
    op.drop_column("exercises", "parameter_vector")
    op.drop_column("exercises", "affinity_tags")
    op.drop_column("exercises", "prerequisite_tags")
    op.drop_column("exercises", "method_tags")
    op.drop_column("exercises", "answer_type")
    op.drop_column("exercises", "variant_seed")
    op.drop_column("exercises", "template_version")
    op.drop_column("exercises", "template_id")
    op.drop_column("exercises", "node_id")
    op.drop_column("exercises", "source_license")
