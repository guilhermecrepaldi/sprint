from fastapi import APIRouter, Depends
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from db import get_db
from engine.rhythm import get_session_recommendation
from models.session import Session as SessionModel

router = APIRouter()


@router.get("/api/student/{student_id}/rhythm")
async def student_rhythm(student_id: str, db: AsyncSession = Depends(get_db)):
    result = await db.execute(
        select(SessionModel)
        .where(SessionModel.student_id == student_id)
        .order_by(SessionModel.started_at)
        .limit(20)
    )
    sessions_db = result.scalars().all()

    sessions = []
    for s in sessions_db:
        if s.started_at is None:
            continue
        sessions.append({
            "duration_ms": getattr(s, "duration_ms", 0) or 0,
            "accuracy": getattr(s, "session_accuracy", 0.0) or 0.0,
            "started_at": s.started_at,
        })

    return get_session_recommendation(sessions)
