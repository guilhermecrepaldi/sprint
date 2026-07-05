import asyncio
import base64
import json
import logging

import anthropic

from db import settings

logger = logging.getLogger(__name__)


def _local_text_fallback(image_base64: str) -> dict:
    if image_base64.startswith(("latex:", "text:")):
        return {"answer_latex": image_base64.split(":", 1)[1].strip() or None, "confidence": 1.0}

    try:
        decoded = base64.b64decode(image_base64, validate=True).decode("utf-8").strip()
    except Exception:
        decoded = ""

    if decoded:
        return {"answer_latex": decoded, "confidence": 0.9}
    return {"answer_latex": None, "confidence": 0.0}


def _extract_answer_sync(image_base64: str) -> dict:
    if not settings.anthropic_api_key:
        return _local_text_fallback(image_base64)

    client = anthropic.Anthropic(api_key=settings.anthropic_api_key)
    message = client.messages.create(
        model=settings.claude_ocr_model,
        max_tokens=256,
        messages=[
            {
                "role": "user",
                "content": [
                    {
                        "type": "image",
                        "source": {
                            "type": "base64",
                            "media_type": "image/png",
                            "data": image_base64,
                        },
                    },
                    {
                        "type": "text",
                        "text": (
                            "Você está analisando um campo de resposta manuscrita de matemática do ensino médio.\n"
                            "Extraia APENAS a resposta final escrita no campo.\n"
                            "Retorne em formato LaTeX.\n"
                            "Se houver múltiplas escritas, use a última (mais abaixo).\n"
                            "Retorne JSON exato: {\"answer_latex\": \"...\", \"confidence\": 0.0}\n"
                            "Se não conseguir ler, retorne: {\"answer_latex\": null, \"confidence\": 0.0}"
                        ),
                    },
                ],
            }
        ],
    )
    return json.loads(message.content[0].text)


async def extract_answer(image_base64: str) -> dict:
    try:
        return await asyncio.to_thread(_extract_answer_sync, image_base64)
    except Exception:
        logger.exception("OCR failed")
        return {"answer_latex": None, "confidence": 0.0}
