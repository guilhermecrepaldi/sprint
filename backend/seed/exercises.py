import asyncio
import sys
from pathlib import Path

from sqlalchemy import select

sys.path.append(str(Path(__file__).resolve().parents[1]))

from db import AsyncSessionLocal
from models.exercise import Exercise


EXERCISES = [
    {
        "statement": "Resolva: x^2 - 9 = 0",
        "expected_answer": "x = \\pm 3",
        "skill_tags": ["equacao_2_grau"],
        "difficulty": 2.5,
        "estimated_time_ms": 45000,
        "subject": "math",
        "canvas_mode": "calculation",
        "validator": "sympy",
    },
    {
        "statement": "Simplifique: (3x^2y) / (xy)",
        "expected_answer": "3x",
        "skill_tags": ["fracao_algebrica", "simplificacao"],
        "difficulty": 3.0,
        "estimated_time_ms": 40000,
        "subject": "math",
        "canvas_mode": "calculation",
        "validator": "sympy",
    },
    {
        "statement": "Calcule: \\frac{2}{3} + \\frac{5}{6}",
        "expected_answer": "\\frac{3}{2}",
        "skill_tags": ["fracao"],
        "difficulty": 2.0,
        "estimated_time_ms": 35000,
        "subject": "math",
        "canvas_mode": "calculation",
        "validator": "sympy",
    },
    {
        "statement": "Fatore: x^2 + 5x + 6",
        "expected_answer": "(x+2)(x+3)",
        "skill_tags": ["fatoracao"],
        "difficulty": 3.5,
        "estimated_time_ms": 55000,
        "subject": "math",
        "canvas_mode": "calculation",
        "validator": "sympy",
    },
    {
        "statement": "Resolva: 2(x - 3) = 4x + 2",
        "expected_answer": "x = -4",
        "skill_tags": ["equacao_1_grau", "distributiva"],
        "difficulty": 2.8,
        "estimated_time_ms": 45000,
        "subject": "math",
        "canvas_mode": "calculation",
        "validator": "sympy",
    },
    {
        "statement": "Resolva: 3x + 7 = 22",
        "expected_answer": "x = 5",
        "skill_tags": ["equacao_1_grau"],
        "difficulty": 2.0,
        "estimated_time_ms": 30000,
        "subject": "math",
        "canvas_mode": "calculation",
        "validator": "sympy",
    },
    {
        "statement": "Calcule: \\frac{7}{8} - \\frac{1}{4}",
        "expected_answer": "\\frac{5}{8}",
        "skill_tags": ["fracao"],
        "difficulty": 2.1,
        "estimated_time_ms": 30000,
        "subject": "math",
        "canvas_mode": "calculation",
        "validator": "sympy",
    },
    {
        "statement": "Simplifique: 4a + 3a - 2a",
        "expected_answer": "5a",
        "skill_tags": ["simplificacao"],
        "difficulty": 2.0,
        "estimated_time_ms": 25000,
        "subject": "math",
        "canvas_mode": "calculation",
        "validator": "sympy",
    },
    {
        "statement": "Resolva: x/3 + 2 = 6",
        "expected_answer": "x = 12",
        "skill_tags": ["equacao_1_grau", "fracao"],
        "difficulty": 2.4,
        "estimated_time_ms": 40000,
        "subject": "math",
        "canvas_mode": "calculation",
        "validator": "sympy",
    },
    {
        "statement": "Expanda: (x + 4)(x - 2)",
        "expected_answer": "x^2 + 2x - 8",
        "skill_tags": ["produtos_notaveis", "distributiva"],
        "difficulty": 3.0,
        "estimated_time_ms": 45000,
        "subject": "math",
        "canvas_mode": "calculation",
        "validator": "sympy",
    },
    {
        "statement": "Fatore: x^2 - 16",
        "expected_answer": "(x-4)(x+4)",
        "skill_tags": ["fatoracao", "diferenca_quadrados"],
        "difficulty": 2.7,
        "estimated_time_ms": 40000,
        "subject": "math",
        "canvas_mode": "calculation",
        "validator": "sympy",
    },
    {
        "statement": "Resolva: x^2 - 5x + 6 = 0",
        "expected_answer": "x = 2, 3",
        "skill_tags": ["equacao_2_grau", "fatoracao"],
        "difficulty": 3.2,
        "estimated_time_ms": 60000,
        "subject": "math",
        "canvas_mode": "calculation",
        "validator": "sympy",
    },
    {
        "statement": "Calcule: 0.25 + \\frac{1}{2}",
        "expected_answer": "\\frac{3}{4}",
        "skill_tags": ["fracao", "decimal"],
        "difficulty": 2.2,
        "estimated_time_ms": 30000,
        "subject": "math",
        "canvas_mode": "calculation",
        "validator": "sympy",
    },
    {
        "statement": "Simplifique: \\frac{12x^3}{4x}",
        "expected_answer": "3x^2",
        "skill_tags": ["fracao_algebrica", "potencias"],
        "difficulty": 3.1,
        "estimated_time_ms": 42000,
        "subject": "math",
        "canvas_mode": "calculation",
        "validator": "sympy",
    },
    {
        "statement": "Resolva: 5 - 2x = 17",
        "expected_answer": "x = -6",
        "skill_tags": ["equacao_1_grau", "sinal"],
        "difficulty": 2.3,
        "estimated_time_ms": 35000,
        "subject": "math",
        "canvas_mode": "calculation",
        "validator": "sympy",
    },
    {
        "statement": "Expanda: (x + 3)^2",
        "expected_answer": "x^2 + 6x + 9",
        "skill_tags": ["produtos_notaveis"],
        "difficulty": 2.8,
        "estimated_time_ms": 40000,
        "subject": "math",
        "canvas_mode": "calculation",
        "validator": "sympy",
    },
    {
        "statement": "Fatore: 2x + 2y",
        "expected_answer": "2(x+y)",
        "skill_tags": ["fatoracao", "fator_comum"],
        "difficulty": 2.4,
        "estimated_time_ms": 35000,
        "subject": "math",
        "canvas_mode": "calculation",
        "validator": "sympy",
    },
    {
        "statement": "Resolva: \\frac{x - 1}{2} = 3",
        "expected_answer": "x = 7",
        "skill_tags": ["equacao_1_grau", "fracao"],
        "difficulty": 2.6,
        "estimated_time_ms": 40000,
        "subject": "math",
        "canvas_mode": "calculation",
        "validator": "sympy",
    },
    {
        "statement": "Calcule: (-3)^2 - 4",
        "expected_answer": "5",
        "skill_tags": ["potencias", "sinal"],
        "difficulty": 2.0,
        "estimated_time_ms": 25000,
        "subject": "math",
        "canvas_mode": "calculation",
        "validator": "sympy",
    },
    {
        "statement": "Resolva: x^2 = 49",
        "expected_answer": "x = \\pm 7",
        "skill_tags": ["equacao_2_grau", "raiz_quadrada"],
        "difficulty": 2.4,
        "estimated_time_ms": 35000,
        "subject": "math",
        "canvas_mode": "calculation",
        "validator": "sympy",
    },
    {
        "statement": "Um corpo percorre 20 m em 4 s. Calcule a velocidade média.",
        "expected_answer": "5",
        "skill_tags": ["cinematica", "velocidade_media"],
        "difficulty": 2.0,
        "estimated_time_ms": 30000,
        "subject": "physics",
        "canvas_mode": "calculation",
        "validator": "sympy",
    },
    {
        "statement": "Uma força de 10 N age em massa de 2 kg. Calcule a aceleração.",
        "expected_answer": "5",
        "skill_tags": ["dinamica", "segunda_lei_newton"],
        "difficulty": 2.2,
        "estimated_time_ms": 35000,
        "subject": "physics",
        "canvas_mode": "calculation",
        "validator": "sympy",
    },
]


async def main() -> None:
    async with AsyncSessionLocal() as db:
        statements = [item["statement"] for item in EXERCISES]
        existing = await db.execute(select(Exercise.statement).where(Exercise.statement.in_(statements)))
        existing_statements = set(existing.scalars().all())

        db.add_all(
            Exercise(source_library="seed_v1", **item)
            for item in EXERCISES
            if item["statement"] not in existing_statements
        )
        await db.commit()


if __name__ == "__main__":
    asyncio.run(main())
