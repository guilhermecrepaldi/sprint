"""
Streak Engine — SPRINT
==========================
Gerencia sequência de dias consecutivos de prática.

Regras:
- Um dia = pelo menos 1 exercício submetido
- Sequência consecutiva = dias corridos sem pular
- Quebra = ficou 1+ dia sem submieter
"""

import uuid
from datetime import UTC, date, datetime, timedelta

from sqlalchemy.ext.asyncio import AsyncSession

from models.streak import StudentStreak


async def update_streak(student_id: uuid.UUID, db: AsyncSession) -> StudentStreak:
    streak = await db.get(StudentStreak, student_id)
    if streak is None:
        streak = StudentStreak(student_id=student_id)
        db.add(streak)

    today = date.today()
    if streak.last_activity_date == today:
        return streak

    if streak.last_activity_date == today - timedelta(days=1):
        streak.current_streak += 1
    else:
        streak.current_streak = 1

    streak.longest_streak = max(streak.longest_streak, streak.current_streak)
    streak.last_activity_date = today
    streak.total_active_days = (streak.total_active_days or 0) + 1
    return streak
