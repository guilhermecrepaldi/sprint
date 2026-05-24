import re

import sympy
from sympy.parsing.sympy_parser import parse_expr, standard_transformations


TRANSFORMATIONS = standard_transformations


def _normalize(value: str | None) -> str:
    if value is None:
        return ""
    normalized = value.lower().strip()
    replacements = {
        "\\left": "",
        "\\right": "",
        "\\,": "",
        " ": "",
        "^": "**",
        "\\pm": "±",
    }
    for source, target in replacements.items():
        normalized = normalized.replace(source, target)
    normalized = re.sub(r"\\frac\{([^{}]+)\}\{([^{}]+)\}", r"(\1)/(\2)", normalized)
    normalized = normalized.replace("{", "(").replace("}", ")")
    return normalized


def _strip_variable_assignment(value: str) -> str:
    if "=" not in value:
        return value
    left, right = value.split("=", 1)
    if re.fullmatch(r"[a-z]", left):
        return right
    if re.fullmatch(r"[a-z]", right):
        return left
    return value


def _parse_expression(value: str):
    normalized = _strip_variable_assignment(_normalize(value))
    return parse_expr(normalized, transformations=TRANSFORMATIONS)


def _split_answer_set(value: str) -> set[str]:
    normalized = _normalize(value)
    normalized = _strip_variable_assignment(normalized)
    normalized = normalized.replace(";", ",")
    if "±" in normalized:
        positive = normalized.replace("±", "")
        negative = normalized.replace("±", "-")
        return {_normalize(positive), _normalize(negative)}
    return {_normalize(part) for part in normalized.split(",") if part}


def _equivalent_answer_sets(recognized: str, expected: str) -> bool:
    rec_set = _split_answer_set(recognized)
    exp_set = _split_answer_set(expected)
    if rec_set == exp_set:
        return True
    try:
        rec_exprs = {sympy.simplify(_parse_expression(item)) for item in rec_set}
        exp_exprs = {sympy.simplify(_parse_expression(item)) for item in exp_set}
        return rec_exprs == exp_exprs
    except Exception:
        return False


def validate_answer(recognized_latex: str | None, expected_latex: str) -> dict:
    if not recognized_latex:
        return {"is_correct": False, "error_type": "ocr_invalido"}

    is_correct = _normalize(recognized_latex) == _normalize(expected_latex)
    if not is_correct:
        is_correct = _equivalent_answer_sets(recognized_latex, expected_latex)

    if not is_correct:
        try:
            rec = _parse_expression(recognized_latex)
            exp = _parse_expression(expected_latex)
            is_correct = sympy.simplify(rec - exp) == 0
        except Exception:
            is_correct = False

    return {"is_correct": bool(is_correct), "error_type": None if is_correct else classify_error(recognized_latex, expected_latex)}


def classify_error(recognized: str, expected: str) -> str:
    rec = _normalize(recognized)
    exp = _normalize(expected)
    if "-" in exp and "-" not in rec:
        return "sinal"
    if "/" in exp or "\\frac" in expected:
        return "fracao"
    if "±" in exp and "±" not in rec:
        return "raiz_quadrada"
    if "^2" in expected or "**2" in exp:
        return "equacao_2_grau"
    return "desconhecido"
