"""
Testes para o FSRS Scheduler (fsrs_scheduler.py).

Verifica:
1. Schedule com diferentes ratings (AGAIN, HARD, GOOD, EASY)
2. Retrievability decay over time
3. Review queue ordering
4. Accuracy-to-rating conversion
"""

from datetime import UTC, datetime, timedelta

import pytest

from engine.fsrs_scheduler import (
    FSRSState,
    FSRSScheduler,
    Rating,
    get_scheduler,
)


class TestFSRSState:
    def test_initial_state(self):
        """Estado inicial tem stability=0, difficulty=5."""
        state = FSRSState()
        assert state.stability == 0.0
        assert state.difficulty == 5.0
        assert state.review_count == 0
        assert state.lapses == 0

    def test_retrievability_perfect_when_new(self):
        """Sem elapsed_days → retrievability = 1.0."""
        state = FSRSState(stability=10.0)
        assert state.retrievability == 1.0

    def test_retrievability_decays(self):
        """Com elapsed_days > 0 → retrievability < 1.0."""
        state = FSRSState(stability=10.0, elapsed_days=5.0)
        assert 0.0 < state.retrievability < 1.0

    def test_retrievability_zero_stability(self):
        """Stability 0 → retrievability = 1.0."""
        state = FSRSState(stability=0.0, elapsed_days=100.0)
        assert state.retrievability == 1.0

    def test_is_overdue(self):
        """Due no passado → is_overdue = True."""
        state = FSRSState(due=datetime.now(UTC) - timedelta(days=1))
        assert state.is_overdue

    def test_is_not_overdue(self):
        """Due no futuro → is_overdue = False."""
        state = FSRSState(due=datetime.now(UTC) + timedelta(days=1))
        assert not state.is_overdue

    def test_to_dict(self):
        """to_dict() serializa campos principais."""
        state = FSRSState(stability=15.0, difficulty=3.0, review_count=5)
        d = state.to_dict()
        assert d["stability"] == 15.0
        assert d["difficulty"] == 3.0
        assert d["review_count"] == 5


class TestFSRSScheduler:
    @pytest.fixture
    def scheduler(self):
        return FSRSScheduler(request_retention=0.9, max_interval=365)

    def test_schedule_again(self, scheduler):
        """AGAIN → estabilidade cai, dificuldade sobe, lapses incrementa."""
        state = FSRSState(stability=10.0, difficulty=5.0, review_count=5)
        new = scheduler.schedule(state, Rating.AGAIN)
        assert new.stability < state.stability  # estabilidade caiu
        assert new.difficulty > state.difficulty  # dificuldade subiu
        assert new.lapses == state.lapses + 1  # lapses incrementou

    def test_schedule_good(self, scheduler):
        """GOOD → estabilidade sobe, dificuldade mantém."""
        state = FSRSState(stability=10.0, difficulty=5.0, review_count=5)
        new = scheduler.schedule(state, Rating.GOOD)
        assert new.stability > state.stability  # estabilidade subiu
        assert new.difficulty == state.difficulty  # manteve
        assert new.lapses == state.lapses  # sem novo lapso

    def test_schedule_easy(self, scheduler):
        """EASY → estabilidade sobe mais, dificuldade cai."""
        state = FSRSState(stability=10.0, difficulty=5.0, review_count=5)
        new = scheduler.schedule(state, Rating.EASY)
        good_state = scheduler.schedule(state, Rating.GOOD)
        assert new.stability > good_state.stability  # EASY > GOOD
        assert new.difficulty < state.difficulty  # dificuldade caiu

    def test_first_review(self, scheduler):
        """Primeira review (stability=0) → estabilidade inicial positiva."""
        state = FSRSState(review_count=0)
        new = scheduler.schedule(state, Rating.GOOD)
        assert new.stability > 0
        assert new.review_count == 1

    def test_schedule_with_accuracy(self, scheduler):
        """Accuracy 0.9 → converte para GOOD."""
        state = FSRSState(stability=10.0, difficulty=5.0)
        new = scheduler.schedule(state, accuracy=0.9)
        assert new.stability > state.stability

    def test_schedule_with_low_accuracy(self, scheduler):
        """Accuracy 0.3 → converte para AGAIN."""
        state = FSRSState(stability=10.0, difficulty=5.0, lapses=0)
        new = scheduler.schedule(state, accuracy=0.3)
        assert new.lapses == 1

    def test_next_interval_respects_max(self, scheduler):
        """Intervalo nunca ultrapassa max_interval."""
        state = FSRSState(stability=1000.0, difficulty=1.0, review_count=20)
        new = scheduler.schedule(state, Rating.EASY)
        assert new.due is not None
        interval = (new.due - datetime.now(UTC)).days
        assert interval <= 365

    def test_get_review_queue_empty(self, scheduler):
        """Sem skills → fila vazia."""
        queue = scheduler.get_review_queue({}, datetime.now(UTC))
        assert queue == []

    def test_get_review_queue_orders_by_priority(self, scheduler):
        """Skills com retrievability baixa → maior prioridade."""
        now = datetime.now(UTC)
        states = {
            "ruim": FSRSState(stability=2.0, elapsed_days=5.0, review_count=5),
            "boa": FSRSState(stability=50.0, elapsed_days=1.0, review_count=5),
        }
        queue = scheduler.get_review_queue(states, now)
        assert len(queue) >= 1
        # A skill "ruim" deve ter maior prioridade
        assert queue[0][0] == "ruim"

    def test_new_skills_excluded_from_queue(self, scheduler):
        """Skills sem review (review_count=0) são excluídas."""
        now = datetime.now(UTC)
        states = {
            "nova": FSRSState(review_count=0),
            "antiga": FSRSState(stability=5.0, elapsed_days=10.0, review_count=5),
        }
        queue = scheduler.get_review_queue(states, now)
        skills = [s for s, _, _ in queue]
        assert "nova" not in skills
        assert "antiga" in skills


class TestIntegration:
    """Testes de integração: scheduler + state working together."""

    def test_lifecycle_improving_student(self):
        """Aluno que melhora: estabilidade cresce, dificuldade cai."""
        scheduler = FSRSScheduler()
        state = FSRSState()

        # 5 sessões de estudo com accuracy crescente
        accuracies = [0.5, 0.7, 0.8, 0.9, 0.95]
        for acc in accuracies:
            state.elapsed_days = 3.0  # estuda a cada 3 dias
            state = scheduler.schedule(state, accuracy=acc)

        assert state.stability > 1.0  # memória estável
        assert state.review_count == 5
        # Retrievability deve estar alta
        assert state.retrievability > 0.8

    def test_lifecycle_struggling_student(self):
        """Aluno com dificuldade: lapses altos, dificuldade sobe."""
        scheduler = FSRSScheduler()
        state = FSRSState()

        for _ in range(5):
            state.elapsed_days = 1.0
            state = scheduler.schedule(state, accuracy=0.3)  # erros

        assert state.lapses >= 3  # muitos lapses
        assert state.difficulty > 6.0  # dificuldade cresceu

    def test_scheduler_singleton(self):
        """get_scheduler() retorna mesma instância."""
        s1 = get_scheduler()
        s2 = get_scheduler()
        assert s1 is s2
