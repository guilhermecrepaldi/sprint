"""
fsrs_scheduler.py — Free Spaced Repetition Scheduler

Implementação simplificada do FSRS-5 (Free Spaced Repetition Scheduler),
o algoritmo moderno usado no Anki 23.10+.

Inspirado por:
- Anki FSRS (original): https://github.com/open-spaced-repetition/py-fsrs
- SPRINT's own BKT engine: substitui a lógica simples de revisão
  por um scheduler matematicamente fundamentado

Diferenças do BKT existente:
- BKT: foca em MASTERY de uma skill (probabilidade de saber)
- FSRS: foca em WHEN revisar (timing ótimo baseado em memória)
- Juntos: BKT diz O QUE revisar, FSRS diz QUANDO revisar

Parâmetros FSRS-5:
- stability (S): quão estável é a memória (em dias)
- difficulty (D): quão difícil é o card (0-10)
- retrievability (R): probabilidade de lembrar hoje
- elapsed_days: dias desde última revisão
"""

import math
from dataclasses import dataclass, field
from datetime import UTC, datetime, timedelta
from enum import Enum
from typing import Any


class Rating(Enum):
    """Qualidade da recall, igual ao Anki."""
    AGAIN = 0    # Esqueceu completamente
    HARD = 1     # Lembrou com dificuldade
    GOOD = 2     # Lembrou normalmente
    EASY = 3     # Lembrou facilmente


@dataclass
class FSRSState:
    """Estado do FSRS para um card/skill."""
    stability: float = 0.0        # em dias
    difficulty: float = 5.0       # 0 (fácil) a 10 (difícil)
    due: datetime | None = None   # próxima revisão
    elapsed_days: float = 0.0
    review_count: int = 0
    lapses: int = 0               # vezes que esqueceu
    
    @property
    def retrievability(self) -> float:
        """Probabilidade de lembrar hoje (0.0 a 1.0)."""
        if self.stability <= 0 or self.elapsed_days <= 0:
            return 1.0
        return pow(1 + self.elapsed_days / self.stability, -1.0)
    
    @property
    def is_overdue(self) -> bool:
        """Se está atrasado para revisão."""
        if self.due is None:
            return False
        return datetime.now(UTC) > self.due
    
    @property
    def overdue_days(self) -> float:
        """Dias de atraso (0 se não atrasado)."""
        if not self.is_overdue or self.due is None:
            return 0.0
        return (datetime.now(UTC) - self.due).total_seconds() / 86400.0
    
    def to_dict(self) -> dict[str, Any]:
        return {
            "stability": round(self.stability, 2),
            "difficulty": round(self.difficulty, 2),
            "due": self.due.isoformat() if self.due else None,
            "elapsed_days": round(self.elapsed_days, 2),
            "retrievability": round(self.retrievability, 3),
            "review_count": self.review_count,
            "lapses": self.lapses,
            "is_overdue": self.is_overdue,
        }


class FSRSScheduler:
    """
    FSRS-5 simplificado.
    
    Parâmetros default (calibrados para material didático):
    - request_retention: 0.9 (90% — mais rigoroso que Anki default de 0.8)
    - max_interval: 365 dias
    - ease_factor: 2.5 (igual Anki default)
    """
    
    def __init__(
        self,
        request_retention: float = 0.9,
        max_interval: int = 365,
        ease_factor: float = 2.5,
    ):
        self.request_retention = request_retention
        self.max_interval = max_interval
        self.ease_factor = ease_factor
    
    def schedule(
        self,
        state: FSRSState,
        rating: Rating | None = None,
        accuracy: float | None = None,
    ) -> FSRSState:
        """
        Atualiza o estado FSRS baseado no rating da recall.
        
        Se accuracy for fornecido (0.0-1.0 do BKT), converte para Rating:
        - accuracy < 0.4 → AGAIN
        - accuracy < 0.7 → HARD
        - accuracy < 0.9 → GOOD
        - accuracy >= 0.9 → EASY
        
        Se nem rating nem accuracy forem fornecidos, assume GOOD.
        """
        if accuracy is not None:
            rating = self._accuracy_to_rating(accuracy)
        
        state.review_count += 1
        new_state = FSRSState(
            review_count=state.review_count,
            lapses=state.lapses,
        )
        
        if rating == Rating.AGAIN:
            # Esqueceu: reset parcial
            new_state.stability = self._initial_stability(state.difficulty) * 0.3
            new_state.difficulty = min(10.0, state.difficulty + 1.5)
            new_state.lapses = state.lapses + 1
        elif rating == Rating.HARD:
            # Difícil: estabilidade cresce pouco
            new_state.stability = state.stability * 1.2 if state.stability > 0 else self._initial_stability(state.difficulty)
            new_state.difficulty = min(10.0, state.difficulty + 0.8)
            new_state.lapses = state.lapses
        elif rating == Rating.GOOD:
            # Normal: crescimento padrão
            new_state.stability = state.stability * self.ease_factor if state.stability > 0 else self._initial_stability(state.difficulty)
            new_state.difficulty = state.difficulty
            new_state.lapses = state.lapses
        elif rating == Rating.EASY:
            # Fácil: crescimento acelerado
            new_state.stability = state.stability * (self.ease_factor * 1.3) if state.stability > 0 else self._initial_stability(state.difficulty) * 1.5
            new_state.difficulty = max(0.0, state.difficulty - 0.5)
            new_state.lapses = state.lapses
        
        # Próxima revisão: quando retrievability cair abaixo de request_retention
        interval = self._next_interval(new_state.stability)
        new_state.due = datetime.now(UTC) + timedelta(days=interval)
        new_state.elapsed_days = 0.0
        
        return new_state
    
    def get_review_queue(
        self,
        skill_states: dict[str, FSRSState],
        now: datetime | None = None,
    ) -> list[tuple[str, FSRSState, float]]:
        """
        Retorna lista de (skill_name, state, priority_score) ordenada
        por prioridade de revisão.
        
        Priority = retrievability gap: quanto mais abaixo do
        request_retention, maior a prioridade.
        
        Skills com retrievability > 0.95 (muito recentes) são excluídas.
        """
        if now is None:
            now = datetime.now(UTC)
        
        queue = []
        for skill, state in skill_states.items():
            # Só inclui se já foi revisado pelo menos uma vez
            if state.review_count == 0:
                continue
            
            # Calcula elapsed_days atual
            if state.due:
                state.elapsed_days = (now - state.due).total_seconds() / 86400.0
            
            r = state.retrievability
            
            # Só inclui se retrievability está abaixo do limiar
            if r > 0.95:
                continue
            
            # Priority: quanto menor a retrievability, maior a prioridade
            priority = max(0.0, self.request_retention - r)
            if state.is_overdue:
                priority += 0.2 * state.overdue_days  # bônus por atraso
            
            queue.append((skill, state, round(priority, 3)))
        
        # Ordena por prioridade decrescente
        queue.sort(key=lambda x: -x[2])
        return queue
    
    # ── Private ───────────────────────────────────────────────────────
    
    def _initial_stability(self, difficulty: float) -> float:
        """Estabilidade inicial baseada na dificuldade."""
        # Quanto mais difícil, menor a estabilidade inicial
        return max(0.5, 3.0 - difficulty * 0.2)
    
    def _next_interval(self, stability: float) -> int:
        """Intervalo até próxima revisão em dias."""
        interval = stability * (
            math.log(self.request_retention) / math.log(0.9)
        )
        return max(1, min(self.max_interval, round(interval)))
    
    def _accuracy_to_rating(self, accuracy: float) -> Rating:
        """Converte accuracy (0.0-1.0) para Rating."""
        if accuracy < 0.4:
            return Rating.AGAIN
        elif accuracy < 0.7:
            return Rating.HARD
        elif accuracy < 0.9:
            return Rating.GOOD
        else:
            return Rating.EASY


# ── Factory ────────────────────────────────────────────────────────────

_default_scheduler: FSRSScheduler | None = None


def get_scheduler(
    request_retention: float = 0.9,
    max_interval: int = 365,
) -> FSRSScheduler:
    """Singleton factory."""
    global _default_scheduler
    if _default_scheduler is None:
        _default_scheduler = FSRSScheduler(
            request_retention=request_retention,
            max_interval=max_interval,
        )
    return _default_scheduler
