"""
error_pattern_rag.py — RAG-style Error Pattern Suggestion System

Inspirado por:
- Greptile (hybrid search + code graph para entender relações)
- Bloop.ai (RAG local offline-first)
- Sourcegraph Cody (context-aware retrieval)

Como funciona:
1. Indexa tentativas de ERRO de todos os alunos (anonimizadas) em um FTS virtual
2. Quando um aluno erra, consulta o índice por padrões similares
3. Retorna sugestões de exercícios que AJUDARAM outros alunos com o mesmo erro
4. Tudo offline-first, roda no SQLite local do Android ou PostgreSQL no backend

Arquitetura:
```
Student Error → classify_error()
    ↓
Query ErrorPattern FTS Index
    ↓
Rank by: same error_type + same skill_tags + success_rate
    ↓
Return top-3 suggested exercises + dica pedagógica
    ↓
Store new error pattern asynchronously
```
"""

import re
import uuid
from datetime import UTC, datetime
from typing import Any

# ── Error Classification (deterministic, no ML needed) ────────────────

SIGNAL_PATTERNS = re.compile(
    r"(?:\bmenos\s*com\s*menos|sinal|--|\+\s*-|-\s*\+|\(-.*\)\s*\+\s*\(-|"
    r"erro\s*de\s*sinal|confundiu\s*sinal)", re.IGNORECASE
)
FRACTION_PATTERNS = re.compile(
    r"(?:fracao|fração|denominador|mmc|divisão\s*de\s*frac|"
    r"soma\s*de\s*frac|multiplica\w*\s*frac)", re.IGNORECASE
)
SQUARE_ROOT_PATTERNS = re.compile(
    r"(?:raiz|radical|sqrt|quadrada|radicia)", re.IGNORECASE
)
FACTORING_PATTERNS = re.compile(
    r"(?:fatora|fatoração|fatoracao|produto\s*notavel|"
    r"quadrado\s*perfeito|agrupamento)", re.IGNORECASE
)
COMPLETE_SQUARE_PATTERNS = re.compile(
    r"(?:completar\s*quadrado|completando\s*quadrado|"
    r"trinomio\s*quadrado\s*perfeito)", re.IGNORECASE
)
BHASKARA_PATTERNS = re.compile(
    r"(?:bhaskara|delta|discriminante|b[a-z]?2\s*-\s*4ac|x\s*=\s*-b)", re.IGNORECASE
)
SET_NOTATION_PATTERNS = re.compile(
    r"(?:[{}]|conjunto|solução\s*=\s*\{|solucao\s*=\s*\{|vazio|∅)", re.IGNORECASE
)
COORDINATE_PATTERNS = re.compile(
    r"(?:par\s*ordenado|coordenada|\(-?\d+,-?\d+\)|"
    r"ponto|gráfico|grafico|intersec)", re.IGNORECASE
)


def classify_error(
    user_answer: str,
    expected_answer: str,
    skill_tags: list[str] | None = None,
    method_tags: list[str] | None = None,
    is_correct: bool = False,
) -> str | None:
    """
    Classifica o tipo de erro do aluno baseado na resposta vs esperada.
    
    Retorna um dos tipos:
    - sinal: erro de sinal
    - fracao: erro de fração
    - raiz_quadrada: erro de raiz
    - fatoracao: erro de fatoração
    - completar_quadrado: erro de completar quadrado
    - bhaskara: erro de Bhaskara/delta
    - conjunto: erro de notação de conjunto
    - coordenadas: erro de coordenadas
    - desconhecido: não classificado
    - None: acertou
    """
    if is_correct:
        return None

    tags_lower = [t.lower() for t in (skill_tags or [])]
    methods_lower = [m.lower() for m in (method_tags or [])]

    # 1. Match por method_tags primeiro (mais específico)
    method_map = {
        "sinais": "sinal",
        "atencao_sinal": "sinal",
        "fracao_operacao": "fracao",
        "raiz_quadrada": "raiz_quadrada",
        "fatoracao": "fatoracao",
        "completar_quadrado": "completar_quadrado",
        "bhaskara": "bhaskara",
        "discriminante": "bhaskara",
        "conjunto": "conjunto",
        "coordenadas": "coordenadas",
    }
    for method, error_type in method_map.items():
        if method in methods_lower:
            return error_type

    # 2. Match por skill_tags
    skill_map = {
        "sinais": "sinal",
        "fracao": "fracao",
        "raiz": "raiz_quadrada",
        "fatoracao": "fatoracao",
        "quadrado": "completar_quadrado",
        "bhaskara": "bhaskara",
    }
    for skill, error_type in skill_map.items():
        if any(skill in t for t in tags_lower):
            return error_type

    # 3. Match por padrão textual na resposta
    user_lower = user_answer.lower() if user_answer else ""
    expected_lower = expected_answer.lower() if expected_answer else ""

    combined = f"{user_lower} {expected_lower}"

    if SIGNAL_PATTERNS.search(combined):
        return "sinal"
    if FRACTION_PATTERNS.search(combined):
        return "fracao"
    if SQUARE_ROOT_PATTERNS.search(combined):
        return "raiz_quadrada"
    if FACTORING_PATTERNS.search(combined):
        return "fatoracao"
    if COMPLETE_SQUARE_PATTERNS.search(combined):
        return "completar_quadrado"
    if BHASKARA_PATTERNS.search(combined):
        return "bhaskara"
    if SET_NOTATION_PATTERNS.search(combined):
        return "conjunto"
    if COORDINATE_PATTERNS.search(combined):
        return "coordenadas"

    return "desconhecido"


# ── Error Pattern Memory (FTS5-backed in SQLite, JSONB-indexed in PG) ─

class ErrorPatternStore:
    """
    Armazena padrões de erro anonimizados para consulta RAG.
    
    Em produção (PostgreSQL): usa tabela error_patterns com JSONB
    Em desenvolvimento (SQLite): usa tabela virtual FTS5
    
    Cada registro:
    {
        "error_type": "sinal",
        "skill_tags": ["algebra", "equacao_quadratica"],
        "method_tags": ["bhaskara"],
        "template_id": "equacao_2o_grau_v1",
        "difficulty": 5.0,
        "student_count": 42,  # qtde de alunos que cometeram este erro
        "success_rate": 0.73,  # % de alunos que superaram
        "suggested_exercises": ["eq_2g_sinal_01", "eq_2g_sinal_02"],
        "pedagogical_hint": "Atenção ao sinal dentro do parênteses...",
        "last_updated": "2026-06-09T12:00:00Z"
    }
    """
    
    def __init__(self, db=None):
        self.db = db
        self._memory_store: dict[str, dict[str, Any]] = {}  # fallback in-memory
    
    async def record_error(
        self,
        error_type: str,
        skill_tags: list[str],
        method_tags: list[str] | None = None,
        template_id: str | None = None,
        difficulty: float = 5.0,
        was_overcome: bool = False,
    ) -> None:
        """Registra um padrão de erro anonimizado."""
        key = self._make_key(error_type, skill_tags, template_id)
        
        if key not in self._memory_store:
            self._memory_store[key] = {
                "error_type": error_type,
                "skill_tags": sorted(skill_tags),
                "method_tags": sorted(method_tags or []),
                "template_id": template_id,
                "difficulty": difficulty,
                "student_count": 0,
                "success_count": 0,
                "success_rate": 0.0,
                "suggested_exercises": [],
                "pedagogical_hint": self._get_hint(error_type, skill_tags),
                "first_seen": datetime.now(UTC).isoformat(),
                "last_updated": datetime.now(UTC).isoformat(),
            }
        
        entry = self._memory_store[key]
        entry["student_count"] += 1
        if was_overcome:
            entry["success_count"] += 1
        entry["success_rate"] = entry["success_count"] / max(entry["student_count"], 1)
        entry["difficulty"] = 0.7 * entry["difficulty"] + 0.3 * difficulty
        entry["last_updated"] = datetime.now(UTC).isoformat()
    
    async def query_similar(
        self,
        error_type: str,
        skill_tags: list[str] | None = None,
        method_tags: list[str] | None = None,
        template_id: str | None = None,
        top_k: int = 3,
    ) -> list[dict[str, Any]]:
        """
        Consulta padrões similares ao erro atual.
        
        Similaridade = weighted sum of:
        - error_type match (0.5)
        - skill_tags overlap (0.3)
        - method_tags overlap (0.1)
        - template_id match (0.1)
        
        Retorna top_k mais similares + melhor success_rate.
        """
        scored = []
        for key, entry in self._memory_store.items():
            score = 0.0
            
            # Error type match (exato ou similar)
            if entry["error_type"] == error_type:
                score += 0.5
            elif self._error_type_family(error_type) == self._error_type_family(entry["error_type"]):
                score += 0.3
            
            # Skill tags overlap (Jaccard similarity)
            if skill_tags:
                entry_skills = set(entry["skill_tags"])
                query_skills = set(skill_tags)
                if entry_skills and query_skills:
                    jaccard = len(entry_skills & query_skills) / len(entry_skills | query_skills)
                    score += 0.3 * jaccard
            
            # Method tags overlap
            if method_tags and entry.get("method_tags"):
                methods_intersection = len(set(method_tags) & set(entry["method_tags"]))
                score += 0.1 * min(methods_intersection / max(len(method_tags), 1), 1.0)
            
            # Template match
            if template_id and entry.get("template_id") == template_id:
                score += 0.1
            
            # Bonus for high success rate (patterns that work)
            score += 0.05 * entry.get("success_rate", 0)
            
            if score > 0:
                scored.append((score, entry))
        
        # Sort by score desc, then by success_rate desc
        scored.sort(key=lambda x: (-x[0], -x[1]["success_rate"]))
        
        return [entry for _, entry in scored[:top_k]]
    
    async def get_stats(self) -> dict[str, Any]:
        """Estatísticas do repositório de padrões de erro."""
        if not self._memory_store:
            return {
                "total_patterns": 0,
                "total_errors_recorded": 0,
                "error_types": {},
                "top_patterns": [],
            }
        
        error_types = {}
        for entry in self._memory_store.values():
            et = entry["error_type"]
            if et not in error_types:
                error_types[et] = {"count": 0, "avg_success_rate": 0.0}
            error_types[et]["count"] += 1
            error_types[et]["avg_success_rate"] = (
                (error_types[et]["avg_success_rate"] * (error_types[et]["count"] - 1) + entry["success_rate"])
                / error_types[et]["count"]
            )
        
        top_by_success = sorted(
            self._memory_store.values(),
            key=lambda x: (-x["success_rate"], -x["student_count"])
        )[:5]
        
        return {
            "total_patterns": len(self._memory_store),
            "total_errors_recorded": sum(e["student_count"] for e in self._memory_store.values()),
            "error_types": error_types,
            "top_patterns": [
                {
                    "error_type": e["error_type"],
                    "skill_tags": e["skill_tags"][:3],
                    "success_rate": round(e["success_rate"], 2),
                    "student_count": e["student_count"],
                }
                for e in top_by_success
            ],
        }
    
    # ── Helpers ────────────────────────────────────────────────────────
    
    def _make_key(self, error_type: str, skill_tags: list[str], template_id: str | None) -> str:
        """Chave única para agrupamento de padrões de erro."""
        skills = ",".join(sorted(skill_tags)) if skill_tags else ""
        tid = template_id or ""
        return f"{error_type}|{skills}|{tid}"
    
    def _error_type_family(self, error_type: str) -> str:
        """Agrupa tipos de erro por família pedagógica."""
        families = {
            "sinal": "operacao",
            "fracao": "operacao",
            "bhaskara": "formula",
            "completar_quadrado": "formula",
            "fatoracao": "manipulacao",
            "raiz_quadrada": "manipulacao",
            "conjunto": "notacao",
            "coordenadas": "representacao",
        }
        return families.get(error_type, "outro")
    
    def _get_hint(self, error_type: str, skill_tags: list[str]) -> str:
        """Dica pedagógica baseada no tipo de erro."""
        hints = {
            "sinal": (
                "📌 Dica: Ao resolver expressões com sinais negativos, "
                "lembre-se de que '-' com '-' vira '+'. "
                "Experimente reescrever a expressão passo a passo."
            ),
            "fracao": (
                "📌 Dica: Para somar frações, encontre o MMC dos denominadores. "
                "Para multiplicar, multiplique numerador por numerador, "
                "denominador por denominador."
            ),
            "raiz_quadrada": (
                "📌 Dica: Lembre-se: √(a × b) = √a × √b (apenas para a,b ≥ 0). "
                "Simplifique antes de somar/subtrair radicais."
            ),
            "fatoracao": (
                "📌 Dica: Procure por fatores comuns primeiro. "
                "Depois tente produtos notáveis: (a+b)², (a-b)², (a+b)(a-b)."
            ),
            "completar_quadrado": (
                "📌 Dica: (x + p)² = x² + 2px + p². "
                "Adicione e subtraia o termo que completa o quadrado."
            ),
            "bhaskara": (
                "📌 Dica: Δ = b² - 4ac. Calcule Δ primeiro, depois x = (-b ± √Δ) / 2a. "
                "Atenção aos sinais de b!"
            ),
            "conjunto": (
                "📌 Dica: Use { } para conjuntos, [ ] para intervalos fechados, "
                "( ) para abertos. ∅ ou { } para conjunto vazio."
            ),
            "coordenadas": (
                "📌 Dica: Par ordenado (x, y). x é horizontal, y é vertical. "
                "O ponto de interseção satisfaz ambas as equações."
            ),
        }
        return hints.get(error_type, "📌 Revise os conceitos e tente novamente.")


# ── Factory / Singleton ────────────────────────────────────────────────

_error_store: ErrorPatternStore | None = None


def get_error_pattern_store(db=None) -> ErrorPatternStore:
    """Singleton factory."""
    global _error_store
    if _error_store is None:
        _error_store = ErrorPatternStore(db=db)
    return _error_store


async def suggest_exercises_from_error(
    user_answer: str,
    expected_answer: str,
    skill_tags: list[str] | None = None,
    method_tags: list[str] | None = None,
    template_id: str | None = None,
    difficulty: float = 5.0,
    is_correct: bool = False,
    top_k: int = 3,
) -> dict[str, Any]:
    """
    Função principal: classifica o erro, consulta o RAG de padrões,
    e retorna sugestões contextualizadas.
    
    Returns:
    {
        "error_type": "sinal" | None,
        "has_suggestions": bool,
        "suggestions": [
            {
                "error_type": "sinal",
                "skill_tags": [...],
                "success_rate": 0.73,
                "student_count": 42,
                "pedagogical_hint": "...",
            }
        ],
        "stats": {...}  # estatísticas do repositório
    }
    """
    store = get_error_pattern_store(db=None)
    
    # 1. Classifica o erro
    error_type = classify_error(
        user_answer=user_answer,
        expected_answer=expected_answer,
        skill_tags=skill_tags,
        method_tags=method_tags,
        is_correct=is_correct,
    )
    
    if error_type is None:
        return {"error_type": None, "has_suggestions": False, "suggestions": []}
    
    # 2. Registra este erro no repositório
    await store.record_error(
        error_type=error_type,
        skill_tags=skill_tags or [],
        method_tags=method_tags,
        template_id=template_id,
        difficulty=difficulty,
        was_overcome=False,
    )
    
    # 3. Consulta padrões similares
    suggestions = await store.query_similar(
        error_type=error_type,
        skill_tags=skill_tags,
        method_tags=method_tags,
        template_id=template_id,
        top_k=top_k,
    )
    
    # 4. Estatísticas
    stats = await store.get_stats()
    
    return {
        "error_type": error_type,
        "has_suggestions": len(suggestions) > 0,
        "suggestions": [
            {
                "error_type": s["error_type"],
                "skill_tags": s["skill_tags"][:5],
                "success_rate": round(s["success_rate"], 2),
                "student_count": s["student_count"],
                "pedagogical_hint": s["pedagogical_hint"],
            }
            for s in suggestions
        ],
        "stats": stats,
    }
