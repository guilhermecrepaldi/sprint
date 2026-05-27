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

## Densidade e fixacao

Existem dois modos de foco:

- Foco por tema: muitos exercicios do mesmo tema/skill.
- Zoom exato: muitos exercicios do mesmo template do exercicio atual.

Exemplo esperado: se o aluno esta em potencia dificil e quer fixar aquele formato, ele aumenta a densidade daquele exercicio exato. O sistema cria ou seleciona cerca de 200 variacoes extremamente semelhantes, mudando parametros sem mudar o metodo mental principal.

`zoom exato` e escolha explicita. Nao substituir por dificuldade manual. O aluno pode escolher tema/densidade/zoom/dificuldade nos scrolls secundarios.

## Arquitetura atual

- Android/Kotlin/Jetpack Compose em `app/`.
- Backend FastAPI/SQLAlchemy em `backend/`.
- Motor adaptativo em `backend/engine/adaptive.py`.
- Expansao de foco exato em `backend/engine/focus_expansion.py`.
- Seed parametrico em `backend/seed/generate_exercises.py`.
- Historico e timeline em `backend/api/activity.py`.

## Contratos importantes

- `skill_pin`: fixa o tema.
- `template_pin`: fixa o padrao/template.
- `focus_source_exercise_id`: exercicio-semente do zoom exato.
- `focus_target_count`: alvo de repeticoes/variacoes.
- `difficulty_block_size`: tamanho do bloco antes da dificuldade mudar.
- `difficulty_step`: velocidade de progressao.
- `fixation_density`: densidade registrada da sessao (`leve`, `fixa`, `densa`, `exata`).

## Score e sugestoes

- 5 erros consecutivos disparam alerta discreto: `5 erros consecutivos podem afetar seu score. Deseja permanecer nesta Sprint?`
- `permanecer` mantem a Sprint exatamente como esta.
- `ajustar` abre controles de tema/densidade/zoom/dificuldade para o usuario escolher.
- A engine nao reduz dificuldade automaticamente por erro consecutivo.
- Mastery/acertos seguidos podem sugerir proximo tema, mas so avancam por toque do usuario.

## Contrato de escrita e submissao

- `FolhaUiState.folhaId` protege traços/eventos contra mistura entre exercicios.
- `fieldAnswerStrokes` e a fonte enviada para OCR/correcao.
- `fieldScratchStrokes` e rascunho/telemetria, nao resposta final hoje.
- `fieldEvents` e `fieldTiming` devem acompanhar a tentativa.
- `MainActivity` deve submeter somente o estado mais recente quando `folhaId` bate com a folha atual.
- Canvas da resposta deve resetar a cada novo exercicio usando chave composta por `folhaId`, `exerciseId` e `fieldIndex`.
- No fluxo Sprint atual, `exercisesPerPage = 1` para cada enter virar uma tentativa.

## Validacao minima

- Mudancas Android: `.\gradlew.bat :app:assembleDebug`.
- Mudancas backend: `python -m unittest`.
- Mudancas mecanicas: `git diff --check` nos arquivos tocados.
