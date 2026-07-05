# SPRINT — Software Specification Document v1.0

> Sistema de educação matemática adaptativa com reconhecimento de escrita à mão,
> engine procedimental de exercícios, e modo simulado com exportação.
> Android (Kotlin/Jetpack Compose) + Backend (Python/FastAPI) + Banco Local (SQLite/Room)

---

## 1. ARQUITETURA GERAL

```
┌─────────────────────────────────────────────────────────┐
│                    ANDROID APP                           │
│  ┌──────────┐  ┌──────────┐  ┌───────────────────────┐  │
│  │   UI     │  │ ViewModel│  │  Domain (local)        │  │
│  │ Compose  │──│ StateFlow│──│  ProceduralEngine       │  │
│  │ Screens  │  │          │  │  DeterministicValidator │  │
│  └──────────┘  └──────────┘  │  LocalSprintRepository  │  │
│                              │  Room DB (SQLite)       │  │
│                              └───────────────────────┘  │
│                                     │                    │
│                              ┌──────┴──────┐             │
│                              │  HTTP/WS     │            │
│                              │  Backend API │            │
│                              └──────┬──────┘             │
└─────────────────────────────────────┼─────────────────────┘
                                      │
                              ┌───────┴────────┐
                              │   BACKEND       │
                              │   FastAPI       │
                              │   PostgreSQL    │
                              │   OpenAI/Claude │
                              └────────────────┘
```

---

## 2. ESTRUTURA DE DIRETÓRIOS

### 2.1 Android App (`app/src/main/java/com/sprint/`)

```
com.sprint/
├── MainActivity.kt                          # Entry point, tab navigation
├── design/                                   # Tema visual
│   ├── CanvasColors.kt                       # Paleta de cores
│   ├── Color.kt
│   ├── Glassmorphism.kt                      # Efeito vidro fosco
│   ├── Spacing.kt                            # Espaçamentos padrao
│   ├── Theme.kt                              # Tema Material3
│   └── Type.kt                               # Tipografia
│
├── model/                                    # Data classes (Android)
│   ├── Folha.kt                              # FolhaField + Folha
│   ├── SessionConfig.kt                      # Config de sessao + enums
│   ├── SubmitResult.kt                       # Resultados de submissao
│   ├── PenEvent.kt                           # Eventos de caneta
│   ├── GestureConfig.kt                      # Gestos personalizaveis
│   ├── MathCurriculum.kt                     # Arvore curricular (286 linhas)
│   ├── Activity.kt, Calibration.kt, Drill.kt
│   ├── Profile.kt, Ranking.kt
│   └── TelemetrySyncModels.kt
│
├── domain/procedural/                        # Engine procedimental (LOCAL)
│   ├── ProceduralEngine.kt                   # Orquestrador principal (392 linhas)
│   ├── ProceduralExercise.kt                 # Data class (inline no engine)
│   ├── ProceduralAlgebra.kt                  # Algebra (439 linhas)
│   ├── ProceduralArithmetic.kt               # Aritmetica
│   ├── ProceduralCalculus.kt                 # Calculo
│   ├── ProceduralGeometry.kt                 # Geometria
│   ├── ProceduralFunctions.kt                # Funcoes
│   ├── ProceduralStats.kt                    # Estatistica
│   ├── ProceduralProportions.kt              # Proporcoes
│   ├── ProceduralLogic.kt                    # Logica
│   ├── ProceduralNumberTheory.kt             # Teoria dos numeros
│   ├── ProceduralCompMath.kt                 # Matematica computacional
│   ├── ProceduralLinearAlgebra.kt            # Algebra linear
│   ├── ProceduralGraphs.kt                   # Grafos
│   ├── MathBktEngine.kt                      # BKT (Bayesian Knowledge Tracing)
│   ├── ArenaManager.kt                       # Sistema competitivo
│   └── EloMatchmaker.kt                      # Matchmaking ELO
│
├── data/                                     # Persistencia local
│   ├── local/
│   │   ├── catalog/                          # Catalogo de exercicios (SQLite)
│   │   │   ├── ExerciseDao.kt
│   │   │   ├── ExamDao.kt
│   │   │   ├── CatalogEntities.kt
│   │   │   ├── ExerciseSeedInstaller.kt      # Instalacao do seed (SQL)
│   │   │   └── SprintCatalogDatabase.kt
│   │   ├── runtime/                          # Dados de sessao (Room)
│   │   │   ├── RuntimeEntities.kt
│   │   │   ├── RuntimeDaos.kt
│   │   │   ├── ChallengeDao.kt
│   │   │   ├── SyncDao.kt
│   │   │   └── SprintRuntimeDatabase.kt
│   │   └── repository/
│   │       ├── LocalSprintRepository.kt      # Repositorio principal (486 linhas)
│   │       ├── DeterministicValidator.kt     # Validador matematico (152 linhas)
│   │       └── ExamRepository.kt
│   └── worker/
│       └── ChallengeSyncWorker.kt            # Sincronizacao em background
│
├── recognizer/                               # Reconhecimento de escrita
│   ├── MathRecognizer.kt                     # Interface
│   ├── MlKitRecognizer.kt                    # Google ML Kit OCR (172 linhas)
│   ├── RecognizerChain.kt                    # Cadeia de fallback
│   ├── IinkRecognizer.kt                     # MyScript iink stub
│   ├── DigitalInkMathRecognizer.kt           # ML Kit Digital Ink
│   ├── CharacterRecognizer.kt                # Caractere por caractere
│   └── StrokeCharacterRecognizer.kt          # Por segmentacao de tracos
│
├── ui/                                       # Telas e componentes
│   ├── folha/                                # Nucleo do Sprint (tela principal)
│   │   ├── FolhaScreen.kt                    # Tela principal (1408 linhas)
│   │   ├── ExerciseField.kt                  # Campo de exercicio (526 linhas)
│   │   ├── InkCanvas.kt                      # Canvas de tinta digital (313 linhas)
│   │   ├── ZoomableCanvas.kt                 # Canvas com zoom
│   │   ├── PlatformMap.kt                    # Mapa de habilidades
│   │   ├── CurriculumTreeSelector.kt         # Seletor de curriculo
│   │   ├── GeometricDiagram.kt               # Diagramas geometricos (920 linhas)
│   │   ├── LatexRenderer.kt                  # Renderizacao LaTeX (108 linhas)
│   │   ├── AnswerPad.kt                      # Teclado numerico
│   │   ├── NoteCanvas.kt                     # Canvas de anotacoes
│   │   ├── InkToolbar.kt                     # Barra de ferramentas
│   │   ├── FolhaSettingsSheet.kt             # Painel de configuracoes
│   │   ├── SplitHeightHandle.kt              # Alca de redimensionamento
│   │   ├── SplitRatioPrefs.kt                # Preferencias de split
│   │   ├── SimuladoBuilder.kt                # Construtor de simulado
│   │   ├── MathUXHelpers.kt                  # Helpers de UX
│   │   ├── CanvasCaptureModifier.kt          # Captura de tela
│   │   ├── ExportUtils.kt                    # Exportacao
│   │   ├── ImageUtils.kt                     # Utilidades de imagem
│   │   └── SprintNoteSheet.kt                # Notas durante sprint
│   │
│   ├── tabs/                                 # Abas de navegacao
│   │   ├── PenTab.kt                         # Configuracao de caneta
│   │   ├── DashboardTab.kt                   # Painel (920 linhas)
│   │   ├── TreeTab.kt                        # Arvore curricular
│   │   ├── NotesTab.kt                       # Notas
│   │   ├── GesturesTab.kt                    # Gestos
│   │   ├── NotebookTab.kt                    # Caderno
│   │   ├── PaperTab.kt                       # Papel em branco
│   │   └── SimuladoTab.kt                    # Simulado
│   │
│   ├── components/                           # Componentes reutilizaveis
│   │   ├── SessionSummaryScreen.kt           # Resumo de sessao
│   │   ├── ErrorBoundary.kt                  # Limite de erro
│   │   ├── LiveLogDialog.kt                  # Dialog de log ao vivo
│   │   ├── WritingGuideBanner.kt             # Banner de orientacao
│   │   ├── SimuladoPopup.kt                  # Popup de confirmacao
│   │   ├── ShareSheet.kt                     # Compartilhar
│   │   ├── StreakBadge.kt                    # Badge de sequencia
│   │   ├── ThermometerView.kt                # Termometro de progresso
│   │   └── UnlockProgressCard.kt             # Card de desbloqueio
│   │
│   ├── simulado/                             # Telas do Simulado
│   │   ├── SimuladoConfigScreen.kt
│   │   ├── SimuladoExportScreen.kt
│   │   ├── SimuladoSessionScreen.kt
│   │   └── SimuladoResultScreen.kt
│   │
│   ├── canvas/                               # Canvas complexos
│   ├── dashboard/                            # Dashboard
│   ├── drill/                                # Modo treino
│   ├── config/                               # Configuracao
│   ├── calibration/                          # Calibracao
│   ├── onboarding/                           # Onboarding
│   ├── profile/                              # Perfil
│   ├── ranking/                              # Ranking
│   ├── result/                               # Resultados
│   └── summary/                              # Sumario
│
└── viewmodel/                                # ViewModels (StateFlow)
    ├── SessionViewModel.kt                   # ViewModel principal (769 linhas)
    ├── SessionUiReducer.kt                   # Reducer puro (testavel)
    ├── FolhaViewModel.kt                     # Estado da folha (175 linhas)
    ├── DrillViewModel.kt                     # Treino
    ├── SimuladoViewModel.kt                  # Simulado
    └── SprintNote.kt                         # Notas
```

### 2.2 Backend (`backend/`)

```
backend/
├── main.py                      # FastAPI app (87 linhas)
├── db.py                        # Config + conexao (42 linhas)
├── docker-compose.yml           # PostgreSQL + Redis
│
├── models/                      # SQLAlchemy models
│   ├── student.py               # Student (22 linhas)
│   ├── session.py               # Session + SessionConfig (85 linhas)
│   ├── exercise.py              # Exercise (40 linhas)
│   ├── attempt.py               # ExerciseAttempt (64 linhas)
│   ├── streak.py                # Streak
│   ├── track.py                 # Track
│   └── vector.py                # CognitiveVector
│
├── schemas/                     # Pydantic schemas
│   ├── session.py               # SessionConfigIn, SessionStartIn/Out, FolhaOut
│   ├── submit.py                # FieldSubmit, SubmitIn/Out, FieldResult
│   ├── calibration.py           # CalibrationIn/Out
│   └── identify_topic.py        # IdentifyTopicIn/Out
│
├── api/                         # Rotas FastAPI (19 arquivos)
│   ├── session.py               # POST /api/session/start
│   ├── submit.py                # POST /api/session/{id}/submit (529 linhas)
│   ├── activity.py              # GET /api/student/{id}/activity
│   ├── calibration.py           # POST /api/student/{id}/calibrate
│   ├── drill.py                 # GET /api/drill/arithmetic
│   ├── exercise_generation.py   # POST /api/exercises/generate/*
│   ├── export.py                # GET /api/session/{id}/export
│   ├── health.py                # GET /api/health
│   ├── identify_topic.py        # POST /api/identify-topic
│   ├── ml_data.py               # GET /api/ml/dataset (114 linhas)
│   ├── profile.py               # GET /api/profile/{slug}
│   ├── ranking.py               # GET /api/ranking/weekly
│   ├── rhythm.py                # GET /api/student/{id}/rhythm
│   ├── student_analytics.py     # GET /api/student/{id}/error-analysis
│   ├── streak.py                # GET /api/student/{id}/streak
│   ├── telemetry.py             # POST /api/telemetry/sync + WS stream
│   ├── unlock_progress.py       # GET /api/student/{id}/unlock-progress
│   └── error_rag.py             # GET /student/{id}/error-suggestions
│
├── engine/                      # Logica de negocio (backend)
│   ├── adaptive.py              # Engine adaptativa (511 linhas)
│   ├── batch_ocr_validate.py    # OCR em lote (144 linhas)
│   ├── ocr.py                   # OCR individual (70 linhas)
│   ├── text_validate.py         # Validacao textual
│   ├── validators.py            # Validadores matematicos
│   ├── scoring.py               # Pontuacao
│   ├── correction.py            # Correcão
│   ├── mastery.py               # Dominio de habilidades
│   ├── streak.py                # Streak
│   ├── unlock.py                # Desbloqueio
│   ├── arithmetic_drill.py      # Treino aritmetico
│   ├── focus_expansion.py       # Expansao de foco
│   ├── fsrs_scheduler.py        # FSRS spaced repetition
│   ├── exercise_library.py      # Biblioteca de exercicios
│   ├── error_pattern_rag.py     # RAG de erros (469 linhas)
│   ├── rhythm.py                # Ritmo
│   └── vector.py                # Vetores cognitivos
│
├── agents/                      # Agentes de IA
│   ├── exercise_creator_agent.py # Criacao de exercicios via LLM
│   ├── feedback_agent.py         # Feedback automatico
│   ├── progression_agent.py      # Progression advisor
│   └── scoring_agent.py          # Scoring engine
│
├── seed/                        # Seeds do banco
│   ├── exercises.py             # Exercicios pre-programados (427 linhas)
│   ├── expand_modular_trig.py   # Expansao trigonometria modular
│   ├── generate_exercises.py    # Gerador de seed (596 linhas)
│   └── tracks.py                # Trilhas de aprendizado
│
├── migrations/                  # Alembic (16 migrations)
├── tests/                       # Testes (18 arquivos)
└── scripts/                     # Scripts utilitarios
    ├── generate_exercises.py   # Geracao via LLM (851 linhas)
    ├── simulate_user_flow.py   # Simulacao de fluxo
    └── smoke_backend.py        # Teste de fumaca
```

---

## 3. MODELOS DE DADOS

### 3.1 Android (Kotlin)

#### `SessionConfig` — Configuração de sessão
| Campo | Tipo | Descrição |
|-------|------|-----------|
| subject | String = "math" | Matéria |
| showThermometer | Boolean = true | Mostrar termômetro |
| background | String = "lightblue" | Fundo |
| penColor | String = "#1a1a1a" | Cor da caneta |
| durationMode | String = "unlimited" | unlimited/timed/pages |
| durationLimitMs | Int? = null | Limite de tempo (ms) |
| exercisesPerPage | Int = 5 | Exercícios por página |
| blindMode | Boolean = false | Sem feedback durante |
| requireCorrectToAdvance | Boolean = false | Só avança se acertar |
| simuladoRulesJson | String? = null | Regras do simulado |
| guideMode | String = "nenhuma" | Modo de guia |
| penWidth | Float = 2.2f | Grossura da caneta |
| *mais 15 campos* | | Dificuldade, dígitos, etc |

#### `FolhaField` — Um campo de exercício na folha
| Campo | Tipo | Descrição |
|-------|------|-----------|
| fieldIndex | Int | Índice do campo |
| exerciseId | String | ID do exercício |
| statement | String | Enunciado |
| skillTags | List\<String\> | Tags de habilidade |
| expectedAnswer | String? = null | Resposta esperada |
| canvasMode | String = "calculation" | Modo do canvas |
| answerType | String = "numeric" | Tipo de resposta |
| validatorType | String = "exact" | Tipo de validação |

#### `Folha` — Uma página de exercícios
| Campo | Tipo | Descrição |
|-------|------|-----------|
| folhaId | String | ID da folha |
| pageIndex | Int | Índice da página |
| difficulty | Double | Dificuldade |
| fields | List\<FolhaField\> | Campos |

#### `ProceduralExercise` — Exercício gerado pela engine
| Campo | Tipo | Descrição |
|-------|------|-----------|
| id | String | UUID |
| statement | String | Enunciado |
| expectedAnswer | String | Resposta esperada |
| primarySkill | String | Habilidade principal |
| difficulty | Double | Dificuldade |
| templateId | String | Template usado |
| validatorType | String = "exact" | Validador |

#### `SessionStatus` (enum)
GESTURE_ONBOARDING → CONFIG → CALIBRATION → DASHBOARD → ACTIVE → SUBMITTING → RESULT → SESSION_SUMMARY → FINISHED

### 3.2 Backend (Python/SQLAlchemy)

#### `Student` (`students`)
| Campo | Tipo | Descrição |
|-------|------|-----------|
| id | UUID PK | ID do aluno |
| name | String(255) | Nome |
| age | Int? | Idade |
| role | String(20) | "student" ou "contributor" |
| xp_total | Int = 0 | XP total |

#### `Session` (`sessions`)
| Campo | Tipo | Descrição |
|-------|------|-----------|
| id | UUID PK | ID da sessão |
| student_id | UUID FK | Aluno |
| config_id | UUID FK | Config |
| status | String = "active" | Estado |
| exercise_count | Int = 0 | Total de exercícios |
| session_accuracy | Float? | Precisão |
| competitive_score | Int = 0 | Pontuação competitiva |
| duration_ms | Int? | Duração |

#### `SessionConfig` (`session_configs`)
| Campo | Tipo | Descrição |
|-------|------|-----------|
| id | UUID PK | ID |
| student_id | UUID FK | Aluno |
| subject | String = "math" | Matéria |
| config_json | JSONB | Config completa |
| *mais campos* | | Mesmos do Android |

#### `Exercise` (`exercises`)
| Campo | Tipo | Descrição |
|-------|------|-----------|
| id | UUID PK | ID |
| statement | Text | Enunciado |
| expected_answer | String | Resposta |
| difficulty | Float(1.0-10.0) | Dificuldade |
| skill_tags | ARRAY[String] | Tags |
| estimated_time_ms | Int? | Tempo estimado |

#### `ExerciseAttempt` (`exercise_attempts`)
| Campo | Tipo | Descrição |
|-------|------|-----------|
| id | UUID PK | ID |
| session_id | UUID FK | Sessão |
| student_id | UUID FK | Aluno |
| exercise_id | UUID FK | Exercício |
| field_index | Int | Índice |
| is_correct | Boolean | Correto? |
| recognized_answer | String? | Resposta reconhecida |
| score | Int | Pontuação |
| stroke_count | Int? | Qtde de traços |
| cognitive_vector | JSONB? | Vetor cognitivo |

---

## 4. ENDPOINTS DA API

### 4.1 Sessão

| Método | Rota | Descrição |
|--------|------|-----------|
| POST | `/api/session/start` | Iniciar sessão |
| POST | `/api/session/{session_id}/submit` | Submeter folha |

### 4.2 Aluno

| Método | Rota | Descrição |
|--------|------|-----------|
| GET | `/api/student/{student_id}/activity` | Histórico de atividade |
| GET | `/api/student/{student_id}/skill-progress` | Progresso por habilidade |
| GET | `/api/student/{student_id}/sessions` | Sessões do aluno |
| GET | `/api/student/{student_id}/timeline` | Timeline |
| GET | `/api/student/{student_id}/streak` | Sequência de dias |
| GET | `/api/student/{student_id}/rhythm` | Ritmo de estudo |
| GET | `/api/student/{student_id}/error-analysis` | Análise de erros |
| GET | `/api/student/{student_id}/fragility-heatmap` | Mapa de fragilidades |
| GET | `/api/student/{student_id}/review-plan` | Plano de revisão |
| POST | `/api/student/{student_id}/calibrate` | Calibrar dificuldade |
| GET | `/api/student/{student_id}/unlock-progress` | Progresso de desbloqueio |

### 4.3 Treino

| Método | Rota | Descrição |
|--------|------|-----------|
| GET | `/api/drill/arithmetic` | Treino aritmético |
| POST | `/api/drill/flush` | Finalizar treino |
| GET | `/api/student/{student_id}/error-suggestions` | Sugestões de erro |

### 4.4 Exercícios

| Método | Rota | Descrição |
|--------|------|-----------|
| POST | `/api/exercises/generate/openai` | Gerar com OpenAI |
| POST | `/api/exercises/generate/gemini` | Gerar com Gemini |
| POST | `/api/exercises/generate/local` | Gerar local (Ollama) |
| POST | `/api/identify-topic` | Identificar tópico por imagem |

### 4.5 Dados

| Método | Rota | Descrição |
|--------|------|-----------|
| GET | `/api/ml/dataset` | Dataset completo (CSV/JSON) |
| GET | `/api/profile/{slug}` | Perfil público |
| GET | `/api/session/{session_id}/export` | Exportar sessão |
| GET | `/api/ranking/weekly` | Ranking semanal |
| GET | `/api/ranking/arena/weekly` | Ranking arena semanal |

### 4.6 Telemetria

| Método | Rota | Descrição |
|--------|------|-----------|
| POST | `/api/telemetry/sync` | Sincronizar telemetria |
| WS | `/api/telemetry/stream` | Stream de telemetria |

### 4.7 Utilitários

| Método | Rota | Descrição |
|--------|------|-----------|
| GET | `/api/health` | Health check |
| GET | `/` | Root |

---

## 5. TELAS DO APP (Android)

### 5.1 Navegação Principal (MainActivity)

```
TopRail (abas horizontais)
├── Ajustes (PenTab + NotebookTab + GesturesTab)
├── Painel (DashboardTab)
├── Árvore (TreeTab)
├── Sprint (ZoomableCanvas + FolhaScreen) ← TELA PRINCIPAL
├── Notas (NotesTab)
└── Simulado (SimuladoTab)
```

### 5.2 Tela Sprint (FolhaScreen)

A tela principal do app. Layout:

```
┌─────────────────────────────────────┐
│  Barra Superior                      │
│  [Piloto Auto] [Seletor de Matéria] │
├─────────────────────────────────────┤
│                                     │
│  Grade de Exercícios (35% altura)   │
│  ┌──────────────────────────────┐   │
│  │  ExerciseField (compact mode) │   │
│  │  25 + 30 = [?]               │   │
│  └──────────────────────────────┘   │
│                                     │
├─────────────────────────────────────┤
│                                     │
│  Rascunho Global (65% altura)       │
│  ┌──────────────────────────────┐   │
│  │  InkCanvas (scratch)         │   │
│  │                             │   │
│  │  [Desfazer]  [Apagar Tela]  │   │
│  └──────────────────────────────┘   │
│                                     │
│  ┌──────┐                           │
│  │Enter │ ◄── Botão para avançar    │
│  │Square│                           │
│  └──────┘                           │
└─────────────────────────────────────┘
```

**Componentes:**
- `ZoomableCanvas` — Pinça-para-zoom + mapa de habilidades
- `FolhaScreen` — Layout 35/65 com grade de exercícios + rascunho
- `ExerciseField` — Enunciado + canvas de resposta (compacto ou normal)
- `InkCanvas` — Canvas de tinta com suporte a caneta, borracha, undo/redo
- `EnterSquare` — Botão flutuante para submeter/avançar
- `SprintScrollConfigPage` — Configuração de matéria/densidade

### 5.3 Tela Simulado (SimuladoTab)

```
CONFIG → EXPORT → REDIRECIONA PARA SPRINT
```

1. **CONFIG**: Escolhe número de questões (5/10/15/20) + tempo (min)
2. **EXPORT**: SimuladoExportScreen — Exportar gabarito ou "Fazer no app"
3. **REDIR**: Cria sessão real → redireciona para tab Sprint com os exercícios do simulado (blindMode=true)

### 5.4 ExerciseField (modos)

| Modo | Uso | Layout |
|------|-----|--------|
| `isCompact=true` | Grade (vários na tela) | Row: enunciado + caixa resposta 260dp |
| `isCompact=false` | Tela cheia | Canvas rascunho + enunciado flutuante + caixa resposta |

### 5.5 InkCanvas

Canvas de tinta com:
- Suporte a caneta (stylus) + toque + mouse
- Borracha (botão ou botão da caneta)
- Undo/redo por sinal (LaunchedEffect)
- Modos de guia: "lined" (linhas), "dots" (pontos), "single" (linha única)
- Sincronização de strokes via callback `onSyncStrokes`

---

## 6. ENGINES

### 6.1 ProceduralEngine (local, Android)

Gera exercícios matematicamente sem banco de dados ou LLM.

**Habilidades suportadas** (34+):
| Grupo | Habilidades |
|-------|-------------|
| Aritmética | soma_subtracao, multiplicacao_divisao |
| Álgebra | equacoes_quadraticas, polinomios, fatoracao, inequacoes, funcoes |
| Geometria | plana, espacial, analitica, trigonometria |
| Cálculo | limites, derivadas, integrais |
| Proporções | regra_de_3, porcentagem, juros |
| Lógica | conectivos, tabela_verdade, quantificadores |
| Estatística | media, mediana, desvio_padrao, probabilidade |
| Teoria dos números | mmc_mdc, divisibilidade, primos |
| Matemática computacional | erro, otimização, sistemas dinâmicos |

**Geração:** Baseada em MMR (Measure of Mathematical Readiness), dificuldade progressiva.

### 6.2 DeterministicValidator

Valida resposta do usuário contra gabarito. Modos:
- `"exact"` — string match + numérico + equação
- `"numeric"` — valor numérico com tolerância
- `"regex"` — expressão regular
- `"fraction"` — fração
- `"equation"` — equação (x = ...)

**Regra de segurança:** resposta precisa ter pelo menos o mesmo número de dígitos que o gabarito.

### 6.3 BKT Engine (MathBktEngine)

Bayesian Knowledge Tracing para cada habilidade:
- `p_learn` — 0.15 (probabilidade de aprender)
- `p_guess` — 0.10 (probabilidade de chutar)
- `p_slip` — 0.05 (probabilidade de errar sabendo)
- `p_init` — 0.20 (probabilidade inicial de saber)

### 6.4 Backend Engines

| Engine | Descrição |
|--------|-----------|
| `adaptive.py` | Engine adaptativa: seleciona próximo exercício baseado em desempenho, memória de habilidade, vetores cognitivos |
| `batch_ocr_validate.py` | OCR em lote via OpenAI Vision (gpt-4o-mini) |
| `ocr.py` | OCR individual via Claude Haiku |
| `text_validate.py` | Validação textual com sympy |
| `scoring.py` | Pontuação por exercício |
| `fsrs_scheduler.py` | FSRS (Free Spaced Repetition Scheduler) |
| `mastery.py` | Cálculo de domínio por habilidade |
| `vector.py` | Geração de vetores cognitivos (17 dimensões) |

---

## 7. RECONHECIMENTO DE ESCRITA

### 7.1 Arquitetura

```
InkCanvas (strokes)
    │
    ▼
onSyncAnswer
    │
    ▼
MlKitRecognizer.recognize(strokes, expected)
    │
    ├── strokesToBitmap() → ML Kit TextRecognition OCR
    │       │
    │       └── rawText → return (sem postProcess)
    │
    └── fallback: null
```

### 7.2 MlKitRecognizer (ativo)

Usa Google ML Kit `TextRecognition` (latin OCR). Renderiza strokes em bitmap branco e reconhece o texto. Retorna raw text SEM postProcess bloqueador.

### 7.3 Reconhecedores alternativos (não ativos)

| Recognizer | Status | Descrição |
|-----------|--------|-----------|
| DigitalInkMathRecognizer | ⛔ Não ativo | Tentativa com modelo de símbolos (não baixado no emulador) |
| CharacterRecognizer | ⛔ Não ativo | Caractere por caractere (segmentação imprecisa) |
| IinkRecognizer | ⛔ Stub | MyScript iink SDK (requer certificado) |

---

## 8. FLUXO COMPLETO

### 8.1 Sprint (modo normal)

```
1. Abre app → MainActivity.onCreate()
2. SessionViewModel.init() → startSessionFromDashboard()
3. localRepository.startSession() → cria sessão na DB + primeira folha
4. UI: SprintTab → ZoomableCanvas → FolhaScreen
5. Usuário escreve no InkCanvas (rascunho + resposta)
6. Pressiona Enter → dispatchKeyEvent/onKeyListener
7. doAdvance() → submitFolha(folhaState)
7a. Usuário aperta botão verde (✓) à direita do campo de resposta
7b. onConfirmStroke → MlKitRecognizer.recognize(strokes, expectedAnswer)
7c. Resultado syncado como typedAnswer no FolhaViewModel
7d. Enter → activateField(nextFieldIndex)
8. Último exercício: localRepository.submitFolha() → DeterministicValidator.evaluate()
9. Resultado (RESULT) → feedback: verde/vermelho
10. Avança para próxima folha ou finaliza sessão
```

### 8.2 Simulado

```
1. Tab SIMULADO → SimuladoTab (screenMode = "config")
2. Usuário escolhe questões + tempo
3. Gera → ProceduralEngine.generate() N vezes
4. Export → SimuladoExportScreen (exportar gabarito ou fazer no app)
5. "Fazer no app" →
   a. Gera sessionId sincrono
   b. _setSimuladoState(folha, simConfig)
   c. Cria sessão na DB em background
   d. onGoToSprint() → tab SPRINT
6. Sprint renderiza FolhaScreen com os exercícios do simulado + blindMode
7. Usuário faz a prova (cronômetro rodando)
7a. Escreve resposta no canvas → aperta ✓ verde → reconhece → Enter próximo
8. Finaliza → tela de revisão com gabarito + exportar resultado
```

### 8.3 Ciclo de estados (SessionStatus)

```
GESTURE_ONBOARDING → CONFIG → CALIBRATION → DASHBOARD
                                                      │
                                                      ▼
                                                 ACTIVE ←──────┐
                                                   │            │
                                                   ▼            │
                                              SUBMITTING        │
                                                   │            │
                                                   ▼            │
                                               RESULT ──────────┘
                                                   │ (se ultima pagina)
                                                   ▼
                                           SESSION_SUMMARY
                                                   │
                                                   ▼
                                              FINISHED
```

---

## 9. BANCO DE DADOS

### 9.1 Local (Android — Room/SQLite)

| Banco | Arquivo | Conteúdo |
|-------|---------|----------|
| Catalog | `exercise_catalog.db` | Catálogo de exercícios (seed SQL) |
| Runtime | Room runtime | Sessões, tentativas, streaks, vetores |

### 9.2 Servidor (PostgreSQL)

**Tabelas:** students, sessions, session_configs, exercises, exercise_attempts, pen_events, cognitive_vectors, student_skill_memory, streaks, tracks, exercise_library

---

## 10. SEGURANÇA

| Risco | Status |
|-------|--------|
| API keys no frontend | ✅ Seguro (nenhuma no código) |
| Backend sem autenticação | 🔴 10 rotas críticas sem auth |
| CORS aberto (`allow_origins=["*"]`) | 🟠 Alto |
| Rate limiting | 🔴 Ausente |
| SQL injection | ✅ ORM parametrizado |
| .env no .gitignore | ✅ Configurado |

---

## 11. ESPECIFICAÇÃO TÉCNICA

| Item | Valor |
|------|-------|
| Min SDK | 26 (Android 8.0) |
| Target SDK | 35 |
| Compose BOM | 2024.10.00 |
| Kotlin | 2.0+ |
| Room | 2.8.4 |
| ML Kit Text | 16.0.0 |
| ML Kit Digital Ink | 18.1.0 |
| Backend Python | 3.11+ |
| Backend Framework | FastAPI + Uvicorn |
| DB Servidor | PostgreSQL 15+ |
| DB Local | SQLite via Room |
| OCR Cloud | OpenAI gpt-4o-mini / Claude Haiku |
| Docker | docker-compose.yml (PostgreSQL + Redis) |
