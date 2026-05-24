import json
import os
import time
import urllib.error
import urllib.request
import uuid


API_URL = os.getenv("API_URL", "http://localhost:8000")


def post_json(path: str, payload: dict) -> dict:
    data = json.dumps(payload).encode("utf-8")
    request = urllib.request.Request(
        f"{API_URL}{path}",
        data=data,
        headers={"Content-Type": "application/json"},
        method="POST",
    )
    try:
        with urllib.request.urlopen(request, timeout=20) as response:
            return json.loads(response.read().decode("utf-8"))
    except urllib.error.HTTPError as exc:
        body = exc.read().decode("utf-8", errors="replace")
        raise RuntimeError(f"POST {path} failed: {exc.code} {body}") from exc


def get_json(path: str) -> dict:
    try:
        with urllib.request.urlopen(f"{API_URL}{path}", timeout=20) as response:
            return json.loads(response.read().decode("utf-8"))
    except urllib.error.HTTPError as exc:
        body = exc.read().decode("utf-8", errors="replace")
        raise RuntimeError(f"GET {path} failed: {exc.code} {body}") from exc


def main() -> int:
    health = get_json("/api/health")
    student_id = str(uuid.uuid4())
    start_payload = {
        "student_id": student_id,
        "config": {
            "show_thermometer": True,
            "background": "white",
            "pen_color": "#1a1a1a",
            "duration_mode": "pages",
            "pages_limit": 2,
            "difficulty_start": 2.0,
            "exercises_per_page": 3,
        },
    }

    start = post_json("/api/session/start", start_payload)
    first_folha = start["first_folha"]
    fields = []
    for field in first_folha["fields"]:
        fields.append(
            {
                "field_index": field["field_index"],
                "exercise_id": field["exercise_id"],
                "image_base64": "latex:x = 5",
                "total_time_ms": 30000,
                "time_to_first_stroke_ms": 1000,
                "pen_events": [
                    {
                        "ts": 0,
                        "x": 120.0,
                        "y": 80.0,
                        "pressure": 0.7,
                        "tilt": 0.2,
                        "velocity": 1.1,
                        "event_type": "stroke_start",
                    },
                    {
                        "ts": 500,
                        "x": 140.0,
                        "y": 88.0,
                        "pressure": 0.75,
                        "tilt": 0.2,
                        "velocity": 1.3,
                        "event_type": "stroke_end",
                    },
                ],
            }
        )

    submit = post_json(
        f"/api/session/{start['session_id']}/submit",
        {
            "folha_id": first_folha["folha_id"],
            "submitted_at_ms": int(time.time() * 1000),
            "fields": fields,
        },
    )

    print(json.dumps({"health": health, "start": start, "submit": submit}, ensure_ascii=False, indent=2))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
