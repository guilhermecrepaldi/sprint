# Sprint Session
Projeto: SPRINT (Strava da Matemática)
Data: 2026-05-26
Início: 00:13
Duração até agora: ~1h 30min
Tarefas: 8/8 (todas concluídas neste sprint)
Pass rate: 100%

## ✅ Concluídas (sessões anteriores)

- [x] [CODE] Renomear app: LOVE CLASS → SPRINT
- [x] [CODE] 7-tab HorizontalPager com fade (MainActivity.kt)
- [x] [CODE] NoteCanvas + SprintNoteSheet (anotações mid-sprint)
- [x] [CODE] NotesTab — listagem de notas com preview de traços
- [x] [CODE] TabPill + SettingsTabScaffold (shared UI)
- [x] [CODE] PenTab — seleção de cor e traçado
- [x] [CODE] NotebookTab — cor de fundo + modo de linhas
- [x] [CODE] GesturesTab — configurador interativo (não guia estático)
- [x] [CODE] DashboardTab — lista vertical Claude-style (Perfil / Histórico)
- [x] [CODE] MathTreeTab — árvore de skills com nós tocáveis
- [x] [CODE] BackgroundMode expandido: WHITE, PARCHMENT, SLATE, DARK
- [x] [CODE] GestureConfig model — mapa configurável ação→gesto
- [x] [CODE] SprintNote model — anotações tagged a sessão+exercício
- [x] [CODE] SessionViewModel: sessionCorrect, sessionTotal, notes, gestureConfig
- [x] [CODE] SessionConfig: skillPin, guideMode adicionados
- [x] [ARCH] skill_pin wiring backend (submit.py → get_next_folha)
- [x] [CODE] Bug fix: NotesTab `remember` shadowing built-in
- [x] [CODE] Bug fix: GestureConfig import missing in SessionViewModel
- [x] [CODE] Bug fix: TextAlign import missing in SprintNoteSheet

## ✅ Concluídas (sprint 2026-05-26)

- [x] [CODE] Bug fix: `getOffsetFractionForPage` → `currentPageOffsetFraction` (BUILD OK)
- [x] [ARCH] Sprint automation: CLAUDE.md project-level auto-start sprint on session open
- [x] [CODE] .sprint/session.md criado como persistent state
- [x] [CODE] Gesture wiring: 2-finger tap → advance via PointerEventPass.Initial em FolhaScreen
- [x] [CODE] GestureConfig param adicionado a FolhaScreen + MainActivity
- [x] [CODE] Parchment/Slate adicionados como opções de fundo em FolhaSettingsSheet (4 swatches)
- [x] [ARCH] Backend: GET /api/student/{student_id}/sessions endpoint (activity.py)
- [x] [CODE] Android: SprintHistoryItem model, StravaMathApi.getSessionHistory, SessionViewModel.fetchHistory
- [x] [CODE] DashboardTab: real API history, groupLabelFor(), displaySkill(), remove mock
- [x] [DOCS] Documentacao viva criada em `docs/`: PROJECT_SPEC, UX_SPEC, ROADMAP, HANDOFF e FUTURE_SPEC
- [x] [DOCS] `AGENTS.md` criado para leitura obrigatoria por agentes futuros
- [x] [DOCS] `CLAUDE.md` atualizado para apontar para `AGENTS.md` e `docs/`
- [x] [DOCS] `docs/README.md` criado como indice oficial de leitura
- [x] [DOCS] Specs e handoffs antigos marcados como historicos
- [x] [CODE] Painel/Perfil ligado a dados reais de progresso/historico em vez de mocks
- [x] [CODE] Historico/progresso atualizados apos iniciar/submeter sessao
- [x] [CODE] Estado local da folha limpo ao iniciar nova sessao
- [x] [UX] Scroll superior ampliado e troca de aba estabilizada
- [x] [CODE] Densidade da sessao persistida em `fixation_density` e exibida no Historico
- [x] [CODE] Historico expoe `template` para zoom exato e Painel mostra marcador discreto
- [x] [CODE] Arvore inicia Sprint com a skill tocada diretamente e limpa zoom anterior
- [x] [UX] Painel abre direto no Perfil com dados reais
- [x] [UX] Feedback breve `certo`/`erro` antes de avancar apos submit
- [x] [CODE] Sprint força `exercisesPerPage = 1` para registrar cada exercício individualmente
- [x] [UX] Feedback mostra a interpretacao reconhecida em verde/vermelho e nao bloqueia avanço
- [x] [BUGFIX] Canvas de resposta agora reseta por `folha_id + exercise_id`, nao por `fieldIndex`
- [x] [BUGFIX] Estado enviado ao backend agora pertence a `folhaId`; traços antigos não são reutilizados se `fieldIndex` repetir
- [x] [BUGFIX] Escrita restaurada: callbacks de traço agora salvam com `folhaId` explícito e submit usa o estado mais recente da folha atual
- [x] [BUGFIX] Canvas troca `detectDragGestures` por captura bruta de ponteiro: traços curtos, pontos e sinais começam no primeiro toque
- [x] [BUGFIX] Removido `clickable` vazio envolvendo a folha para reduzir competição de gestos com o canvas
- [x] [DOCS] Handoff atualizado para passagem ao Claude com bugfix de escrita, riscos e checklist manual
- [x] [DOCS] Criado `docs/TRANSITION_SPEC.md` como spec de transição Codex -> Claude
- [x] [DOCS] `CLAUDE.md`, `AGENTS.md`, `docs/README.md`, `PROJECT_SPEC.md` e `UX_SPEC.md` apontam para contratos de transição/escrita

## ✅ Concluídas (sprint 2026-05-26 lote 3 — agentes paralelos)

- [x] [ARCH] BuildConfig.API_BASE_URL: buildConfig=true + buildConfigField por variant (debug/release)
- [x] [CODE] ApiClient.create() usa BuildConfig.API_BASE_URL como default
- [x] [CODE] TAB_SPRINT: SprintErrorState + SprintLoadingState (ui visível em erro/conexão)
- [x] [CODE] Notes persistence: SprintNoteJson + NotesJson + toJson/toSprintNote (SharedPrefs sprint_notes_v1)
- [x] [CODE] SessionViewModel.init carrega notas do disco; addNote() persiste após cada adição
- [x] [CODE] GuideMode wired: ExerciseField recebe userGuideMode, mapeia "horizontal"→"lined", "dots"→"dots", "nenhuma"→field.canvasMode
- [x] [CODE] FolhaScreen passa userGuideMode = config.guideMode ao ExerciseField
- [x] [BUILD] BUILD SUCCESSFUL 1s (38 tasks UP-TO-DATE) — integração limpa

## 🔄 Em progresso

(nenhuma)

## ⛔ Bloqueadas

(nenhuma)

## 📋 Pendentes

- [ ] [FEAT] ZoomableCanvas / Platform Map — canvas infinito zoomável (plano salvo em .claude/plans/floofy-coalescing-mccarthy.md)
- [ ] [CODE] MathTreeTab: Canvas API completo — árvore visual com círculos, linhas, labels (low priority — grid atual funciona)
- [ ] [QA] Teste end-to-end: abrir app → exercício → anotar → ver nota em NotesTab
- [ ] [QA] Teste de gesto: 2-finger tap → avança exercício
- [ ] [QA] Teste de background: parchment/slate mudam fundo corretamente
- [ ] [CODE] DashboardTab Perfil: conectar com API real (stats reais: XP, sequência, etc.)
- [ ] [INFRA] Sprint automation at 9h: instalar_task.ps1 (requer usuário rodar como Administrador)
- [ ] [INFRA] Preencher .env com ANTHROPIC_API_KEY + WA_PHONE/WA_APIKEY ou TG_BOT_TOKEN/TG_CHAT_ID
- [ ] [PROD] Release URL: substituir placeholder "SEU_SERVIDOR.com" em build.gradle.kts release buildType

## Decisões tomadas

- [ARCH] ZoomableCanvas removido — navegação via HorizontalPager com fade
- [UX] Sprint na aba 4 (centro), pill volta ao Sprint de qualquer aba
- [UX] Gestos configuráveis — GestureConfig com defaults, usuário sobrescreve
- [UX] Dashboard abre lista vertical Claude-style: Perfil | Histórico
- [UX] Anotações são telas cheias mid-sprint, salvas como cards com preview
- [ARCH] `getOffsetFractionForPage` não disponível no BOM 2024.10.00 — usar `currentPage - page + currentPageOffsetFraction`
- [ARCH] SprintHistoryItem usa `skill_pin` do `session_configs` como skill label
- [ARCH] Gesture detection usa PointerEventPass.Initial para interceptar multi-touch antes do InkCanvas
- [DOCS] `docs/` passa a ser a fonte curta e atual do produto; specs antigas ficam como referencia historica
- [DOCS] Arquivos antigos na raiz nao devem ser usados como fonte primaria sem conferir `docs/`
- [UX] Sprint limpa continua regra central: sem tutorial/texto fixo, configuracoes por gesto e scroll secundario
- [ARCH] Densidade exata usa `template_pin` + `focus_source_exercise_id`
- [ARCH] Historico deve diferenciar densidade `leve`, `fixa`, `densa` e `exata`
- [UX] Painel pode ter dados textuais, mas Sprint continua sem UI explicativa fixa
- [UX] Feedback de resultado deve ser momentaneo e discreto, sem interromper fluxo
- [UX] Se errar, apenas contabiliza erro, mostra leitura reconhecida em vermelho e avanca
- [ARCH] Registro de acerto/erro acontece por exercício no Sprint, nao por lote visual
- [BUGFIX] Como `exercisesPerPage = 1`, `fieldIndex` fica sempre 0; nunca usar apenas `fieldIndex` como chave visual do canvas
- [BUGFIX] `FolhaUiState` deve carregar `folhaId` para proteger OCR/submissao contra respostas antigas
- [BUGFIX] Callbacks do canvas precisam receber `folhaId` no MainActivity; método legado sem id só deve ser fallback quando o estado já está escopado
- [BUGFIX] Para escrita matemática, não usar detector com touch slop no canvas principal; números pequenos, pontos e sinais precisam ser registrados imediatamente no down

## Retomar em

[ ] QA Android Studio/emulador: scroll superior, triplo toque no enter, bolinha divisoria e scrolls secundarios
[ ] QA dispositivo real/tablet USB: confirmar escrita na resposta, avanço, leitura reconhecida e reset da resposta no próximo exercício
[ ] MathTreeTab Canvas API (baixa prioridade)
[ ] QA testing (alta prioridade — app nunca foi testado em dispositivo após as mudanças)

## Log de lotes

- [00:13] Lote 1: 3/3 ✅ — 100% — build fix + 2-finger tap + settings
- [01:30] Lote 2: 5/5 ✅ — 100% — sprint automation + backend history API + dashboard real data
- [02:15] Lote 3: 8/8 ✅ — 100% — BuildConfig URL, error/loading UI, notes persistence, guideMode wiring; BUILD SUCCESSFUL 1s
