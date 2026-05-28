# SPRINT Project Spec

## Identidade

SPRINT e um app de estudo continuo de matematica. A meta e manter o aluno por horas resolvendo exercicios em um fluxo calmo, progressivo, registravel e adaptativo.

Inspiracao principal: Kumon, mas com escrita digital, telemetria, analise de fluxo, historico eterno e futura camada social/global.

## Principio do produto

O aluno aprende por resolucao repetida, graduada e parametrizada. Cada exercicio feito vira dado: resposta, tempo, escrita, apagamentos, pausa, dificuldade, template, tema, acerto, erro e contexto da sessao.

Regra central do motor: a engine sugere, sinaliza e mede; ela nao muda tema, densidade, zoom ou dificuldade como decisao oculta. Mudancas de Sprint devem ser confirmadas pelo usuario.

## Experiencia central

A tela inicial real e a aba Sprint. Ela deve mostrar somente:

- Scroll discreto superior de paginas/abas.
- Enunciado do exercicio.
- Area de resolucao/rascunho.
- Area de resposta.
- Enter circular.
- Divisor discreto com bolinha para altura da resolucao.

Nao colocar explicacoes fixas, tutorial, textos de feature ou paineis pesados dentro da Sprint.

## Modelo de exercicio

Cada exercicio deve ser parametrizado por:

- `skill_tags`
- `node_id`
- `template_id`
- `template_version`
- `variant_seed`
- `answer_type`
- `method_tags`
- `prerequisite_tags`
- `affinity_tags`
- `parameter_vector`
- `difficulty_vector`

O `template_id` e fundamental: ele representa o padrao do exercicio. Densidade exata usa esse campo para gerar exercicios extremamente semelhantes.

## Pesquisa e fontes abertas

Fontes abertas podem orientar mapa de conteudo, familias e progressao, mas os exercicios inseridos devem ser gerados localmente ou licenciados de forma clara. Para modular/trigonometria, o seed `sprint_parametric_modular_trig_v1` foi inspirado por mapas curriculares de OpenStax, Khan Academy e Grasple, sem copiar listas literalmente.

Ao ampliar biblioteca:

- registrar `source_library` e `source_license`;
- preservar `skill_tags`, `node_id`, `template_id`, `method_tags`, `parameter_vector` e `difficulty_vector`;
- rodar seed de forma aditiva/idempotente;
- nunca apagar tentativas, sessoes, `PenEvent` ou historico.

## Densidade e fixacao

Existem dois modos de foco:

- Foco por tema: muitos exercicios do mesmo tema/skill.
- Zoom exato: muitos exercicios do mesmo template do exercicio atual.

Exemplo esperado: se o aluno esta em potencia dificil e quer fixar aquele formato, ele aumenta a densidade daquele exercicio exato. O sistema cria ou seleciona cerca de 200 variacoes extremamente semelhantes, mudando parametros sem mudar o metodo mental principal.

`zoom exato` e escolha explicita. Nao substituir por dificuldade manual. O aluno pode escolher tema/densidade/zoom/dificuldade nos scrolls secundarios.

## Arquitetura offline atual

Sprint central no Android agora roda offline-first, sem backend em runtime e sem IA/OCR:

- Catalogo estatico Room/SQLite em `app/src/main/assets/databases/exercise_catalog.db`.
- Exportador dev-time em `backend/scripts/export_sqlite_catalog.py`, gerando 16.576 exercicios a partir do Postgres local.
- Banco runtime local em `sprint_runtime.db`, com `students`, `sessions`, `exercise_attempts`, `pen_events` e `student_skill_memory`.
- Repositorio local em `app/src/main/java/com/strava_matematica/data/local/repository/LocalSprintRepository.kt`.
- Validador deterministico em `DeterministicValidator.kt`: `exact`, `options`, `regex`, `numeric`, `fraction` e `equation`.
- `SessionViewModel` inicia sessao, submete resposta, atualiza historico, progresso e calendario pelo Room local.
- Caneta nao e interpretada. Ela fica como rascunho/telemetria. A resposta corrigida vem do teclado matematico local no campo de resposta.

Regra nova: qualquer fluxo que prometa correcao automatica de escrita livre esta fora do escopo enquanto o produto estiver 100% deterministico e sem IA.

## Arquitetura legada/dev-time

- Android/Kotlin/Jetpack Compose em `app/`.
- Backend FastAPI/SQLAlchemy em `backend/` permanece como fonte de seed, testes historicos e exportacao, mas nao e necessario para o Sprint central offline.
- Motor adaptativo em `backend/engine/adaptive.py`.
- Expansao de foco exato em `backend/engine/focus_expansion.py`.
- Seed parametrico em `backend/seed/generate_exercises.py`.
- Seed focado modular/trigonometria em `backend/seed/expand_modular_trig.py`.
- Historico e timeline em `backend/api/activity.py`.
- Calendario recente do Painel/Perfil via `GET /api/student/{student_id}/activity?days=35`.

## Contratos importantes

- `skill_pin`: fixa o tema.
- `template_pin`: fixa o padrao/template.
- `focus_source_exercise_id`: exercicio-semente do zoom exato.
- `focus_target_count`: alvo de repeticoes/variacoes.
- `difficulty_block_size`: tamanho do bloco antes da dificuldade mudar.
- `difficulty_step`: velocidade de progressao.
- `fixation_density`: densidade registrada da sessao (`leve`, `fixa`, `densa`, `exata`).

## Score e sugestoes

- Score publico pode ultrapassar 1000 em exercicios dificeis para permitir leitura e-sports/maratona.
- Escalas internas do motor adaptativo continuam normalizadas em 0..10; score bruto acima de 1000 nao deve empurrar thresholds alem de 10.
- 5 erros consecutivos disparam alerta discreto: `5 erros consecutivos podem afetar seu score. Deseja permanecer nesta Sprint?`
- `permanecer` mantem a Sprint exatamente como esta.
- `ajustar` abre controles de tema/densidade/zoom/dificuldade para o usuario escolher.
- A engine nao reduz dificuldade automaticamente por erro consecutivo.
- Mastery/acertos seguidos podem sugerir proximo tema, mas so avancam por toque do usuario.

## Sprint Arena

Arena e a camada competitiva separada da Sprint livre. A Sprint continua limpa; Arena vive no backend/Painel.

Contratos atuais:

- `SessionConfig.ranked_mode`: marca uma sessao como ranqueada.
- `SessionConfig.rules_version`: versao da regra competitiva; default ranqueado atual e `arena_v1`.
- `SessionConfig.arena_seed`: futuro seed de pacote fixo.
- `ExerciseAttempt.competitive_score`: score que pode entrar em ranking.
- `ExerciseAttempt.competitive_valid`: se a tentativa passou auditoria leve.
- `ExerciseAttempt.audit_flags`: motivos como `text_payload`, `too_fast`, `no_strokes`, `low_recognition_confidence`.
- `Session.competitive_score`, `Session.competitive_valid`, `Session.audit_flags`: agregado competitivo da sessao.

Ranking Arena:

- endpoint `GET /api/ranking/arena/weekly`;
- soma apenas sessoes publicas, ranqueadas, validas, `rules_version = arena_v1`;
- XP semanal antigo continua existindo como ranking de atividade, nao como ranking competitivo justo.

## Contrato de escrita e submissao

- O `studentId` local do Android deve ser persistido; reiniciar o app nao pode criar outro aluno e esconder historico/progresso.
- `FolhaUiState.folhaId` protege traços/eventos contra mistura entre exercicios.
- `fieldAnswerStrokes` e a fonte enviada para OCR/correcao.
- `fieldScratchStrokes` e rascunho/telemetria, nao resposta final hoje.
- `fieldEvents` e `fieldTiming` devem acompanhar a tentativa.
- `MainActivity` deve submeter somente o estado mais recente quando `folhaId` bate com a folha atual.
- Canvas da resposta deve resetar a cada novo exercicio usando chave composta por `folhaId`, `exerciseId` e `fieldIndex`.
- No fluxo Sprint atual, `exercisesPerPage = 1` para cada enter virar uma tentativa.
- Painel/Perfil deve buscar historico, progresso e activity do mesmo `studentId` persistido para nao fragmentar o aluno.

## Validacao minima

- Mudancas Android: `.\gradlew.bat :app:assembleDebug`.
- Mudancas backend: `python -m unittest`.
- Mudancas mecanicas: `git diff --check` nos arquivos tocados.
