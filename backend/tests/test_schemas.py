import unittest
import uuid

from pydantic import ValidationError

from schemas.session import SessionConfigIn
from schemas.submit import FieldSubmit, SubmitIn


class SessionConfigSchemaTests(unittest.TestCase):
    def test_timed_mode_requires_duration_limit(self):
        with self.assertRaises(ValidationError):
            SessionConfigIn(duration_mode="timed")

    def test_pages_mode_requires_pages_limit(self):
        with self.assertRaises(ValidationError):
            SessionConfigIn(duration_mode="pages")

    def test_accepts_complete_pages_mode(self):
        config = SessionConfigIn(duration_mode="pages", pages_limit=2)
        self.assertEqual(config.pages_limit, 2)


class SubmitSchemaTests(unittest.TestCase):
    def test_requires_at_least_one_field(self):
        with self.assertRaises(ValidationError):
            SubmitIn(folha_id=uuid.uuid4(), fields=[])

    def test_rejects_duplicate_field_indexes(self):
        exercise_id = uuid.uuid4()
        field = FieldSubmit(
            field_index=0,
            exercise_id=exercise_id,
            image_base64="latex:x = 5",
            total_time_ms=1000,
        )
        with self.assertRaises(ValidationError):
            SubmitIn(folha_id=uuid.uuid4(), fields=[field, field])

    def test_rejects_unknown_pen_event_type(self):
        with self.assertRaises(ValidationError):
            FieldSubmit(
                field_index=0,
                exercise_id=uuid.uuid4(),
                image_base64="latex:x = 5",
                total_time_ms=1000,
                pen_events=[{"ts": 0, "x": 1, "y": 1, "event_type": "tap"}],
            )


if __name__ == "__main__":
    unittest.main()
