"""
error_rag_router.py — API endpoints for Error Pattern RAG

GET  /api/student/{id}/error-suggestions?answer=...&expected=...&skill_tags=...
    → Classifica erro + busca padrões similares + sugere exercícios

GET  /api/error-patterns/stats
    → Estatísticas do repositório de padrões de erro

POST /api/error-patterns/record
    → Registra manualmente um padrão de erro (para testes/feedback)
"""

import uuid
from typing import Any

from fastapi import APIRouter, Depends, Query
from sqlalchemy.ext.asyncio import AsyncSession

from db import get_db
from engine.error_pattern_rag import (
    get_error_pattern_store,
    suggest_exercises_from_error,
)

router = APIRouter(prefix="/api", tags=["error-patterns"])


@router.get("/student/{student_id}/error-suggestions")
async def get_error_suggestions(
    student_id: uuid.UUID,
    answer: str = Query(..., description="Resposta do aluno"),
    expected: str = Query(..., description="Resposta esperada"),
    skill_tags: str = Query("", description="Skill tags separadas por vírgula"),
    method_tags: str = Query("", description="Method tags separadas por vírgula"),
    template_id: str | None = Query(None, description="Template ID do exercício"),
    difficulty: float = Query(5.0, ge=1.0, le=10.0, description="Dificuldade"),
    db: AsyncSession = Depends(get_db),
) -> dict[str, Any]:
    """
    Classifica o erro do aluno, consulta o repositório de padrões
    anonimizados, e retorna sugestões pedagógicas contextualizadas.
    """
    skills = [s.strip() for s in skill_tags.split(",") if s.strip()] if skill_tags else []
    methods = [m.strip() for m in method_tags.split(",") if m.strip()] if method_tags else []
    
    return await suggest_exercises_from_error(
        user_answer=answer,
        expected_answer=expected,
        skill_tags=skills,
        method_tags=methods,
        template_id=template_id,
        difficulty=difficulty,
        is_correct=False,
        top_k=3,
    )


@router.get("/error-patterns/stats")
async def get_error_stats(db: AsyncSession = Depends(get_db)) -> dict[str, Any]:
    """Estatísticas do repositório de padrões de erro."""
    store = get_error_pattern_store(db=db)
    return await store.get_stats()


@router.post("/error-patterns/record")
async def record_error_pattern(
    error_type: str = Query(..., description="Tipo do erro (sinal, fracao, ...)"),
    skill_tags: str = Query("", description="Skill tags separadas por vírgula"),
    method_tags: str = Query("", description="Method tags separadas por vírgula"),
    template_id: str | None = Query(None),
    difficulty: float = Query(5.0),
    was_overcome: bool = Query(False, description="Aluno superou este erro depois?"),
    db: AsyncSession = Depends(get_db),
) -> dict[str, str]:
    """Registra manualmente um padrão de erro (feedback loop)."""
    store = get_error_pattern_store(db=db)
    skills = [s.strip() for s in skill_tags.split(",") if s.strip()] if skill_tags else []
    methods = [m.strip() for m in method_tags.split(",") if m.strip()] if method_tags else []
    
    await store.record_error(
        error_type=error_type,
        skill_tags=skills,
        method_tags=methods,
        template_id=template_id,
        difficulty=difficulty,
        was_overcome=was_overcome,
    )
    
    return {"status": "recorded", "error_type": error_type}
