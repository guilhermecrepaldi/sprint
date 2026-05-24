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
