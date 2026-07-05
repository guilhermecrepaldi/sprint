"""add last_practiced_at to student_skill_memory

Revision ID: 0007_add_last_practiced_at
Revises: 0006_add_skill_attempt_count
Create Date: 2026-05-25 00:00:00
"""
from collections.abc import Sequence

from alembic import op
import sqlalchemy as sa

revision: str = "0007_add_last_practiced_at"
down_revision: str | None = "0006_add_skill_attempt_count"
branch_labels: str | Sequence[str] | None = None
depends_on: str | Sequence[str] | None = None


def upgrade() -> None:
    op.add_column(
        "student_skill_memory",
        sa.Column(
            "last_practiced_at",
            sa.DateTime(timezone=True),
            nullable=False,
            server_default=sa.text("now()"),
        ),
    )


def downgrade() -> None:
    op.drop_column("student_skill_memory", "last_practiced_at")
