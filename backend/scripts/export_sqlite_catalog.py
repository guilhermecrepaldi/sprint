"""Exporta o catalogo atual do Postgres para um SQLite embarcavel no Android.

Uso:
    python backend/scripts/export_sqlite_catalog.py
"""

from __future__ import annotations

import asyncio
import json
import sqlite3
import sys
from pathlib import Path

from sqlalchemy import text

ROOT = Path(__file__).resolve().parents[2]
sys.path.append(str(ROOT / "backend"))

from db import AsyncSessionLocal  # noqa: E402

ASSET_PATH = ROOT / "app" / "src" / "main" / "assets" / "databases" / "exercise_catalog.db"


def _validator_type(validator: str | None, answer_type: str | None, answer: str) -> str:
    clean = (validator or "").lower()
    kind = (answer_type or "").lower()
    if "|" in answer:
        return "options"
    if clean in {"regex", "numeric", "fraction", "exact", "equation"}:
        return clean
    if kind in {"number", "integer", "decimal"}:
        return "numeric"
    if "=" in answer:
        return "equation"
    return "exact"


def _create_schema(conn: sqlite3.Connection) -> None:
    conn.executescript(
        """
        DROP TABLE IF EXISTS exercises;
        CREATE TABLE exercises (
            id TEXT NOT NULL PRIMARY KEY,
            statement TEXT NOT NULL,
            expected_answer TEXT NOT NULL,
            primary_skill TEXT NOT NULL,
            skill_tags_json TEXT NOT NULL,
            difficulty REAL NOT NULL,
            estimated_time_ms INTEGER,
            source_library TEXT,
            source_license TEXT,
            subject TEXT NOT NULL,
            canvas_mode TEXT NOT NULL,
            validator_type TEXT NOT NULL,
            validator_config_json TEXT,
            node_id TEXT,
            template_id TEXT,
            template_version INTEGER NOT NULL,
            variant_seed INTEGER,
            answer_type TEXT NOT NULL,
            method_tags_json TEXT,
            prerequisite_tags_json TEXT,
            affinity_tags_json TEXT,
            parameter_vector_json TEXT,
            difficulty_vector_json TEXT
        );
        CREATE INDEX index_exercises_primary_skill_difficulty
            ON exercises(primary_skill, difficulty);
        CREATE INDEX index_exercises_template_id
            ON exercises(template_id);
        """
    )


async def main() -> None:
    ASSET_PATH.parent.mkdir(parents=True, exist_ok=True)
    if ASSET_PATH.exists():
        ASSET_PATH.unlink()

    async with AsyncSessionLocal() as db:
        rows = (
            await db.execute(
                text(
                    """
                    SELECT
                        id::text AS id,
                        statement,
                        expected_answer,
                        skill_tags,
                        difficulty,
                        estimated_time_ms,
                        source_library,
                        source_license,
                        subject,
                        canvas_mode,
                        validator,
                        node_id,
                        template_id,
                        template_version,
                        variant_seed,
                        answer_type,
                        method_tags,
                        prerequisite_tags,
                        affinity_tags,
                        parameter_vector::text AS parameter_vector_json,
                        difficulty_vector::text AS difficulty_vector_json
                    FROM exercises
                    ORDER BY skill_tags[1], difficulty, statement, id
                    """
                )
            )
        ).mappings().all()

    conn = sqlite3.connect(ASSET_PATH)
    try:
        _create_schema(conn)
        payload = []
        for row in rows:
            skills = list(row["skill_tags"] or [])
            primary_skill = skills[0] if skills else "math"
            answer = row["expected_answer"]
            payload.append(
                (
                    row["id"],
                    row["statement"],
                    answer,
                    primary_skill,
                    json.dumps(skills, ensure_ascii=False),
                    float(row["difficulty"]),
                    row["estimated_time_ms"],
                    row["source_library"],
                    row["source_license"],
                    row["subject"] or "math",
                    row["canvas_mode"] or "calculation",
                    _validator_type(row["validator"], row["answer_type"], answer),
                    None,
                    row["node_id"],
                    row["template_id"],
                    row["template_version"] or 1,
                    row["variant_seed"],
                    row["answer_type"] or "expression",
                    json.dumps(list(row["method_tags"] or []), ensure_ascii=False),
                    json.dumps(list(row["prerequisite_tags"] or []), ensure_ascii=False),
                    json.dumps(list(row["affinity_tags"] or []), ensure_ascii=False),
                    row["parameter_vector_json"],
                    row["difficulty_vector_json"],
                )
            )

        conn.executemany(
            """
            INSERT INTO exercises (
                id, statement, expected_answer, primary_skill, skill_tags_json,
                difficulty, estimated_time_ms, source_library, source_license,
                subject, canvas_mode, validator_type, validator_config_json,
                node_id, template_id, template_version, variant_seed, answer_type,
                method_tags_json, prerequisite_tags_json, affinity_tags_json,
                parameter_vector_json, difficulty_vector_json
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """,
            payload,
        )
        conn.commit()
        count = conn.execute("SELECT COUNT(*) FROM exercises").fetchone()[0]
        print(f"Catalogo exportado: {count} exercicios -> {ASSET_PATH}")
    finally:
        conn.close()


if __name__ == "__main__":
    asyncio.run(main())
