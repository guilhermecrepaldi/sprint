from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.ext.asyncio import AsyncSession

from db import get_db
from engine.adaptive import build_folha_out, get_first_folha
from models.session import Session, SessionConfig
from models.student import Student
from schemas.session import SessionStartIn, SessionStartOut

router = APIRouter()


@router.post("/api/session/start", response_model=SessionStartOut)
async def start_session(body: SessionStartIn, db: AsyncSession = Depends(get_db)) -> SessionStartOut:
    student = await db.get(Student, body.student_id)
    if student is None:
        student = Student(id=body.student_id, name="Aluno")
        db.add(student)
        await db.flush()

    config = SessionConfig(student_id=body.student_id, **body.config.model_dump())
    db.add(config)
    await db.flush()

    session = Session(
        student_id=body.student_id,
        config_id=config.id,
        current_difficulty=config.difficulty_start,
    )
    db.add(session)
    await db.flush()

    folha, exercises = await get_first_folha(
        db=db,
        session=session,
        difficulty=config.difficulty_start,
        exercises_per_page=config.exercises_per_page,
        student_id=body.student_id,
        subject=config.subject,
        skill_pin=config.skill_pin,
        template_pin=config.template_pin,
        config=config,
    )
    if not exercises:
        await db.rollback()
        raise HTTPException(status_code=409, detail="No exercises available. Run seed/exercises.py first.")

    await db.commit()

    return SessionStartOut(
        session_id=session.id,
        config_id=config.id,
        first_folha=build_folha_out(folha, exercises),
    )
