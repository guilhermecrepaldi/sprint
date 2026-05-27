import unittest
import uuid

from fastapi import HTTPException

import api.submit as submit_module
from api.session import start_session
from api.submit import submit_folha
from models.attempt import ExerciseAttempt
from models.exercise import Exercise
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
        self.assertGreaterEqual(submit.page_score, 1000)
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

    async def test_physics_session_returns_physics_canvas_fields(self):
        start = await start_session(
            SessionStartIn(
                student_id=uuid.uuid4(),
                config=SessionConfigIn(
                    subject="physics",
                    duration_mode="pages",
                    pages_limit=1,
                    exercises_per_page=1,
                ),
            ),
            db=self.db,
        )

        field = start.first_folha.fields[0]
        self.assertEqual(field.subject, "physics")
        self.assertEqual(field.canvas_mode, "calculation")

    async def test_wrong_answer_returns_feedback(self):
        start = await start_session(
            SessionStartIn(
                student_id=uuid.uuid4(),
                config=SessionConfigIn(duration_mode="pages", pages_limit=1, exercises_per_page=1),
            ),
            db=self.db,
        )

        submit = await submit_folha(
            start.session_id,
            self._submit_body_for_folha(start.first_folha.folha_id, start.first_folha.fields, answer="x = 4"),
            db=self.db,
        )

        self.assertFalse(submit.results[0].is_correct)
        self.assertTrue(submit.results[0].feedback)
        self.assertEqual(submit.results[0].recognition_engine, "local_text_fallback")
        self.assertIsNotNone(submit.results[0].analysis_reliable)

    async def test_exact_exercise_density_creates_similar_focus_pool(self):
        source = Exercise(
            id=uuid.uuid4(),
            statement="Calcule: 3^{4}",
            expected_answer=str(3 ** 4),
            skill_tags=["potenciacao_radiciacao"],
            difficulty=3.5,
            estimated_time_ms=30000,
            source_library="test",
            source_license="proprietary_generated",
            subject="math",
            canvas_mode="calculation",
            validator="sympy",
            node_id="potenciacao_radiciacao.core",
            template_id="potenciacao_radiciacao.family_power",
            method_tags=["potenciacao_radiciacao"],
            affinity_tags=["fixacao"],
            parameter_vector={"family_index": 1, "variation_index": 1},
            difficulty_vector={"algebra": 0.3, "steps": 0.2, "error_risk": 0.3},
        )
        self.db.add(source)

        start = await start_session(
            SessionStartIn(
                student_id=uuid.uuid4(),
                config=SessionConfigIn(
                    duration_mode="pages",
                    pages_limit=1,
                    exercises_per_page=5,
                    skill_pin="potenciacao_radiciacao",
                    template_pin=source.template_id,
                    focus_source_exercise_id=source.id,
                    focus_mode=True,
                    difficulty_block_size=200,
                    focus_target_count=200,
                ),
            ),
            db=self.db,
        )

        focus_pool = [
            exercise for exercise in self.db.all_of(Exercise)
            if exercise.template_id == source.template_id
        ]
        self.assertGreaterEqual(len(focus_pool), 200)
        self.assertEqual({field.template_id for field in start.first_folha.fields}, {source.template_id})
        self.assertTrue(all("fixacao_exata" in (exercise.affinity_tags or []) for exercise in focus_pool if exercise.id != source.id))

    def _submit_body_for_folha(self, folha_id: uuid.UUID, fields: list, answer: str = "x = 5") -> SubmitIn:
        return SubmitIn(
            folha_id=folha_id,
            submitted_at_ms=123456,
            fields=[
                FieldSubmit(
                    field_index=field.field_index,
                    exercise_id=field.exercise_id,
                    image_base64=f"latex:{answer}",
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
