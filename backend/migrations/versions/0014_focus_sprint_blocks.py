"""focus sprint blocks

Revision ID: 0014_focus_sprint_blocks
Revises: 0013_attempt_recognition_audit
Create Date: 2026-05-26
"""
from collections.abc import Sequence

import sqlalchemy as sa
from alembic import op
from sqlalchemy.dialects import postgresql

revision: str = "0014_focus_sprint_blocks"
down_revision: str | None = "0013_attempt_recognition_audit"
branch_labels: str | Sequence[str] | None = None
depends_on: str | Sequence[str] | None = None


def upgrade() -> None:
    op.add_column("session_configs", sa.Column("template_pin", sa.String(length=140), nullable=True))
    op.add_column("session_configs", sa.Column("focus_source_exercise_id", postgresql.UUID(as_uuid=True), nullable=True))
    op.add_column("session_configs", sa.Column("focus_mode", sa.Boolean(), server_default="false", nullable=False))
    op.add_column("session_configs", sa.Column("difficulty_block_size", sa.Integer(), server_default="30", nullable=False))
    op.add_column("session_configs", sa.Column("focus_target_count", sa.Integer(), server_default="300", nullable=False))
    op.add_column("session_configs", sa.Column("fixation_density", sa.String(length=20), server_default="fixa", nullable=False))


def downgrade() -> None:
    op.drop_column("session_configs", "fixation_density")
    op.drop_column("session_configs", "focus_target_count")
    op.drop_column("session_configs", "difficulty_block_size")
    op.drop_column("session_configs", "focus_mode")
    op.drop_column("session_configs", "focus_source_exercise_id")
    op.drop_column("session_configs", "template_pin")
