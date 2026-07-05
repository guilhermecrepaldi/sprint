"""
Progresso de desbloqueio por skill — visível para o aluno.

GET /api/student/{student_id}/unlock-progress
  → Mapa de skill -> status de desbloqueio com progresso por pré-requisito
"""

import uuid

from fastapi import APIRouter, Depends
from pydantic import BaseModel
from sqlalchemy.ext.asyncio import AsyncSession

from db import get_db
from engine.adaptive import get_skill_memory
from engine.unlock import PREREQUISITE_TREE, MASTERY_THRESHOLD, MIN_EXERCISES, check_unlock

router = APIRouter()


class PrereqProgress(BaseModel):
    mastery: float
    exercises: int
    mastery_needed: float = MASTERY_THRESHOLD
    exercises_needed: int = MIN_EXERCISES
    mastery_ok: bool = False
    exercises_ok: bool = False


class SkillUnlockStatus(BaseModel):
    unlocked: bool
    prerequisites: dict[str, PrereqProgress]


@router.get("/api/student/{student_id}/unlock-progress")
async def get_unlock_progress(
    student_id: uuid.UUID,
    db: AsyncSession = Depends(get_db),
) -> dict[str, SkillUnlockStatus]:
    memories = await get_skill_memory(db, student_id)
    result: dict[str, SkillUnlockStatus] = {}
    for target_skill, prereqs in PREREQUISITE_TREE.items():
        skill_memory_dict = {
            skill: {
                "accuracy": mem.accuracy,
                "attempt_count": mem.attempt_count,
            }
            for skill, mem in memories.items()
        }
        status = check_unlock(target_skill, skill_memory_dict)
        prereq_progress = {
            prereq: PrereqProgress(
                mastery=p.get("mastery", 0.0),
                exercises=p.get("exercises", 0),
                mastery_ok=p.get("mastery_ok", False),
                exercises_ok=p.get("exercises_ok", False),
            )
            for prereq, p in status.progress.items()
        }
        result[target_skill] = SkillUnlockStatus(
            unlocked=status.unlocked,
            prerequisites=prereq_progress,
        )
    return result
