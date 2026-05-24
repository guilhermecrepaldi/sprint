"""add session_accuracy and duration_ms to sessions

Revision ID: 0008_session_metrics
Revises: 0007_add_last_practiced_at
Create Date: 2026-05-25 00:00:00
"""
from collections.abc import Sequence

from alembic import op
import sqlalchemy as sa

revision: str = "0008_session_metrics"
down_revision: str | None = "0007_add_last_practiced_at"
branch_labels: str | Sequence[str] | None = None
depends_on: str | Sequence[str] | None = None


def upgrade() -> None:
    op.add_column(
        "sessions",
        sa.Column("duration_ms", sa.Integer(), nullable=False, server_default="0"),
    )
    op.add_column(
        "sessions",
        sa.Column("session_accuracy", sa.Float(), nullable=False, server_default="0.0"),
    )


def downgrade() -> None:
    op.drop_column("sessions", "session_accuracy")
    op.drop_column("sessions", "duration_ms")
