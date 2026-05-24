import unittest
import uuid

from fastapi import HTTPException

import api.submit as submit_module
from api.session import start_session
from api.submit import submit_folha
from models.attempt import ExerciseAttempt
from models.session import Folha, FolhaExercise
from models.vector import CognitiveVector, StudentSkillMemory
from schemas.session import SessionConfigIn, SessionStartIn
from schemas.submit import FieldSubmit, PenEventIn, SubmitIn
from tests.fakes import FakeAsyncSession


async def fake_extract_answer(image_base64: str) -> dict:
    return {"answer_latex": image_base64.split(":", 1)[1], "confidence": 1.0}


class UserWorkflowTests(unittest.IsolatedAsyncioTestCase):
    async def asyncSetUp(self) -> None:
        self.db = FakeAsyncSession()
        self.db.seed_exercises()
        self._original_extract_answer = submit_module.extract_answer
        submit_module.extract_answer = fake_extract_answer

    async def asyncTearDown(self) -> None:
        submit_module.extract_answer = self._original_extract_answer

    async def test_student_starts_session_submits_page_and_receives_next_page(self):
        student_id = uuid.uuid4()
        start = await start_session(
            SessionStartIn(
                student_id=student_id,
                config=SessionConfigIn(duration_mode="pages", pages_limit=2, exercises_per_page=3),
            ),
            db=self.db,
        )

        self.assertEqual(len(start.first_folha.fields), 3)
        self.assertEqual(len(self.db.all_of(Folha)), 1)
        self.assertEqual(len(self.db.all_of(FolhaExercise)), 3)

        submit = await submit_folha(
            start.session_id,
            self._submit_body_for_folha(start.first_folha.folha_id, start.first_folha.fields),
            db=self.db,
        )

        self.assertEqual(len(submit.results), 3)
        self.assertEqual(submit.page_score, 1000)
        self.assertGreaterEqual(submit.thermometer.value, 0.6)
        self.assertEqual(submit.session_status, "active")
        self.assertIsNotNone(submit.next_folha)
        self.assertEqual(len(submit.next_folha.fields), 3)
        self.assertEqual(len(self.db.all_of(ExerciseAttempt)), 3)
        self.assertEqual(len(self.db.all_of(CognitiveVector)), 3)
        self.assertEqual(len(self.db.all_of(StudentSkillMemory)), 1)

    async def test_student_cannot_submit_incomplete_page(self):
        start = await start_session(
            SessionStartIn(
                student_id=uuid.uuid4(),
                config=SessionConfigIn(duration_mode="pages", pages_limit=2, exercises_per_page=3),
            ),
            db=self.db,
        )
        fields = start.first_folha.fields[:-1]

        with self.assertRaises(HTTPException) as raised:
            await submit_folha(
                start.session_id,
                self._submit_body_for_folha(start.first_folha.folha_id, fields),
                db=self.db,
            )

        self.assertEqual(raised.exception.status_code, 400)
        self.assertEqual(raised.exception.detail["missing"], [2])

    async def test_student_cannot_resubmit_same_page(self):
        start = await start_session(
            SessionStartIn(
                student_id=uuid.uuid4(),
                config=SessionConfigIn(duration_mode="pages", pages_limit=2, exercises_per_page=3),
            ),
            db=self.db,
        )
        body = self._submit_body_for_folha(start.first_folha.folha_id, start.first_folha.fields)

        await submit_folha(start.session_id, body, db=self.db)

        with self.assertRaises(HTTPException) as raised:
            await submit_folha(start.session_id, body, db=self.db)

        self.assertEqual(raised.exception.status_code, 409)
        self.assertEqual(raised.exception.detail, "Folha already submitted")

    async def test_page_limited_session_finishes_without_next_page(self):
        start = await start_session(
            SessionStartIn(
                student_id=uuid.uuid4(),
                config=SessionConfigIn(duration_mode="pages", pages_limit=1, exercises_per_page=2),
            ),
            db=self.db,
        )

        submit = await submit_folha(
            start.session_id,
            self._submit_body_for_folha(start.first_folha.folha_id, start.first_folha.fields),
            db=self.db,
        )

        self.assertEqual(submit.session_status, "finished")
        self.assertIsNone(submit.next_folha)

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
                        PenEventIn(
                            ts=0,
                            x=10,
                            y=20,
                            pressure=0.7,
                            tilt=0.1,
                            velocity=1.0,
                            event_type="stroke_start",
                        ),
                        PenEventIn(
                            ts=400,
                            x=25,
                            y=22,
                            pressure=0.72,
                            tilt=0.1,
                            velocity=1.2,
                            event_type="stroke_end",
                        ),
                    ],
                )
                for field in fields
            ],
        )


if __name__ == "__main__":
    unittest.main()
