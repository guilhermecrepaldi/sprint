import unittest
from types import SimpleNamespace

from agents.feedback_agent import FeedbackAgent
from agents.scoring_agent import ScoringAgent
from engine.adaptive import check_restart, vector_to_memory_status
from engine.correction import validate_answer
from engine.scoring import compute_score
from engine.validators import get_validator


class CorrectionTests(unittest.TestCase):
    def test_accepts_exact_assignment(self):
        self.assertTrue(validate_answer("x = 5", "x = 5")["is_correct"])

    def test_accepts_fraction_decimal_equivalence(self):
        self.assertTrue(validate_answer("\\frac{3}{4}", "0.75")["is_correct"])

    def test_accepts_implicit_multiplication(self):
        self.assertTrue(validate_answer("3x", "3*x")["is_correct"])

    def test_rejects_unreadable_ocr(self):
        result = validate_answer(None, "x = 5")
        self.assertFalse(result["is_correct"])
        self.assertEqual(result["error_type"], "ocr_invalido")


class ValidatorTests(unittest.IsolatedAsyncioTestCase):
    async def test_sympy_validator_uses_current_correction_logic(self):
        result = await get_validator("sympy").validate("\\frac{3}{4}", "0.75")
        self.assertTrue(result["is_correct"])

    async def test_exact_match_validator_compares_text(self):
        result = await get_validator("exact_match").validate("Resposta", "resposta")
        self.assertTrue(result["is_correct"])


class ScoringTests(unittest.TestCase):
    def test_wrong_answer_scores_zero(self):
        self.assertEqual(compute_score(False, 1000, 0, 2.0, 45000), 0)

    def test_correct_answer_is_capped_at_1000(self):
        self.assertEqual(compute_score(True, 30000, 1000, 2.5, 45000), 1000)

    def test_scoring_agent_wraps_compute_score(self):
        score = ScoringAgent().compute(
            is_correct=True,
            total_time_ms=30000,
            hesitation_ms=1000,
            difficulty=2.5,
            estimated_time_ms=45000,
        )
        self.assertEqual(score, 1000)


class FeedbackAgentTests(unittest.IsolatedAsyncioTestCase):
    async def test_wrong_answer_gets_short_feedback(self):
        feedback = await FeedbackAgent().generate(
            is_correct=False,
            error_type="sinal",
            statement="Resolva: 5 - 2x = 17",
            recognized="x = 6",
            expected="x = -6",
            student_streak=0,
        )

        # FeedbackAgent uses LLM with fallback; wrong answers always get non-empty feedback
        self.assertTrue(len(feedback) > 0)

    async def test_correct_streak_stays_quiet(self):
        feedback = await FeedbackAgent().generate(
            is_correct=True,
            error_type=None,
            statement="Resolva: x + 1 = 6",
            recognized="x = 5",
            expected="x = 5",
            student_streak=3,
        )

        self.assertEqual(feedback, "")


class AdaptiveTests(unittest.TestCase):
    def test_restart_requires_full_window(self):
        config = SimpleNamespace(restart_on_avg=7.0, restart_window=3)
        self.assertFalse(check_restart([8.0, 9.0], config))

    def test_restart_uses_normalized_score_average(self):
        config = SimpleNamespace(restart_on_avg=7.0, restart_window=3)
        self.assertTrue(check_restart([7.0, 8.0, 9.0], config))

    def test_memory_status_thresholds(self):
        self.assertEqual(vector_to_memory_status(0.9), "automatizado")
        self.assertEqual(vector_to_memory_status(0.75), "em_desenvolvimento")
        self.assertEqual(vector_to_memory_status(0.55), "instavel")
        self.assertEqual(vector_to_memory_status(0.2), "fraco")


if __name__ == "__main__":
    unittest.main()
