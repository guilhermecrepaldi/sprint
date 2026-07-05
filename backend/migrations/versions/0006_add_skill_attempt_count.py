"""add skill attempt count

Revision ID: 0006_add_skill_attempt_count
Revises: 0005_add_subject_fields
Create Date: 2026-05-25 00:00:00
"""
from collections.abc import Sequence

from alembic import op
import sqlalchemy as sa

revision: str = "0006_add_skill_attempt_count"
down_revision: str | None = "0005_add_subject_fields"
branch_labels: str | Sequence[str] | None = None
depends_on: str | Sequence[str] | None = None


def upgrade() -> None:
    op.add_column(
        "student_skill_memory",
        sa.Column("attempt_count", sa.Integer(), nullable=False, server_default="0"),
    )


def downgrade() -> None:
    op.drop_column("student_skill_memory", "attempt_count")
