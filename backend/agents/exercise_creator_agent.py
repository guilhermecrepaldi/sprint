import json

import anthropic
from db import settings


class ExerciseCreatorAgent:
    def __init__(self):
        self._client = None

    @property
    def client(self):
        if self._client is None:
            self._client = anthropic.Anthropic(api_key=settings.anthropic_api_key)
        return self._client

    async def generate(
        self,
        focus_skill: str,
        difficulty: float,
        subject: str = "math",
        narrative: str = "",
    ) -> dict | None:
        prompt = (
            f"{narrative}\n\n" if narrative else ""
        ) + (
            f"Crie um exercício de {subject} com estas regras:\n"
            f"- Habilidade alvo: {focus_skill}\n"
            f"- Dificuldade: {difficulty:.1f} (escala 1–10)\n"
            "- Resposta deve ser inteiro ou fração simples\n"
            "- Enunciado em português, conciso\n\n"
            'Retorne JSON exato:\n'
            '{"statement": "...", "expected_answer": "...", '
            '"skill_tags": ["..."], "difficulty": ' + f'{difficulty:.1f}' + ', '
            '"estimated_time_ms": 45000}'
        )
        try:
            response = self.client.messages.create(
                model="claude-sonnet-4-5",
                max_tokens=300,
                messages=[{"role": "user", "content": prompt}],
            )
            text = response.content[0].text.strip()
            # Extrair JSON mesmo se vier com markdown
            if "```" in text:
                text = text.split("```")[1].lstrip("json").strip()
            return json.loads(text)
        except Exception:
            return None
