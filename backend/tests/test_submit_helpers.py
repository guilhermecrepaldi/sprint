import unittest
import uuid
from datetime import UTC, datetime, timedelta
from types import SimpleNamespace

from api.submit import _recent_scores, _should_finish_session
from models.attempt import ExerciseAttempt
from tests.fakes import FakeAsyncSession


class SubmitHelperTests(unittest.TestCase):
    def test_pages_mode_finishes_at_page_limit(self):
        session = SimpleNamespace(page_count=2, started_at=datetime.now(UTC))
        config = SimpleNamespace(duration_mode="pages", pages_limit=2, duration_limit_ms=None)
        self.assertTrue(_should_finish_session(session, config))

    def test_pages_mode_stays_active_before_page_limit(self):
        session = SimpleNamespace(page_count=1, started_at=datetime.now(UTC))
        config = SimpleNamespace(duration_mode="pages", pages_limit=2, duration_limit_ms=None)
        self.assertFalse(_should_finish_session(session, config))

    def test_timed_mode_finishes_after_duration(self):
        session = SimpleNamespace(page_count=0, started_at=datetime.now(UTC) - timedelta(seconds=10))
        config = SimpleNamespace(duration_mode="timed", pages_limit=None, duration_limit_ms=1000)
        self.assertTrue(_should_finish_session(session, config))


class RecentScoresTests(unittest.IsolatedAsyncioTestCase):
    async def test_recent_scores_clamps_esports_score_to_adaptive_scale(self):
        db = FakeAsyncSession()
        session_id = uuid.uuid4()
        db.add(ExerciseAttempt(session_id=session_id, score=1900))
        db.add(ExerciseAttempt(session_id=session_id, score=850))

        self.assertEqual(await _recent_scores(db, session_id, limit=10), [8.5, 10.0])


if __name__ == "__main__":
    unittest.main()
