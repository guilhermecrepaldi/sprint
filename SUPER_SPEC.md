# SUPER SPEC v2.0 — "Strava da Matemática"
# Math Ink Vector Memory Engine

> STATUS: HISTORICO. Para a fonte curta e atual do projeto SPRINT, leia primeiro `AGENTS.md` e `docs/README.md`.
> Este arquivo continua util como contexto de arquitetura original, mas pode estar defasado em relacao a Sprint limpa, scrolls, densidade e zoom exato.

> Este documento substitui e expande o SPEC.txt original.
> É auto-suficiente: qualquer IA ou dev pode começar a construção lendo só este arquivo.

---

## 1. Visão em Uma Frase

App de treino matemático para ensino médio onde a caneta é sensor de aprendizagem:
cada traço vira dado, vetor cognitivo e memória de longo prazo.

---

## 2. Público-Alvo

Adolescentes 13–17 anos. Foco: vestibular e alta performance algébrica.
Design: minimalista, sem gamificação infantil. Estética "Deep Work".

---

## 3. Filosofia Técnica

- **Thin Client:** o app Android só captura e exibe. Zero OCR, zero correção no device.
- **Result-Only Correction:** apenas a resposta final (escrita em campo demarcado) é OCR'd.
- **Field-Based Layout:** a folha tem N campos numerados. O sistema sabe onde está cada resposta antes de ler.
- **Determinístico sempre que possível:** correção, score, seleção de exercícios por regra. IA só entra para OCR e diagnóstico narrativo.

---

## 4. O Loop Principal

```
[SessionConfig] → [Folha renderizada com N campos]
→ [Aluno escreve dentro dos campos]
→ [Submit: imagem de cada campo + telemetria de caneta]
→ [Backend: OCR → SymPy → Score → Vetor → Memória → Próxima folha]
→ [App exibe correção + termômetro]
→ repete até fim da sessão
```

---

## 5. Session Config ("Folha Editável")

Antes de iniciar, o aluno configura a sessão. Essas configurações governam todo o comportamento da sessão.

### 5.1 Campos da Session Config

| Campo | Tipo | Padrão | Descrição |
|---|---|---|---|
| `show_thermometer` | bool | true | Exibe termômetro de performance durante a sessão |
| `background` | enum | `white` | `white` ou `dark` |
| `pen_color` | hex string | `#1a1a1a` | Cor da caneta no canvas |
| `duration_mode` | enum | `unlimited` | `unlimited`, `timed`, `pages` |
| `duration_limit_ms` | int | null | Duração em ms (ex: 7200000 = 2h). Só se `timed` |
| `pages_limit` | int | null | Qtd de folhas. Só se `pages` |
| `difficulty_progression` | enum | `arithmetic` | `arithmetic` ou `geometric` |
| `difficulty_start` | float | 2.0 | Dificuldade inicial (escala 1.0–10.0) |
| `difficulty_step` | float | 0.5 | Incremento aritmético por folha |
| `difficulty_ratio` | float | 1.15 | Multiplicador geométrico por folha |
| `restart_on_avg` | float | null | Se média das últimas N respostas >= valor, reinicia ciclo |
| `restart_window` | int | 10 | Janela de exercícios para calcular a média do restart |
| `exercises_per_page` | int | 5 | Exercícios por folha (folha 5x padrão) |

### 5.2 Comportamento do Restart

Se `restart_on_avg` = 7.0 e `restart_window` = 10:
- O sistema calcula a média de score normalizado (0–10) dos últimos 10 exercícios.
- Se média >= 7.0 → reinicia a progressão de dificuldade do início, mas aumenta o `difficulty_start` em 0.5.
- Isso cria um ciclo progressivo: o aluno "passa de fase" automaticamente.

### 5.3 Termômetro

Indicador visual com 3 dimensões combinadas:
- Acerto (peso 0.5)
- Velocidade normalizada (peso 0.3)
- Fluidez (peso 0.2)

Escala 0.0–1.0. Exibido em tempo real após cada exercício.
Se `show_thermometer = false`, o aluno não vê feedback durante a sessão (modo "blind training").

---

## 6. Arquitetura

```
Android App (Kotlin)
│
├── SessionConfigScreen  → POST /api/session/start
├── FolhaScreen          → renderiza campos, captura stylus
│   └── InkCanvas        → MotionEvent → PenEvent stream
│
├── POST /api/session/{id}/submit  → envia imagens + telemetria
└── WebSocket /api/telemetry/stream → Ghost Racing (fase futura)

Backend (Python / FastAPI)
│
├── POST /api/session/start     → cria sessão, retorna config + primeira folha
├── POST /api/session/{id}/submit → recebe folha, retorna correções + próxima
├── WS /api/telemetry/stream    → eventos em tempo real
│
├── engine/
│   ├── ocr.py          → Claude Vision API → LaTeX
│   ├── correction.py   → SymPy → is_correct + error_type
│   ├── scoring.py      → Time-Decay Score
│   ├── vector.py       → CognitiveVector generation
│   └── adaptive.py     → próxima folha baseada em memória + config
│
└── models/ (SQLAlchemy)
    students, exercises, sessions, session_configs,
    exercise_attempts, pen_events, cognitive_vectors, student_skill_memory

PostgreSQL + Redis (cache de sessão ativa)
```

---

## 7. Banco de Dados — Schema Completo

### 7.1 students
```sql
CREATE TABLE students (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(255) NOT NULL,
    age         INT,
    created_at  TIMESTAMP DEFAULT NOW()
);
```

### 7.2 exercises
```sql
CREATE TABLE exercises (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    statement        TEXT NOT NULL,
    expected_answer  TEXT NOT NULL,  -- LaTeX ou plain (ex: "x = 3", "1/2")
    skill_tags       TEXT[] NOT NULL,
    difficulty       FLOAT NOT NULL CHECK (difficulty BETWEEN 1.0 AND 10.0),
    estimated_time_ms INT,
    source_library   VARCHAR(100)
);
```

### 7.3 session_configs
```sql
CREATE TABLE session_configs (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id              UUID REFERENCES students(id),
    show_thermometer        BOOLEAN DEFAULT true,
    background              VARCHAR(10) DEFAULT 'white',
    pen_color               VARCHAR(7) DEFAULT '#1a1a1a',
    duration_mode           VARCHAR(20) DEFAULT 'unlimited',
    duration_limit_ms       INT,
    pages_limit             INT,
    difficulty_progression  VARCHAR(20) DEFAULT 'arithmetic',
    difficulty_start        FLOAT DEFAULT 2.0,
    difficulty_step         FLOAT DEFAULT 0.5,
    difficulty_ratio        FLOAT DEFAULT 1.15,
    restart_on_avg          FLOAT,
    restart_window          INT DEFAULT 10,
    exercises_per_page      INT DEFAULT 5,
    created_at              TIMESTAMP DEFAULT NOW()
);
```

### 7.4 sessions
```sql
CREATE TABLE sessions (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id          UUID REFERENCES students(id),
    config_id           UUID REFERENCES session_configs(id),
    started_at          TIMESTAMP DEFAULT NOW(),
    ended_at            TIMESTAMP,
    current_difficulty  FLOAT NOT NULL,
    page_count          INT DEFAULT 0,
    exercise_count      INT DEFAULT 0,
    restart_count       INT DEFAULT 0,
    status              VARCHAR(20) DEFAULT 'active'  -- active, paused, finished
);
```

### 7.5 folhas (páginas geradas)
```sql
CREATE TABLE folhas (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id  UUID REFERENCES sessions(id),
    page_index  INT NOT NULL,
    difficulty  FLOAT NOT NULL,
    created_at  TIMESTAMP DEFAULT NOW()
);
```

### 7.6 exercise_attempts
```sql
CREATE TABLE exercise_attempts (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    folha_id                UUID REFERENCES folhas(id),
    session_id              UUID REFERENCES sessions(id),
    student_id              UUID REFERENCES students(id),
    exercise_id             UUID REFERENCES exercises(id),
    field_index             INT NOT NULL,
    recognized_answer       TEXT,
    expected_answer         TEXT NOT NULL,
    is_correct              BOOLEAN,
    score                   INT,
    total_time_ms           INT,
    time_to_first_stroke_ms INT,
    stroke_count            INT DEFAULT 0,
    erase_count             INT DEFAULT 0,
    pause_count             INT DEFAULT 0,
    average_pressure        FLOAT,
    average_velocity        FLOAT,
    error_type              VARCHAR(50),
    ocr_confidence          FLOAT,
    created_at              TIMESTAMP DEFAULT NOW()
);
```

### 7.7 pen_events
```sql
CREATE TABLE pen_events (
    id          BIGSERIAL PRIMARY KEY,
    attempt_id  UUID REFERENCES exercise_attempts(id),
    ts          BIGINT NOT NULL,  -- ms desde início do exercício
    x           FLOAT NOT NULL,
    y           FLOAT NOT NULL,
    pressure    FLOAT,
    tilt        FLOAT,
    velocity    FLOAT,
    event_type  VARCHAR(20) NOT NULL  -- stroke_start, stroke_move, stroke_end, erase
);
```

### 7.8 cognitive_vectors
```sql
CREATE TABLE cognitive_vectors (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id          UUID REFERENCES students(id),
    attempt_id          UUID REFERENCES exercise_attempts(id),
    correctness         FLOAT,
    speed_score         FLOAT,
    hesitation_score    FLOAT,
    fluency_score       FLOAT,
    pressure_stability  FLOAT,
    erase_score         FLOAT,
    difficulty_level    FLOAT,
    skill_vector        JSONB,
    fatigue_index       FLOAT,
    created_at          TIMESTAMP DEFAULT NOW()
);
```

### 7.9 student_skill_memory
```sql
CREATE TABLE student_skill_memory (
    student_id   UUID REFERENCES students(id),
    skill        VARCHAR(100),
    accuracy     FLOAT DEFAULT 0.5,
    fluency      FLOAT DEFAULT 0.5,
    retention    FLOAT DEFAULT 0.5,
    fatigue_avg  FLOAT DEFAULT 0.0,
    status       VARCHAR(30) DEFAULT 'novo',
    last_updated TIMESTAMP DEFAULT NOW(),
    PRIMARY KEY (student_id, skill)
);
```

---

## 8. Contratos de API

### 8.1 POST /api/session/start

**Request:**
```json
{
  "student_id": "stu-uuid",
  "config": {
    "show_thermometer": true,
    "background": "white",
    "pen_color": "#1a1a1a",
    "duration_mode": "timed",
    "duration_limit_ms": 7200000,
    "difficulty_progression": "geometric",
    "difficulty_start": 2.0,
    "difficulty_ratio": 1.15,
    "restart_on_avg": 7.0,
    "restart_window": 10,
    "exercises_per_page": 5
  }
}
```

**Response:**
```json
{
  "session_id": "ses-uuid",
  "config_id": "cfg-uuid",
  "first_folha": {
    "folha_id": "folha-uuid",
    "page_index": 0,
    "difficulty": 2.0,
    "fields": [
      {
        "field_index": 0,
        "exercise_id": "ex-uuid",
        "statement": "Resolva: 2x² - 8 = 0",
        "skill_tags": ["equacao_2_grau"],
        "estimated_time_ms": 45000
      }
    ]
  }
}
```

---

### 8.2 POST /api/session/{session_id}/submit

**Request:**
```json
{
  "folha_id": "folha-uuid",
  "submitted_at_ms": 1710000000000,
  "fields": [
    {
      "field_index": 0,
      "exercise_id": "ex-uuid",
      "image_base64": "<base64 do crop do campo>",
      "total_time_ms": 38000,
      "time_to_first_stroke_ms": 3200,
      "pen_events": [
        {"ts": 0, "x": 120.0, "y": 80.0, "pressure": 0.7, "tilt": 0.2, "velocity": 1.1, "event_type": "stroke_start"},
        {"ts": 150, "x": 125.0, "y": 82.0, "pressure": 0.72, "tilt": 0.21, "velocity": 1.3, "event_type": "stroke_move"}
      ]
    }
  ]
}
```

**Response:**
```json
{
  "results": [
    {
      "field_index": 0,
      "recognized_answer": "x = \\pm 2",
      "expected_answer": "x = \\pm 2",
      "is_correct": true,
      "score": 920,
      "error_type": null,
      "vector": {
        "correctness": 1.0,
        "speed_score": 0.78,
        "hesitation_score": 0.3,
        "fluency_score": 0.82,
        "pressure_stability": 0.71,
        "erase_score": 1.0,
        "difficulty_level": 2.0,
        "skill_vector": {"equacao_2_grau": 1.0},
        "fatigue_index": 0.1
      }
    }
  ],
  "page_score": 847,
  "thermometer": {
    "value": 0.81,
    "trend": "up"
  },
  "restart_triggered": false,
  "session_status": "active",
  "next_folha": {
    "folha_id": "folha-uuid-2",
    "page_index": 1,
    "difficulty": 2.3,
    "fields": [...]
  }
}
```

---

### 8.3 WebSocket /api/telemetry/stream

**Client → Server (eventos em tempo real):**
```json
{
  "type": "pen_event",
  "session_id": "ses-uuid",
  "field_index": 0,
  "ts": 12345,
  "x": 438.2,
  "y": 192.6,
  "pressure": 0.72,
  "tilt": 0.31,
  "velocity": 1.42,
  "event_type": "stroke_move"
}
```

**Server → Client:**
```json
{
  "type": "ghost_update",
  "ghost_x": 440.1,
  "ghost_y": 190.2,
  "ghost_ahead": true,
  "time_delta_ms": -230
}
```

---

## 9. Engines do Backend

### 9.1 engine/ocr.py

```python
# Responsabilidade: receber image_base64 de um campo, retornar LaTeX da resposta
# Usa Claude Vision API (claude-haiku-4-5 para custo, claude-sonnet-4-6 para precisão)

def extract_answer(image_base64: str, context_hint: str = "") -> dict:
    # Retorna: {"answer_latex": "x = \\pm 2", "confidence": 0.94}
```

**Prompt do Claude Vision:**
```
Você está analisando a foto de um campo de resposta de um exercício de matemática
escrito à mão por um estudante do ensino médio.

Extraia APENAS a resposta final escrita no campo.
Retorne em formato LaTeX.
Se houver múltiplas escritas, use a última (mais recente / mais abaixo).
Se não conseguir ler, retorne {"answer_latex": null, "confidence": 0.0}.

Retorne JSON: {"answer_latex": "...", "confidence": 0.0-1.0}
```

---

### 9.2 engine/correction.py

```python
# Responsabilidade: validar recognized vs expected usando SymPy
# Lida com equivalências: 1/2 == 0.5, x=2 == 2=x, -2±3 == ±3-2

def validate_answer(recognized_latex: str, expected_latex: str) -> dict:
    # Retorna: {"is_correct": bool, "error_type": str | None}
    # error_type: "tabuada", "fracao", "equacao_2_grau", "sinal", "ordem_operacoes", etc.
```

**Lógica:**
1. Converter LaTeX → SymPy expression
2. `sympy.simplify(recognized - expected) == 0`
3. Se correto → error_type = None
4. Se errado → tentar classificar o erro por categoria

---

### 9.3 engine/scoring.py

```python
# Time-Decay Score: penaliza hesitação, premia velocidade

def compute_score(
    is_correct: bool,
    total_time_ms: int,
    hesitation_ms: int,  # time_to_first_stroke_ms
    difficulty: float,
    estimated_time_ms: int
) -> int:
    if not is_correct:
        return 0
    
    time_ratio = total_time_ms / estimated_time_ms  # 1.0 = exato, >1 = lento
    decay = 1 / (1 + 0.3 * max(0, time_ratio - 1))
    hesitation_penalty = 1 - min(0.3, hesitation_ms / 10000)
    difficulty_bonus = 1 + (difficulty - 1) * 0.1
    
    raw = 1000 * decay * hesitation_penalty * difficulty_bonus
    return int(min(1000, max(0, raw)))
```

---

### 9.4 engine/vector.py

```python
# Gera CognitiveVector a partir de um attempt

def generate_vector(attempt: ExerciseAttempt, events: list[PenEvent]) -> CognitiveVector:
    # correctness: 1.0 se correto, 0.0 se errado
    # speed_score: 1 - (total_time / estimated_time) clampado 0-1
    # hesitation_score: time_to_first_stroke / 5000 clampado 0-1
    # fluency_score: 1 - (pause_count / stroke_count) clampado 0-1
    # pressure_stability: 1 - stddev(pressures) / mean(pressures)
    # erase_score: 1 - min(1, erase_count / 3)
    # fatigue_index: calculado pelo session_order (exercícios tardios têm fatigue maior)
```

---

### 9.5 engine/adaptive.py

```python
# Seleciona próxima folha baseado em memória do aluno + config da sessão

def get_next_folha(session: Session, config: SessionConfig, 
                   skill_memory: dict, recent_scores: list[float]) -> Folha:
    # 1. Verificar restart condition
    # 2. Calcular nova dificuldade (arithmetic ou geometric)
    # 3. Identificar skills fracas do aluno
    # 4. Selecionar N exercícios da biblioteca priorizando skills fracas
    # 5. Retornar Folha com campos demarcados
```

---

## 10. Estrutura de Arquivos do Backend

```
backend/
├── main.py                  # FastAPI app, routers, startup
├── db.py                    # SQLAlchemy engine, SessionLocal, Base
├── requirements.txt
│
├── models/
│   ├── __init__.py
│   ├── student.py
│   ├── exercise.py
│   ├── session.py           # Session + SessionConfig + Folha
│   ├── attempt.py           # ExerciseAttempt + PenEvent
│   └── vector.py            # CognitiveVector + StudentSkillMemory
│
├── schemas/                 # Pydantic request/response models
│   ├── __init__.py
│   ├── session.py
│   └── submit.py
│
├── api/
│   ├── __init__.py
│   ├── session.py           # POST /api/session/start
│   ├── submit.py            # POST /api/session/{id}/submit
│   └── telemetry.py         # WebSocket /api/telemetry/stream
│
├── engine/
│   ├── __init__.py
│   ├── ocr.py               # Claude Vision → LaTeX
│   ├── correction.py        # SymPy validation
│   ├── scoring.py           # Time-Decay Score
│   ├── vector.py            # CognitiveVector generation
│   └── adaptive.py          # Próxima folha
│
├── migrations/              # Alembic
│   └── versions/
│
└── seed/
    └── exercises.py         # Exercícios iniciais para teste
```

---

## 11. Estrutura do App Android

```
app/src/main/java/com/strava_matematica/
│
├── ui/
│   ├── config/
│   │   └── SessionConfigScreen.kt   # UI de configuração pré-sessão
│   ├── folha/
│   │   ├── FolhaScreen.kt           # Tela principal de resolução
│   │   ├── InkCanvas.kt             # Canvas com stylus input
│   │   ├── FieldOverlay.kt          # Demarcação dos campos na tela
│   │   └── ThermometerView.kt       # Indicador de performance
│   └── result/
│       └── PageResultScreen.kt      # Feedback após cada folha
│
├── network/
│   ├── ApiClient.kt                 # Retrofit REST
│   └── TelemetrySocket.kt           # OkHttp WebSocket
│
├── model/
│   ├── SessionConfig.kt
│   ├── Folha.kt
│   ├── PenEvent.kt
│   └── SubmitResult.kt
│
└── viewmodel/
    ├── SessionViewModel.kt
    └── FolhaViewModel.kt
```

---

## 12. Fases de Build

### FASE 1 — Backend Foundation (comece aqui)

**Objetivo:** backend rodando com DB e modelos.

Tarefas:
1. `db.py`: SQLAlchemy engine + `DATABASE_URL` via env var
2. `models/`: todos os modelos SQLAlchemy mapeando o schema da Seção 7
3. Alembic: `alembic init migrations` + primeira migration
4. `main.py`: incluir routers, lifespan, CORS
5. `seed/exercises.py`: inserir 20 exercícios de álgebra básica (nível 2.0–4.0)

Verificação: `alembic upgrade head` sem erros. `GET /` retorna status.

---

### FASE 2 — Session Start (fluxo de config)

**Objetivo:** criar sessão + retornar primeira folha.

Tarefas:
1. `schemas/session.py`: Pydantic models para request/response do start
2. `engine/adaptive.py`: `get_first_folha()` — seleciona N exercícios por dificuldade_start
3. `api/session.py`: `POST /api/session/start` — cria SessionConfig + Session + Folha no DB
4. Retorna `first_folha` no formato da Seção 8.1

Verificação: POST com payload de exemplo retorna folha com 5 exercícios.

---

### FASE 3 — Submit + Correction Engine

**Objetivo:** receber folha preenchida, corrigir, retornar resultado.

Tarefas:
1. `engine/ocr.py`: integrar Claude Vision API. Recebe base64, retorna LaTeX.
2. `engine/correction.py`: SymPy validation. Lida com LaTeX → SymPy expression.
3. `engine/scoring.py`: Time-Decay Score conforme fórmula da Seção 9.3.
4. `schemas/submit.py`: Pydantic models para request/response do submit
5. `api/submit.py`: `POST /api/session/{id}/submit`
   - Para cada campo: OCR → correct → score → salvar attempt + pen_events
   - Retorna `results[]` + `page_score`

Verificação: POST com imagem de "x = 2" retorna `is_correct: true`.

---

### FASE 4 — Cognitive Engine + Adaptive

**Objetivo:** gerar vetores, atualizar memória, selecionar próxima folha.

Tarefas:
1. `engine/vector.py`: `generate_vector()` para cada attempt
2. Salvar `cognitive_vectors` no DB
3. `engine/adaptive.py`: `update_skill_memory()` + `get_next_folha()`
   - Verificar restart condition
   - Calcular nova dificuldade (arithmetic/geometric)
   - Priorizar skills fracas
4. `POST /submit` retorna `next_folha` + `thermometer` + `restart_triggered`

Verificação: após 10 submissões, `student_skill_memory` reflete padrão de erros.

---

### FASE 5 — Android App (MVP)

**Objetivo:** app funcional — config → escrever → ver correção.

Tarefas:
1. `SessionConfigScreen.kt`: toggles e sliders para todos os campos da config
2. `InkCanvas.kt`: captura `MotionEvent` do stylus → lista de `PenEvent`
3. `FieldOverlay.kt`: renderiza N campos demarcados sobre o canvas
4. `FolhaViewModel.kt`: controla estado do campo ativo, timing, submissão
5. `ApiClient.kt`: chamadas REST ao backend
6. `PageResultScreen.kt`: exibe score + correção de cada campo
7. `ThermometerView.kt`: barra de progresso baseada no valor do termômetro

Verificação: ciclo completo — config → escrever em 5 campos → ver correções.

---

### FASE 6 — Ghost Racing (futuro)

**Objetivo:** multiplayer assíncrono contra fantasma.

Tarefas:
1. Gravar trace vetorial das melhores sessões como "ghost"
2. WebSocket transmite ghost_x, ghost_y em tempo real durante resolução
3. UI do app exibe traço do fantasma semitransparente durante escrita
4. Score compara tempo do aluno vs tempo do ghost

---

## 13. Variáveis de Ambiente

```env
DATABASE_URL=postgresql://user:password@localhost:5432/strava_math
REDIS_URL=redis://localhost:6379
ANTHROPIC_API_KEY=sk-ant-...
CLAUDE_OCR_MODEL=claude-haiku-4-5-20251001
```

---

## 14. Dependências (requirements.txt final)

```
fastapi
uvicorn
websockets
sympy
glicko2
redis
pydantic
sqlalchemy
alembic
asyncpg
anthropic
python-multipart
pillow
python-dotenv
```

---

## 15. Exercícios Seed (exemplos para teste)

```json
[
  {"statement": "Resolva: x² - 9 = 0", "expected_answer": "x = \\pm 3", "skill_tags": ["equacao_2_grau"], "difficulty": 2.5},
  {"statement": "Simplifique: (3x²y) / (xy)", "expected_answer": "3x", "skill_tags": ["fracao_algebrica", "simplificacao"], "difficulty": 3.0},
  {"statement": "Calcule: \\frac{2}{3} + \\frac{5}{6}", "expected_answer": "\\frac{3}{2}", "skill_tags": ["fracao"], "difficulty": 2.0},
  {"statement": "Fatore: x² + 5x + 6", "expected_answer": "(x+2)(x+3)", "skill_tags": ["fatoracao"], "difficulty": 3.5},
  {"statement": "Resolva: 2(x - 3) = 4x + 2", "expected_answer": "x = -4", "skill_tags": ["equacao_1_grau", "distributiva"], "difficulty": 2.8}
]
```

---

## 16. Restrições e Decisões de Design

1. **Sem OCR offline.** Todo reconhecimento matemático é feito no backend via Claude Vision.
2. **SymPy para tudo numérico.** IA só entra quando SymPy falha (expressões ambíguas).
3. **Pen events salvos sempre**, mesmo que OCR falhe — os vetores comportamentais têm valor independente.
4. **Redis para sessão ativa** — estado da sessão em memória enquanto ativa, PostgreSQL para histórico.
5. **Folha 5x é o padrão** — 5 campos por folha. Configurável via `exercises_per_page`.
6. **Score máximo: 1000 por exercício.** Normalizado 0–10 para cálculo do termômetro e restart.

---

*Documento gerado em 2026-05-23. Para dúvidas sobre qualquer seção, referenciar SPEC.txt original.*

---

## 17. Atualizações Arquiteturais (v2.1 - Drill Engine & Proof of Work)

*Adendos baseados em definições estratégicas de Produto (MVP 2.1)*

1. **Marathon Mode Exclusivo**: O sistema opera estritamente em "Modo Maratona". Não existe o conceito de resolver "um exercício avulso". Toda interação é tratada como um lote contínuo (*Batch*), avaliando resistência e velocidade.
2. **Deep Work UX**: A interface do aplicativo Android (FolhaScreen) é totalmente minimalista. Sem gamificação infantil, sem sirenes ou cores vermelhas agressivas ao errar. O foco absoluto é o "Estado de Fluxo" (Zen/Deep Work).
3. **Folha Flexível (1 a 30 itens)**: A renderização visual permite desde 1 exercício focado na tela (para cálculos complexos em LaTeX) até 30 questões listadas para simular planilhas de *sprint* rápido, exigindo o mínimo de esforço do usuário para transitar entre questões.
4. **Sprints Algorítmicos On-The-Fly**: Para construir agilidade de sinais (5-2, -5+8), as rotas de API injetarão milhares de cálculos baseados apenas em algoritmos python, sem depender de IA ou banco de dados. UX projetada para auto-submit (o aluno digita e a tela avança no milissegundo seguinte).
5. **Proof of Work & Public Profile**: O Dashboard do usuário consolida os dados de *telemetria de stylus* e performance para gerar um Perfil Público compartilhável. Ele exibirá "Trilhas Concluídas" (Tracks), um *Heatmap* de constância e métricas de foco, agindo como um portfólio matemático técnico.
6. **Arquitetura Modular (Future-Proof)**: O código nasce preparado para expansões B2B/B2C, contendo chaves de roles e relatórios estruturados para, no futuro, acoplar painéis de supervisão para Professores e Pais (multi-licenças).
