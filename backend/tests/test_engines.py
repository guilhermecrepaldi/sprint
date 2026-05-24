import unittest
from types import SimpleNamespace

from engine.adaptive import check_restart, vector_to_memory_status
from engine.correction import validate_answer
from engine.scoring import compute_score


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


class ScoringTests(unittest.TestCase):
    def test_wrong_answer_scores_zero(self):
        self.assertEqual(compute_score(False, 1000, 0, 2.0, 45000), 0)

    def test_correct_answer_is_capped_at_1000(self):
        self.assertEqual(compute_score(True, 30000, 1000, 2.5, 45000), 1000)


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
