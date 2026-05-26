# HANDOFF — Biblioteca de Exercícios LOVE CLASS
**Para:** Codex / próximo agente  
**Projeto:** `D:\LOVE CLASS\`  
**Tarefa:** Gerar, validar e persistir a biblioteca de exercícios de matemática

> STATUS: HISTORICO. Para estado atual da biblioteca, leia `AGENTS.md`, `docs/README.md`, `docs/PROJECT_SPEC.md` e `docs/HANDOFF.md`.

---

## Contexto em 3 linhas

LOVE CLASS é um app Android de treino de matemática para concursandos brasileiros. O aluno **escreve** as respostas à mão (caneta/stylus). Há um banco de exercícios no PostgreSQL — atualmente com ~47 exercícios de seed. A missão é expandir para **~3.000 exercícios** cobrindo as 34 skills da trilha pedagógica, usando Claude Haiku como gerador.

---

## O que já existe (NÃO RECRIAR)

```
backend/
  scripts/generate_exercises.py   ← script completo, pronto para uso
  seed/exercises.py               ← seed manual (47 exercícios, já no banco)
  models/exercise.py              ← modelo SQLAlchemy
  engine/batch_ocr_validate.py    ← validação em batch via Claude
```

### Modelo `Exercise` (tabela `exercises`)
```python
statement: str          # enunciado (LaTeX onde necessário)
expected_answer: str    # resposta correta (LaTeX onde necessário)
skill_tags: list[str]   # ex: ["equacoes_lineares"]
difficulty: float       # 1.0–10.0
estimated_time_ms: int  # default 45000 (45s)
source_library: str     # "generated_v1" para gerados
verified_count: int     # quantos experts acertaram (default 0)
expert_avg_time_ms: int # tempo médio real de experts
calibrated_difficulty: float # dificuldade recalibrada por experts
```

---

## SKILL_DICT — as 34 skills e suas faixas

O script já tem o `SKILL_DICT` completo em `generate_exercises.py`. Resumo:

| Skill | Faixa dificuldade | Tema |
|---|---|---|
| soma_subtracao | 1.0–2.5 | Adição e subtração |
| multiplicacao_divisao | 1.5–3.0 | Multiplicação e divisão |
| fracoes_decimais | 1.8–3.5 | Frações e decimais |
| porcentagem_razao | 2.0–4.0 | Porcentagem e proporção |
| potenciacao_radiciacao | 1.8–4.0 | Potências e raízes |
| equacoes_lineares | 1.8–3.5 | Eq. 1º grau |
| sistemas_equacoes | 2.8–4.5 | Sistemas 2×2 |
| fatoracao_produtos_notaveis | 2.5–4.5 | Fatoração e produtos |
| inequacoes | 2.2–4.0 | Inequações 1º grau |
| equacoes_quadraticas | 2.4–5.0 | Eq. 2º grau / Bhaskara |
| funcao_afim | 2.5–4.0 | Função linear |
| funcao_quadratica | 3.0–5.5 | Função quadrática |
| funcao_exponencial | 3.0–5.5 | Exponencial |
| funcao_logaritmica | 3.2–6.0 | Logaritmo |
| funcao_modular | 3.5–5.5 | Valor absoluto |
| geometria_plana | 2.0–4.5 | Áreas e perímetros |
| geometria_espacial | 3.0–5.5 | Volumes |
| geometria_analitica | 3.5–5.5 | Retas e pontos |
| progressoes_pa_pg | 3.0–5.0 | PA e PG |
| combinatoria | 3.5–6.0 | Arranjo, permutação, C(n,k) |
| probabilidade | 3.0–5.5 | Probabilidade clássica |
| trig_razoes | 3.0–5.0 | Sen/cos/tan no triângulo |
| trig_seno_cosseno_tangente | 3.5–5.5 | Ciclo trigonométrico |
| trig_identidades | 4.0–6.5 | Identidades e adição |
| trig_equacoes | 4.5–7.0 | Equações trigonométricas |
| nocao_de_limite | 4.5–6.5 | Limites básicos |
| continuidade | 5.0–7.0 | Continuidade |
| derivadas_basicas | 5.0–7.0 | Regra da potência |
| derivadas_regra_cadeia | 5.5–7.5 | Regra da cadeia |
| derivadas_produto_quociente | 5.5–7.5 | Produto e quociente |
| aplicacoes_derivadas | 6.0–8.0 | Máximos e mínimos |
| integrais_indefinidas | 6.0–8.0 | Antiderivadas |
| integrais_definidas | 6.5–8.5 | Teorema Fundamental |
| aplicacoes_integrais | 7.0–9.0 | Área entre curvas |

---

## Tarefa 1 — TESTE COM EQUAÇÕES (fazer primeiro)

Valida o pipeline completo com 3 skills de equações antes de rodar tudo.

```bash
cd "D:\LOVE CLASS\backend"

# Passo 1: dry-run (não salva no banco, só mostra os exercícios gerados)
python scripts/generate_exercises.py \
  --skills equacoes_lineares,equacoes_quadraticas,sistemas_equacoes \
  --count 5 \
  --dry-run \
  --api-key sk-ant-SEU_KEY_AQUI

# Passo 2: verificar output esperado (veja exemplo abaixo)
# Passo 3: se ok, inserir no banco (remover --dry-run)
python scripts/generate_exercises.py \
  --skills equacoes_lineares,equacoes_quadraticas,sistemas_equacoes \
  --count 5 \
  --api-key sk-ant-SEU_KEY_AQUI
```

### Output esperado do dry-run (exemplo para equacoes_lineares)
```
[1/3] equacoes_lineares - gerando 5 exercicios...
  OK 5 gerados (3.2s)
    [2.1] Resolva: 3x + 7 = 22
           = x = 5
    [2.4] Resolva: 2(x - 3) = 4x + 2
           = x = -4
    [2.8] Resolva: \frac{x-1}{2} = 3
           = x = 7
    [3.1] Um número somado com o triplo do seu sucessor é igual a 27. Qual é o número?
           = x = 6
    [3.4] Resolva: \frac{2x+1}{3} - \frac{x-2}{4} = 2
           = x = 5
```

### Critérios de aprovação do teste
- [ ] JSON válido retornado pelo Claude (sem erro de parse)
- [ ] `difficulty` dentro da faixa da skill
- [ ] LaTeX correto onde esperado (`\frac{}{}`, `\sqrt{}`, etc.)
- [ ] `expected_answer` matematicamente correto
- [ ] Sem duplicatas nos enunciados (script já verifica)
- [ ] Inserção no banco sem erro de constraint

### Verificar no banco após inserção
```bash
# Conectar ao PostgreSQL e checar
psql $DATABASE_URL -c "
  SELECT skill_tags[1] as skill, count(*), 
         round(min(difficulty)::numeric, 1) as min_d,
         round(max(difficulty)::numeric, 1) as max_d
  FROM exercises
  WHERE source_library = 'generated_v1'
  GROUP BY skill_tags[1]
  ORDER BY min_d;
"
```

---

## Tarefa 2 — BIBLIOTECA COMPLETA (após teste aprovado)

### Meta: ~3.060 exercícios (34 skills × 90 cada)

```bash
cd "D:\LOVE CLASS\backend"

# Opção A: tudo de uma vez (demora ~20min, cuidado com rate limit)
python scripts/generate_exercises.py \
  --all \
  --count 90 \
  --api-key sk-ant-SEU_KEY_AQUI

# Opção B: em grupos por nível (mais seguro, permite inspeção)
# Grupo 1: fundamental (skills mais básicas)
python scripts/generate_exercises.py \
  --skills soma_subtracao,multiplicacao_divisao,fracoes_decimais,porcentagem_razao,potenciacao_radiciacao \
  --count 90 --api-key sk-ant-SEU_KEY_AQUI

# Grupo 2: álgebra
python scripts/generate_exercises.py \
  --skills equacoes_lineares,sistemas_equacoes,fatoracao_produtos_notaveis,inequacoes,equacoes_quadraticas \
  --count 90 --api-key sk-ant-SEU_KEY_AQUI

# Grupo 3: funções
python scripts/generate_exercises.py \
  --skills funcao_afim,funcao_quadratica,funcao_exponencial,funcao_logaritmica,funcao_modular \
  --count 90 --api-key sk-ant-SEU_KEY_AQUI

# Grupo 4: geometria e contagem
python scripts/generate_exercises.py \
  --skills geometria_plana,geometria_espacial,geometria_analitica,progressoes_pa_pg,combinatoria,probabilidade \
  --count 90 --api-key sk-ant-SEU_KEY_AQUI

# Grupo 5: trigonometria
python scripts/generate_exercises.py \
  --skills trig_razoes,trig_seno_cosseno_tangente,trig_identidades,trig_equacoes \
  --count 90 --api-key sk-ant-SEU_KEY_AQUI

# Grupo 6: cálculo
python scripts/generate_exercises.py \
  --skills nocao_de_limite,continuidade,derivadas_basicas,derivadas_regra_cadeia,derivadas_produto_quociente,aplicacoes_derivadas,integrais_indefinidas,integrais_definidas,aplicacoes_integrais \
  --count 90 --api-key sk-ant-SEU_KEY_AQUI
```

### Custo estimado (Claude Haiku)
- 3060 exercícios × ~400 tokens input + ~150 tokens output ≈ 1.7M tokens
- Haiku: $0.80/M input + $4/M output ≈ ~$2–3 total

---

## Tarefa 3 — VALIDAÇÃO APÓS GERAÇÃO

```bash
# Contagem por skill
psql $DATABASE_URL -c "
  SELECT skill_tags[1] as skill, count(*) as total,
         round(avg(difficulty)::numeric, 2) as avg_diff,
         sum(CASE WHEN verified_count > 0 THEN 1 ELSE 0 END) as verified
  FROM exercises
  WHERE source_library IN ('seed_v2', 'generated_v1')
  GROUP BY skill_tags[1]
  ORDER BY avg_diff;
"

# Total geral
psql $DATABASE_URL -c "
  SELECT source_library, count(*) FROM exercises GROUP BY source_library;
"

# Exercícios com dificuldade fora do range permitido (devem ser 0)
psql $DATABASE_URL -c "
  SELECT count(*) FROM exercises 
  WHERE difficulty < 1.0 OR difficulty > 10.0;
"
```

### Critérios de aprovação da biblioteca
- [ ] >= 80 exercícios por skill (90 target, aceita 80+)
- [ ] Distribuição de dificuldade uniforme dentro de cada skill
- [ ] Zero exercícios com `difficulty` fora de 1.0–10.0
- [ ] Zero enunciados duplicados
- [ ] Total >= 2720 exercícios novos (seed_v2 + generated_v1)

---

## Tarefa 4 — MELHORIAS NO SCRIPT (opcional, se qualidade ruim)

Se o dry-run mostrar problemas de qualidade (respostas erradas, LaTeX quebrado):

### Melhorar o prompt em `generate_exercises.py`

Localizar `USER_TEMPLATE` (~linha 50) e adicionar exemplos few-shot:

```python
USER_TEMPLATE = """\
Gere {count} exercícios de matemática sobre: {topic}

Dificuldade: de {min_diff} a {max_diff} (1.0=básico, 10.0=olimpíadas).
Restrições: {constraints}

EXEMPLOS do formato esperado (NÃO reutilize estes):
[
  {{"statement": "Resolva: 3x + 7 = 22", "expected_answer": "x = 5", "difficulty": 2.1}},
  {{"statement": "Resolva: 2(x-3) = 4x + 2", "expected_answer": "x = -4", "difficulty": 2.8}}
]

Agora gere {count} NOVOS exercícios. Retorne APENAS JSON array, mesmo formato.
"""
```

### Se houver muitas duplicatas entre runs

O script já faz dedup por `statement` exato. Para dedup semântico (mesmo exercício com palavras diferentes), adicionar verificação de `expected_answer` + `skill_tags`:

```python
# Em insert_exercises(), após existing_set:
existing_answers = await db.execute(
    select(Exercise.expected_answer)
    .where(Exercise.skill_tags.contains([skill]))
    .where(Exercise.source_library == "generated_v1")
)
existing_answers_set = set(existing_answers.scalars().all())

new_exercises = [
    Exercise(...)
    for x in items
    if x["statement"] not in existing_set
    and x["expected_answer"] not in existing_answers_set  # dedup semântico
]
```

---

## Estrutura de arquivos relevantes

```
D:\LOVE CLASS\
  backend/
    scripts/
      generate_exercises.py      ← SCRIPT PRINCIPAL — use este
    seed/
      exercises.py               ← seed manual (não modificar)
    models/
      exercise.py                ← modelo (não modificar)
    migrations/versions/
      0009_contributor_mode.py   ← última migration aplicada
    .env.example                 ← criar .env com ANTHROPIC_API_KEY
```

---

## Como rodar o backend para verificar

```bash
cd "D:\LOVE CLASS\backend"

# Criar .env se não existir
copy .env.example .env
# Editar .env com valores reais

# Rodar backend
python main.py
# ou
uvicorn main:app --host 0.0.0.0 --port 8000 --reload

# Verificar exercícios disponíveis (API)
curl http://localhost:8000/api/health
```

---

## Definição de DONE

```
[ ] Teste com equações passou (dry-run + inserção + verificação SQL)
[ ] >= 2720 exercícios no banco (generated_v1)
[ ] Todos os 34 skills com >= 80 exercícios cada
[ ] Zero erros de constraint no banco
[ ] Script idempotente (rodar 2× não duplica)
[ ] Exercícios de equacoes_lineares testados manualmente no app
```

---

## Observações importantes

1. **Rate limit Haiku**: o script já tem 500ms de pausa entre skills. Se der 429, aumentar para 2s em `time.sleep(0.5)` → `time.sleep(2)`.

2. **LaTeX**: o app Android renderiza LaTeX via MathJax. Usar `\frac{a}{b}`, `\sqrt{}`, `x^2`, `\pm`, `\leq`, `\geq`. NÃO usar `$$` ou `$` delimitadores — só o conteúdo LaTeX puro.

3. **Exercícios de equação prioritários**: a trilha principal de um concursando começa em `equacoes_lineares`. Garantir qualidade extra nessa skill — são os primeiros que o aluno vai ver.

4. **O script é idempotente**: pode rodar múltiplas vezes. Dedup é automático por `statement`. Se uma skill já tiver 90 exercícios, não adiciona mais.

5. **contributor_mode**: experts que usam o app com `contributor_mode=true` calibram automaticamente `expert_avg_time_ms` e `calibrated_difficulty` por exercício. Isso melhora a dificuldade ao longo do tempo sem precisar reeditar manualmente.

---

## APPEND 2026-05-24 — OpenAI provider preparado, geração bloqueada por quota

Pedido atual: gerar 5.000 exercícios usando OpenAI em vez de Anthropic.

### O que foi alterado

- `backend/scripts/generate_exercises.py` agora aceita `--provider openai`.
- Detecção automática escolhe OpenAI quando a chave vem de `OPENAI_API_KEY` ou começa com `sk-proj-`/`sk-`.
- `--model` permite sobrescrever o modelo; padrão OpenAI atual no script: `gpt-4o-mini`.
- `--batch-size` evita respostas enormes; para 5.000 exercícios use lotes de 25 por padrão.
- A chamada OpenAI usa Responses API com `text.format.type = json_schema` para forçar saída estruturada.
- O script agora sai com código diferente de zero se todos os lotes falharem, evitando falso sucesso em dry-run/CI.
- Não foi adicionada dependência nova; a chamada OpenAI usa `urllib.request`.

### Comando pretendido para 5.032 exercícios

```powershell
cd "D:\LOVE CLASS\backend"
$env:OPENAI_API_KEY = "<OPENAI_API_KEY>"
python scripts\generate_exercises.py --all --count 148 --batch-size 25 --provider openai
```

34 skills × 148 exercícios = 5.032 tentativas de geração.

### Teste obrigatório executado

```powershell
python scripts\generate_exercises.py --skills equacoes_lineares,equacoes_quadraticas,sistemas_equacoes --count 5 --batch-size 5 --dry-run --provider openai
```

Resultado: falhou antes de gerar JSON. A API retornou `HTTP 429` com `code = insufficient_quota` para as 3 skills. Portanto:

- JSON: não gerado.
- LaTeX: não verificável.
- respostas matemáticas: não verificáveis.
- inserção no banco: não executada.

### Estado do banco após tentativa

Consulta executada em `strava_math_postgres`:

```sql
SELECT COALESCE(source_library,'NULL') AS source_library, COUNT(*)
FROM exercises
GROUP BY source_library
ORDER BY source_library;
```

Resultado atual:

| source_library | count |
|---|---:|
| seed_v2 | 47 |

`generated_v1` permanece com 0 exercícios.

### Verificação local

- `python -m py_compile scripts\generate_exercises.py` passou.
- `python -m unittest -v` falhou em 9 testes por causa de `api.submit.extract_answer` ausente. Essa falha é fora do gerador e apareceu no setup dos testes de workflow/submissão.

### Próximo passo

Resolver billing/quota da chave OpenAI ou trocar por uma chave OpenAI com quota ativa. Depois repetir primeiro o dry-run de 5 exercícios nas 3 skills; somente se a amostra estiver correta, rodar o comando de 5.032 tentativas.

---

## APPEND 2026-05-24 — Endpoint OpenAI para geração

Pedido: criar endpoint para OpenAI também.

### Endpoint novo

`POST /api/exercises/generate/openai`

Body exemplo para dry-run:

```json
{
  "skills": ["equacoes_lineares"],
  "count": 5,
  "batch_size": 5,
  "dry_run": true
}
```

Body exemplo para inserir 5.032 exercícios:

```json
{
  "all": true,
  "count": 148,
  "batch_size": 25,
  "dry_run": false
}
```

Resposta inclui `total_generated`, `total_inserted`, `total_duplicates`, `total_errors` e o detalhamento por skill. Em `dry_run=true`, retorna `preview` com enunciado, resposta esperada e dificuldade, sem inserir no banco.

### Arquivos tocados

- `backend/api/exercise_generation.py`: rota FastAPI e schemas Pydantic.
- `backend/main.py`: include do router novo.
- `backend/requirements.txt`: adiciona `openai`, necessário pelos endpoints/engines OpenAI já presentes no backend.

### Verificação executada

- `python -m py_compile api\exercise_generation.py main.py scripts\generate_exercises.py` passou.
- Teste rápido com `TestClient` sem chave retornou `503` e `{"detail": "OPENAI_API_KEY nao configurada"}`.
- `python -m unittest -v` continua falhando nos mesmos 9 testes por `api.submit.extract_answer` ausente. Não é uma falha do endpoint novo.

---

## APPEND 2026-05-24 — Gemini e IA local

Pedido: tentar Gemini e considerar IA local.

### Estado atual do banco

Após a geração com Gemini ser interrompida manualmente duas vezes, o banco ficou com:

| source_library | count |
|---|---:|
| generated_v1 | 2838 |
| seed_v2 | 47 |

Critério mínimo do handoff (`generated_v1 >= 2720`) já foi atingido. Para a meta informal de 5.000, faltam cerca de 2.162 exercícios.

Skills ainda abaixo de 148 exercícios:

- 16 skills com 0 exercícios: `aplicacoes_derivadas`, `aplicacoes_integrais`, `combinatoria`, `continuidade`, `derivadas_basicas`, `derivadas_produto_quociente`, `derivadas_regra_cadeia`, `integrais_definidas`, `integrais_indefinidas`, `nocao_de_limite`, `probabilidade`, `progressoes_pa_pg`, `trig_equacoes`, `trig_identidades`, `trig_razoes`, `trig_seno_cosseno_tangente`.
- `funcao_modular`: 25 exercícios.

### Gemini

`backend/scripts/generate_exercises.py` agora aceita:

```powershell
python scripts\generate_exercises.py --skills equacoes_lineares --count 5 --dry-run --provider gemini
```

Também existe endpoint:

```text
POST /api/exercises/generate/gemini
```

O dry-run com Gemini em `equacoes_lineares`, `equacoes_quadraticas` e `sistemas_equacoes` gerou JSON válido. A amostra completa foi conferida manualmente e estava matematicamente correta. Depois o teste real inseriu 15 exercícios no banco sem duplicatas.

### IA local

Ollama está instalado localmente e foi iniciado com:

```powershell
ollama serve
```

Modelos disponíveis incluem `qwen2.5:14b`, `qwen2.5:7b`, `llama3.1:8b`, `mistral`, `gemma3`, entre outros.

`backend/scripts/generate_exercises.py` agora aceita:

```powershell
python scripts\generate_exercises.py --skills equacoes_lineares --count 1 --dry-run --provider ollama --model qwen2.5:14b
```

Endpoint local:

```text
POST /api/exercises/generate/local
```

Observação importante: a primeira amostra local com `qwen2.5:14b` retornou JSON parseável, mas uma resposta matemática errada. Uma segunda amostra via endpoint local veio correta. Portanto, IA local funciona tecnicamente, mas ainda não deve inserir em lote sem um filtro/verificador matemático.

### Arquivos tocados

- `backend/scripts/generate_exercises.py`: providers `gemini` e `ollama`, parser mais tolerante para array, objeto único ou `{ "exercises": [...] }`.
- `backend/api/exercise_generation.py`: endpoints `/openai`, `/gemini` e `/local` compartilham a mesma rotina.
- `backend/db.py`: settings para OpenAI, Gemini e Ollama preservando Anthropic.
- `backend/.env.example`: variáveis dos três provedores.

### Próximo passo recomendado

Continuar com Gemini para preencher as skills faltantes, porque a qualidade da amostra foi melhor. Usar IA local apenas depois de adicionar validação automática por SymPy nos tipos de exercício mais estruturados.
