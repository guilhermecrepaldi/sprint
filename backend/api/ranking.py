from datetime import UTC, datetime, timedelta

from fastapi import APIRouter, Depends, Query
from pydantic import BaseModel
from sqlalchemy import func, select
from sqlalchemy.ext.asyncio import AsyncSession

from db import get_db
from models.session import Session
from models.student import Student

router = APIRouter()


class RankingEntry(BaseModel):
    rank: int
    student_name: str
    slug: str | None
    xp_week: int
    xp_total: int


class WeeklyRankingOut(BaseModel):
    entries: list[RankingEntry]
    week_start: str  # ISO date (YYYY-MM-DD)


@router.get("/api/ranking/weekly", response_model=WeeklyRankingOut)
async def weekly_ranking(
    limit: int = Query(default=20, ge=1, le=100),
    db: AsyncSession = Depends(get_db),
) -> WeeklyRankingOut:
    week_start = datetime.now(UTC) - timedelta(days=7)

    # XP ganho na semana por student (soma das sessions dos últimos 7 dias)
    xp_week_subq = (
        select(
            Session.student_id.label("student_id"),
            func.coalesce(func.sum(Session.xp), 0).label("xp_week"),
        )
        .where(
            Session.student_id.is_not(None),
            Session.started_at >= week_start,
        )
        .group_by(Session.student_id)
        .subquery()
    )

    result = await db.execute(
        select(
            Student.name,
            Student.slug,
            Student.xp_total,
            func.coalesce(xp_week_subq.c.xp_week, 0).label("xp_week"),
        )
        .outerjoin(xp_week_subq, Student.id == xp_week_subq.c.student_id)
        .where(Student.is_public == True)  # noqa: E712
        .order_by(
            func.coalesce(xp_week_subq.c.xp_week, 0).desc(),
            Student.xp_total.desc(),
        )
        .limit(limit)
    )

    rows = result.all()
    entries = [
        RankingEntry(
            rank=i + 1,
            student_name=row.name,
            slug=row.slug,
            xp_week=row.xp_week,
            xp_total=row.xp_total,
        )
        for i, row in enumerate(rows)
    ]

    return WeeklyRankingOut(
        entries=entries,
        week_start=week_start.date().isoformat(),
    )
