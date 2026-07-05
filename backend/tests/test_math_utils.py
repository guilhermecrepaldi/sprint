"""Testes para math_utils.py — gerados via S1 (Ollama local)."""

import pytest
from utils.math_utils import mdc, mmc, simplificar_fracao, resolver_equacao_2o_grau


class TestMdc:
    def test_mdc_basic(self):
        assert mdc(12, 8) == 4
        assert mdc(18, 12) == 6
        assert mdc(7, 5) == 1

    def test_mdc_with_zero(self):
        assert mdc(0, 5) == 5
        assert mdc(12, 0) == 12
        assert mdc(0, 0) == 0

    def test_mdc_negative(self):
        assert mdc(-12, 8) == 4
        assert mdc(12, -8) == 4
        assert mdc(-12, -8) == 4


class TestMmc:
    def test_mmc_basic(self):
        assert mmc(12, 8) == 24
        assert mmc(6, 4) == 12
        assert mmc(7, 5) == 35

    def test_mmc_with_zero(self):
        assert mmc(0, 5) == 0
        assert mmc(12, 0) == 0


class TestSimplificarFracao:
    def test_simplificar_fracao_basic(self):
        assert simplificar_fracao(12, 8) == (3, 2)
        assert simplificar_fracao(18, 27) == (2, 3)

    def test_simplificar_fracao_irredutivel(self):
        assert simplificar_fracao(7, 5) == (7, 5)
        assert simplificar_fracao(3, 4) == (3, 4)

    def test_simplificar_fracao_denominador_zero(self):
        with pytest.raises(ValueError, match="Denominador não pode ser zero"):
            simplificar_fracao(5, 0)


class TestResolverEquacao2oGrau:
    def test_duas_raizes_reais(self):
        delta, x1, x2 = resolver_equacao_2o_grau(1, -3, 2)
        assert delta == 1
        assert x1 == 2.0
        assert x2 == 1.0

    def test_uma_raiz(self):
        delta, x1, x2 = resolver_equacao_2o_grau(1, -2, 1)
        assert delta == 0
        assert x1 == 1.0
        assert x2 == 1.0

    def test_sem_raiz_real(self):
        delta, x1, x2 = resolver_equacao_2o_grau(1, 0, 1)
        assert delta < 0
        assert x1 is None
        assert x2 is None

    def test_a_zero_raise_exception(self):
        with pytest.raises(ValueError, match="Coeficiente 'a' não pode ser zero"):
            resolver_equacao_2o_grau(0, 2, 3)
