import uuid
from collections import Counter
from dataclasses import dataclass, field

from sqlalchemy import desc, select
from sqlalchemy.ext.asyncio import AsyncSession

from models.attempt import ExerciseAttempt
from models.vector import StudentSkillMemory


@dataclass
class StudentBrief:
    student_id: str
    focus_skills: list[str] = field(default_factory=list)
    recurring_errors: list[str] = field(default_factory=list)
    recommended_difficulty: float = 2.0
    narrative: str = ""


class ProgressionResearchAgent:
    async def analyze(
        self,
        db: AsyncSession,
        student_id: uuid.UUID,
        recent_attempts: list | None = None,
        skill_memories: list | None = None,
    ) -> StudentBrief:
        """
        Analisa histórico e produz brief para o ExerciseCreator.
        Roda a cada 5 exercícios em background task.
        """
        if recent_attempts is None:
            attempts_result = await db.execute(
                select(ExerciseAttempt)
                .where(ExerciseAttempt.student_id == student_id)
                .order_by(desc(ExerciseAttempt.created_at))
                .limit(20)
            )
            recent_attempts = list(attempts_result.scalars().all())

        if skill_memories is None:
            memory_result = await db.execute(
                select(StudentSkillMemory).where(StudentSkillMemory.student_id == student_id)
            )
            skill_memories = list(memory_result.scalars().all())

        if not skill_memories:
            return StudentBrief(student_id=str(student_id))

        # Skills fracas: accuracy < 0.70
        weak = sorted(
            [m for m in skill_memories if m.accuracy < 0.70],
            key=lambda m: m.accuracy,
        )
        focus = [m.skill for m in weak[:2]]

        # Erros recorrentes dos últimos 10 attempts
        error_types = [
            a.error_type
            for a in recent_attempts[-10:]
            if a.error_type and a.error_type != "desconhecido"
        ]
        recurring = [e for e, _ in Counter(error_types).most_common(2)]

        # Dificuldade recomendada = média das últimas 5 attempts com acerto
        correct_recent = [a for a in recent_attempts[-10:] if a.is_correct]
        if correct_recent:
            recommended = sum(a.difficulty for a in correct_recent) / len(correct_recent)
        else:
            recommended = (
                skill_memories[0].last_difficulty
                if hasattr(skill_memories[0], "last_difficulty")
                else 2.0
            )

        narrative = (
            f"Aluno {student_id}. "
            + (f"Skills fracas: {', '.join(focus)}. " if focus else "Bom desempenho geral. ")
            + (f"Erros recorrentes: {', '.join(recurring)}. " if recurring else "")
            + f"Dificuldade recomendada: {recommended:.1f}."
        )

        return StudentBrief(
            student_id=str(student_id),
            focus_skills=focus,
            recurring_errors=recurring,
            recommended_difficulty=round(recommended, 1),
            narrative=narrative,
        )
