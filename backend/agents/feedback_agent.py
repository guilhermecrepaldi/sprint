# DEPRECADO — feedback textual removido.
# O "feedback" agora é a próxima folha: o motor adaptativo seleciona
# exercícios de revisão da skill onde o aluno errou.
# Este arquivo existe apenas para não quebrar testes legados.

import anthropic
from db import settings


class FeedbackAgent:
    def __init__(self):
        self._client = None

    @property
    def client(self):
        if self._client is None:
            self._client = anthropic.Anthropic(api_key=settings.anthropic_api_key)
        return self._client

    async def generate(
        self,
        *,
        is_correct: bool,
        error_type: str | None,
        statement: str,
        recognized: str | None,
        expected: str,
        student_streak: int,
    ) -> str:
        # Streak >= 3 acertos seguidos: silêncio total
        if is_correct and student_streak >= 3:
            return ""
        if is_correct:
            return "✓"

        # Feedback de erro: 1 frase, máx 12 palavras, não resolve
        prompt = (
            f"Exercício: {statement}\n"
            f"Resposta do aluno: {recognized}\n"
            f"Resposta correta: {expected}\n"
            f"Tipo de erro: {error_type or 'desconhecido'}\n\n"
            "Escreva UMA frase curta (máximo 12 palavras) em português apontando onde errou. "
            "Não resolva. Não explique tudo. Só aponte.\n"
            "Exemplos: 'Sinal trocou ao transpor o termo.' / '3×4 = 12, não 11.'"
        )
        try:
            response = self.client.messages.create(
                model="claude-haiku-4-5-20251001",
                max_tokens=60,
                messages=[{"role": "user", "content": prompt}],
            )
            return response.content[0].text.strip()
        except Exception:
            # Nunca travar o fluxo por falha de feedback
            return "Revise o cálculo."
