"""add folha exercises

Revision ID: 0002_add_folha_exercises
Revises: 0001_initial
Create Date: 2026-05-23 23:35:00
"""
from collections.abc import Sequence

from alembic import op
import sqlalchemy as sa
from sqlalchemy.dialects import postgresql

revision: str = "0002_add_folha_exercises"
down_revision: str | None = "0001_initial"
branch_labels: str | Sequence[str] | None = None
depends_on: str | Sequence[str] | None = None


def upgrade() -> None:
    op.create_table(
        "folha_exercises",
        sa.Column("id", postgresql.UUID(as_uuid=True), primary_key=True, server_default=sa.text("gen_random_uuid()")),
        sa.Column("folha_id", postgresql.UUID(as_uuid=True), sa.ForeignKey("folhas.id"), nullable=False),
        sa.Column("exercise_id", postgresql.UUID(as_uuid=True), sa.ForeignKey("exercises.id"), nullable=False),
        sa.Column("field_index", sa.Integer(), nullable=False),
    )
    op.create_unique_constraint("uq_folha_exercises_field", "folha_exercises", ["folha_id", "field_index"])


def downgrade() -> None:
    op.drop_constraint("uq_folha_exercises_field", "folha_exercises", type_="unique")
    op.drop_table("folha_exercises")
