"""add attempt submit guard

Revision ID: 0003_add_attempt_submit_guard
Revises: 0002_add_folha_exercises
Create Date: 2026-05-24 00:05:00
"""
from collections.abc import Sequence

from alembic import op

revision: str = "0003_add_attempt_submit_guard"
down_revision: str | None = "0002_add_folha_exercises"
branch_labels: str | Sequence[str] | None = None
depends_on: str | Sequence[str] | None = None


def upgrade() -> None:
    op.create_unique_constraint(
        "uq_attempt_session_folha_field",
        "exercise_attempts",
        ["session_id", "folha_id", "field_index"],
    )


def downgrade() -> None:
    op.drop_constraint("uq_attempt_session_folha_field", "exercise_attempts", type_="unique")
