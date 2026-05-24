def compute_score(
    is_correct: bool,
    total_time_ms: int,
    hesitation_ms: int,
    difficulty: float,
    estimated_time_ms: int,
) -> int:
    if not is_correct:
        return 0

    safe_estimated_time = max(1, estimated_time_ms)
    time_ratio = total_time_ms / safe_estimated_time
    decay = 1 / (1 + 0.3 * max(0, time_ratio - 1))
    hesitation_penalty = 1 - min(0.3, hesitation_ms / 10000)
    difficulty_bonus = 1 + (difficulty - 1) * 0.1

    raw = 1000 * decay * hesitation_penalty * difficulty_bonus
    return int(min(1000, max(0, raw)))
