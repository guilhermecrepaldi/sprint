"""
Simula um aluno real atravessando SPRINT por API local.

Fluxo:
- cria um aluno publico de teste;
- inicia 4 sprints por tema;
- em cada tema, faz 10 exercicios: 5 certos e 5 errados;
- valida historico, skill-progress, activity/calendario, timeline e perfil.
"""

from __future__ import annotations

import argparse
import asyncio
import json
import sys
import uuid
from dataclasses import dataclass
from datetime import UTC, date, datetime
from typing import Any
from urllib.error import HTTPError, URLError
from urllib.request import Request, urlopen

from pathlib import Path

from sqlalchemy import select

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from db import AsyncSessionLocal
from models.exercise import Exercise
from models.student import Student


DEFAULT_THEMES = [
    ("soma_subtracao", "+-", [True, True, True, True, True, False, False, False, False, False]),
    ("equacoes_lineares", "equacao", [True, True, True, True, True, False, False, False, False, False]),
    ("trig_razoes", "trigonometria", [False, False, False, False, False, True, True, True, True, True]),
    ("funcao_logaritmica", "logaritmo", [True, True, True, True, True, False, False, False, False, False]),
]


@dataclass
class ThemeRun:
    skill: str
    label: str
    session_id: str
    outcomes: list[bool]
    correct: int
    wrong: int


def _request_json(method: str, url: str, body: dict[str, Any] | None = None) -> Any:
    data = None if body is None else json.dumps(body).encode("utf-8")
    request = Request(
        url,
        data=data,
        method=method,
        headers={"Content-Type": "application/json"},
    )
    try:
        with urlopen(request, timeout=20) as response:
            raw = response.read().decode("utf-8")
            return json.loads(raw) if raw else None
    except HTTPError as exc:
        detail = exc.read().decode("utf-8", errors="replace")
        raise RuntimeError(f"{method} {url} -> HTTP {exc.code}: {detail}") from exc
    except URLError as exc:
        raise RuntimeError(f"{method} {url} failed: {exc}") from exc


async def _ensure_public_student(student_id: uuid.UUID, slug: str) -> None:
    async with AsyncSessionLocal() as db:
        student = await db.get(Student, student_id)
        if student is None:
            student = Student(id=student_id, name="Aluno Simulado", slug=slug, is_public=True)
            db.add(student)
        else:
            student.name = "Aluno Simulado"
            student.slug = slug
            student.is_public = True
        await db.commit()


def _submit_payload(folha: dict[str, Any], answer: str, attempt_index: int) -> dict[str, Any]:
    return {
        "folha_id": folha["folha_id"],
        "submitted_at_ms": int(datetime.now(UTC).timestamp() * 1000),
        "fields": [
            {
                "field_index": field["field_index"],
                "exercise_id": field["exercise_id"],
                "image_base64": f"latex:{answer}",
                "total_time_ms": 15_000 + attempt_index * 250,
                "time_to_first_stroke_ms": 650,
                "pen_events": [
                    {
                        "ts": 0,
                        "x": 10,
                        "y": 20,
                        "pressure": 0.7,
                        "tilt": 0.1,
                        "velocity": 1.0,
                        "event_type": "stroke_start",
                    },
                    {
                        "ts": 480,
                        "x": 48,
                        "y": 22,
                        "pressure": 0.72,
                        "tilt": 0.1,
                        "velocity": 1.2,
                        "event_type": "stroke_end",
                    },
                ],
            }
            for field in folha["fields"]
        ],
    }


def _start_session(base_url: str, student_id: uuid.UUID, skill: str) -> dict[str, Any]:
    return _request_json(
        "POST",
        f"{base_url}/api/session/start",
        {
            "student_id": str(student_id),
            "config": {
                "duration_mode": "unlimited",
                "exercises_per_page": 1,
                "skill_pin": skill,
                "focus_mode": True,
                "fixation_density": "fixa",
            },
        },
    )


async def _expected_answer(exercise_id: str) -> str:
    async with AsyncSessionLocal() as db:
        result = await db.execute(select(Exercise.expected_answer).where(Exercise.id == uuid.UUID(exercise_id)))
        expected = result.scalar_one_or_none()
        if expected is None:
            raise AssertionError(f"Exercise {exercise_id} nao encontrado")
        return expected


async def _run_theme(
    base_url: str,
    student_id: uuid.UUID,
    skill: str,
    label: str,
    expected_outcomes: list[bool],
) -> ThemeRun:
    start = _start_session(base_url, student_id, skill)
    session_id = start["session_id"]
    folha = start["first_folha"]
    correct = 0
    wrong = 0

    if len(expected_outcomes) != 10:
        raise AssertionError(f"Tema {skill} precisa de exatamente 10 resultados esperados")

    actual_outcomes: list[bool] = []
    for index, should_be_correct in enumerate(expected_outcomes):
        expected = await _expected_answer(folha["fields"][0]["exercise_id"])
        answer = expected if should_be_correct else "__wrong__"
        submit = _request_json(
            "POST",
            f"{base_url}/api/session/{session_id}/submit",
            _submit_payload(folha, answer, index),
        )
        result = submit["results"][0]
        if result["is_correct"]:
            correct += 1
        else:
            wrong += 1
        actual_outcomes.append(bool(result["is_correct"]))
        folha = submit.get("next_folha")
        if folha is None and index < 9:
            raise AssertionError(f"Sessao {skill} terminou antes de 10 exercicios")

    if actual_outcomes != expected_outcomes:
        raise AssertionError(f"Tema {skill} teve ordem inesperada: {actual_outcomes}")

    return ThemeRun(skill=skill, label=label, session_id=session_id, outcomes=actual_outcomes, correct=correct, wrong=wrong)


def _find_today(activity: dict[str, Any]) -> dict[str, Any]:
    today = date.today().isoformat()
    return next((item for item in activity["days"] if item["date"] == today), {"date": today, "count": 0})


def _assert_flow(
    *,
    base_url: str,
    student_id: uuid.UUID,
    slug: str,
    runs: list[ThemeRun],
) -> dict[str, Any]:
    expected_total = len(runs) * 10
    expected_correct = len(runs) * 5
    expected_wrong = len(runs) * 5
    all_marks = [mark for run in runs for mark in run.outcomes]
    first_25_marks = all_marks[:25]
    first_25_correct = sum(1 for mark in first_25_marks if mark)
    first_25_wrong = len(first_25_marks) - first_25_correct
    if (first_25_correct, first_25_wrong) != (10, 15):
        raise AssertionError(
            f"Checkpoint enter 25 deveria ser 10 acertos/15 erros, "
            f"veio {first_25_correct}/{first_25_wrong}"
        )

    sessions = _request_json("GET", f"{base_url}/api/student/{student_id}/sessions?limit=20")
    progress = _request_json("GET", f"{base_url}/api/student/{student_id}/skill-progress")
    activity = _request_json("GET", f"{base_url}/api/student/{student_id}/activity?days=7")
    timeline = _request_json("GET", f"{base_url}/api/student/{student_id}/timeline?days=7")
    profile = _request_json("GET", f"{base_url}/api/profile/{slug}")

    run_session_ids = {run.session_id for run in runs}
    relevant_sessions = [item for item in sessions if item["session_id"] in run_session_ids]
    if len(relevant_sessions) != len(runs):
        raise AssertionError(f"Historico deveria ter {len(runs)} sessoes da simulacao, veio {len(relevant_sessions)}")
    for item in relevant_sessions:
        if item["exercises_done"] != 10:
            raise AssertionError(f"Historico de {item['skill']} deveria ter 10 exercicios: {item}")
        if item["accuracy"] != 50:
            raise AssertionError(f"Historico de {item['skill']} deveria ter 50%: {item}")

    progress_by_skill = {item["skill"]: item for item in progress}
    for run in runs:
        item = progress_by_skill.get(run.skill)
        if item is None:
            raise AssertionError(f"Skill {run.skill} nao apareceu em skill-progress")
        if item["attempt_count"] < 10:
            raise AssertionError(f"Skill {run.skill} deveria ter pelo menos 10 tentativas: {item}")

    today_activity = _find_today(activity)
    if today_activity["count"] < expected_total:
        raise AssertionError(f"Calendario deveria ter pelo menos {expected_total} hoje: {today_activity}")

    day = timeline["days"][0] if timeline["days"] else None
    if day is None:
        raise AssertionError("Timeline nao retornou o dia da simulacao")
    if day["exercise_count"] < expected_total or day["correct_count"] < expected_correct:
        raise AssertionError(f"Timeline nao refletiu a simulacao: {day}")

    if profile["stats"]["total_exercises"] < expected_total:
        raise AssertionError(f"Perfil deveria ter pelo menos {expected_total} exercicios: {profile['stats']}")
    profile_today = next((item for item in profile["heatmap"] if item["date"] == date.today().isoformat()), None)
    if profile_today is None or profile_today["count"] < expected_total:
        raise AssertionError(f"Perfil/heatmap nao refletiu a simulacao: {profile_today}")

    return {
        "student_id": str(student_id),
        "slug": slug,
        "themes": [
            {
                "skill": run.skill,
                "label": run.label,
                "session_id": run.session_id,
                "outcomes": ["certo" if mark else "erro" for mark in run.outcomes],
                "correct": run.correct,
                "wrong": run.wrong,
            }
            for run in runs
        ],
        "totals": {
            "exercises": expected_total,
            "correct": expected_correct,
            "wrong": expected_wrong,
            "enter_register_expected_if_same_study_session": "acumulado simulado: 20/40",
            "enter_register_checkpoint_25": {
                "exercises": len(first_25_marks),
                "correct": first_25_correct,
                "wrong": first_25_wrong,
            },
        },
        "checks": {
            "history_sessions": len(relevant_sessions),
            "history_each_session": "10 exercicios, 50% acerto",
            "activity_today_count": today_activity["count"],
            "timeline_today_exercises": day["exercise_count"],
            "timeline_today_correct": day["correct_count"],
            "profile_total_exercises": profile["stats"]["total_exercises"],
            "profile_heatmap_today": profile_today["count"],
        },
    }


async def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--base-url", default="http://127.0.0.1:8000")
    parser.add_argument("--student-id", default=None)
    parser.add_argument("--slug", default=None)
    args = parser.parse_args()

    student_id = uuid.UUID(args.student_id) if args.student_id else uuid.uuid4()
    slug = args.slug or f"sim-{student_id.hex[:12]}"
    base_url = args.base_url.rstrip("/")

    await _ensure_public_student(student_id, slug)

    runs = []
    for skill, label, outcomes in DEFAULT_THEMES:
        runs.append(await _run_theme(base_url, student_id, skill, label, outcomes))
    summary = _assert_flow(base_url=base_url, student_id=student_id, slug=slug, runs=runs)
    print(json.dumps(summary, ensure_ascii=False, indent=2))
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(asyncio.run(main()))
    except Exception as exc:
        print(f"SIMULATION FAILED: {exc}", file=sys.stderr)
        raise
