"""
GET /api/profile/{slug}  — Perfil público do aluno

Exibe "Proof of Work": constância, trilhas dominadas e heatmap de atividade.
Não exibe erros individuais — só estamina e volume.
"""
import uuid
from datetime import UTC, datetime, timedelta

from fastapi import APIRouter, Depends, HTTPException
from pydantic import BaseModel
from sqlalchemy import func, select, text
from sqlalchemy.ext.asyncio import AsyncSession

from db import get_db
from models.attempt import ExerciseAttempt
from models.student import Student
from models.track import Track

router = APIRouter()


# ── Schemas ──────────────────────────────────────────────────────────────────

class HeatmapDay(BaseModel):
    date: str   # "2026-05-24"
    count: int  # exercícios naquele dia


class TrackProgress(BaseModel):
    slug: str
    name: str
    total_skills: int
    attempted_skills: int   # skills com >= 1 tentativa
    progress: float         # 0.0–1.0


class ProfileStats(BaseModel):
    total_exercises: int
    streak_days: int        # dias consecutivos até hoje
    xp_total: int


class PublicProfileOut(BaseModel):
    student_name: str
    slug: str
    xp_total: int
    member_since: str       # "2026-05"
    stats: ProfileStats
    tracks: list[TrackProgress]
    heatmap: list[HeatmapDay]   # últimos 365 dias


# ── Endpoint ─────────────────────────────────────────────────────────────────

@router.get("/api/profile/{slug}", response_model=PublicProfileOut)
async def get_public_profile(
    slug: str,
    db: AsyncSession = Depends(get_db),
) -> PublicProfileOut:
    """Perfil público de um aluno. Somente visível se student.is_public = true."""
    result = await db.execute(
        select(Student).where(Student.slug == slug)
    )
    student = result.scalar_one_or_none()

    if student is None or not student.is_public:
        raise HTTPException(status_code=404, detail="Perfil não encontrado")

    # ── Heatmap: exercícios por dia (últimos 365 dias) ────────────────────────
    cutoff = datetime.now(UTC) - timedelta(days=365)
    heatmap_rows = await db.execute(
        select(
            func.date(ExerciseAttempt.created_at).label("day"),
            func.count().label("cnt"),
        )
        .where(
            ExerciseAttempt.student_id == student.id,
            ExerciseAttempt.created_at >= cutoff,
        )
        .group_by(func.date(ExerciseAttempt.created_at))
        .order_by(func.date(ExerciseAttempt.created_at))
    )
    heatmap = [
        HeatmapDay(date=str(row.day), count=row.cnt)
        for row in heatmap_rows
    ]

    # ── Stats gerais ─────────────────────────────────────────────────────────
    total_row = await db.execute(
        select(func.count())
        .select_from(ExerciseAttempt)
        .where(ExerciseAttempt.student_id == student.id)
    )
    total_exercises = total_row.scalar_one() or 0

    streak = _compute_streak(heatmap)

    # ── Skills tentadas pelo aluno ────────────────────────────────────────────
    skill_rows = await db.execute(
        text("""
            SELECT DISTINCT e.skill_tags[1] AS skill
            FROM exercise_attempts ea
            JOIN exercises e ON e.id = ea.exercise_id
            WHERE ea.student_id = :sid
        """),
        {"sid": str(student.id)},
    )
    attempted_skills: set[str] = {row.skill for row in skill_rows if row.skill}

    # ── Trilhas ───────────────────────────────────────────────────────────────
    tracks_rows = await db.execute(
        select(Track).order_by(Track.display_order)
    )
    tracks_out = []
    for track in tracks_rows.scalars().all():
        total_skills = len(track.skill_tags)
        attempted = len(attempted_skills & set(track.skill_tags))
        tracks_out.append(TrackProgress(
            slug=track.slug,
            name=track.name,
            total_skills=total_skills,
            attempted_skills=attempted,
            progress=round(attempted / total_skills, 2) if total_skills else 0.0,
        ))

    member_since = student.created_at.strftime("%Y-%m") if student.created_at else "—"

    return PublicProfileOut(
        student_name=student.name,
        slug=student.slug or slug,
        xp_total=student.xp_total or 0,
        member_since=member_since,
        stats=ProfileStats(
            total_exercises=total_exercises,
            streak_days=streak,
            xp_total=student.xp_total or 0,
        ),
        tracks=tracks_out,
        heatmap=heatmap,
    )


# ── Helpers ───────────────────────────────────────────────────────────────────

def _compute_streak(heatmap: list[HeatmapDay]) -> int:
    """Dias consecutivos de atividade terminando em hoje (ou ontem)."""
    if not heatmap:
        return 0

    active_dates = {day.date for day in heatmap}
    today = datetime.now(UTC).date()
    streak = 0

    current = today
    while str(current) in active_dates:
        streak += 1
        current -= timedelta(days=1)

    # Se não treinou hoje, verifica se ontem iniciava a sequência
    if streak == 0:
        current = today - timedelta(days=1)
        while str(current) in active_dates:
            streak += 1
            current -= timedelta(days=1)

    return streak
