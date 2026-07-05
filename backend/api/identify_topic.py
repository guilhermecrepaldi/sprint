"""
POST /api/identify-topic
Recebe um crop de escrita manuscrita e retorna qual tópico da
árvore matemática o aluno está querendo estudar.

Usado pelo DashboardScreen para auto-selecionar a trilha
quando o aluno escreve uma equação no canvas de intenção.
"""

import json
import logging

import openai
from fastapi import APIRouter

from db import settings
from schemas.identify_topic import IdentifyTopicIn, IdentifyTopicOut

logger = logging.getLogger(__name__)
router = APIRouter()

# ── Mapeamento conceito → skill_tag ──────────────────────────────────────────

_CONCEPT_TO_SKILL: dict[str, str] = {
    "addition":                   "soma_subtracao",
    "subtraction":                "soma_subtracao",
    "soma":                       "soma_subtracao",
    "subtracao":                  "soma_subtracao",
    "multiplication":             "multiplicacao_divisao",
    "division":                   "multiplicacao_divisao",
    "fraction":                   "fracoes_decimais",
    "decimal":                    "fracoes_decimais",
    "percentage":                 "porcentagem_razao",
    "ratio":                      "porcentagem_razao",
    "power":                      "potenciacao_radiciacao",
    "root":                       "potenciacao_radiciacao",
    "exponent":                   "potenciacao_radiciacao",
    "linear_equation":            "equacoes_lineares",
    "linear equation":            "equacoes_lineares",
    "first_degree":               "equacoes_lineares",
    "system":                     "sistemas_equacoes",
    "factoring":                  "fatoracao_produtos_notaveis",
    "polynomial":                 "fatoracao_produtos_notaveis",
    "inequality":                 "inequacoes",
    "quadratic":                  "equacoes_quadraticas",
    "second_degree":              "equacoes_quadraticas",
    "linear_function":            "funcao_afim",
    "affine_function":            "funcao_afim",
    "quadratic_function":         "funcao_quadratica",
    "parabola":                   "funcao_quadratica",
    "exponential":                "funcao_exponencial",
    "logarithm":                  "funcao_logaritmica",
    "log":                        "funcao_logaritmica",
    "absolute_value":             "funcao_modular",
    "modulus":                    "funcao_modular",
    "plane_geometry":             "geometria_plana",
    "triangle":                   "geometria_plana",
    "circle":                     "geometria_plana",
    "solid_geometry":             "geometria_espacial",
    "volume":                     "geometria_espacial",
    "analytic_geometry":          "geometria_analitica",
    "coordinate":                 "geometria_analitica",
    "arithmetic_progression":     "progressoes_pa_pg",
    "geometric_progression":      "progressoes_pa_pg",
    "sequence":                   "progressoes_pa_pg",
    "combinatorics":              "combinatoria",
    "permutation":                "combinatoria",
    "combination":                "combinatoria",
    "probability":                "probabilidade",
    "trigonometry":               "trig_razoes",
    "sine":                       "trig_seno_cosseno_tangente",
    "cosine":                     "trig_seno_cosseno_tangente",
    "tangent":                    "trig_seno_cosseno_tangente",
    "trig_identity":              "trig_identidades",
    "trig_equation":              "trig_equacoes",
    "limit":                      "nocao_de_limite",
    "continuity":                 "continuidade",
    "derivative":                 "derivadas_basicas",
    "chain_rule":                 "derivadas_regra_cadeia",
    "product_rule":               "derivadas_produto_quociente",
    "quotient_rule":              "derivadas_produto_quociente",
    "integral":                   "integrais_indefinidas",
    "definite_integral":          "integrais_definidas",
    "area_under_curve":           "aplicacoes_integrais",
}

_SKILL_DISPLAY: dict[str, str] = {
    "soma_subtracao":              "Soma e Subtração",
    "multiplicacao_divisao":       "Multiplicação e Divisão",
    "fracoes_decimais":            "Frações e Decimais",
    "porcentagem_razao":           "Porcentagem e Razão",
    "potenciacao_radiciacao":      "Potenciação e Radiciação",
    "equacoes_lineares":           "Equações Lineares",
    "sistemas_equacoes":           "Sistemas de Equações",
    "fatoracao_produtos_notaveis": "Fatoração",
    "inequacoes":                  "Inequações",
    "equacoes_quadraticas":        "Equações Quadráticas",
    "funcao_afim":                 "Função Afim",
    "funcao_quadratica":           "Função Quadrática",
    "funcao_exponencial":          "Função Exponencial",
    "funcao_logaritmica":          "Função Logarítmica",
    "funcao_modular":              "Função Modular",
    "geometria_plana":             "Geometria Plana",
    "geometria_espacial":          "Geometria Espacial",
    "geometria_analitica":         "Geometria Analítica",
    "progressoes_pa_pg":           "PA e PG",
    "combinatoria":                "Combinatória",
    "probabilidade":               "Probabilidade",
    "trig_razoes":                 "Razões Trigonométricas",
    "trig_seno_cosseno_tangente":  "Sen, Cos e Tan",
    "trig_identidades":            "Identidades Trigonométricas",
    "trig_equacoes":               "Equações Trigonométricas",
    "nocao_de_limite":             "Noção de Limite",
    "continuidade":                "Continuidade",
    "derivadas_basicas":           "Derivadas",
    "derivadas_regra_cadeia":      "Regra da Cadeia",
    "derivadas_produto_quociente": "Produto e Quociente",
    "aplicacoes_derivadas":        "Aplicações de Derivadas",
    "integrais_indefinidas":       "Integrais Indefinidas",
    "integrais_definidas":         "Integrais Definidas",
    "aplicacoes_integrais":        "Aplicações de Integrais",
}

_FALLBACK = "equacoes_lineares"

# ── Lazy client ───────────────────────────────────────────────────────────────

_client: openai.OpenAI | None = None


def _get_client() -> openai.OpenAI:
    global _client
    if _client is None:
        _client = openai.OpenAI(api_key=settings.openai_api_key)
    return _client


# ── Endpoint ──────────────────────────────────────────────────────────────────

@router.post("/api/identify-topic", response_model=IdentifyTopicOut)
async def identify_topic(body: IdentifyTopicIn) -> IdentifyTopicOut:
    """
    Identifica o conceito matemático numa imagem de escrita manuscrita.
    Retorna o skill_tag correspondente da árvore de pré-requisitos.
    """
    # Fallback local para testes sem Anthropic
    if body.image_base64.startswith("latex:"):
        text = body.image_base64[6:].lower().strip()
        skill = _concept_to_skill(text)
        return IdentifyTopicOut(
            skill_tag=skill,
            display_name=_SKILL_DISPLAY.get(skill, skill),
            confidence=0.9,
        )

    try:
        concepts = list(_CONCEPT_TO_SKILL.keys())
        prompt = (
            "Analise esta imagem de escrita matemática manuscrita.\n"
            "Identifique o conceito matemático principal.\n"
            f"Responda com um dos conceitos desta lista:\n{', '.join(concepts)}\n\n"
            "Retorne JSON exato: {\"concept\": \"...\", \"confidence\": 0.0}\n"
            "confidence entre 0.0 e 1.0. Se não reconhecer, use 'linear equation' com confidence 0.3."
        )
        resp = _get_client().chat.completions.create(
            model=settings.openai_model,
            max_tokens=80,
            messages=[{
                "role": "user",
                "content": [
                    {
                        "type": "image_url",
                        "image_url": {
                            "url": f"data:image/png;base64,{body.image_base64}",
                            "detail": "low",
                        },
                    },
                    {"type": "text", "text": prompt},
                ],
            }],
        )
        data = json.loads(resp.choices[0].message.content)
        concept = data.get("concept", "linear equation").lower().strip()
        confidence = float(data.get("confidence", 0.5))
        skill = _concept_to_skill(concept)

        return IdentifyTopicOut(
            skill_tag=skill,
            display_name=_SKILL_DISPLAY.get(skill, skill),
            confidence=confidence,
        )

    except Exception as exc:
        logger.warning("identify_topic failed: %s", exc)
        return IdentifyTopicOut(
            skill_tag=_FALLBACK,
            display_name=_SKILL_DISPLAY[_FALLBACK],
            confidence=0.0,
        )


def _concept_to_skill(concept: str) -> str:
    """Mapeia texto livre para skill_tag. Tenta correspondência exata,
    depois substring, depois retorna fallback."""
    concept = concept.lower().replace("-", "_").replace(" ", "_")
    if concept in _CONCEPT_TO_SKILL:
        return _CONCEPT_TO_SKILL[concept]
    for key, skill in _CONCEPT_TO_SKILL.items():
        if key.replace(" ", "_") in concept or concept in key.replace(" ", "_"):
            return skill
    return _FALLBACK
