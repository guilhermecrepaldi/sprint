import unittest

from fastapi.testclient import TestClient

import api.submit as submit_module
from db import get_db
from main import create_app
from tests.fakes import FakeAsyncSession


async def fake_extract_answer(image_base64: str) -> dict:
    return {"answer_latex": image_base64.split(":", 1)[1], "confidence": 1.0}


class HttpUserWorkflowTests(unittest.TestCase):
    def setUp(self) -> None:
        self.db = FakeAsyncSession()
        self.db.seed_exercises()
        self.app = create_app(run_startup_db=False)

        async def override_get_db():
            yield self.db

        self.app.dependency_overrides[get_db] = override_get_db
        self._original_extract_answer = submit_module.extract_answer
        submit_module.extract_answer = fake_extract_answer
        self.client = TestClient(self.app)

    def tearDown(self) -> None:
        submit_module.extract_answer = self._original_extract_answer
        self.app.dependency_overrides.clear()
        self.client.close()

    def test_user_workflow_over_http(self):
        start_response = self.client.post(
            "/api/session/start",
            json={
                "student_id": "11111111-1111-4111-8111-111111111111",
                "config": {
                    "duration_mode": "pages",
                    "pages_limit": 2,
                    "exercises_per_page": 3,
                    "difficulty_start": 2.0,
                },
            },
        )
        self.assertEqual(start_response.status_code, 200, start_response.text)
        start = start_response.json()
        self.assertEqual(len(start["first_folha"]["fields"]), 3)

        submit_response = self.client.post(
            f"/api/session/{start['session_id']}/submit",
            json=self._submit_payload(start["first_folha"]),
        )
        self.assertEqual(submit_response.status_code, 200, submit_response.text)
        submit = submit_response.json()

        self.assertEqual(len(submit["results"]), 3)
        self.assertEqual(submit["page_score"], 1000)
        self.assertEqual(submit["session_status"], "active")
        self.assertIsNotNone(submit["next_folha"])

    def test_http_rejects_incomplete_page(self):
        start_response = self.client.post(
            "/api/session/start",
            json={
                "student_id": "22222222-2222-4222-8222-222222222222",
                "config": {"duration_mode": "pages", "pages_limit": 2, "exercises_per_page": 3},
            },
        )
        start = start_response.json()
        payload = self._submit_payload(start["first_folha"])
        payload["fields"] = payload["fields"][:-1]

        submit_response = self.client.post(f"/api/session/{start['session_id']}/submit", json=payload)

        self.assertEqual(submit_response.status_code, 400)
        self.assertEqual(submit_response.json()["detail"]["missing"], [2])

    def _submit_payload(self, folha: dict) -> dict:
        return {
            "folha_id": folha["folha_id"],
            "submitted_at_ms": 123456,
            "fields": [
                {
                    "field_index": field["field_index"],
                    "exercise_id": field["exercise_id"],
                    "image_base64": "latex:x = 5",
                    "total_time_ms": 20000,
                    "time_to_first_stroke_ms": 500,
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
                            "ts": 400,
                            "x": 25,
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


if __name__ == "__main__":
    unittest.main()
