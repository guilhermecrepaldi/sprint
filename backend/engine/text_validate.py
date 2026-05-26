"""
Valida resposta reconhecida localmente (ML Kit / iink) contra a resposta esperada.

Dois níveis:
  1. Comparação de string normalizada (rápido, sem dependências)
  2. Equivalência algébrica via sympy (para "x = 5" vs "5 = x", "6/2" vs "3", etc.)

Chamado no submit quando o campo tem `recognized_text` — sem chamada ao Claude.
"""
import re


def text_matches(recognized: str, expected: str) -> bool:
    """Return True if recognized answer equals expected answer."""
    r = _normalize(recognized)
    e = _normalize(expected)

    if r == e:
        return True

    # Try sympy for algebraic equivalence ("6/2" == "3", "2*x" == "x*2", etc.)
    try:
        import sympy
        # Strip "x = " prefix so sympy compares the RHS value
        r_expr = _strip_variable_prefix(r)
        e_expr = _strip_variable_prefix(e)
        diff = sympy.simplify(sympy.sympify(r_expr) - sympy.sympify(e_expr))
        return diff == 0
    except Exception:
        pass

    return False


def _normalize(text: str) -> str:
    text = text.strip().lower()
    # Remove whitespace
    text = re.sub(r"\s+", "", text)
    # Unify operators
    text = text.replace("×", "*").replace("÷", "/")
    text = text.replace("−", "-").replace("–", "-")
    # "1,5" → "1.5" (comma as decimal separator in pt-BR)
    text = text.replace(",", ".")
    return text


def _strip_variable_prefix(expr: str) -> str:
    """Remove 'x=', 'y=', etc. so sympy evaluates the numeric/algebraic value."""
    match = re.match(r"^[a-z]=(.+)$", expr)
    return match.group(1) if match else expr
