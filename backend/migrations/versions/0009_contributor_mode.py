"""contributor mode: xp, verified exercises, student role, calibrated difficulty

Revision ID: 0009_contributor_mode
Revises: 0008_session_metrics
Create Date: 2026-05-24 00:00:00
"""
from collections.abc import Sequence

import sqlalchemy as sa
from alembic import op

revision: str = "0009_contributor_mode"
down_revision: str | None = "0008_session_metrics"
branch_labels: str | Sequence[str] | None = None
depends_on: str | Sequence[str] | None = None


def upgrade() -> None:
    # Student: papel (student | contributor | admin) + XP total acumulado
    op.add_column("students", sa.Column("role", sa.String(20), nullable=False, server_default="student"))
    op.add_column("students", sa.Column("xp_total", sa.Integer(), nullable=False, server_default="0"))

    # SessionConfig: modo contribuidor liga calibração automática
    op.add_column("session_configs", sa.Column("contributor_mode", sa.Boolean(), nullable=False, server_default="false"))

    # Session: XP ganho nessa sessão
    op.add_column("sessions", sa.Column("xp", sa.Integer(), nullable=False, server_default="0"))

    # Exercise: quantos contributors acertaram (valida o exercício)
    # + tempo médio real medido em sessões contributor
    # + dificuldade recalibrada a partir dos tempos reais
    op.add_column("exercises", sa.Column("verified_count", sa.Integer(), nullable=False, server_default="0"))
    op.add_column("exercises", sa.Column("expert_avg_time_ms", sa.Integer(), nullable=True))
    op.add_column("exercises", sa.Column("calibrated_difficulty", sa.Float(), nullable=True))


def downgrade() -> None:
    op.drop_column("exercises", "calibrated_difficulty")
    op.drop_column("exercises", "expert_avg_time_ms")
    op.drop_column("exercises", "verified_count")
    op.drop_column("sessions", "xp")
    op.drop_column("session_configs", "contributor_mode")
    op.drop_column("students", "xp_total")
    op.drop_column("students", "role")
