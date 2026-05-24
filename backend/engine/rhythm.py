"""
Macro-ritmo: análise de padrões entre sessões.
Roda sob demanda (GET /api/student/{id}/rhythm).
"""
from statistics import mean


def get_session_recommendation(sessions: list[dict]) -> dict:
    """
    `sessions`: lista de dicts com keys:
        duration_ms: int
        accuracy: float       (0.0–1.0)
        started_at: datetime
    """
    if not sessions:
        return {
            "trend": "no_data",
            "suggested_duration_ms": 3600000,   # 1h padrão
            "best_hour": None,
            "message": "Complete sua primeira sessão para receber recomendações.",
        }

    # Tendência de acurácia (últimas 5 sessões)
    recent = sessions[-5:]
    accuracies = [s["accuracy"] for s in recent]
    if len(accuracies) >= 3:
        first_half = mean(accuracies[:len(accuracies) // 2])
        second_half = mean(accuracies[len(accuracies) // 2:])
        if second_half > first_half + 0.05:
            trend = "improving"
        elif second_half < first_half - 0.05:
            trend = "declining"
        else:
            trend = "stable"
    else:
        trend = "stable"

    # Duração sugerida: média das últimas sessões, cap 2h, min 20min
    avg_ms = mean(s["duration_ms"] for s in recent)
    if trend == "improving":
        suggested = min(7200000, int(avg_ms * 1.10))
    elif trend == "declining":
        suggested = max(1200000, int(avg_ms * 0.85))
    else:
        suggested = int(avg_ms)

    # Melhor horário: hora com maior accuracy média
    hour_acc: dict[int, list[float]] = {}
    for s in sessions:
        h = s["started_at"].hour
        hour_acc.setdefault(h, []).append(s["accuracy"])
    best_hour = max(hour_acc, key=lambda h: mean(hour_acc[h])) if hour_acc else None

    messages = {
        "improving": "Você está evoluindo. Tente uma sessão um pouco mais longa.",
        "declining": "Sessões mais curtas e focadas podem ajudar agora.",
        "stable":    "Ritmo consistente. Mantenha.",
    }

    return {
        "trend": trend,
        "suggested_duration_ms": suggested,
        "best_hour": best_hour,
        "message": messages[trend],
    }
