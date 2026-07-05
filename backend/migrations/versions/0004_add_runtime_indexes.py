"""add runtime indexes

Revision ID: 0004_add_runtime_indexes
Revises: 0003_add_attempt_submit_guard
Create Date: 2026-05-24 00:20:00
"""
from collections.abc import Sequence

from alembic import op

revision: str = "0004_add_runtime_indexes"
down_revision: str | None = "0003_add_attempt_submit_guard"
branch_labels: str | Sequence[str] | None = None
depends_on: str | Sequence[str] | None = None


def upgrade() -> None:
    op.create_index(
        "ix_exercise_attempts_session_created",
        "exercise_attempts",
        ["session_id", "created_at"],
    )
    op.create_index("ix_folha_exercises_folha", "folha_exercises", ["folha_id"])
    op.create_index(
        "ix_student_skill_memory_student_status",
        "student_skill_memory",
        ["student_id", "status"],
    )


def downgrade() -> None:
    op.drop_index("ix_student_skill_memory_student_status", table_name="student_skill_memory")
    op.drop_index("ix_folha_exercises_folha", table_name="folha_exercises")
    op.drop_index("ix_exercise_attempts_session_created", table_name="exercise_attempts")
