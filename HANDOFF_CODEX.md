# HANDOFF — Strava da Matemática
# Para: Codex / Gemini
# Data: 2026-05-23

---

## CONTEXTO

Você está construindo o backend de um app de treino matemático chamado "Strava da Matemática".
Leia `SUPER_SPEC.md` antes de qualquer coisa. Este handoff é o guia de execução.

Stack:
- Python 3.11+
- FastAPI + SQLAlchemy (async) + Alembic
- PostgreSQL + Redis
- Claude Vision API (Anthropic SDK) para OCR
- SymPy para correção matemática

Diretório de trabalho: `D:\LOVE CLASS\backend\`

---

## ESTADO ATUAL

O arquivo `main.py` já existe com um esqueleto mínimo (FastAPI + WebSocket stub).
O `requirements.txt` existe mas está incompleto.

**Você vai:**
1. Atualizar `requirements.txt` com todas as deps
2. Criar toda a estrutura de arquivos descrita na SUPER_SPEC Seção 10
3. Implementar as Fases 1 a 4 da Seção 12, nessa ordem

---

## FASE 1 — Foundation (comece aqui)

### 1.1 Atualizar requirements.txt
```
fastapi
uvicorn[standard]
websockets
sympy
glicko2
redis[hiredis]
pydantic
pydantic-settings
sqlalchemy[asyncio]
alembic
asyncpg
anthropic
python-multipart
pillow
python-dotenv
```

### 1.2 Criar backend/.env.example
```
DATABASE_URL=postgresql+asyncpg://user:password@localhost:5432/strava_math
REDIS_URL=redis://localhost:6379
ANTHROPIC_API_KEY=sk-ant-...
CLAUDE_OCR_MODEL=claude-haiku-4-5-20251001
```

### 1.3 Criar backend/db.py
```python
from sqlalchemy.ext.asyncio import create_async_engine, AsyncSession, async_sessionmaker
from sqlalchemy.orm import DeclarativeBase
from pydantic_settings import BaseSettings

class Settings(BaseSettings):
    database_url: str
    redis_url: str
    anthropic_api_key: str
    claude_ocr_model: str = "claude-haiku-4-5-20251001"
    class Config:
        env_file = ".env"

settings = Settings()

engine = create_async_engine(settings.database_url, echo=False)
AsyncSessionLocal = async_sessionmaker(engine, expire_on_commit=False)

class Base(DeclarativeBase):
    pass

async def get_db():
    async with AsyncSessionLocal() as session:
        yield session
```

### 1.4 Criar todos os models SQLAlchemy

Use o schema exato da SUPER_SPEC Seção 7.
Crie um arquivo por model:
- `models/student.py` → class Student
- `models/exercise.py` → class Exercise
- `models/session.py` → class SessionConfig, Session, Folha
- `models/attempt.py` → class ExerciseAttempt, PenEvent
- `models/vector.py` → class CognitiveVector, StudentSkillMemory
- `models/__init__.py` → importa tudo

Todos os modelos devem importar `Base` de `db.py`.
Use `UUID` como tipo de PK com `default=uuid.uuid4`.
Use `Mapped` e `mapped_column` do SQLAlchemy 2.0.

### 1.5 Configurar Alembic
```bash
alembic init migrations
```
Editar `alembic.ini` para usar `DATABASE_URL` do env.
Editar `migrations/env.py` para importar `Base` e todos os models.
Criar primeira migration: `alembic revision --autogenerate -m "initial"`

### 1.6 Seed de exercícios
Criar `seed/exercises.py` com os 5 exemplos da SUPER_SPEC Seção 15 + mais 15 exercícios variados.
Script deve ser rodável: `python seed/exercises.py`

### 1.7 Atualizar main.py
```python
from contextlib import asynccontextmanager
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from db import engine, Base

@asynccontextmanager
async def lifespan(app: FastAPI):
    async with engine.begin() as conn:
        await conn.run_sync(Base.metadata.create_all)
    yield

app = FastAPI(title="Strava da Matemática API", lifespan=lifespan)
app.add_middleware(CORSMiddleware, allow_origins=["*"], allow_methods=["*"], allow_headers=["*"])

# routers serão incluídos nas fases seguintes
```

**Verificação Fase 1:** `uvicorn main:app --reload` sobe sem erro. `GET /docs` abre. Alembic migrations rodam sem erro.

---

## FASE 2 — Session Start

### 2.1 schemas/session.py
Criar Pydantic models para:
- `SessionConfigIn` — campos da SUPER_SPEC Seção 5.1
- `FolhaField` — {field_index, exercise_id, statement, skill_tags, estimated_time_ms}
- `FolhaOut` — {folha_id, page_index, difficulty, fields: list[FolhaField]}
- `SessionStartIn` — {student_id, config: SessionConfigIn}
- `SessionStartOut` — {session_id, config_id, first_folha: FolhaOut}

### 2.2 engine/adaptive.py — get_first_folha()
```python
async def get_first_folha(db, session_id, difficulty, exercises_per_page, student_id) -> Folha:
    # Buscar N exercícios com difficulty <= difficulty_start + 0.5
    # Priorizar skills fracas do aluno se houver histórico
    # Criar Folha no DB e retornar
```

### 2.3 api/session.py
```python
@router.post("/api/session/start", response_model=SessionStartOut)
async def start_session(body: SessionStartIn, db: AsyncSession = Depends(get_db)):
    # 1. Criar SessionConfig no DB
    # 2. Criar Session no DB (current_difficulty = difficulty_start)
    # 3. Chamar get_first_folha()
    # 4. Retornar SessionStartOut
```

Incluir router no main.py.

**Verificação Fase 2:** POST /api/session/start retorna folha com N exercícios populados.

---

## FASE 3 — Submit + Correction

### 3.1 engine/ocr.py
```python
import anthropic
import base64
from db import settings

client = anthropic.Anthropic(api_key=settings.anthropic_api_key)

async def extract_answer(image_base64: str) -> dict:
    message = client.messages.create(
        model=settings.claude_ocr_model,
        max_tokens=256,
        messages=[{
            "role": "user",
            "content": [
                {
                    "type": "image",
                    "source": {
                        "type": "base64",
                        "media_type": "image/png",
                        "data": image_base64
                    }
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
                    )
                }
            ]
        }]
    )
    import json
    return json.loads(message.content[0].text)
```

### 3.2 engine/correction.py
```python
import sympy
from sympy.parsing.latex import parse_latex

def validate_answer(recognized_latex: str, expected_latex: str) -> dict:
    try:
        rec = parse_latex(recognized_latex)
        exp = parse_latex(expected_latex)
        diff = sympy.simplify(rec - exp)
        is_correct = diff == 0
    except Exception:
        # fallback: comparação de string normalizada
        is_correct = recognized_latex.strip() == expected_latex.strip()
    
    error_type = None
    if not is_correct:
        error_type = classify_error(recognized_latex, expected_latex)
    
    return {"is_correct": is_correct, "error_type": error_type}

def classify_error(recognized: str, expected: str) -> str:
    # Lógica simples de classificação por heurística
    # Expandir com mais regras conforme dados reais aparecerem
    return "desconhecido"
```

### 3.3 engine/scoring.py
Implementar `compute_score()` exatamente conforme fórmula da SUPER_SPEC Seção 9.3.

### 3.4 schemas/submit.py
Criar Pydantic models:
- `PenEventIn`
- `FieldSubmit` — {field_index, exercise_id, image_base64, total_time_ms, time_to_first_stroke_ms, pen_events}
- `SubmitIn` — {folha_id, submitted_at_ms, fields}
- `FieldResult` — {field_index, recognized_answer, expected_answer, is_correct, score, error_type, vector}
- `ThermometerOut` — {value, trend}
- `SubmitOut` — {results, page_score, thermometer, restart_triggered, session_status, next_folha}

### 3.5 api/submit.py
```python
@router.post("/api/session/{session_id}/submit", response_model=SubmitOut)
async def submit_folha(session_id: str, body: SubmitIn, db: AsyncSession = Depends(get_db)):
    results = []
    for field in body.fields:
        # 1. OCR
        ocr_result = await extract_answer(field.image_base64)
        # 2. Buscar exercise no DB
        exercise = await db.get(Exercise, field.exercise_id)
        # 3. Corrigir
        correction = validate_answer(ocr_result["answer_latex"], exercise.expected_answer)
        # 4. Score
        score = compute_score(
            is_correct=correction["is_correct"],
            total_time_ms=field.total_time_ms,
            hesitation_ms=field.time_to_first_stroke_ms,
            difficulty=exercise.difficulty,
            estimated_time_ms=exercise.estimated_time_ms or 45000
        )
        # 5. Salvar ExerciseAttempt + PenEvents no DB
        # 6. Gerar vetor
        vector = generate_vector(attempt, field.pen_events)
        # 7. Salvar CognitiveVector + atualizar StudentSkillMemory
        results.append(FieldResult(...))
    
    # Calcular page_score, termômetro, verificar restart, selecionar next_folha
    return SubmitOut(...)
```

**Verificação Fase 3:** POST /submit com imagem de "x = 3" retorna is_correct=true e score > 0.

---

## FASE 4 — Cognitive Engine + Adaptive

### 4.1 engine/vector.py
Implementar `generate_vector(attempt, pen_events)` conforme SUPER_SPEC Seção 9.4.
Retornar objeto com todos os 9 campos do CognitiveVector.

### 4.2 engine/adaptive.py — update_skill_memory()
```python
async def update_skill_memory(db, student_id, skill_tags, vector):
    for skill in skill_tags:
        # Buscar ou criar StudentSkillMemory para (student_id, skill)
        # Atualizar accuracy, fluency com média exponencial:
        #   new_val = 0.8 * old_val + 0.2 * new_observation
        # Atualizar status baseado em accuracy:
        #   >= 0.85 → "automatizado"
        #   0.70–0.85 → "em_desenvolvimento"
        #   0.50–0.70 → "instavel"
        #   < 0.50 → "fraco"
```

### 4.3 engine/adaptive.py — check_restart()
```python
def check_restart(recent_scores: list[float], config: SessionConfig) -> bool:
    if config.restart_on_avg is None:
        return False
    if len(recent_scores) < config.restart_window:
        return False
    window = recent_scores[-config.restart_window:]
    avg = sum(window) / len(window)
    return avg >= config.restart_on_avg
```

### 4.4 engine/adaptive.py — get_next_folha()
```python
async def get_next_folha(db, session, config, recent_scores, skill_memory) -> Folha:
    restart = check_restart(recent_scores, config)
    
    if restart:
        # Reinicia dificuldade + incrementa difficulty_start em 0.5
        new_difficulty = config.difficulty_start + 0.5
        session.restart_count += 1
    else:
        # Progressão normal
        if config.difficulty_progression == "arithmetic":
            new_difficulty = session.current_difficulty + config.difficulty_step
        else:  # geometric
            new_difficulty = session.current_difficulty * config.difficulty_ratio
    
    new_difficulty = min(10.0, new_difficulty)
    session.current_difficulty = new_difficulty
    
    # Priorizar exercises das skills fracas do aluno
    weak_skills = [s for s, m in skill_memory.items() if m["status"] in ("fraco", "instavel")]
    # Buscar exercícios com skill_tags intersectando weak_skills e difficulty próxima
    # Fallback: qualquer exercício na dificuldade atual
    
    return criar_folha(db, session, exercises, new_difficulty)
```

**Verificação Fase 4:** após 10 submissões com erros em "fracao", a próxima folha prioriza exercícios de fração.

---

## REGRAS GERAIS DE IMPLEMENTAÇÃO

1. **Async everywhere.** Todas as funções de DB são `async`. Use `await db.execute(select(...))`.
2. **Sem prints em produção.** Use `logging` do Python.
3. **Erros de OCR não param o fluxo.** Se OCR falhar, salvar `recognized_answer = null`, `is_correct = false`, continuar.
4. **Pen events em lote.** Inserir todos os pen_events de um campo com `db.add_all()`, não um a um.
5. **Redis para sessão ativa.** Salvar `session_state` em Redis (TTL 3h) para evitar queries a cada evento.
6. **Não implementar Ghost Racing.** Está fora do escopo das Fases 1–4.

---

## ORDEM DE COMMITS SUGERIDA

```
feat: add db.py with async SQLAlchemy engine
feat: add SQLAlchemy models (student, exercise, session, attempt, vector)
feat: add alembic initial migration
feat: add exercise seed script
feat: add POST /api/session/start endpoint
feat: add Claude Vision OCR engine
feat: add SymPy correction engine
feat: add time-decay scoring
feat: add POST /api/session/{id}/submit endpoint
feat: add cognitive vector generation
feat: add adaptive engine (skill memory + next folha selection)
```

---

## DÚVIDAS FREQUENTES

**Q: O aluno precisa marcar a resposta de alguma forma?**
A: Não. A folha tem N campos demarcados. O aluno escreve dentro do campo. O app já sabe o bounding box de cada campo. O OCR recebe apenas o crop daquele campo.

**Q: O que fazer se o SymPy não conseguir parsear o LaTeX?**
A: Fallback para comparação de string normalizada (lowercase, sem espaços). Log o erro. Não travar o fluxo.

**Q: Qual modelo Claude usar para OCR?**
A: `claude-haiku-4-5-20251001` para velocidade e custo. Se precisar de mais precisão em equações complexas, trocar para `claude-sonnet-4-6` configurando `CLAUDE_OCR_MODEL` no env.

**Q: O Android app precisa estar pronto para testar o backend?**
A: Não. Use o `test_ws.py` existente e crie scripts de teste em `tests/` que simulam payloads do submit endpoint com imagens base64 de teste.

---

*Handoff gerado em 2026-05-23. Spec completa em SUPER_SPEC.md.*

---

# APPEND — Estado Atual para Continuidade
# Data: 2026-05-24
# Autor da execução: Codex

Este é o ponto único de handoff a partir daqui. Não substituir o conteúdo anterior; continuar anexando novas seções `APPEND — ...` no fim deste arquivo sempre que houver avanço relevante, bloqueio, decisão técnica ou mudança de plano.

Repositório privado:

- `https://github.com/guilhermecrepaldi/love-class`
- Branch principal: `main`
- Working tree esperado após este append: limpo e sincronizado com `origin/main`

## Resumo Executivo

O backend MVP está praticamente pronto em código e testes. As Fases 1 a 4 foram implementadas em primeira versão:

- Fase 1: foundation, DB models, Alembic, seed, Docker Compose, requirements.
- Fase 2: `POST /api/session/start`, criação de aluno/config/sessão/folha.
- Fase 3: `POST /api/session/{session_id}/submit`, OCR, correção, score, attempts e pen events.
- Fase 4: vetores cognitivos, memória por skill, seleção adaptativa da próxima folha, restart/checks de sessão.

O único bloqueio real para carimbar o backend MVP é runtime com Postgres/Redis real. O Docker Desktop existe na máquina, mas a engine Linux não responde; o serviço `com.docker.service` está parado e o Windows negou start sem permissão elevada.

## Commits já enviados

Ordem mais recente:

```text
37af0d2 test: add http workflow coverage and ci
65c59d1 test: add user workflow coverage
9c7dafa feat: guard folha submit lifecycle
39e7c0d chore: harden api payload validation
cdb5e4f feat: persist folha exercise assignments
12563f5 chore: add backend smoke test
12d07a1 feat: add session and adaptive submit APIs
7cdc900 feat: add backend foundation
```

## Arquitetura Implementada

Arquivos principais:

- `backend/db.py`: settings, engine SQLAlchemy async, session dependency.
- `backend/main.py`: `create_app(run_startup_db=True)`, CORS, routers, lifespan DB.
- `backend/models/`: SQLAlchemy models.
- `backend/schemas/`: Pydantic contracts.
- `backend/api/session.py`: start session.
- `backend/api/submit.py`: submit folha.
- `backend/api/health.py`: DB healthcheck.
- `backend/engine/ocr.py`: Claude Vision com fallback local `latex:...` para testes.
- `backend/engine/correction.py`: correção por normalização/SymPy.
- `backend/engine/scoring.py`: Time-Decay Score.
- `backend/engine/vector.py`: cognitive vector.
- `backend/engine/adaptive.py`: primeira/próxima folha, skill memory, restart.
- `backend/seed/exercises.py`: 20 exercícios seed.
- `backend/scripts/smoke_backend.py`: smoke HTTP real quando DB/API estiverem rodando.
- `.github/workflows/backend-tests.yml`: GitHub Actions rodando unittest.

Migrations:

- `0001_initial.py`: schema base.
- `0002_add_folha_exercises.py`: vínculo persistente folha/campo/exercício.
- `0003_add_attempt_submit_guard.py`: constraint contra resubmissão por campo.

## Decisões Técnicas Importantes

1. `folha_exercises` foi adicionada porque a spec original tinha `folhas`, mas não persistia quais exercícios estavam nos campos da folha. Sem isso, o submit confiava demais no client.
2. `submit` exige exatamente todos os campos da folha. Submissão parcial é rejeitada.
3. Reenvio da mesma folha é rejeitado com `409`.
4. Sessões não `active` rejeitam submit.
5. `duration_mode="timed"` exige `duration_limit_ms`; `duration_mode="pages"` exige `pages_limit`.
6. OCR tem fallback local: se `image_base64` vier como `latex:x = 5`, retorna `x = 5` sem Anthropic. Isso é intencional para testes/smoke sem custo.
7. `create_app(run_startup_db=False)` existe para testes HTTP sem Postgres.
8. Redis ainda não foi integrado de verdade, apesar de estar no compose/requirements. Está previsto, mas não bloqueia o MVP HTTP.

## Testes Existentes

Rodar:

```powershell
cd "D:\LOVE CLASS\backend"
python -m unittest -v
```

Último resultado observado:

```text
Ran 24 tests in ~0.27s
OK
```

Cobertura de intenção:

- Engines: correction, scoring, adaptive restart/status.
- Schemas: validação de config, submit, event types, duplicidade.
- Submit helpers: término por páginas/tempo.
- Workflow handler-level: start -> submit -> next folha, incompleto, reenvio, término.
- Workflow HTTP-level com `TestClient`: cliente chama endpoints reais com fake DB.

## Como Fechar o Backend MVP

Assim que houver permissão para iniciar Docker Desktop Service ou Postgres local:

```powershell
cd "D:\LOVE CLASS\backend"
docker compose up -d
python -m alembic upgrade head
python seed/exercises.py
python -m uvicorn main:app --reload
```

Em outro terminal:

```powershell
cd "D:\LOVE CLASS\backend"
python scripts/smoke_backend.py
```

Critério de aceite do backend MVP:

- `GET /api/health` retorna `{status: ok, database: ok}`.
- `POST /api/session/start` retorna folha com N exercícios.
- `POST /api/session/{id}/submit` retorna results, page_score, thermometer e next_folha ou status finished.
- `python -m unittest -v` continua verde.
- `python scripts/smoke_backend.py` passa contra servidor real.

## Bloqueio Atual

Docker:

- `docker info` falha com pipe Linux engine ausente.
- `Get-Service` mostrou `com.docker.service` parado.
- `Start-Service -Name com.docker.service` falhou por permissão: precisa terminal elevado/admin.
- `localhost:5432` não está ouvindo.

Próximo operador deve primeiro tentar:

```powershell
# Em terminal Windows elevado/admin:
Start-Service -Name com.docker.service
```

Depois voltar para:

```powershell
cd "D:\LOVE CLASS\backend"
docker compose up -d
```

## Próximas Melhorias Recomendadas

Backend antes do Android:

1. Rodar smoke real com Postgres.
2. Corrigir qualquer bug de runtime que apareça no smoke.
3. Adicionar indexes básicos:
   - `exercise_attempts(session_id, created_at)`
   - `folha_exercises(folha_id)`
   - `student_skill_memory(student_id, status)`
4. Decidir se `Base.metadata.create_all` no lifespan continua em dev ou se produção deve depender só de Alembic.
5. Integrar Redis apenas para sessão ativa se realmente necessário agora; senão deixar para pós-MVP.

Android MVP depois do backend:

0. Ler `APP_LAYOUT_SPEC.md` para a direção visual e layout completo.
1. Criar projeto Android/Kotlin.
2. `SessionConfigScreen`.
3. `FolhaScreen` com campos.
4. `InkCanvas` capturando stylus.
5. Retrofit client.
6. Submit de crops/telemetria.
7. Tela de resultado/termômetro.

## Estimativa de Falta

Backend MVP:

- Falta estimada: 3-6%.
- Natureza do restante: runtime integrado com DB real, não lógica principal.

Projeto completo com Android:

- Falta estimada: ~45%.
- Motivo: backend quase fechado; Android ainda não começou.

## Regra para Próximos Appends

Sempre que continuar:

1. Rodar `git status -sb`.
2. Implementar mudança pequena e verificável.
3. Rodar `python -m unittest -v` se tocou backend.
4. Commitar com mensagem curta.
5. Push para `origin/main`.
6. Anexar uma nova seção abaixo desta com:
   - data/hora,
   - commit,
   - o que mudou,
   - testes rodados,
   - bloqueios,
   - próxima ação.

---

# APPEND — Continuidade 2026-05-24: Runtime Indexes

## Avaliação

O backend já tinha boa cobertura de lógica e workflow, mas ainda faltava uma melhoria recomendada no próprio handoff: indexes para os caminhos quentes de runtime. Sem eles, o app poderia funcionar no smoke, mas começar a degradar quando houvesse muitas tentativas, folhas e memórias por aluno.

## Melhoria Implementada

Adicionados indexes nos models e migration nova:

- `ix_exercise_attempts_session_created` em `exercise_attempts(session_id, created_at)`
  - Usado para buscar scores recentes por sessão.
- `ix_folha_exercises_folha` em `folha_exercises(folha_id)`
  - Usado para validar rapidamente os campos/exercícios da folha no submit.
- `ix_student_skill_memory_student_status` em `student_skill_memory(student_id, status)`
  - Usado para filtrar skills fracas/instáveis por aluno.

Arquivos tocados:

- `backend/models/attempt.py`
- `backend/models/session.py`
- `backend/models/vector.py`
- `backend/migrations/versions/0004_add_runtime_indexes.py`

## Testes Rodados

```powershell
cd "D:\LOVE CLASS\backend"
python -m unittest -v
python -m compileall .
python -m alembic history --verbose
python -m alembic upgrade head --sql
```

Resultado observado:

- `python -m unittest -v`: 24 testes OK.
- `compileall`: OK.
- Alembic head agora é `0004_add_runtime_indexes`.
- SQL offline gerou:
  - `CREATE INDEX ix_exercise_attempts_session_created ...`
  - `CREATE INDEX ix_folha_exercises_folha ...`
  - `CREATE INDEX ix_student_skill_memory_student_status ...`

## Bloqueio Atual

Inalterado: ainda falta runtime integrado com Postgres real. Docker Desktop/engine Linux continua sendo o bloqueio operacional conhecido. Código e migrations estão prontos para aplicar quando o serviço estiver disponível.

## Próxima Ação Recomendada

1. Commitar e fazer push deste pacote se ainda não tiver sido feito.
2. Com permissão admin no Windows, iniciar `com.docker.service`.
3. Rodar:

```powershell
cd "D:\LOVE CLASS\backend"
docker compose up -d
python -m alembic upgrade head
python seed/exercises.py
python -m uvicorn main:app --reload
python scripts/smoke_backend.py
```

## Estimativa Atualizada

- Backend MVP: 3-5% faltando.
- Projeto completo com Android: ~45% faltando.

---

# APPEND — Continuidade 2026-05-24: Startup Schema Policy

## Avaliação

O handoff anterior ainda deixava aberta a decisão sobre `Base.metadata.create_all` no startup. Isso é conveniente em desenvolvimento, mas em produção/migração real o schema deve ser controlado por Alembic. A mudança feita agora torna esse comportamento explícito e configurável.

## Melhoria Implementada

Adicionado setting:

```env
AUTO_CREATE_TABLES=true
```

Comportamento:

- Default `true`: mantém experiência dev fácil.
- `false`: servidor sobe sem rodar `Base.metadata.create_all`; Alembic passa a ser a fonte única de schema.

Arquivos tocados:

- `backend/db.py`: novo `Settings.auto_create_tables`.
- `backend/main.py`: `app = create_app(run_startup_db=settings.auto_create_tables)`.
- `backend/.env.example`: adiciona `AUTO_CREATE_TABLES=true`.
- `backend/README.md`: documenta usar `AUTO_CREATE_TABLES=false` fora de dev.
- `backend/tests/test_settings.py`: cobre default e normalização de URL Postgres para asyncpg.

## Testes Rodados

```powershell
cd "D:\LOVE CLASS\backend"
python -m unittest -v
python -m compileall .
python -c "import main; print([route.path for route in main.app.routes]); print(main.settings.auto_create_tables)"
```

Resultado observado:

- `python -m unittest -v`: 26 testes OK.
- `compileall`: OK.
- Rotas seguem registradas:
  - `/api/health`
  - `/api/session/start`
  - `/api/session/{session_id}/submit`
  - `/api/telemetry/stream`
- `settings.auto_create_tables` retornou `True` com default atual.

## Bloqueio Atual

Inalterado: falta Postgres/Redis real para smoke integrado. Docker Desktop Service ainda precisa ser iniciado com permissão elevada/admin.

## Próxima Ação Recomendada

1. Commitar e fazer push deste pacote se ainda não tiver sido feito.
2. Tentar novamente runtime real:

```powershell
cd "D:\LOVE CLASS\backend"
docker compose up -d
python -m alembic upgrade head
python seed/exercises.py
python -m uvicorn main:app --reload
python scripts/smoke_backend.py
```

3. Se Docker continuar bloqueado, próximo ganho sem DB é começar o scaffold Android seguindo `APP_LAYOUT_SPEC.md`.

## Estimativa Atualizada

- Backend MVP: 3-5% faltando.
- Projeto completo com Android: ~45% faltando.

---

# APPEND — Continuidade 2026-05-24: Android Compose Scaffold

## Avaliação

Como o backend está bloqueado apenas no runtime real com Postgres/Docker, o melhor avanço foi iniciar a Fase Android seguindo `APP_LAYOUT_SPEC.md`. Não havia projeto Android no repositório. Também foi confirmado que `gradle` não está disponível no PATH desta máquina, então a validação local do Android ficou limitada a estrutura/conteúdo. O backend foi retestado para garantir que nada regrediu.

## Melhoria Implementada

Criado scaffold Android/Jetpack Compose:

- `settings.gradle.kts`
- `build.gradle.kts`
- `app/build.gradle.kts`
- `app/src/main/AndroidManifest.xml`
- `app/src/main/res/values/styles.xml`
- `app/src/main/java/com/strava_matematica/MainActivity.kt`
- `app/src/main/java/com/strava_matematica/design/*`
- `app/src/main/java/com/strava_matematica/model/*`
- `app/src/main/java/com/strava_matematica/network/*`
- `app/src/main/java/com/strava_matematica/viewmodel/*`
- `app/src/main/java/com/strava_matematica/ui/*`
- `app/README.md`

O app agora tem um fluxo demo em Compose:

```text
SessionConfigScreen
-> FolhaScreen
-> PageResultScreen
-> SessionSummaryScreen
```

O `SessionViewModel` usa dados demo para permitir navegação visual sem backend. Os modelos Kotlin espelham os contratos atuais da API. A interface visual segue o spec: tema white/dark, tela de config, folha com campos, termômetro, toolbar de tinta, tela de resultado e resumo.

## Limitações Conhecidas

- Android ainda não foi compilado localmente porque `gradle` não existe no PATH e não há Gradle wrapper versionado.
- `InkCanvas` ainda é visual-only; falta captura real de stylus, strokes, pressure, tilt e velocity.
- Ainda falta exportar crop bitmap por campo para `image_base64`.
- Retrofit está definido, mas o fluxo demo ainda não chama backend real.
- Layout é primeira base funcional, não QA visual final.

## Testes/Verificações Rodadas

```powershell
cd "D:\LOVE CLASS"
rg --files app | Sort-Object
rg "TODO|Future|import .*\\*|com\\.jakewharton|TODO" app backend

cd "D:\LOVE CLASS\backend"
python -m unittest -v
python -m compileall .
```

Resultado observado:

- Estrutura Android criada com 24 arquivos iniciais.
- Backend: 26 testes OK.
- Backend compileall OK.
- Gradle local não rodou por ausência de tooling.

## Próxima Ação Recomendada

Para Android:

1. Abrir o projeto no Android Studio.
2. Criar/baixar Gradle wrapper.
3. Rodar:

```powershell
.\gradlew.bat :app:assembleDebug
```

4. Corrigir erros de primeira compilação Android se aparecerem.
5. Substituir ações demo do `SessionViewModel` por chamadas reais via `ApiClient`.
6. Implementar captura real no `InkCanvas`.
7. Implementar crop por campo para submit.

Para backend:

1. Quando Docker/Postgres estiver disponível, rodar smoke real.

## Estimativa Atualizada

- Backend MVP: 3-5% faltando.
- Android MVP: ~35-40% faltando.
- Projeto completo: ~40-45% faltando.
