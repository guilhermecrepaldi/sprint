"""tracks table and student public profile fields

Revision ID: 0010_tracks_and_profiles
Revises: 0009_contributor_mode
Create Date: 2026-05-24 00:00:00
"""
from collections.abc import Sequence

import sqlalchemy as sa
from alembic import op
from sqlalchemy.dialects import postgresql

revision: str = "0010_tracks_and_profiles"
down_revision: str | None = "0009_contributor_mode"
branch_labels: str | Sequence[str] | None = None
depends_on: str | Sequence[str] | None = None


def upgrade() -> None:
    # ── tracks ───────────────────────────────────────────────────────────────
    op.create_table(
        "tracks",
        sa.Column("id", postgresql.UUID(as_uuid=True), primary_key=True, server_default=sa.text("gen_random_uuid()")),
        sa.Column("slug", sa.String(60), nullable=False),
        sa.Column("name", sa.String(120), nullable=False),
        sa.Column("description", sa.Text, nullable=False, server_default=""),
        sa.Column("skill_tags", postgresql.ARRAY(sa.String), nullable=False),
        sa.Column("display_order", sa.Integer, nullable=False, server_default="0"),
        sa.Column("created_at", sa.DateTime(timezone=True), server_default=sa.text("now()")),
        sa.UniqueConstraint("slug", name="uq_tracks_slug"),
    )

    # ── students: slug público + visibilidade ────────────────────────────────
    op.add_column("students", sa.Column(
        "slug", sa.String(80), nullable=True,
    ))
    op.create_unique_constraint("uq_students_slug", "students", ["slug"])

    op.add_column("students", sa.Column(
        "is_public", sa.Boolean, nullable=False, server_default="false",
    ))


def downgrade() -> None:
    op.drop_constraint("uq_students_slug", "students", type_="unique")
    op.drop_column("students", "is_public")
    op.drop_column("students", "slug")
    op.drop_table("tracks")
