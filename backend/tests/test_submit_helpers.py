import unittest
from datetime import UTC, datetime, timedelta
from types import SimpleNamespace

from api.submit import _should_finish_session


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


if __name__ == "__main__":
    unittest.main()
