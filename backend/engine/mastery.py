import math

MASTERY_THRESHOLD = 0.90
MIN_EXERCISES = 100


def update_mastery(current: float, correct: bool) -> float:
    """Rendimento decrescente nos acertos. Regressão proporcional nos erros."""
    if correct:
        gain = 0.08 * (1.0 - current)
        return min(1.0, current + gain)
    else:
        loss = 0.06 * current
        return max(0.0, current - loss)


def apply_decay(mastery: float, days_since_last_practice: int) -> float:
    """
    Domínio decai sem prática:
    - <= 1 dia: sem decaimento
    - 3 dias: ~1.6%
    - 7 dias: ~2.9%
    - 30 dias: ~5.1%
    """
    if days_since_last_practice <= 1:
        return mastery
    decay = 0.015 * math.log(days_since_last_practice)
    return max(0.0, mastery - decay)
