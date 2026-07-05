"""add skill_pin to session_configs

Revision ID: 0011_add_skill_pin
Revises: 0010_tracks_and_profiles
Create Date: 2026-05-25
"""
from collections.abc import Sequence

import sqlalchemy as sa
from alembic import op

revision: str = "0011_add_skill_pin"
down_revision: str | None = "0010_tracks_and_profiles"
branch_labels: str | Sequence[str] | None = None
depends_on: str | Sequence[str] | None = None


def upgrade() -> None:
    op.add_column(
        "session_configs",
        sa.Column("skill_pin", sa.String(80), nullable=True),
    )


def downgrade() -> None:
    op.drop_column("session_configs", "skill_pin")
