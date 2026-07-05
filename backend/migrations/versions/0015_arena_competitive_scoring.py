"""arena competitive scoring

Revision ID: 0015_arena_competitive_scoring
Revises: 0014_focus_sprint_blocks
Create Date: 2026-05-27
"""
from collections.abc import Sequence

import sqlalchemy as sa
from alembic import op
from sqlalchemy.dialects import postgresql

revision: str = "0015_arena_competitive_scoring"
down_revision: str | None = "0014_focus_sprint_blocks"
branch_labels: str | Sequence[str] | None = None
depends_on: str | Sequence[str] | None = None


def upgrade() -> None:
    op.add_column("session_configs", sa.Column("ranked_mode", sa.Boolean(), server_default="false", nullable=False))
    op.add_column("session_configs", sa.Column("arena_seed", sa.String(length=80), nullable=True))
    op.add_column("session_configs", sa.Column("rules_version", sa.String(length=40), server_default="free_v1", nullable=False))

    op.add_column("sessions", sa.Column("competitive_score", sa.Integer(), server_default="0", nullable=False))
    op.add_column("sessions", sa.Column("competitive_valid", sa.Boolean(), server_default="true", nullable=False))
    op.add_column("sessions", sa.Column("audit_flags", postgresql.ARRAY(sa.String()), nullable=True))

    op.add_column("exercise_attempts", sa.Column("competitive_score", sa.Integer(), server_default="0", nullable=False))
    op.add_column("exercise_attempts", sa.Column("competitive_valid", sa.Boolean(), server_default="true", nullable=False))
    op.add_column("exercise_attempts", sa.Column("audit_flags", postgresql.ARRAY(sa.Text()), nullable=True))


def downgrade() -> None:
    op.drop_column("exercise_attempts", "audit_flags")
    op.drop_column("exercise_attempts", "competitive_valid")
    op.drop_column("exercise_attempts", "competitive_score")

    op.drop_column("sessions", "audit_flags")
    op.drop_column("sessions", "competitive_valid")
    op.drop_column("sessions", "competitive_score")

    op.drop_column("session_configs", "rules_version")
    op.drop_column("session_configs", "arena_seed")
    op.drop_column("session_configs", "ranked_mode")
