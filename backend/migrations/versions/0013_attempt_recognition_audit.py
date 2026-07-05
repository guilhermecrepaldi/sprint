"""attempt recognition audit

Revision ID: 0013_attempt_recognition_audit
Revises: 0012_exercise_library_metadata
Create Date: 2026-05-26
"""
from collections.abc import Sequence

import sqlalchemy as sa
from alembic import op

revision: str = "0013_attempt_recognition_audit"
down_revision: str | None = "0012_exercise_library_metadata"
branch_labels: str | Sequence[str] | None = None
depends_on: str | Sequence[str] | None = None


def upgrade() -> None:
    op.add_column("exercise_attempts", sa.Column("recognized_latex", sa.Text(), nullable=True))
    op.add_column("exercise_attempts", sa.Column("recognition_engine", sa.String(length=60), nullable=True))
    op.add_column("exercise_attempts", sa.Column("analysis_reliable", sa.Boolean(), nullable=True))
    op.add_column("exercise_attempts", sa.Column("analysis_notes", sa.Text(), nullable=True))


def downgrade() -> None:
    op.drop_column("exercise_attempts", "analysis_notes")
    op.drop_column("exercise_attempts", "analysis_reliable")
    op.drop_column("exercise_attempts", "recognition_engine")
    op.drop_column("exercise_attempts", "recognized_latex")
