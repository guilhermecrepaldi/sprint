import unittest

from schemas.calibration import CalibrationIn, CalibrationOut, CharCalibrationResult, CharSample


class CalibrationSchemaTests(unittest.TestCase):
    def test_overall_score_structure(self):
        out = CalibrationOut(
            results=[],
            weak_chars=[],
            overall_score=0.0,
        )
        self.assertEqual(out.overall_score, 0.0)

    def test_char_sample_requires_fields(self):
        s = CharSample(expected_char="7", image_base64="abc")
        self.assertEqual(s.expected_char, "7")

    def test_calibration_in_accepts_multiple_samples(self):
        body = CalibrationIn(samples=[
            CharSample(expected_char="1", image_base64="aaa"),
            CharSample(expected_char="2", image_base64="bbb"),
        ])
        self.assertEqual(len(body.samples), 2)

    def test_char_calibration_result_fields(self):
        r = CharCalibrationResult(char="7", recognized="7", correct=True, confidence=0.95)
        self.assertTrue(r.correct)
        self.assertEqual(r.confidence, 0.95)

    def test_weak_chars_populated_for_incorrect(self):
        results = [
            CharCalibrationResult(char="4", recognized="9", correct=False, confidence=0.3),
            CharCalibrationResult(char="7", recognized="7", correct=True, confidence=0.9),
        ]
        weak = [r.char for r in results if not r.correct]
        out = CalibrationOut(results=results, weak_chars=weak, overall_score=0.5)
        self.assertEqual(out.weak_chars, ["4"])
        self.assertEqual(out.overall_score, 0.5)
