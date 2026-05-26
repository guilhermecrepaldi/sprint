# SPRINT Agent Brief

Este projeto deve ser lido como produto de estudo continuo, progressivo e adaptativo de matematica. Antes de qualquer alteracao relevante, leia estes documentos nesta ordem:

1. `docs/PROJECT_SPEC.md` - visao, principios, arquitetura e contrato do produto.
2. `docs/UX_SPEC.md` - experiencia da Sprint, gestos, scrolls e regras de interface.
3. `docs/ROADMAP.md` - prioridades atuais e proximos blocos de trabalho.
4. `docs/HANDOFF.md` - estado operacional para o proximo agente.
5. `docs/TRANSITION_SPEC.md` - passagem operacional entre agentes/Codex/Claude e checklist de QA.
6. `docs/FUTURE_SPEC.md` - ideias futuras que nao devem poluir o escopo atual.
6. `.sprint/session.md` - estado da sessao de desenvolvimento.

## Regras permanentes

- A tela Sprint deve permanecer limpa: exercicio, area de resolucao, area de resposta, scroll superior discreto e controles gestuais.
- Configuracoes profundas aparecem por gesto/scroll secundario, nao como UI fixa.
- Registro de exercicios, tempo, escrita, acerto/erro, template e densidade e parte central do produto.
- Densidade pode ser por tema ou por exercicio exato. Zoom exato usa `template_pin` e `focus_source_exercise_id`.
- Ao mudar produto/UX/arquitetura, atualize os documentos em `docs/` e o estado em `.sprint/session.md`.
- Nao reverta mudancas do usuario ou de outros agentes.
- Rode validacoes proporcionais: Android build para UI/app, testes backend para API/motor.
