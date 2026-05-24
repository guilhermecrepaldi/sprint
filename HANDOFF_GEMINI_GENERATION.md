# HANDOFF — Geracao de Exercicios com Gemini

Projeto: LOVE CLASS

Workspace: `D:\LOVE CLASS`

Objetivo: completar a biblioteca de exercicios gerados (`source_library = generated_v1`) usando Gemini, mantendo qualidade matematica e evitando duplicatas.

---

## Estado Atual

Banco: Postgres Docker `strava_math_postgres`

Total atual por origem:

| source_library | count |
|---|---:|
| generated_v1 | 2838 |
| seed_v2 | 47 |

O criterio minimo antigo do handoff (`generated_v1 >= 2720`) ja foi atingido.

Meta informal do usuario: chegar perto de 5000 exercicios gerados. Faltam aproximadamente 2162 novos exercicios.

---

## Provider Recomendado

Use Gemini, nao IA local, para inserir em lote.

Motivo:

- Gemini gerou JSON valido.
- Amostras manuais de `equacoes_lineares`, `equacoes_quadraticas` e `sistemas_equacoes` foram conferidas e estavam matematicamente corretas.
- Ollama local com `qwen2.5:14b` funciona tecnicamente, mas uma amostra veio matematicamente errada. Use local apenas depois de adicionar validador automatico.

Arquivos ja preparados:

- `backend/scripts/generate_exercises.py`
  - Suporta `--provider gemini`
  - Suporta `--provider ollama`
  - Insere com `source_library = generated_v1`
  - Deduplica por `statement`
- `backend/api/exercise_generation.py`
  - `POST /api/exercises/generate/openai`
  - `POST /api/exercises/generate/gemini`
  - `POST /api/exercises/generate/local`
- `backend/db.py`
  - Settings para Anthropic, OpenAI, Gemini e Ollama

Nao grave chaves em arquivos versionados.

Variavel esperada:

```powershell
$env:GEMINI_API_KEY = "<GEMINI_API_KEY>"
```

---

## Skills Que Ainda Precisam Ser Geradas

Fotografia atual por skill (`generated_v1`):

| skill | current | need_to_148 |
|---|---:|---:|
| aplicacoes_derivadas | 0 | 148 |
| aplicacoes_integrais | 0 | 148 |
| continuidade | 0 | 148 |
| derivadas_basicas | 0 | 148 |
| derivadas_produto_quociente | 0 | 148 |
| derivadas_regra_cadeia | 0 | 148 |
| integrais_definidas | 0 | 148 |
| integrais_indefinidas | 0 | 148 |
| nocao_de_limite | 0 | 148 |
| probabilidade | 0 | 148 |
| trig_equacoes | 0 | 148 |
| trig_identidades | 0 | 148 |
| trig_razoes | 0 | 148 |
| trig_seno_cosseno_tangente | 0 | 148 |
| combinatoria | 25 | 123 |
| funcao_modular | 25 | 123 |

`progressoes_pa_pg` ja esta acima de 148 (`246`). Nao gere mais para ela agora.

Tambem nao gere mais para:

`soma_subtracao,multiplicacao_divisao,fracoes_decimais,porcentagem_razao,potenciacao_radiciacao,equacoes_lineares,sistemas_equacoes,fatoracao_produtos_notaveis,inequacoes,equacoes_quadraticas,funcao_afim,funcao_quadratica,funcao_exponencial,funcao_logaritmica,geometria_plana,geometria_espacial,geometria_analitica,progressoes_pa_pg`

---

## Comando Seguro Para Completar Ate 148 Por Skill

Entre no backend:

```powershell
cd "D:\LOVE CLASS\backend"
```

Primeiro rode dry-run em 2 ou 3 skills dificeis:

```powershell
python scripts\generate_exercises.py --skills derivadas_basicas,integrais_indefinidas,trig_equacoes --count 3 --batch-size 3 --dry-run --provider gemini
```

Conferir manualmente:

- JSON parseou sem erro
- LaTeX nao usa delimitadores `$$`
- Resposta esperada esta matematicamente correta
- Dificuldade esta entre 1.0 e 10.0

Se estiver OK, rode a insercao em grupos pequenos:

```powershell
python scripts\generate_exercises.py --skills aplicacoes_derivadas,aplicacoes_integrais,continuidade,derivadas_basicas --count 148 --batch-size 25 --provider gemini
```

```powershell
python scripts\generate_exercises.py --skills derivadas_produto_quociente,derivadas_regra_cadeia,integrais_definidas,integrais_indefinidas --count 148 --batch-size 25 --provider gemini
```

```powershell
python scripts\generate_exercises.py --skills nocao_de_limite,probabilidade,trig_equacoes,trig_identidades --count 148 --batch-size 25 --provider gemini
```

```powershell
python scripts\generate_exercises.py --skills trig_razoes,trig_seno_cosseno_tangente --count 148 --batch-size 25 --provider gemini
```

Complete as duas skills parcialmente cheias:

```powershell
python scripts\generate_exercises.py --skills combinatoria,funcao_modular --count 123 --batch-size 25 --provider gemini
```

Isso deve adicionar aproximadamente:

- 14 skills vazias * 148 = 2072
- 2 skills parciais * 123 = 246
- Total esperado: 2318 tentativas novas

Como ja existem 2838, o total pode chegar perto de 5156 antes de duplicatas.

---

## Endpoint Alternativo

Se preferir usar API em vez do script:

```text
POST /api/exercises/generate/gemini
```

Body exemplo:

```json
{
  "skills": ["derivadas_basicas", "integrais_indefinidas"],
  "count": 148,
  "batch_size": 25,
  "dry_run": false
}
```

Para dry-run:

```json
{
  "skills": ["derivadas_basicas"],
  "count": 3,
  "batch_size": 3,
  "dry_run": true
}
```

---

## Validacao Obrigatoria Depois

Rodar contagem por origem:

```powershell
docker exec strava_math_postgres psql -U user -d strava_math -c "SELECT COALESCE(source_library,'NULL') AS source_library, COUNT(*) FROM exercises GROUP BY source_library ORDER BY source_library;"
```

Rodar contagem por skill:

```powershell
docker exec strava_math_postgres psql -U user -d strava_math -c "SELECT skill_tags[1] AS skill, COUNT(*) FROM exercises WHERE source_library = 'generated_v1' GROUP BY skill_tags[1] ORDER BY skill_tags[1];"
```

Checar dificuldades fora da faixa:

```powershell
docker exec strava_math_postgres psql -U user -d strava_math -c "SELECT COUNT(*) FROM exercises WHERE difficulty < 1.0 OR difficulty > 10.0;"
```

Checar duplicatas exatas:

```powershell
docker exec strava_math_postgres psql -U user -d strava_math -c "SELECT statement, COUNT(*) FROM exercises GROUP BY statement HAVING COUNT(*) > 1 LIMIT 20;"
```

Checar campos obrigatorios vazios:

```powershell
docker exec strava_math_postgres psql -U user -d strava_math -c "SELECT COUNT(*) FROM exercises WHERE statement IS NULL OR btrim(statement) = '' OR expected_answer IS NULL OR btrim(expected_answer) = '' OR skill_tags IS NULL OR array_length(skill_tags, 1) IS NULL;"
```

---

## Testes

Antes de finalizar:

```powershell
cd "D:\LOVE CLASS\backend"
python -m py_compile scripts\generate_exercises.py api\exercise_generation.py db.py main.py
```

`python -m unittest -v` ainda pode falhar em 9 testes por `api.submit.extract_answer` ausente. Essa falha e preexistente e nao e causada pela geracao Gemini.

---

## Regras De Qualidade

- Nao inserir via IA local em massa sem validador automatico.
- Nao gerar mais para skills ja acima de 148, exceto se o usuario pedir explicitamente.
- Nao versionar `.env` nem chaves.
- Se Gemini retornar erro de quota/rate limit, reduza `--batch-size` para 10 ou aguarde.
- Preferir grupos pequenos para facilitar retomada se o usuario interromper.
- Depois de qualquer alteracao de codigo/documentacao: rodar verificacao, commitar, push para `origin/main`, e adicionar APPEND no handoff relevante.

