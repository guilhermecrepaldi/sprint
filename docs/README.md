# SPRINT Docs

Esta pasta e a fonte curta e atual do projeto SPRINT. Agentes, devs e IAs devem ler estes arquivos antes de usar specs antigas da raiz.

## Ordem de leitura

1. `../AGENTS.md` - regras permanentes para agentes.
2. `PROJECT_SPEC.md` - produto, arquitetura e contratos.
3. `UX_SPEC.md` - experiencia da Sprint, gestos e scrolls.
4. `ROADMAP.md` - prioridades e proximos blocos.
5. `HANDOFF.md` - estado operacional atual.
6. `TRANSITION_SPEC.md` - passagem operacional para continuar em outro agente/Claude.
7. `FUTURE_SPEC.md` - ideias futuras fora do escopo imediato.
8. `../.sprint/session.md` - estado de execucao da sessao.

## Fonte de verdade

Use `docs/` como fonte principal. Arquivos antigos na raiz, como `SUPER_SPEC.md`, `APP_LAYOUT_SPEC.md`, `UX_CANVAS_SPEC.md` e handoffs antigos, ficam como historico e referencia contextual.

## Quando atualizar

Atualize estes docs quando mudar:

- Fluxo da Sprint.
- Gestos, scrolls, enter ou bolinha divisoria.
- Biblioteca de exercicios, densidade ou zoom exato.
- Registro/historico de tentativas.
- Contratos backend/API.
- Roadmap e prioridades.
- Passagem entre agentes/Claude e criterios de QA.

## Validacao esperada

- Mudanca Android: `.\gradlew.bat :app:assembleDebug`
- Mudanca backend: `python -m unittest`
- Mudanca documental: `git diff --check`
