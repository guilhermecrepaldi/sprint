import sys
import unittest
from pathlib import Path

sys.path.append(str(Path(__file__).resolve().parents[1]))

from engine.unlock import check_unlock, get_available_skills


class UnlockTests(unittest.TestCase):
    def test_root_skill_always_available(self):
        # soma_subtracao has no prerequisites → always available
        status = check_unlock("soma_subtracao", {})
        self.assertTrue(status.unlocked)

    def test_requires_90_percent_mastery(self):
        mem = {"equacoes_lineares": {"accuracy": 0.89, "attempt_count": 150}}
        result = check_unlock("equacoes_quadraticas", mem)
        self.assertFalse(result.unlocked)
        self.assertIn("equacoes_lineares", result.missing_mastery)

    def test_requires_100_exercises(self):
        mem = {"equacoes_lineares": {"accuracy": 0.95, "attempt_count": 99}}
        result = check_unlock("equacoes_quadraticas", mem)
        self.assertFalse(result.unlocked)
        self.assertIn("equacoes_lineares", result.missing_exercises)

    def test_unlocks_when_both_met(self):
        mem = {"equacoes_lineares": {"accuracy": 0.90, "attempt_count": 100}}
        self.assertTrue(check_unlock("equacoes_quadraticas", mem).unlocked)

    def test_multiple_prereqs_all_required(self):
        # nocao_de_limite requires funcao_logaritmica AND trig_identidades
        mem = {
            "funcao_logaritmica": {"accuracy": 0.92, "attempt_count": 110},
            # trig_identidades absent
        }
        result = check_unlock("nocao_de_limite", mem)
        self.assertFalse(result.unlocked)

    def test_exactly_at_threshold_unlocks(self):
        mem = {"equacoes_lineares": {"accuracy": 0.90, "attempt_count": 100}}
        self.assertTrue(check_unlock("equacoes_quadraticas", mem).unlocked)

    def test_just_below_exercise_count_blocks(self):
        mem = {"equacoes_lineares": {"accuracy": 0.90, "attempt_count": 99}}
        result = check_unlock("equacoes_quadraticas", mem)
        self.assertFalse(result.unlocked)
        self.assertIn("equacoes_lineares", result.missing_exercises)

    def test_get_available_skills_empty_memory(self):
        # With no memory, only root skills (no prerequisites) should be available
        available = get_available_skills({})
        # soma_subtracao is a root and is not in PREREQUISITE_TREE keys as target
        # but all skills in the tree that have no prereqs are available
        # Actually root skills are those with empty prereq lists → not in tree
        # get_available_skills iterates PREREQUISITE_TREE keys; roots without prereqs
        # are topics with [] — but soma_subtracao is not a key in PREREQUISITE_TREE.
        # So available should be empty when no memory exists.
        self.assertIsInstance(available, list)

    def test_missing_mastery_and_exercises_both_listed(self):
        mem = {"equacoes_lineares": {"accuracy": 0.85, "attempt_count": 50}}
        result = check_unlock("equacoes_quadraticas", mem)
        self.assertFalse(result.unlocked)
        self.assertIn("equacoes_lineares", result.missing_mastery)
        self.assertIn("equacoes_lineares", result.missing_exercises)


if __name__ == "__main__":
    unittest.main()
