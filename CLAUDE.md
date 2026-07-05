# SPRINT — Projeto Android (Strava da Matemática)

## Contexto do projeto

App Android em Jetpack Compose. Treino de matemática gamificado — o "Strava da Matemática".
- Pacote: `com.strava_matematica`
- Backend: FastAPI + PostgreSQL (pasta `backend/`)
- Frontend: Android/Kotlin/Compose (pasta `app/`)

## Auto-sprint (OBRIGATÓRIO ao abrir esta sessão)

**Ao iniciar qualquer sessão neste projeto, execute imediatamente os passos abaixo sem esperar input do usuário:**

0. Leia `AGENTS.md` e os documentos vivos em `docs/`:
   - `docs/PROJECT_SPEC.md`
   - `docs/UX_SPEC.md`
   - `docs/ROADMAP.md`
   - `docs/HANDOFF.md`
   - `docs/TRANSITION_SPEC.md`
   - `docs/FUTURE_SPEC.md`
1. Leia `.sprint/session.md` — se existe com tarefas pendentes, exiba um resumo de 3 linhas e retome de onde parou
2. Se não existe ou está vazio, leia:
   - `git log --oneline -10`
   - `git status`
   - O plano em `.claude/plans/` (se houver)
   - `SUPER_SPEC.md` (visão geral do produto)
   - E então inicie um novo sprint com `/sprint`
3. Execute o sprint automaticamente — não espere o usuário digitar nada

**O sprint é o modo padrão de trabalho neste projeto. Toda sessão começa no sprint.**

## Stack técnico

- Kotlin 2.3.21 + Compose BOM mais recente
- `enum.entries` (não `.values()`)
- `HorizontalPager` com fade via `graphicsLayer { alpha = ... }`
- 7 abas: Pen(0), Notebook(1), Gestos(2), Dashboard(3), Sprint(4), MathTree(5), Notes(6)
- `BackgroundMode`: WHITE, PARCHMENT, SLATE, DARK
- `GestureConfig` — mapa configurável de ação → gesto
- `SprintNote` — anotações de sessão tagged com folhaIndex + exerciseIndex

## Sessão sprint persistida

Estado da sessão de desenvolvimento: `.sprint/session.md`
Tarefas decompostas: `.sprint/tasks.md`
Salve após cada lote. Nunca perca estado.

## Documentação viva

Ao alterar produto, UX, arquitetura, motor de exercicios, densidade, registro ou fluxo de Sprint, atualize os arquivos em `docs/`. Eles sao a fonte curta atual; specs antigas continuam como referencia historica.
