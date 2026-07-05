from engine.scoring import compute_score


class ScoringAgent:
    def compute(
        self,
        *,
        is_correct: bool,
        total_time_ms: int,
        hesitation_ms: int,
        difficulty: float,
        estimated_time_ms: int | None,
    ) -> int:
        return compute_score(
            is_correct=is_correct,
            total_time_ms=total_time_ms,
            hesitation_ms=hesitation_ms,
            difficulty=difficulty,
            estimated_time_ms=estimated_time_ms or 45000,
        )
