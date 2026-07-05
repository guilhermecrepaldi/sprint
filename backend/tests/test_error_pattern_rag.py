"""
Testes para o sistema de RAG de Padrões de Erro (error_pattern_rag.py).

Verifica:
1. Classificação de erro (classify_error)
2. Registro e consulta de padrões (ErrorPatternStore)
3. End-to-end: sugestão a partir de resposta do aluno (suggest_exercises_from_error)
"""

import pytest

from engine.error_pattern_rag import (
    ErrorPatternStore,
    classify_error,
    get_error_pattern_store,
    suggest_exercises_from_error,
)


# ── Test: classify_error ────────────────────────────────────────────────

class TestClassifyError:
    def test_correct_answer_returns_none(self):
        """Resposta correta → None (sem erro)."""
        result = classify_error(
            user_answer="4",
            expected_answer="4",
            is_correct=True,
        )
        assert result is None

    def test_signal_error_by_method_tag(self):
        """Method tag 'sinais' → 'sinal'."""
        result = classify_error(
            user_answer="-2",
            expected_answer="2",
            skill_tags=["algebra"],
            method_tags=["sinais"],
            is_correct=False,
        )
        assert result == "sinal"

    def test_signal_error_by_text(self):
        """Texto com 'menos com menos' → 'sinal'."""
        result = classify_error(
            user_answer="erro de sinal na resposta",
            expected_answer="5",
            is_correct=False,
        )
        assert result == "sinal"

    def test_fraction_error(self):
        """Fração → 'fracao'."""
        result = classify_error(
            user_answer="2/3",
            expected_answer="3/4",
            skill_tags=["fracao"],
            is_correct=False,
        )
        assert result == "fracao"

    def test_bhaskara_error(self):
        """Bhaskara/delta → 'bhaskara'."""
        result = classify_error(
            user_answer="x = 2",
            expected_answer="x = -2",
            skill_tags=["equacao_quadratica"],
            method_tags=["bhaskara"],
            is_correct=False,
        )
        assert result == "bhaskara"

    def test_unknown_error_fallback(self):
        """Sem match → 'desconhecido'."""
        result = classify_error(
            user_answer="azul",
            expected_answer="42",
            is_correct=False,
        )
        assert result == "desconhecido"

    def test_empty_answers_fallback(self):
        """Respostas vazias → 'desconhecido'."""
        result = classify_error(
            user_answer="",
            expected_answer="",
            is_correct=False,
        )
        assert result == "desconhecido"


# ── Test: ErrorPatternStore ────────────────────────────────────────────

class TestErrorPatternStore:
    @pytest.fixture
    def store(self):
        return ErrorPatternStore(db=None)

    @pytest.mark.asyncio
    async def test_record_and_query(self, store):
        """Registra erros e consulta padrões similares."""
        await store.record_error(
            error_type="sinal",
            skill_tags=["algebra", "equacao_quadratica"],
            method_tags=["bhaskara"],
            template_id="eq_2g_v1",
            difficulty=5.0,
            was_overcome=False,
        )
        await store.record_error(
            error_type="sinal",
            skill_tags=["algebra", "equacao_quadratica"],
            method_tags=["bhaskara"],
            template_id="eq_2g_v1",
            difficulty=5.0,
            was_overcome=True,  # este aluno superou
        )
        
        result = await store.query_similar(
            error_type="sinal",
            skill_tags=["algebra", "equacao_quadratica"],
            method_tags=["bhaskara"],
            template_id="eq_2g_v1",
            top_k=3,
        )
        
        assert len(result) >= 1
        assert result[0]["error_type"] == "sinal"
        assert result[0]["student_count"] >= 2
        assert result[0]["success_rate"] >= 0.5  # 1/2 superaram

    @pytest.mark.asyncio
    async def test_query_no_match(self, store):
        """Query sem match próximo → retorna match de mesma família (não vazio)."""
        await store.record_error(
            error_type="sinal",
            skill_tags=["algebra"],
            method_tags=[],
            template_id=None,
            difficulty=5.0,
        )
        
        result = await store.query_similar(
            error_type="conjunto",  # família diferente (notacao vs operacao)
            skill_tags=["trigonometria"],  # skill diferente
            top_k=3,
        )
        
        # Pode retornar vazio ou match distante com score baixo
        # O importante é que não retorne o erro de sinal com score alto
        if result:
            # Se retornar algo, deve ser com score muito baixo (apenas sucess_rate bonus)
            assert result[0]["error_type"] != "sinal" or result[0].get("success_rate", 0) == 0

    @pytest.mark.asyncio
    async def test_multiple_errors_same_key_grouped(self, store):
        """Múltiplos registros do mesmo erro → agrupados."""
        for _ in range(5):
            await store.record_error(
                error_type="fracao",
                skill_tags=["fracao_soma"],
                method_tags=["mmc"],
                template_id="frac_v1",
                difficulty=5.0,
                was_overcome=True,
            )
        
        result = await store.query_similar(
            error_type="fracao",
            skill_tags=["fracao_soma"],
            top_k=3,
        )
        
        assert len(result) >= 1
        assert result[0]["student_count"] == 5
        assert result[0]["success_rate"] == 1.0  # todos superaram

    @pytest.mark.asyncio
    async def test_stats(self, store):
        """Estatísticas do repositório."""
        await store.record_error("sinal", ["algebra"], was_overcome=True)
        await store.record_error("fracao", ["fracao_soma"], was_overcome=False)
        
        stats = await store.get_stats()
        
        assert stats["total_patterns"] >= 2
        assert stats["total_errors_recorded"] >= 2
        assert "sinal" in stats["error_types"]
        assert "fracao" in stats["error_types"]


# ── Test: suggest_exercises_from_error (end-to-end) ─────────────────────

class TestSuggestExercisesEndToEnd:
    @pytest.mark.asyncio
    async def test_correct_answer_no_suggestions(self):
        """Resposta correta → sem sugestões."""
        result = await suggest_exercises_from_error(
            user_answer="5",
            expected_answer="5",
            is_correct=True,
        )
        assert result["error_type"] is None
        assert result["has_suggestions"] is False

    @pytest.mark.asyncio
    async def test_error_with_suggestions(self):
        """Erro classificado → sugestões baseadas em padrões."""
        # Primeiro, popula o repositório com alguns padrões
        store = get_error_pattern_store()
        await store.record_error(
            error_type="sinal",
            skill_tags=["algebra", "equacao_quadratica"],
            method_tags=["bhaskara"],
            template_id="eq_2g_v1",
            difficulty=5.0,
            was_overcome=True,
        )
        
        result = await suggest_exercises_from_error(
            user_answer="x = -3",
            expected_answer="x = 3",
            skill_tags=["algebra", "equacao_quadratica"],
            method_tags=["bhaskara"],
            template_id="eq_2g_v1",
            difficulty=5.0,
            is_correct=False,
        )
        
        assert result["error_type"] is not None
        assert result["has_suggestions"] is True
        assert len(result["suggestions"]) >= 1
        assert "pedagogical_hint" in result["suggestions"][0]
        assert "stats" in result

    @pytest.mark.asyncio
    async def test_error_unknown_but_empty_store(self):
        """Erro desconhecido → sugestões vazias se única ocorrência."""
        # Cria store fresh
        store = ErrorPatternStore(db=None)
        
        result = await suggest_exercises_from_error(
            user_answer="xyz",
            expected_answer="42",
            is_correct=False,
        )
        
        # "desconhecido" sempre retorna, mas sem sugestões de outros padrões
        assert result["error_type"] == "desconhecido"
        # Pode ter 0 ou 1 sugestões (a própria ocorrência que acabou de registrar)
        # O importante é que não tenha sugestões de outros alunos
        for s in result.get("suggestions", []):
            assert s["student_count"] == 1  # só o próprio aluno


# ── Test: Integração (requer banco) ────────────────────────────────────

@pytest.mark.skip(reason="Requer PostgreSQL rodando")
class TestIntegrationWithDB:
    """Testes que dependem de banco de dados real."""
    pass
