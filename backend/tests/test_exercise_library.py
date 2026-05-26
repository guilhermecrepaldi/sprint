import unittest
import random

from engine.adaptive import _focus_difficulty
from engine.exercise_library import (
    build_attempt_features,
    generate_quadratic_1var_batch,
    light_intervention_signal,
)
from models.session import SessionConfig
from seed.generate_exercises import TARGET_PER_SKILL, build_skill_items


class QuadraticLibraryTests(unittest.TestCase):
    def test_generates_300_quadratic_exercises_across_10_levels(self):
        items = generate_quadratic_1var_batch(seed=4200)

        self.assertEqual(len(items), 300)
        self.assertEqual(len({item["statement"] for item in items}), 300)
        self.assertEqual({item["node_id"] for item in items}, {"equacoes_quadraticas.1var"})
        self.assertEqual({item["parameter_vector"]["level"] for item in items}, set(range(1, 11)))
        self.assertEqual({item["parameter_vector"]["variation_index"] for item in items}, {1, 2, 3})
        self.assertEqual({item["parameter_vector"]["sprint_block"] for item in items}, set(range(10)))
        self.assertEqual({item["source_library"] for item in items}, {"sprint_parametric_v2"})
        self.assertTrue(all(item["template_id"].startswith("quadratic_1var_l") for item in items))
        self.assertTrue(all("fixacao" in item["affinity_tags"] for item in items))
        families = {}
        for item in items:
            family = item["parameter_vector"]["family_index"]
            families.setdefault(family, []).append(item)
        self.assertEqual(len(families), 100)
        for family_items in families.values():
            self.assertEqual(len(family_items), 3)
            self.assertEqual({item["parameter_vector"]["variation_index"] for item in family_items}, {1, 2, 3})
            self.assertEqual(len({item["template_id"] for item in family_items}), 1)
            self.assertEqual(len({item["parameter_vector"]["level"] for item in family_items}), 1)

    def test_standardizes_linear_equation_sprint_items(self):
        items = build_skill_items("equacoes_lineares", random.Random(42))

        self.assertEqual(len(items), TARGET_PER_SKILL)
        self.assertEqual(len({item["statement"] for item in items}), TARGET_PER_SKILL)
        self.assertEqual({item["node_id"] for item in items}, {"equacoes_lineares.core"})
        self.assertEqual({item["source_library"] for item in items}, {"sprint_parametric_v2"})
        self.assertEqual({item["source_license"] for item in items}, {"proprietary_generated"})
        self.assertEqual({item["parameter_vector"]["variation_index"] for item in items}, {1, 2, 3})
        self.assertEqual({item["parameter_vector"]["sprint_block"] for item in items}, set(range(10)))
        self.assertTrue(all(item["template_id"].startswith("equacoes_lineares.family_") for item in items))
        self.assertTrue(all(item["estimated_time_ms"] > 0 for item in items))
        self.assertTrue(all(item["method_tags"] for item in items))
        self.assertTrue(all(item["difficulty_vector"]["error_risk"] > 0 for item in items))

    def test_focus_difficulty_advances_every_30_exercises(self):
        config = SessionConfig(difficulty_start=2.0, difficulty_step=0.5, difficulty_block_size=30)

        self.assertEqual(_focus_difficulty(config, 0), 2.0)
        self.assertEqual(_focus_difficulty(config, 29), 2.0)
        self.assertEqual(_focus_difficulty(config, 30), 2.5)
        self.assertEqual(_focus_difficulty(config, 59), 2.5)
        self.assertEqual(_focus_difficulty(config, 60), 3.0)

    def test_focus_difficulty_uses_configurable_density_blocks(self):
        fast = SessionConfig(difficulty_start=2.0, difficulty_step=0.8, difficulty_block_size=15)
        slow = SessionConfig(difficulty_start=2.0, difficulty_step=0.25, difficulty_block_size=60)

        self.assertEqual(_focus_difficulty(fast, 14), 2.0)
        self.assertEqual(_focus_difficulty(fast, 15), 2.8)
        self.assertEqual(_focus_difficulty(fast, 30), 3.6)
        self.assertEqual(_focus_difficulty(slow, 59), 2.0)
        self.assertEqual(_focus_difficulty(slow, 60), 2.2)
        self.assertEqual(_focus_difficulty(slow, 120), 2.5)

    def test_attempt_features_capture_speed_and_friction(self):
        exercise = type("Exercise", (), {"estimated_time_ms": 1000})()
        features = build_attempt_features(2500, exercise, erase_count=3, pause_count=1)

        self.assertEqual(features["time_ratio"], 2.5)
        self.assertTrue(features["slow"])
        self.assertTrue(features["high_friction"])

    def test_light_signal_waits_for_repeated_friction(self):
        quiet = light_intervention_signal(
            attempt_count=3,
            is_correct=False,
            error_type="wrong_answer",
            method_tags=["fatoracao"],
            features={"time_ratio": 3.0, "high_friction": True},
        )
        signal = light_intervention_signal(
            attempt_count=8,
            is_correct=False,
            error_type="wrong_answer",
            method_tags=["fatoracao"],
            features={"time_ratio": 3.0, "high_friction": True},
        )

        self.assertIsNone(quiet)
        self.assertIn("soma e produto", signal)


if __name__ == "__main__":
    unittest.main()
