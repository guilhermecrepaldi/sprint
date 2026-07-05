"""
math_utils.py — Funções matemáticas utilitárias para o SPRINT.

Extraído de engine/correction.py para centralizar lógica repetida.
Gerado via S1 (Ollama local - qwen2.5-coder:7b) em 11s.
"""

from typing import Optional, Tuple


def mdc(a: int, b: int) -> int:
    """Calcula o máximo divisor comum (MDC) de a e b usando o algoritmo euclidiano."""
    a, b = abs(a), abs(b)
    while b != 0:
        a, b = b, a % b
    return a


def mmc(a: int, b: int) -> int:
    """Calcula o mínimo múltiplo comum (MMC) de a e b."""
    if a == 0 or b == 0:
        return 0
    return abs(a * b) // mdc(a, b)


def simplificar_fracao(numerador: int, denominador: int) -> Tuple[int, int]:
    """Simplifica a fração numerador/denominador."""
    if denominador == 0:
        raise ValueError("Denominador não pode ser zero")
    divisor = mdc(numerador, denominador)
    return numerador // divisor, denominador // divisor


def resolver_equacao_2o_grau(
    a: float, b: float, c: float
) -> Tuple[float, Optional[float], Optional[float]]:
    """Resolve ax² + bx + c = 0. Retorna (delta, x1, x2). Raízes complexas → None."""
    if a == 0:
        raise ValueError("Coeficiente 'a' não pode ser zero em uma equação do 2º grau")
    delta = b**2 - 4 * a * c
    if delta >= 0:
        x1 = (-b + delta**0.5) / (2 * a)
        x2 = (-b - delta**0.5) / (2 * a)
        return delta, x1, x2
    else:
        return delta, None, None
