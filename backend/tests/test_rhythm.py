import unittest
from datetime import datetime, timezone

from engine.rhythm import get_session_recommendation


def make_session(accuracy, hour=10, duration_ms=3600000):
    return {
        "accuracy": accuracy,
        "duration_ms": duration_ms,
        "started_at": datetime(2026, 1, 1, hour, 0, tzinfo=timezone.utc),
    }


class RhythmTests(unittest.TestCase):
    def test_no_data_returns_default(self):
        r = get_session_recommendation([])
        self.assertEqual(r["trend"], "no_data")
        self.assertIn("message", r)

    def test_no_data_returns_1h_default_duration(self):
        r = get_session_recommendation([])
        self.assertEqual(r["suggested_duration_ms"], 3600000)

    def test_no_data_returns_none_best_hour(self):
        r = get_session_recommendation([])
        self.assertIsNone(r["best_hour"])

    def test_improving_trend_detected(self):
        sessions = [
            make_session(0.5), make_session(0.5), make_session(0.5),
            make_session(0.8), make_session(0.8), make_session(0.9),
        ]
        r = get_session_recommendation(sessions)
        self.assertEqual(r["trend"], "improving")

    def test_declining_trend_detected(self):
        sessions = [
            make_session(0.9), make_session(0.9), make_session(0.9),
            make_session(0.5), make_session(0.5), make_session(0.4),
        ]
        r = get_session_recommendation(sessions)
        self.assertEqual(r["trend"], "declining")

    def test_stable_trend_detected(self):
        sessions = [make_session(0.7)] * 6
        r = get_session_recommendation(sessions)
        self.assertEqual(r["trend"], "stable")

    def test_best_hour_identified(self):
        sessions = [
            make_session(0.9, hour=9),
            make_session(0.5, hour=15),
            make_session(0.85, hour=9),
        ]
        r = get_session_recommendation(sessions)
        self.assertEqual(r["best_hour"], 9)

    def test_suggested_duration_increases_when_improving(self):
        base_ms = 3600000
        sessions = [make_session(0.5, duration_ms=base_ms)] * 3 + \
                   [make_session(0.9, duration_ms=base_ms)] * 3
        r = get_session_recommendation(sessions)
        self.assertGreater(r["suggested_duration_ms"], base_ms)

    def test_suggested_duration_decreases_when_declining(self):
        base_ms = 3600000
        sessions = [make_session(0.9, duration_ms=base_ms)] * 3 + \
                   [make_session(0.4, duration_ms=base_ms)] * 3
        r = get_session_recommendation(sessions)
        self.assertLess(r["suggested_duration_ms"], base_ms)

    def test_suggested_duration_capped_at_2h(self):
        big_ms = 7000000  # > 2h
        sessions = [make_session(0.5, duration_ms=big_ms)] * 3 + \
                   [make_session(0.9, duration_ms=big_ms)] * 3
        r = get_session_recommendation(sessions)
        self.assertLessEqual(r["suggested_duration_ms"], 7200000)

    def test_suggested_duration_floored_at_20min_when_declining(self):
        tiny_ms = 1000000  # < 20min floor after 0.85x
        sessions = [make_session(0.9, duration_ms=tiny_ms)] * 3 + \
                   [make_session(0.4, duration_ms=tiny_ms)] * 3
        r = get_session_recommendation(sessions)
        self.assertGreaterEqual(r["suggested_duration_ms"], 1200000)

    def test_single_session_returns_stable(self):
        r = get_session_recommendation([make_session(0.7)])
        self.assertEqual(r["trend"], "stable")

    def test_message_present_for_all_trends(self):
        for sessions, expected_trend in [
            ([], "no_data"),
            ([make_session(0.7)], "stable"),
        ]:
            r = get_session_recommendation(sessions)
            self.assertIn("message", r)
            self.assertIsInstance(r["message"], str)
            self.assertGreater(len(r["message"]), 0)
