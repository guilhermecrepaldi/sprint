"""add subject fields

Revision ID: 0005_add_subject_fields
Revises: 0004_add_runtime_indexes
Create Date: 2026-05-24 12:45:00
"""
from collections.abc import Sequence

from alembic import op
import sqlalchemy as sa

revision: str = "0005_add_subject_fields"
down_revision: str | None = "0004_add_runtime_indexes"
branch_labels: str | Sequence[str] | None = None
depends_on: str | Sequence[str] | None = None


def upgrade() -> None:
    op.add_column("exercises", sa.Column("subject", sa.String(32), nullable=False, server_default="math"))
    op.add_column(
        "exercises",
        sa.Column("canvas_mode", sa.String(32), nullable=False, server_default="calculation"),
    )
    op.add_column("exercises", sa.Column("validator", sa.String(32), nullable=False, server_default="sympy"))
    op.create_index("ix_exercises_subject", "exercises", ["subject"])
    op.add_column("session_configs", sa.Column("subject", sa.String(32), nullable=False, server_default="math"))


def downgrade() -> None:
    op.drop_column("session_configs", "subject")
    op.drop_index("ix_exercises_subject", table_name="exercises")
    op.drop_column("exercises", "validator")
    op.drop_column("exercises", "canvas_mode")
    op.drop_column("exercises", "subject")
