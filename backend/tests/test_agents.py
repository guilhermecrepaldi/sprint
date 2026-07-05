import unittest

from agents.feedback_agent import FeedbackAgent
from engine.adaptive import adjust_difficulty_for_role, get_exercise_role
from engine.mastery import apply_decay, update_mastery


class MasteryDecayTests(unittest.TestCase):
    def test_no_decay_within_one_day(self):
        self.assertAlmostEqual(apply_decay(0.80, 1), 0.80)

    def test_decay_after_seven_days(self):
        self.assertLess(apply_decay(0.80, 7), 0.80)

    def test_decay_never_goes_negative(self):
        self.assertGreaterEqual(apply_decay(0.01, 365), 0.0)

    def test_correct_answer_increases_mastery(self):
        result = update_mastery(0.5, True)
        self.assertGreater(result, 0.5)

    def test_wrong_answer_decreases_mastery(self):
        result = update_mastery(0.5, False)
        self.assertLess(result, 0.5)

    def test_mastery_never_exceeds_one(self):
        result = update_mastery(1.0, True)
        self.assertLessEqual(result, 1.0)

    def test_mastery_never_goes_below_zero(self):
        result = update_mastery(0.0, False)
        self.assertGreaterEqual(result, 0.0)


class RhythmTests(unittest.TestCase):
    def test_first_three_are_warmup(self):
        for i in range(3):
            self.assertEqual(get_exercise_role(i, 0), "warmup")

    def test_sprint_at_eight_nine(self):
        self.assertEqual(get_exercise_role(8, 0), "sprint")
        self.assertEqual(get_exercise_role(9, 0), "sprint")

    def test_breathing_on_errors(self):
        self.assertEqual(get_exercise_role(7, 2), "breathing")

    def test_no_breathing_without_errors(self):
        self.assertEqual(get_exercise_role(7, 0), "flow")

    def test_warmup_reduces_difficulty(self):
        result = adjust_difficulty_for_role(4.0, "warmup")
        self.assertLess(result, 4.0)

    def test_breathing_reduces_difficulty_more(self):
        warmup = adjust_difficulty_for_role(4.0, "warmup")
        breathing = adjust_difficulty_for_role(4.0, "breathing")
        self.assertLess(breathing, warmup)

    def test_sprint_adds_difficulty(self):
        result = adjust_difficulty_for_role(4.0, "sprint")
        self.assertEqual(result, 4.5)

    def test_flow_unchanged(self):
        result = adjust_difficulty_for_role(4.0, "flow")
        self.assertEqual(result, 4.0)

    def test_difficulty_capped_at_ten(self):
        result = adjust_difficulty_for_role(10.0, "sprint")
        self.assertLessEqual(result, 10.0)

    def test_difficulty_floored_at_one(self):
        result = adjust_difficulty_for_role(1.0, "breathing")
        self.assertGreaterEqual(result, 1.0)

    def test_role_wraps_at_ten(self):
        # Position 10 = same as position 0 = warmup
        self.assertEqual(get_exercise_role(10, 0), "warmup")
        self.assertEqual(get_exercise_role(18, 0), "sprint")


class FeedbackAgentStreakTests(unittest.IsolatedAsyncioTestCase):
    async def test_correct_streak_returns_empty(self):
        feedback = await FeedbackAgent().generate(
            is_correct=True,
            error_type=None,
            statement="Resolva: x + 1 = 6",
            recognized="x = 5",
            expected="x = 5",
            student_streak=3,
        )
        self.assertEqual(feedback, "")

    async def test_correct_below_streak_returns_checkmark(self):
        feedback = await FeedbackAgent().generate(
            is_correct=True,
            error_type=None,
            statement="Resolva: x + 1 = 6",
            recognized="x = 5",
            expected="x = 5",
            student_streak=2,
        )
        self.assertEqual(feedback, "✓")


if __name__ == "__main__":
    unittest.main()
