"""
Seed de exercícios com skill_tags canônicas do PREREQUISITE_TREE.
"""
import asyncio
import sys
from pathlib import Path

from sqlalchemy import select, delete

sys.path.append(str(Path(__file__).resolve().parents[1]))

from db import AsyncSessionLocal
from models.exercise import Exercise
from models.attempt import ExerciseAttempt, PenEvent
from models.session import FolhaExercise
from models.vector import CognitiveVector

EXERCISES = [
    # ── soma_subtracao ────────────────────────────────────────────────────────
    {"statement": "Calcule: 348 + 197", "expected_answer": "545",
     "skill_tags": ["soma_subtracao"], "difficulty": 1.0},
    {"statement": "Calcule: 1002 - 487", "expected_answer": "515",
     "skill_tags": ["soma_subtracao"], "difficulty": 1.2},
    {"statement": "Calcule: 56 + 78 - 34", "expected_answer": "100",
     "skill_tags": ["soma_subtracao"], "difficulty": 1.3},

    # ── multiplicacao_divisao ─────────────────────────────────────────────────
    {"statement": "Calcule: 24 × 13", "expected_answer": "312",
     "skill_tags": ["multiplicacao_divisao"], "difficulty": 1.5},
    {"statement": "Calcule: 288 ÷ 12", "expected_answer": "24",
     "skill_tags": ["multiplicacao_divisao"], "difficulty": 1.5},
    {"statement": "Calcule: 15 × 8 ÷ 6", "expected_answer": "20",
     "skill_tags": ["multiplicacao_divisao"], "difficulty": 1.8},

    # ── fracoes_decimais ──────────────────────────────────────────────────────
    {"statement": r"Calcule: \frac{2}{3} + \frac{5}{6}", "expected_answer": r"\frac{3}{2}",
     "skill_tags": ["fracoes_decimais"], "difficulty": 2.0},
    {"statement": r"Calcule: \frac{7}{8} - \frac{1}{4}", "expected_answer": r"\frac{5}{8}",
     "skill_tags": ["fracoes_decimais"], "difficulty": 2.0},
    {"statement": r"Calcule: 0{,}25 + \frac{1}{2}", "expected_answer": r"\frac{3}{4}",
     "skill_tags": ["fracoes_decimais"], "difficulty": 2.2},
    {"statement": r"Simplifique: \frac{18}{24}", "expected_answer": r"\frac{3}{4}",
     "skill_tags": ["fracoes_decimais"], "difficulty": 1.8},

    # ── porcentagem_razao ─────────────────────────────────────────────────────
    {"statement": "Quanto é 30% de 250?", "expected_answer": "75",
     "skill_tags": ["porcentagem_razao"], "difficulty": 2.0},
    {"statement": "Um produto custa R$80 com 15% de desconto. Qual o preço final?",
     "expected_answer": "68", "skill_tags": ["porcentagem_razao"], "difficulty": 2.3},

    # ── potenciacao_radiciacao ────────────────────────────────────────────────
    {"statement": r"Calcule: 2^{10}", "expected_answer": "1024",
     "skill_tags": ["potenciacao_radiciacao"], "difficulty": 2.0},
    {"statement": r"Calcule: \sqrt{144}", "expected_answer": "12",
     "skill_tags": ["potenciacao_radiciacao"], "difficulty": 1.8},
    {"statement": r"Calcule: (-3)^4", "expected_answer": "81",
     "skill_tags": ["potenciacao_radiciacao"], "difficulty": 2.2},

    # ── equacoes_lineares ─────────────────────────────────────────────────────
    {"statement": "x + 3 = 0", "expected_answer": "x = -3",
     "skill_tags": ["equacoes_lineares"], "difficulty": 1.5},
    {"statement": "Resolva: 3x + 7 = 22", "expected_answer": "x = 5",
     "skill_tags": ["equacoes_lineares"], "difficulty": 2.0},
    {"statement": "Resolva: 5 - 2x = 17", "expected_answer": "x = -6",
     "skill_tags": ["equacoes_lineares"], "difficulty": 2.2},
    {"statement": "Resolva: 2(x - 3) = 4x + 2", "expected_answer": "x = -4",
     "skill_tags": ["equacoes_lineares"], "difficulty": 2.8},
    {"statement": r"Resolva: \frac{x-1}{2} = 3", "expected_answer": "x = 7",
     "skill_tags": ["equacoes_lineares"], "difficulty": 2.6},
    {"statement": r"Resolva: \frac{x}{3} + 2 = 6", "expected_answer": "x = 12",
     "skill_tags": ["equacoes_lineares"], "difficulty": 2.4},

    # ── sistemas_equacoes ─────────────────────────────────────────────────────
    {"statement": "Resolva: x + y = 10 e x - y = 4", "expected_answer": "x = 7, y = 3",
     "skill_tags": ["sistemas_equacoes"], "difficulty": 3.0},
    {"statement": "Resolva: 2x + y = 8 e x - y = 1", "expected_answer": "x = 3, y = 2",
     "skill_tags": ["sistemas_equacoes"], "difficulty": 3.2},

    # ── fatoracao_produtos_notaveis ───────────────────────────────────────────
    {"statement": "Fatore: x² + 5x + 6", "expected_answer": "(x+2)(x+3)",
     "skill_tags": ["fatoracao_produtos_notaveis"], "difficulty": 3.0},
    {"statement": "Fatore: x² - 16", "expected_answer": "(x-4)(x+4)",
     "skill_tags": ["fatoracao_produtos_notaveis"], "difficulty": 2.7},
    {"statement": "Expanda: (x + 3)²", "expected_answer": "x^2 + 6x + 9",
     "skill_tags": ["fatoracao_produtos_notaveis"], "difficulty": 2.8},
    {"statement": "Expanda: (x + 4)(x - 2)", "expected_answer": "x^2 + 2x - 8",
     "skill_tags": ["fatoracao_produtos_notaveis"], "difficulty": 3.0},

    # ── inequacoes ────────────────────────────────────────────────────────────
    {"statement": "Resolva: 2x - 3 > 7", "expected_answer": "x > 5",
     "skill_tags": ["inequacoes"], "difficulty": 2.5},
    {"statement": "Resolva: -3x + 1 ≤ 10", "expected_answer": r"x \geq -3",
     "skill_tags": ["inequacoes"], "difficulty": 2.8},

    # ── equacoes_quadraticas ──────────────────────────────────────────────────
    {"statement": "Resolva: x² - 9 = 0", "expected_answer": r"x = \pm 3",
     "skill_tags": ["equacoes_quadraticas"], "difficulty": 2.5},
    {"statement": "Resolva: x² - 5x + 6 = 0", "expected_answer": "x = 2 ou x = 3",
     "skill_tags": ["equacoes_quadraticas"], "difficulty": 3.2},
    {"statement": "Resolva: 2x² + 5x - 3 = 0", "expected_answer": r"x = \frac{1}{2} ou x = -3",
     "skill_tags": ["equacoes_quadraticas"], "difficulty": 3.5},
    {"statement": "Resolva: x² = 49", "expected_answer": r"x = \pm 7",
     "skill_tags": ["equacoes_quadraticas"], "difficulty": 2.4},

    # ── funcao_afim ───────────────────────────────────────────────────────────
    {"statement": "Dada f(x) = 3x - 2, calcule f(4).", "expected_answer": "10",
     "skill_tags": ["funcao_afim"], "difficulty": 2.5},
    {"statement": "Encontre a raiz de f(x) = 2x + 8.", "expected_answer": "x = -4",
     "skill_tags": ["funcao_afim"], "difficulty": 2.8},

    # ── funcao_quadratica ─────────────────────────────────────────────────────
    {"statement": "Dada f(x) = x² - 4x + 3, calcule o vértice.", "expected_answer": "(2, -1)",
     "skill_tags": ["funcao_quadratica"], "difficulty": 3.5},
    {"statement": "Qual o valor mínimo de f(x) = x² - 6x + 5?", "expected_answer": "-4",
     "skill_tags": ["funcao_quadratica"], "difficulty": 3.5},

    # ── funcao_exponencial ────────────────────────────────────────────────────
    {"statement": "Resolva: 2^x = 32", "expected_answer": "x = 5",
     "skill_tags": ["funcao_exponencial"], "difficulty": 3.5},
    {"statement": "Resolva: 3^(x+1) = 27", "expected_answer": "x = 2",
     "skill_tags": ["funcao_exponencial"], "difficulty": 3.8},

    # ── funcao_logaritmica ────────────────────────────────────────────────────
    {"statement": r"Calcule: \log_2 8", "expected_answer": "3",
     "skill_tags": ["funcao_logaritmica"], "difficulty": 3.5},
    {"statement": r"Resolva: \log_3 x = 4", "expected_answer": "x = 81",
     "skill_tags": ["funcao_logaritmica"], "difficulty": 3.8},
    {"statement": r"Calcule: \log_{10} 1000", "expected_answer": "3",
     "skill_tags": ["funcao_logaritmica"], "difficulty": 3.2},

    # ── geometria_plana ───────────────────────────────────────────────────────
    {"statement": "Calcule a área de um triângulo com base 8 e altura 5.", "expected_answer": "20",
     "skill_tags": ["geometria_plana"], "difficulty": 2.0},
    {"statement": "Calcule a área de um círculo com raio 7. Use π ≈ 3,14.", "expected_answer": "153,86",
     "skill_tags": ["geometria_plana"], "difficulty": 2.5},

    # ── nocao_de_limite ───────────────────────────────────────────────────────
    {"statement": r"Calcule: \lim_{x \to 2} (3x - 1)", "expected_answer": "5",
     "skill_tags": ["nocao_de_limite"], "difficulty": 5.0},

    # ── derivadas_basicas ─────────────────────────────────────────────────────
    {"statement": "Calcule a derivada de f(x) = x³ - 2x + 1.", "expected_answer": "3x^2 - 2",
     "skill_tags": ["derivadas_basicas"], "difficulty": 6.0},
    {"statement": "Calcule f'(x) para f(x) = 5x² + 3x.", "expected_answer": "10x + 3",
     "skill_tags": ["derivadas_basicas"], "difficulty": 5.5},

    # ── integrais_indefinidas ─────────────────────────────────────────────────
    {"statement": r"Calcule: \int (2x + 3)\,dx", "expected_answer": "x^2 + 3x + C",
     "skill_tags": ["integrais_indefinidas"], "difficulty": 7.0},
]

# Campos padrão para todos os exercícios
_DEFAULTS = {
    "estimated_time_ms": 45000,
    "subject": "math",
    "canvas_mode": "calculation",
    "validator": "sympy",
    "source_library": "seed_v2",
}


async def main() -> None:
    async with AsyncSessionLocal() as db:
        # Obter IDs dos exercícios seed_v1
        old_ids_result = await db.execute(
            select(Exercise.id).where(Exercise.source_library == "seed_v1")
        )
        old_ids = list(old_ids_result.scalars().all())

        if old_ids:
            # Obter attempt IDs referenciando esses exercícios
            att_result = await db.execute(
                select(ExerciseAttempt.id).where(ExerciseAttempt.exercise_id.in_(old_ids))
            )
            att_ids = list(att_result.scalars().all())

            # Deletar netos (pen_events, cognitive_vectors) antes dos attempts
            if att_ids:
                await db.execute(
                    delete(PenEvent).where(PenEvent.attempt_id.in_(att_ids))
                )
                await db.execute(
                    delete(CognitiveVector).where(CognitiveVector.attempt_id.in_(att_ids))
                )

            # Deletar filhos (attempts, folha_exercises) antes dos exercises
            await db.execute(
                delete(ExerciseAttempt).where(ExerciseAttempt.exercise_id.in_(old_ids))
            )
            await db.execute(
                delete(FolhaExercise).where(FolhaExercise.exercise_id.in_(old_ids))
            )
            await db.execute(
                delete(Exercise).where(Exercise.id.in_(old_ids))
            )
            await db.commit()
            print(f"Seed v1: {len(old_ids)} exercícios e seus dados removidos.")

        statements = [item["statement"] for item in EXERCISES]
        existing = await db.execute(
            select(Exercise.statement).where(Exercise.statement.in_(statements))
        )
        existing_set = set(existing.scalars().all())

        new_exercises = [
            Exercise(**{**_DEFAULTS, **item})
            for item in EXERCISES
            if item["statement"] not in existing_set
        ]
        db.add_all(new_exercises)
        await db.commit()
        print(f"Seed v2: {len(new_exercises)} exercícios inseridos, {len(existing_set)} já existiam.")


if __name__ == "__main__":
    asyncio.run(main())
