import uuid
import unittest

from api.activity import get_student_timeline
from api.session import start_session
from api.submit import submit_folha
from schemas.session import SessionConfigIn, SessionStartIn
from schemas.submit import FieldSubmit, PenEventIn, SubmitIn
from tests.fakes import FakeAsyncSession


class TimelineTests(unittest.IsolatedAsyncioTestCase):
    async def test_private_timeline_contains_attempt_writing_analysis(self):
        db = FakeAsyncSession()
        db.seed_exercises()
        student_id = uuid.uuid4()
        start = await start_session(
            SessionStartIn(
                student_id=student_id,
                config=SessionConfigIn(duration_mode="pages", pages_limit=1, exercises_per_page=1),
            ),
            db=db,
        )

        await submit_folha(
            start.session_id,
            self._submit_body_for_folha(start.first_folha.folha_id, start.first_folha.fields),
            db=db,
        )

        timeline = await get_student_timeline(student_id, days=30, include_strokes=True, db=db)
        attempt = timeline["days"][0]["sessions"][0]["attempts"][0]

        self.assertEqual(attempt["writing_analysis"]["engine"], "local_text_fallback")
        self.assertIn("speed_score", attempt["writing_analysis"])
        self.assertIsNotNone(attempt["strokes"])

    def _submit_body_for_folha(self, folha_id: uuid.UUID, fields: list) -> SubmitIn:
        return SubmitIn(
            folha_id=folha_id,
            submitted_at_ms=123456,
            fields=[
                FieldSubmit(
                    field_index=field.field_index,
                    exercise_id=field.exercise_id,
                    image_base64="latex:x = 5",
                    total_time_ms=20000,
                    time_to_first_stroke_ms=500,
                    pen_events=[
                        PenEventIn(ts=0, x=10, y=20, pressure=0.7, tilt=0.1, velocity=1.0, event_type="stroke_start"),
                        PenEventIn(ts=400, x=25, y=22, pressure=0.72, tilt=0.1, velocity=1.2, event_type="stroke_end"),
                    ],
                )
                for field in fields
            ],
        )
