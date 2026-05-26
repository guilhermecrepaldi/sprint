"""
Export de dataset comportamental para análise e calibração do motor adaptativo.

GET /api/ml/dataset?format=csv&limit=100000
"""
import csv
import io

from fastapi import APIRouter, Depends, Query
from fastapi.responses import StreamingResponse
from sqlalchemy import text
from sqlalchemy.ext.asyncio import AsyncSession

from db import get_db

router = APIRouter()

_QUERY = text("""
    SELECT
        ea.id::text                                          AS attempt_id,
        ea.student_id::text                                  AS student_id,
        ea.is_correct,
        ea.score,
        ea.total_time_ms,
        ea.time_to_first_stroke_ms,
        ea.stroke_count,
        ea.erase_count,
        ea.pause_count,
        ea.average_pressure,
        ea.average_velocity,
        ea.field_index,
        ea.error_type,
        e.difficulty,
        e.skill_tags[1]                                      AS primary_skill,
        cv.correctness,
        cv.speed_score,
        cv.hesitation_score,
        cv.fluency_score,
        cv.pressure_stability,
        cv.erase_score,
        cv.fatigue_index,
        ssm.accuracy                                         AS skill_accuracy,
        ssm.fluency                                          AS skill_fluency,
        ssm.status                                           AS skill_status,
        ssm.attempt_count                                    AS skill_attempt_count,
        EXTRACT(DAY FROM NOW() - ssm.last_practiced_at)     AS days_since_last_practice,
        ea.created_at
    FROM exercise_attempts ea
    JOIN exercises e ON ea.exercise_id = e.id
    LEFT JOIN cognitive_vectors cv ON cv.attempt_id = ea.id
    LEFT JOIN student_skill_memory ssm
           ON ssm.student_id = ea.student_id
          AND ssm.skill = e.skill_tags[1]
    ORDER BY ea.created_at
    LIMIT :limit
""")

_COLUMNS = [
    "attempt_id", "student_id", "is_correct", "score",
    "total_time_ms", "time_to_first_stroke_ms",
    "stroke_count", "erase_count", "pause_count",
    "average_pressure", "average_velocity", "field_index", "error_type",
    "difficulty", "primary_skill",
    "correctness", "speed_score", "hesitation_score", "fluency_score",
    "pressure_stability", "erase_score", "fatigue_index",
    "skill_accuracy", "skill_fluency", "skill_status",
    "skill_attempt_count", "days_since_last_practice",
    "created_at",
]


@router.get("/api/ml/dataset")
async def export_dataset(
    format: str = Query(default="csv", pattern="^(csv|json)$"),
    limit: int = Query(default=100_000, ge=1, le=1_000_000),
    db: AsyncSession = Depends(get_db),
) -> StreamingResponse:
    result = await db.execute(_QUERY, {"limit": limit})
    rows = result.fetchall()

    if format == "json":
        import json

        data = [dict(zip(_COLUMNS, row)) for row in rows]
        # Converter tipos não-serializáveis
        for record in data:
            if record.get("created_at") is not None:
                record["created_at"] = str(record["created_at"])
            if record.get("days_since_last_practice") is not None:
                record["days_since_last_practice"] = float(record["days_since_last_practice"])
        body = json.dumps(data, default=str)
        return StreamingResponse(
            iter([body]),
            media_type="application/json",
            headers={"Content-Disposition": "attachment; filename=ml_dataset.json"},
        )

    # CSV
    def _iter_csv():
        buf = io.StringIO()
        writer = csv.writer(buf)
        writer.writerow(_COLUMNS)
        yield buf.getvalue()
        for row in rows:
            buf = io.StringIO()
            writer = csv.writer(buf)
            writer.writerow(row)
            yield buf.getvalue()

    return StreamingResponse(
        _iter_csv(),
        media_type="text/csv",
        headers={"Content-Disposition": "attachment; filename=ml_dataset.csv"},
    )
