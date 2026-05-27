# Sprint Session
Projeto: SPRINT (Strava da Matemática)
Data: 2026-05-26
Início: 00:13
Duração até agora: ~3h 00min
Tarefas: 22/29 concluídas
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
- [x] [CODE] Bug fix: `getOffsetFractionForPage` → `currentPageOffsetFraction` (BUILD OK)
- [x] [ARCH] Sprint automation: CLAUDE.md project-level auto-start sprint on session open
- [x] [CODE] .sprint/session.md criado como persistent state
- [x] [CODE] Gesture wiring: 2-finger tap → advance via PointerEventPass.Initial em FolhaScreen
- [x] [CODE] GestureConfig param adicionado a FolhaScreen + MainActivity
- [x] [CODE] Parchment/Slate adicionados como opções de fundo em FolhaSettingsSheet (4 swatches)
- [x] [ARCH] Backend: GET /api/student/{student_id}/sessions endpoint (activity.py)
- [x] [CODE] Android: SprintHistoryItem model, StravaMathApi.getSessionHistory, SessionViewModel.fetchHistory
- [x] [CODE] DashboardTab: real API history, groupLabelFor(), displaySkill(), remove mock
- [x] [CODE] Sprint automation + docs live criados
- [x] [CODE] ApiClient.create() usa BuildConfig.API_BASE_URL como default
- [x] [CODE] TAB_SPRINT: SprintErrorState + SprintLoadingState
- [x] [CODE] Notes persistence: SharedPreferences sprint_notes_v1
- [x] [CODE] GuideMode wired: ExerciseField recebe userGuideMode
- [x] [BUILD] BUILD SUCCESSFUL (lote 3)

## ✅ Concluídas (sprint 2026-05-26 lote 4 — agentes paralelos)

- [x] [BUGFIX] build.gradle.kts: import java.util.Properties + localProps no top-level (corrige BUILD FAILED)
- [x] [CODE] A2: Watermark "resposta" em ExerciseField — texto fantasma alpha=0.18f, some ao primeiro stroke
- [x] [CODE] B2: Toggle inline livre/precisa acertar — ícone Lock/LockOpen no canto superior direito da folha ativa (FolhaScreen.kt)
- [x] [CODE] B2: Sincronizado com FolhaSettingsSheet via config.requireCorrectToAdvance
- [x] [CODE] D1: averageTimePerSession() — helper que extrai avg seg/ex das últimas 7 sessões (DashboardTab.kt)
- [x] [CODE] D1: VelocityGraph composable — gráfico de linha fina Canvas API, pontos, labels MM-DD (DashboardTab.kt)
- [x] [CODE] D1: StatRow "Velocidade média" exibe média geral em seg/ex
- [x] [CODE] D2: MathTreeTab — nós coloridos por accuracy: verde >85%, vermelho <60%, neutro entre os dois
- [x] [CODE] D2: Legenda discreta de cores abaixo do título (3 pontos coloridos)
- [x] [CODE] D3: Alert após 3 falhas consecutivas com botões "continuar"/"ver painel"
- [x] [CODE] requireCorrectToAdvance: campo em SessionConfig + retry via retryCount/clearFieldAndRetry
- [x] [CODE] ResultHistoryRow: □○× histórico dos últimos 7 resultados, tamanho crescente, Canvas-drawn
- [x] [CODE] Haptic feedback: TextHandleMove (acerto), LongPress (erro)
- [x] [CODE] Avanço instantâneo: removido delay 720ms
- [x] [QA] Backend: skill-progress endpoint existe em activity.py; attempt_count mapeado corretamente
- [x] [QA] Backend: adaptive engine N=3 erros consecutivos → RECOVERY_DIFFICULTY_FACTOR=0.65 ✅
- [x] [QA] Backend: 32/34 skills com < 3 templates estruturais — gap documentado para Gemini
- [x] [BUILD] BUILD SUCCESSFUL (lote 4)

## ✅ Concluídas (sprint 2026-05-26 lote 5)

- [x] [BUGFIX] FolhaScreen: exactCurrent defaultava true (templateId != null sempre), skill selecionada nunca aplicada — fix: default false
- [x] [BUGFIX] OCR: fallback para fieldScratchStrokes quando fieldAnswerStrokes vazio (usuário escreve em scratch = pontuação correta)
- [x] [BUGFIX] MainActivity: delay 450ms para feedback ser visível antes de avançar
- [x] [CODE] MathTreeTab: redesign completo como timeline vertical scrollável (LazyColumn + Canvas API linha tracejada + nós circulares)
- [x] [BUILD] BUILD SUCCESSFUL (lote 5) — f271ac9

## ✅ Concluídas (sprint 2026-05-26 lote 6)

- [x] [CODE] SplitRatioPrefs.kt: helper get/set por fieldIndex em SharedPreferences "split_ratio_v1"
- [x] [CODE] ExerciseField.kt: carrega ratio salvo na composição; persiste ao arrastar o handle
- [x] [QA] Canvas guia: "horizontal"→"lined" (linhas 32dp, alpha=0.22), "dots"→"dots" (grade 28dp, alpha=0.12) — PASS ✅
- [x] [QA] z-order: SprintFeedbackOverlay adicionado APÓS FolhaScreen no mesmo Box → renderiza por cima ✅
- [x] [QA] exercisesPerPage=1: densityToConfig() força 1 em todos os modos; enviado no SessionStartRequest.config ✅
- [x] [QA] clearFieldAndRetry(): resetForNextFolha() → FolhaUiState() → retryCount=0 ✅
- [x] [QA] Fluxo completo: write→advanceExercise→submitFolha→OCR(fallback scratch)→RESULT→delay→haptic→advance/retry ✅
- [x] [QA] BACKEND attempt_count: já mapeado como attemptCount em Android (lote 4) ✅
- [x] [FEAT] ZoomableCanvas wired: FolhaScreen envolvido; PlatformMap como mapContent; pauseSession/resumeSession conectados
- [x] [CODE] FolhaScreen: sprintGestureInput simplificado — 2-finger advance migrado para ZoomableCanvas
- [x] [INFRA] Agente remoto CCR criado: trig_01YBesuXGBdHwmn3L51Hh8iy · roda 9h Sao Paulo (12:00 UTC) diariamente · https://claude.ai/code/routines/trig_01YBesuXGBdHwmn3L51Hh8iy
- [x] [BUILD] BUILD SUCCESSFUL (lote 6) — de942ed

## 🔄 Em progresso

(nenhuma)

## ⛔ Bloqueadas

(nenhuma)

## 📋 Pendentes

- [ ] [BACKEND] Exercise library: 25 skills com 1 template estrutural — sessão Gemini para gerar variações
- [ ] [QA] Testar no emulador: ZoomableCanvas pinch revela mapa; 2-finger tap avança sem zoom

## Decisões tomadas

- [ARCH] ZoomableCanvas removido — navegação via HorizontalPager com fade
- [UX] Sprint na aba 4 (centro), pill volta ao Sprint de qualquer aba
- [UX] Gestos configuráveis — GestureConfig com defaults, usuário sobrescreve
- [UX] Dashboard abre lista vertical Claude-style: Perfil | Histórico
- [UX] Anotações são telas cheias mid-sprint, salvas como cards com preview
- [ARCH] `getOffsetFractionForPage` não disponível no BOM 2024.10.00 — usar `currentPage - page + currentPageOffsetFraction`
- [ARCH] SprintHistoryItem usa `skill_pin` do `session_configs` como skill label
- [ARCH] Gesture detection usa PointerEventPass.Initial para interceptar multi-touch antes do InkCanvas
- [UX] Feedback de resultado deve ser momentaneo e discreto, sem interromper fluxo
- [UX] Se errar, apenas contabiliza erro, mostra leitura reconhecida em vermelho e avanca
- [ARCH] Registro de acerto/erro acontece por exercício no Sprint, nao por lote visual
- [BUGFIX] Como `exercisesPerPage = 1`, `fieldIndex` fica sempre 0; nunca usar apenas `fieldIndex` como chave visual do canvas
- [BUGFIX] Para escrita matemática, não usar detector com touch slop no canvas principal; números pequenos, pontos e sinais precisam ser registrados imediatamente no down
- [UX] requireCorrectToAdvance: toggle discreto lock/unlock no canto superior direito do exercício ativo
- [UX] Histórico de resultados: ○□× crescentes, canvas-drawn, canto inferior esquerdo
- [UX] Haptic: leve no acerto, forte no erro
- [ARCH] D1 velocity: avgSecPerExercise = durationMin * 60 / exercisesDone, last 7 sessions
- [ARCH] D2 node colors: verde #388E3C >85%, vermelho #D32F2F <60%, neutro entre os dois
- [ARCH] build.gradle.kts: import java.util.Properties no topo do arquivo, Properties() fora do bloco release

## Retomar em

[ ] Split ratio persistence (Prioridade 4)
[ ] QA emulador: fluxo completo de escrita → avanço → histórico → retry
[ ] ZoomableCanvas (Prioridade baixa — feature futura)

## Log de lotes

- [00:13] Lote 1: 3/3 ✅ — 100% — build fix + 2-finger tap + settings
- [01:30] Lote 2: 5/5 ✅ — 100% — sprint automation + backend history API + dashboard real data
- [02:15] Lote 3: 8/8 ✅ — 100% — BuildConfig URL, error/loading UI, notes persistence, guideMode wiring; BUILD SUCCESSFUL 1s
- [03:00] Lote 4: 18/18 ✅ — 100% — build.gradle fix, A2 watermark, B2 lock toggle, D1 velocity graph, D2 node colors, D3 alert, requireCorrectToAdvance, ResultHistoryRow, haptic, instant advance; BUILD SUCCESSFUL 3s
- [03:30] Lote 5: 5/5 ✅ — 100% — skill selection bug, OCR scratch fallback, feedback delay, MathTreeTab timeline redesign; BUILD SUCCESSFUL 2s
- [04:15] Lote 6: 12/12 ✅ — 100% — split ratio persistence, QA (6 itens), ZoomableCanvas wired, FolhaScreen simplificado, CCR routine; BUILD SUCCESSFUL 4s
