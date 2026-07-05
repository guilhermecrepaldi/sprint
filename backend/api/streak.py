"""
Gerenciamento de streaks (dias consecutivos) do aluno.

GET /api/student/{student_id}/streak
  → Streak atual, recorde, achievements
"""

import uuid

from fastapi import APIRouter, Depends
from pydantic import BaseModel
from sqlalchemy.ext.asyncio import AsyncSession

from db import get_db
from models.streak import StudentStreak

router = APIRouter()


class AchievementOut(BaseModel):
    id: str
    label: str
    unlocked: bool


class StreakOut(BaseModel):
    current_streak: int
    longest_streak: int
    total_active_days: int
    achievements: list[AchievementOut]


ACHIEVEMENT_DEFS = [
    ("first_drill", "Primeiro treino!", lambda s: (s.total_active_days or 0) >= 1),
    ("week_streak", "Uma semana!", lambda s: (s.current_streak or 0) >= 7),
    ("month_streak", "Mestre da consistência!", lambda s: (s.current_streak or 0) >= 30),
    ("century", "Centenário", lambda s: (s.total_active_days or 0) >= 100),
    ("perfect_session", "Sessão perfeita!", lambda s: False),
]


@router.get("/api/student/{student_id}/streak")
async def get_streak(
    student_id: uuid.UUID,
    db: AsyncSession = Depends(get_db),
) -> StreakOut:
    streak = await db.get(StudentStreak, student_id)
    if streak is None:
        return StreakOut(
            current_streak=0,
            longest_streak=0,
            total_active_days=0,
            achievements=[AchievementOut(id=aid, label=alabel, unlocked=False) for aid, alabel, _ in ACHIEVEMENT_DEFS],
        )

    achievements = [
        AchievementOut(id=aid, label=alabel, unlocked=condition(streak))
        for aid, alabel, condition in ACHIEVEMENT_DEFS
    ]
    return StreakOut(
        current_streak=streak.current_streak or 0,
        longest_streak=streak.longest_streak or 0,
        total_active_days=streak.total_active_days or 0,
        achievements=achievements,
    )
