"""
Gerador de exercícios via LLM.

O SKILL_DICT é o "dicionário": define o tema, faixa de dificuldade e restrições
de cada skill. O modelo gera os exercícios em JSON.

Uso:
    python scripts/generate_exercises.py --all --count 15
    python scripts/generate_exercises.py --skills equacoes_lineares,funcao_quadratica --count 20
    python scripts/generate_exercises.py --all --count 10 --dry-run
    python scripts/generate_exercises.py --all --count 148 --provider openai
"""
import argparse
import asyncio
import json
import os
import re
import sys
import time
import urllib.error
import urllib.request
from pathlib import Path
from typing import Protocol

import anthropic
from sqlalchemy import select

sys.path.append(str(Path(__file__).resolve().parents[1]))

from db import AsyncSessionLocal, settings
from models.exercise import Exercise

OPENAI_RESPONSES_URL = "https://api.openai.com/v1/responses"
GEMINI_GENERATE_URL = "https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent"
OLLAMA_GENERATE_URL = "{base_url}/api/generate"
DEFAULT_ANTHROPIC_MODEL = "claude-haiku-4-5-20251001"
DEFAULT_OPENAI_MODEL = "gpt-4o-mini"
DEFAULT_GEMINI_MODEL = "gemini-2.5-flash"
DEFAULT_OLLAMA_MODEL = "qwen2.5:14b"
DEFAULT_OLLAMA_BASE_URL = "http://localhost:11434"


class ExerciseGenerator(Protocol):
    def generate(self, skill: str, count: int) -> list[dict]:
        ...

# ── Dicionário de skills ─────────────────────────────────────────────────────
# Cada skill tem: topic, difficulty_range (min, max), constraints
# difficulty: 1.0=básico fundamental, 5.0=ensino médio avançado, 8.0+=universitário/olimpíadas

SKILL_DICT: dict[str, dict] = {
    "soma_subtracao": {
        "topic": "Adição e subtração de números inteiros e decimais",
        "difficulty_range": (1.0, 2.5),
        "constraints": (
            "Números inteiros e decimais simples. "
            "Pode incluir expressões com mais de dois termos. "
            "Resposta numérica."
        ),
    },
    "multiplicacao_divisao": {
        "topic": "Multiplicação e divisão de inteiros e decimais",
        "difficulty_range": (1.5, 3.0),
        "constraints": (
            "Inclua casos com decimais, divisões exatas e não-exatas. "
            "Resposta numérica."
        ),
    },
    "fracoes_decimais": {
        "topic": "Frações e números decimais: operações e simplificações",
        "difficulty_range": (1.8, 3.5),
        "constraints": (
            "Inclua: somar/subtrair frações, multiplicar/dividir frações, "
            "converter decimal↔fração, simplificar. "
            "Use LaTeX para frações (\\frac{a}{b}). "
            "Resposta como fração simplificada ou decimal."
        ),
    },
    "porcentagem_razao": {
        "topic": "Porcentagem, razão e proporção em contextos práticos",
        "difficulty_range": (2.0, 4.0),
        "constraints": (
            "Contextos do dia-a-dia: desconto, aumento, juros simples. "
            "Inclua regra de três. "
            "Resposta numérica ou percentual."
        ),
    },
    "potenciacao_radiciacao": {
        "topic": "Potenciação e radiciação: propriedades e cálculos",
        "difficulty_range": (1.8, 4.0),
        "constraints": (
            "Inclua: potências negativas, raiz quadrada/cúbica, "
            "propriedades de potência (a^m * a^n, (a^m)^n). "
            "Use LaTeX (2^{10}, \\sqrt{144}, \\sqrt[3]{27}). "
            "Resposta numérica ou em forma de potência simplificada."
        ),
    },
    "equacoes_lineares": {
        "topic": "Equações de 1º grau em uma incógnita",
        "difficulty_range": (1.8, 3.5),
        "constraints": (
            "Use x como incógnita. Inclua: equações simples, com parênteses, "
            "com frações. LaTeX para frações. "
            "Resposta: 'x = N'."
        ),
    },
    "sistemas_equacoes": {
        "topic": "Sistemas de equações lineares 2×2",
        "difficulty_range": (2.8, 4.5),
        "constraints": (
            "Dois métodos: substituição e adição. "
            "Coeficientes inteiros pequenos. "
            "Resposta: 'x = A, y = B'."
        ),
    },
    "fatoracao_produtos_notaveis": {
        "topic": "Fatoração algébrica e produtos notáveis",
        "difficulty_range": (2.5, 4.5),
        "constraints": (
            "Inclua: (a+b)², (a-b)², (a+b)(a-b), fatoração por agrupamento, "
            "fator comum, trinômio quadrado perfeito. "
            "Use LaTeX (x^2, x^3). "
            "Resposta em forma fatorada ou expandida."
        ),
    },
    "inequacoes": {
        "topic": "Inequações de 1º grau",
        "difficulty_range": (2.2, 4.0),
        "constraints": (
            "Inclua: inverter sinal ao multiplicar/dividir por negativo, "
            "notação de intervalo. "
            "Resposta: 'x > N', 'x \\leq N', etc."
        ),
    },
    "equacoes_quadraticas": {
        "topic": "Equações de 2º grau: Bhaskara, completar quadrado, fatoração",
        "difficulty_range": (2.4, 5.0),
        "constraints": (
            "Inclua: discriminante positivo/zero/negativo, "
            "produto e soma das raízes (Vieta). "
            "Resposta: 'x = A ou x = B', '\\pm N', ou 'sem raízes reais'."
        ),
    },
    "funcao_afim": {
        "topic": "Função afim (linear): gráfico, raiz, coeficientes, aplicações",
        "difficulty_range": (2.5, 4.0),
        "constraints": (
            "Inclua: calcular f(x), encontrar raiz, "
            "determinar f(x) dados dois pontos, interpretar gráfico. "
            "Resposta numérica ou 'x = N'."
        ),
    },
    "funcao_quadratica": {
        "topic": "Função quadrática: vértice, raízes, gráfico, sinal",
        "difficulty_range": (3.0, 5.5),
        "constraints": (
            "Inclua: coordenadas do vértice, valor máximo/mínimo, "
            "intervalo onde a função é positiva/negativa. "
            "Use LaTeX. Resposta como par ordenado ou numérica."
        ),
    },
    "funcao_exponencial": {
        "topic": "Função exponencial: propriedades e equações exponenciais",
        "difficulty_range": (3.0, 5.5),
        "constraints": (
            "Inclua: resolver a^x = b com mesma base, "
            "crescimento/decrescimento, aplicação (juros compostos). "
            "Resposta: 'x = N'."
        ),
    },
    "funcao_logaritmica": {
        "topic": "Função logarítmica: propriedades, mudança de base, equações",
        "difficulty_range": (3.2, 6.0),
        "constraints": (
            "Inclua: log na base 2, 3, 10, e; "
            "propriedades (log(ab), log(a/b), log(a^n)); "
            "equações logarítmicas. "
            "Use LaTeX (\\log_2 8, \\ln x). Resposta numérica ou 'x = N'."
        ),
    },
    "funcao_modular": {
        "topic": "Função modular: equações e inequações com valor absoluto",
        "difficulty_range": (3.5, 5.5),
        "constraints": (
            "Inclua: |ax + b| = c, |f(x)| > k, gráfico de |x|. "
            "Use LaTeX (|x - 3| = 5). "
            "Resposta: valor(es) de x ou intervalo."
        ),
    },
    "geometria_plana": {
        "topic": "Geometria plana: áreas, perímetros, Pitágoras, círculo",
        "difficulty_range": (2.0, 4.5),
        "constraints": (
            "Inclua: triângulo, retângulo, paralelogramo, trapézio, círculo. "
            "Use π ≈ 3,14 quando necessário. "
            "Resposta numérica com unidade (cm², m)."
        ),
    },
    "geometria_espacial": {
        "topic": "Geometria espacial: volume e área de sólidos",
        "difficulty_range": (3.0, 5.5),
        "constraints": (
            "Inclua: cubo, paralelepípedo, cilindro, cone, esfera, pirâmide. "
            "Use π ≈ 3,14. Resposta numérica com unidade."
        ),
    },
    "geometria_analitica": {
        "topic": "Geometria analítica: distância, ponto médio, equação da reta",
        "difficulty_range": (3.5, 5.5),
        "constraints": (
            "Inclua: distância entre pontos, ponto médio, "
            "equação geral e reduzida da reta, coeficiente angular, "
            "distância ponto-reta. Resposta numérica ou equação."
        ),
    },
    "progressoes_pa_pg": {
        "topic": "Progressões Aritmética (PA) e Geométrica (PG)",
        "difficulty_range": (3.0, 5.0),
        "constraints": (
            "Inclua: termo geral, soma dos termos, interpolação, "
            "identificar razão/ratio. Varie entre PA e PG. "
            "Resposta numérica."
        ),
    },
    "combinatoria": {
        "topic": "Análise combinatória: arranjo, permutação, combinação",
        "difficulty_range": (3.5, 6.0),
        "constraints": (
            "Inclua: fatorial, C(n,k), P(n), A(n,k). "
            "Contextos práticos (senhas, comissões, anagramas). "
            "Resposta numérica inteira."
        ),
    },
    "probabilidade": {
        "topic": "Probabilidade clássica: eventos, complemento, condicional",
        "difficulty_range": (3.0, 5.5),
        "constraints": (
            "Inclua: espaço amostral, eventos independentes, "
            "probabilidade de A∩B, A∪B, complementar. "
            "Resposta como fração ou decimal."
        ),
    },
    "trig_razoes": {
        "topic": "Trigonometria no triângulo retângulo: seno, cosseno, tangente",
        "difficulty_range": (3.0, 5.0),
        "constraints": (
            "Inclua: calcular razão dado um ângulo e um lado, "
            "calcular lado desconhecido. "
            "Use ângulos notáveis (30°, 45°, 60°). "
            "Resposta numérica ou em forma de raiz."
        ),
    },
    "trig_seno_cosseno_tangente": {
        "topic": "Seno, cosseno e tangente no ciclo trigonométrico",
        "difficulty_range": (3.5, 5.5),
        "constraints": (
            "Inclua: valores em radianos e graus, "
            "sinal em cada quadrante, arco complementar/suplementar. "
            "Use LaTeX (\\sin, \\cos, \\tan, \\frac{\\pi}{4}). "
            "Resposta numérica ou em forma exata."
        ),
    },
    "trig_identidades": {
        "topic": "Identidades trigonométricas fundamentais e adição de arcos",
        "difficulty_range": (4.0, 6.5),
        "constraints": (
            "Inclua: sen²+cos²=1, tan=sen/cos, "
            "fórmulas de adição (sen(a+b), cos(a+b)). "
            "Peça simplificação ou verificação. "
            "Use LaTeX. Resposta: forma simplificada."
        ),
    },
    "trig_equacoes": {
        "topic": "Equações trigonométricas",
        "difficulty_range": (4.5, 7.0),
        "constraints": (
            "Inclua: sen(x)=k, cos(x)=k, 2cos²(x)-1=0. "
            "Intervalo [0, 2π]. "
            "Use LaTeX. Resposta: valor(es) de x em radianos."
        ),
    },
    "nocao_de_limite": {
        "topic": "Limite de funções: conceito e cálculo direto",
        "difficulty_range": (4.5, 6.5),
        "constraints": (
            "Inclua: limite por substituição, formas indeterminadas 0/0 "
            "resolvidas por fatoração, limite no infinito. "
            "Use LaTeX (\\lim_{x \\to a}). Resposta numérica ou ±∞."
        ),
    },
    "continuidade": {
        "topic": "Continuidade de funções: verificação e pontos de descontinuidade",
        "difficulty_range": (5.0, 7.0),
        "constraints": (
            "Inclua: verificar se f é contínua em x=a, "
            "encontrar valor de parâmetro que torna f contínua. "
            "Resposta: 'contínua' / 'descontínua em x=N' / valor do parâmetro."
        ),
    },
    "derivadas_basicas": {
        "topic": "Derivadas: regras básicas (potência, constante, soma/subtração)",
        "difficulty_range": (5.0, 7.0),
        "constraints": (
            "Inclua: d/dx(x^n), derivada de constante, "
            "soma e diferença. Não inclua regra do produto/quociente/cadeia aqui. "
            "Use LaTeX (f'(x), \\frac{d}{dx}). "
            "Resposta: expressão da derivada."
        ),
    },
    "derivadas_regra_cadeia": {
        "topic": "Derivadas: regra da cadeia",
        "difficulty_range": (5.5, 7.5),
        "constraints": (
            "Inclua: derivada de f(g(x)), composições com polinômios, "
            "exponencial, logaritmo. "
            "Use LaTeX. Resposta: expressão da derivada."
        ),
    },
    "derivadas_produto_quociente": {
        "topic": "Derivadas: regra do produto e do quociente",
        "difficulty_range": (5.5, 7.5),
        "constraints": (
            "Inclua: (uv)' = u'v + uv', (u/v)' = (u'v - uv')/v². "
            "Funções polinomiais e trigonométricas. "
            "Use LaTeX. Resposta: expressão da derivada simplificada."
        ),
    },
    "aplicacoes_derivadas": {
        "topic": "Aplicações de derivadas: máximos, mínimos, taxas de variação",
        "difficulty_range": (6.0, 8.0),
        "constraints": (
            "Inclua: pontos críticos, segunda derivada (côncavo/convexo), "
            "problemas de otimização contextualizados. "
            "Resposta: valor de x e o extremo, ou valor otimizado."
        ),
    },
    "integrais_indefinidas": {
        "topic": "Integrais indefinidas: regras básicas e por substituição",
        "difficulty_range": (6.0, 8.0),
        "constraints": (
            "Inclua: ∫x^n dx, ∫e^x dx, ∫cos(x) dx, substituição simples. "
            "Use LaTeX (\\int, dx, + C). "
            "Resposta: antiderivada + C."
        ),
    },
    "integrais_definidas": {
        "topic": "Integrais definidas: Teorema Fundamental do Cálculo",
        "difficulty_range": (6.5, 8.5),
        "constraints": (
            "Inclua: calcular ∫_a^b f(x)dx com limites numéricos. "
            "Use LaTeX (\\int_0^2). "
            "Resposta: valor numérico."
        ),
    },
    "aplicacoes_integrais": {
        "topic": "Aplicações de integrais: área entre curvas, volume de revolução",
        "difficulty_range": (7.0, 9.0),
        "constraints": (
            "Inclua: área entre f(x) e g(x), volume pelo método dos discos. "
            "Problemas contextualizados. "
            "Resposta: valor numérico com unidade."
        ),
    },
}

# ── Prompt template ──────────────────────────────────────────────────────────

SYSTEM_PROMPT = (
    "Você é um professor de matemática especializado em preparar concursandos brasileiros. "
    "Gera exercícios corretos, variados e com respostas exatas. "
    "Use LaTeX somente dentro de contexto matemático. "
    "Retorne SOMENTE JSON válido, sem texto adicional."
)

USER_TEMPLATE = """\
Gere {count} exercícios de matemática sobre o tema: {topic}

Dificuldade: de {min_diff} a {max_diff} (escala 1.0 a 10.0).
Distribua as dificuldades de forma uniforme dentro da faixa.

Restrições: {constraints}

Retorne um JSON array com exatamente este formato:
[
  {{
    "statement": "enunciado em português, LaTeX quando necessário",
    "expected_answer": "resposta correta, LaTeX quando necessário",
    "difficulty": 2.5
  }}
]

Regras:
- Cada exercício deve ser único e testável.
- O campo difficulty deve ser um número entre {min_diff} e {max_diff}.
- Não inclua explicações ou texto fora do JSON.
- Garanta que expected_answer está correto matematicamente.
"""

# ── Extrator de JSON da resposta ─────────────────────────────────────────────

def extract_json(text: str) -> list[dict]:
    """Remove markdown code fences se presentes e faz parse do JSON."""
    text = text.strip()
    # Remove ```json ... ``` ou ``` ... ```
    text = re.sub(r"^```(?:json)?\s*", "", text)
    text = re.sub(r"\s*```$", "", text)
    return json.loads(text)

# ── Geração via provedores ───────────────────────────────────────────────────

def build_prompt(skill: str, count: int) -> tuple[str, float, float]:
    params = SKILL_DICT[skill]
    min_d, max_d = params["difficulty_range"]
    prompt = USER_TEMPLATE.format(
        count=count,
        topic=params["topic"],
        min_diff=min_d,
        max_diff=max_d,
        constraints=params["constraints"],
    )
    return prompt, min_d, max_d


def normalize_items(items: list[dict], min_d: float, max_d: float) -> list[dict]:
    result = []
    for item in items:
        difficulty = float(item.get("difficulty", (min_d + max_d) / 2))
        difficulty = max(min_d, min(max_d, difficulty))  # clamp
        difficulty = max(1.0, min(10.0, difficulty))      # hard limits do DB
        result.append({
            "statement": str(item["statement"]).strip(),
            "expected_answer": str(item["expected_answer"]).strip(),
            "difficulty": round(difficulty, 1),
        })
    return result


def coerce_items(parsed: object) -> list[dict]:
    if isinstance(parsed, list):
        return parsed
    if isinstance(parsed, dict):
        if isinstance(parsed.get("exercises"), list):
            return parsed["exercises"]
        if {"statement", "expected_answer", "difficulty"}.issubset(parsed):
            return [parsed]
    raise ValueError("JSON gerado nao contem lista de exercicios valida")


class AnthropicExerciseGenerator:
    def __init__(self, api_key: str, model: str = DEFAULT_ANTHROPIC_MODEL) -> None:
        self._client = anthropic.Anthropic(api_key=api_key)
        self._model = model

    def generate(self, skill: str, count: int) -> list[dict]:
        prompt, min_d, max_d = build_prompt(skill, count)
        response = self._client.messages.create(
            model=self._model,
            max_tokens=4096,
            system=SYSTEM_PROMPT,
            messages=[{"role": "user", "content": prompt}],
        )

        raw = response.content[0].text
        return normalize_items(extract_json(raw), min_d, max_d)


class OpenAIExerciseGenerator:
    def __init__(self, api_key: str, model: str = DEFAULT_OPENAI_MODEL) -> None:
        self._api_key = api_key
        self._model = model

    def generate(self, skill: str, count: int) -> list[dict]:
        prompt, min_d, max_d = build_prompt(skill, count)
        payload = {
            "model": self._model,
            "input": [
                {"role": "system", "content": SYSTEM_PROMPT},
                {"role": "user", "content": prompt},
            ],
            "max_output_tokens": 12000,
            "text": {
                "format": {
                    "type": "json_schema",
                    "name": "exercise_batch",
                    "strict": True,
                    "schema": {
                        "type": "object",
                        "additionalProperties": False,
                        "required": ["exercises"],
                        "properties": {
                            "exercises": {
                                "type": "array",
                                "minItems": count,
                                "maxItems": count,
                                "items": {
                                    "type": "object",
                                    "additionalProperties": False,
                                    "required": [
                                        "statement",
                                        "expected_answer",
                                        "difficulty",
                                    ],
                                    "properties": {
                                        "statement": {"type": "string"},
                                        "expected_answer": {"type": "string"},
                                        "difficulty": {"type": "number"},
                                    },
                                },
                            }
                        },
                    },
                }
            },
        }
        request = urllib.request.Request(
            OPENAI_RESPONSES_URL,
            data=json.dumps(payload).encode("utf-8"),
            headers={
                "Authorization": f"Bearer {self._api_key}",
                "Content-Type": "application/json",
            },
            method="POST",
        )
        try:
            with urllib.request.urlopen(request, timeout=120) as response:
                data = json.loads(response.read().decode("utf-8"))
        except urllib.error.HTTPError as e:
            body = e.read().decode("utf-8", errors="replace")
            raise RuntimeError(f"OpenAI HTTP {e.code}: {body}") from e

        raw = data.get("output_text") or _extract_openai_output_text(data)
        parsed = extract_json(raw)
        items = coerce_items(parsed)
        return normalize_items(items, min_d, max_d)


def _extract_openai_output_text(data: dict) -> str:
    for output in data.get("output", []):
        if output.get("type") != "message":
            continue
        for content in output.get("content", []):
            if "text" in content:
                return content["text"]
    raise ValueError("Resposta OpenAI sem output_text")


class GeminiExerciseGenerator:
    def __init__(self, api_key: str, model: str = DEFAULT_GEMINI_MODEL) -> None:
        self._api_key = api_key
        self._model = model

    def generate(self, skill: str, count: int) -> list[dict]:
        prompt, min_d, max_d = build_prompt(skill, count)
        payload = {
            "contents": [{
                "role": "user",
                "parts": [{"text": f"{SYSTEM_PROMPT}\n\n{prompt}"}],
            }],
            "generationConfig": {
                "responseMimeType": "application/json",
                "responseJsonSchema": {
                    "type": "object",
                    "required": ["exercises"],
                    "properties": {
                        "exercises": {
                            "type": "array",
                            "minItems": count,
                            "maxItems": count,
                            "items": {
                                "type": "object",
                                "required": [
                                    "statement",
                                    "expected_answer",
                                    "difficulty",
                                ],
                                "properties": {
                                    "statement": {"type": "string"},
                                    "expected_answer": {"type": "string"},
                                    "difficulty": {"type": "number"},
                                },
                            },
                        }
                    },
                },
                "maxOutputTokens": 12000,
            },
        }
        url = GEMINI_GENERATE_URL.format(model=self._model)
        request = urllib.request.Request(
            url,
            data=json.dumps(payload).encode("utf-8"),
            headers={
                "x-goog-api-key": self._api_key,
                "Content-Type": "application/json",
            },
            method="POST",
        )
        try:
            with urllib.request.urlopen(request, timeout=120) as response:
                data = json.loads(response.read().decode("utf-8"))
        except urllib.error.HTTPError as e:
            body = e.read().decode("utf-8", errors="replace")
            raise RuntimeError(f"Gemini HTTP {e.code}: {body}") from e

        raw = _extract_gemini_output_text(data)
        parsed = extract_json(raw)
        items = coerce_items(parsed)
        return normalize_items(items, min_d, max_d)


def _extract_gemini_output_text(data: dict) -> str:
    candidates = data.get("candidates") or []
    for candidate in candidates:
        content = candidate.get("content") or {}
        for part in content.get("parts") or []:
            if "text" in part:
                return part["text"]
    raise ValueError("Resposta Gemini sem texto")


class OllamaExerciseGenerator:
    def __init__(
        self,
        model: str = DEFAULT_OLLAMA_MODEL,
        base_url: str = DEFAULT_OLLAMA_BASE_URL,
    ) -> None:
        self._model = model
        self._base_url = base_url.rstrip("/")

    def generate(self, skill: str, count: int) -> list[dict]:
        prompt, min_d, max_d = build_prompt(skill, count)
        payload = {
            "model": self._model,
            "system": SYSTEM_PROMPT,
            "prompt": prompt,
            "stream": False,
            "format": "json",
            "options": {
                "temperature": 0.4,
                "num_predict": 12000,
            },
        }
        request = urllib.request.Request(
            OLLAMA_GENERATE_URL.format(base_url=self._base_url),
            data=json.dumps(payload).encode("utf-8"),
            headers={"Content-Type": "application/json"},
            method="POST",
        )
        try:
            with urllib.request.urlopen(request, timeout=240) as response:
                data = json.loads(response.read().decode("utf-8"))
        except urllib.error.HTTPError as e:
            body = e.read().decode("utf-8", errors="replace")
            raise RuntimeError(f"Ollama HTTP {e.code}: {body}") from e
        except urllib.error.URLError as e:
            raise RuntimeError(f"Ollama indisponivel em {self._base_url}: {e}") from e

        raw = data.get("response", "")
        parsed = extract_json(raw)
        items = coerce_items(parsed)
        return normalize_items(items, min_d, max_d)

# ── Inserção no banco ────────────────────────────────────────────────────────

_DEFAULTS = {
    "estimated_time_ms": 45000,
    "subject": "math",
    "canvas_mode": "calculation",
    "validator": "sympy",
    "source_library": "generated_v1",
}

async def insert_exercises(skill: str, items: list[dict]) -> tuple[int, int]:
    """Insere exercícios novos. Retorna (inseridos, duplicatas)."""
    async with AsyncSessionLocal() as db:
        statements = [x["statement"] for x in items]
        existing = await db.execute(
            select(Exercise.statement).where(Exercise.statement.in_(statements))
        )
        existing_set = set(existing.scalars().all())

        new_exercises = [
            Exercise(**{
                **_DEFAULTS,
                "skill_tags": [skill],
                "statement": x["statement"],
                "expected_answer": x["expected_answer"],
                "difficulty": x["difficulty"],
            })
            for x in items
            if x["statement"] not in existing_set
        ]

        if new_exercises:
            db.add_all(new_exercises)
            await db.commit()

        return len(new_exercises), len(items) - len(new_exercises)

# ── CLI ──────────────────────────────────────────────────────────────────────

async def main() -> None:
    parser = argparse.ArgumentParser(description="Gerador de exercícios via LLM")
    group = parser.add_mutually_exclusive_group(required=True)
    group.add_argument("--all", action="store_true", help="Gerar para todas as skills")
    group.add_argument("--skills", type=str, help="Skills separadas por vírgula")
    parser.add_argument("--count", type=int, default=10, help="Exercícios por skill (padrão: 10)")
    parser.add_argument("--batch-size", type=int, default=25, help="Chamadas por skill em lotes (padrão: 25)")
    parser.add_argument("--dry-run", action="store_true", help="Apenas gera, nao salva no banco")
    parser.add_argument("--provider", choices=["auto", "anthropic", "openai", "gemini", "ollama"], default="auto")
    parser.add_argument("--model", type=str, help="Modelo do provedor escolhido")
    parser.add_argument("--api-key", type=str, help="API key (sobrescreve .env/variavel de ambiente)")
    args = parser.parse_args()

    if args.all:
        skills = list(SKILL_DICT.keys())
    else:
        skills = [s.strip() for s in args.skills.split(",")]
        unknown = [s for s in skills if s not in SKILL_DICT]
        if unknown:
            print(f"❌ Skills desconhecidas: {unknown}")
            print(f"   Disponíveis: {list(SKILL_DICT.keys())}")
            sys.exit(1)

    if args.count < 1:
        print("ERRO: --count deve ser >= 1")
        sys.exit(1)
    if args.batch_size < 1:
        print("ERRO: --batch-size deve ser >= 1")
        sys.exit(1)

    provider = args.provider
    if provider == "auto":
        explicit_key = (
            args.api_key
            or os.environ.get("OPENAI_API_KEY", "")
            or os.environ.get("GEMINI_API_KEY", "")
            or os.environ.get("GOOGLE_API_KEY", "")
        )
        if explicit_key.startswith(("sk-proj-", "sk-")):
            provider = "openai"
        elif explicit_key.startswith("AIza") or os.environ.get("GEMINI_API_KEY"):
            provider = "gemini"
        else:
            provider = "anthropic"

    if provider == "openai":
        api_key = args.api_key or os.environ.get("OPENAI_API_KEY", "")
        model = args.model or os.environ.get("OPENAI_MODEL", DEFAULT_OPENAI_MODEL)
        missing_help = [
            "  1) Set variavel de ambiente OPENAI_API_KEY",
            "  2) python generate_exercises.py ... --provider openai --api-key sk-proj-...",
        ]
        generator: ExerciseGenerator = OpenAIExerciseGenerator(api_key, model)
    elif provider == "gemini":
        api_key = (
            args.api_key
            or os.environ.get("GEMINI_API_KEY", "")
            or os.environ.get("GOOGLE_API_KEY", "")
        )
        model = args.model or os.environ.get("GEMINI_MODEL", DEFAULT_GEMINI_MODEL)
        missing_help = [
            "  1) Set variavel de ambiente GEMINI_API_KEY",
            "  2) python generate_exercises.py ... --provider gemini --api-key AIza...",
        ]
        generator = GeminiExerciseGenerator(api_key, model)
    elif provider == "ollama":
        api_key = "local"
        model = args.model or os.environ.get("OLLAMA_MODEL", DEFAULT_OLLAMA_MODEL)
        base_url = os.environ.get("OLLAMA_BASE_URL", DEFAULT_OLLAMA_BASE_URL)
        missing_help = ["  1) Inicie Ollama: ollama serve"]
        generator = OllamaExerciseGenerator(model=model, base_url=base_url)
    else:
        api_key = (
            args.api_key
            or getattr(settings, "anthropic_api_key", "")
            or os.environ.get("ANTHROPIC_API_KEY", "")
        )
        model = args.model or os.environ.get("ANTHROPIC_MODEL", DEFAULT_ANTHROPIC_MODEL)
        missing_help = [
            "  1) python generate_exercises.py ... --api-key sk-ant-...",
            "  2) Crie backend/.env com ANTHROPIC_API_KEY=sk-ant-...",
            "  3) Set variavel de ambiente ANTHROPIC_API_KEY",
        ]
        generator = AnthropicExerciseGenerator(api_key, model)

    if not api_key:
        print("ERRO: API key nao encontrada.")
        print("  Opcoes:")
        print("\n".join(missing_help))
        sys.exit(1)

    total_inserted = 0
    total_skipped = 0
    total_errors = 0
    print(f"Provider: {provider} | Model: {model} | Batch size: {args.batch_size}")

    for i, skill in enumerate(skills, 1):
        print(f"[{i}/{len(skills)}] {skill} - gerando {args.count} exercicios...")
        skill_items: list[dict] = []
        remaining = args.count
        while remaining > 0:
            batch_count = min(args.batch_size, remaining)
            t0 = time.monotonic()
            try:
                items = generator.generate(skill, batch_count)
            except (json.JSONDecodeError, KeyError, ValueError, RuntimeError) as e:
                total_errors += 1
                print(f"  ERRO ao gerar lote de {batch_count} para '{skill}': {e}")
                break

            elapsed = time.monotonic() - t0
            skill_items.extend(items)
            remaining -= batch_count
            print(f"  OK lote {len(items)} gerados ({elapsed:.1f}s), faltam {remaining}")
            if remaining > 0:
                time.sleep(0.5)

        if not skill_items:
            continue

        if args.dry_run:
            for item in skill_items:
                print(f"    [{item['difficulty']:.1f}] {item['statement'][:70]}")
                print(f"           = {item['expected_answer'][:50]}")
        else:
            inserted, skipped = await insert_exercises(skill, skill_items)
            total_inserted += inserted
            total_skipped += skipped
            print(f"  DB: {inserted} inseridos, {skipped} duplicatas ignoradas")

        # Pausa entre chamadas para não estourar rate limit
        if i < len(skills):
            time.sleep(0.5)

    if not args.dry_run:
        print(
            f"\nTotal: {total_inserted} exercicios inseridos, "
            f"{total_skipped} duplicatas, {total_errors} erros de lote"
        )
        if total_inserted == 0 and total_errors:
            sys.exit(1)
    elif total_errors and total_errors >= len(skills):
        sys.exit(1)


if __name__ == "__main__":
    asyncio.run(main())
