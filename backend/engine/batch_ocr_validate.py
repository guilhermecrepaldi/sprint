"""
Batch OCR + correção: envia N imagens de resposta para OpenAI em UMA call.

Só faz o necessário: ler a escrita do aluno e dizer se está correto.
Sem dicas, sem feedback textual — a trilha adaptativa é o feedback.

N chamadas individuais → 1 chamada por folha.
"""
import asyncio
import json
import logging

import openai

from db import settings

logger = logging.getLogger(__name__)

_SYSTEM = (
    "Você é um professor de matemática lendo respostas manuscritas de concursandos. "
    "Leia cada imagem e verifique se a resposta está matematicamente correta. "
    "Aceite equivalências (ex: x=5 e 5=x são iguais; 3/2 e 1.5 são iguais). "
    "Retorne SOMENTE JSON válido, sem texto adicional."
)

_INSTRUCTIONS = """
Para cada exercício acima (em ordem):
1. Leia o que o aluno escreveu na imagem correspondente
2. Verifique se é matematicamente equivalente à resposta correta

Retorne APENAS este JSON array (um objeto por exercício, mesma ordem):
[
  {"index": 1, "recognized": "resposta lida", "correct": true, "confidence": 0.95},
  {"index": 2, "recognized": "resposta lida", "correct": false, "confidence": 0.85}
]

Se não conseguir ler uma imagem: {"recognized": null, "correct": false, "confidence": 0.0}
"""


def _build_content_blocks(items: list[dict]) -> list[dict]:
    """Intercala contexto + imagem para cada exercício, depois as instruções."""
    blocks = []
    for i, item in enumerate(items, 1):
        blocks.append({
            "type": "text",
            "text": (
                f"Exercício {i}: {item['statement']}\n"
                f"Resposta correta: {item['expected_answer']}\n"
                f"Imagem {i}:"
            ),
        })
        blocks.append({
            "type": "image_url",
            "image_url": {
                "url": f"data:image/png;base64,{item['image_base64']}",
                "detail": "low",   # low = faster + cheaper; suficiente para OCR de math
            },
        })
    blocks.append({"type": "text", "text": _INSTRUCTIONS})
    return blocks


def _batch_validate_sync(items: list[dict]) -> list[dict]:
    if not settings.openai_api_key:
        return [_fallback_result(item) for item in items]

    client = openai.OpenAI(api_key=settings.openai_api_key)
    response = client.chat.completions.create(
        model=settings.openai_model,
        max_tokens=256 + 80 * len(items),
        messages=[
            {"role": "system", "content": _SYSTEM},
            {"role": "user", "content": _build_content_blocks(items)},
        ],
    )

    raw = response.choices[0].message.content.strip()
    if raw.startswith("```"):
        raw = raw.split("```", 2)[1]
        if raw.startswith("json"):
            raw = raw[4:]
        raw = raw.rsplit("```", 1)[0].strip()

    parsed: list[dict] = json.loads(raw)

    results = []
    for i in range(len(items)):
        entry = next((p for p in parsed if p.get("index") == i + 1), None)
        if entry is None:
            entry = {"recognized": None, "correct": False, "confidence": 0.0}
        results.append({
            "recognized_answer": entry.get("recognized"),
            "is_correct": bool(entry.get("correct", False)),
            "confidence": float(entry.get("confidence", 0.0)),
            "error_type": None if entry.get("correct") else "wrong_answer",
        })
    return results


def _fallback_result(item: dict) -> dict:
    """Sem API: tenta decodificar texto puro do campo image_base64."""
    import base64
    image_b64 = item.get("image_base64", "")
    if image_b64.startswith(("latex:", "text:")):
        recognized = image_b64.split(":", 1)[1].strip() or None
    else:
        try:
            recognized = base64.b64decode(image_b64, validate=True).decode("utf-8").strip() or None
        except Exception:
            recognized = None
    return {
        "recognized_answer": recognized,
        "is_correct": False,
        "confidence": 0.0,
        "error_type": "no_api",
    }


async def batch_ocr_validate(items: list[dict]) -> list[dict]:
    """
    Valida N exercícios em 1 chamada OpenAI (gpt-4o-mini vision).

    items: lista de dicts com:
        - statement: str
        - expected_answer: str
        - image_base64: str (PNG em base64)

    Retorna lista na mesma ordem, cada item com:
        - recognized_answer: str | None
        - is_correct: bool
        - confidence: float
        - error_type: str | None
    """
    if not items:
        return []
    try:
        return await asyncio.to_thread(_batch_validate_sync, items)
    except Exception:
        logger.exception("Batch OCR+validate falhou")
        return [
            {"recognized_answer": None, "is_correct": False, "confidence": 0.0, "error_type": "batch_error"}
            for _ in items
        ]
