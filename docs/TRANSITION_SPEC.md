# SPRINT Transition Spec - Codex para Claude

Este arquivo e o ponto de passagem para continuar o trabalho no Claude. Leia junto com `PROJECT_SPEC.md`, `UX_SPEC.md`, `ROADMAP.md`, `HANDOFF.md` e `.sprint/session.md`.

## Objetivo imediato

Validar e estabilizar o fluxo central da Sprint:

- aluno escreve a resposta;
- app registra traços e timing;
- enter envia um exercicio;
- sistema mostra leitura reconhecida em verde/vermelho;
- app avanca sem bloquear;
- proxima resposta vem limpa;
- Painel/Perfil refletem exercicios realmente feitos.
- engine sinaliza risco/sugestao sem decidir pelo usuario.

Nao mexer agora na biblioteca ampla de exercicios, salvo se o usuario pedir explicitamente.

## Estado tecnico atual

Android:

- `MainActivity.kt` orquestra abas, Sprint, feedback e submit.
- `FolhaScreen.kt` monta a folha, enter, scrolls secundarios, gesto de Sprint e registro de sessao.
- `ExerciseField.kt` contem `InkCanvas`, divisor de rascunho/resposta e captura de escrita.
- `FolhaViewModel.kt` guarda traços, eventos, timing e `folhaId`.
- `SessionViewModel.kt` inicia sessoes, aplica tema/densidade/zoom, submete e atualiza historico/progresso.
- `SessionViewModel.kt` tambem busca activity recente para o calendario compacto do Painel/Perfil.

Backend:

- `backend/api/submit.py` corrige e registra tentativas.
- `backend/api/activity.py` expoe historico/progresso.
- `backend/engine/adaptive.py` escolhe proximos exercicios.
- `backend/engine/focus_expansion.py` cria variacoes para zoom exato em alguns templates.
- Banco local deve estar em migration `0014_focus_sprint_blocks`; `simular_android.ps1` aplica `python -m alembic upgrade head` antes de abrir o app.

## Correcoes recentes que nao devem regredir

1. Escrita imediata no canvas:
   - `InkCanvas` nao deve usar `detectDragGestures` para a escrita principal.
   - O traco deve nascer no primeiro `down`, inclusive ponto e traço curto.

2. Escopo por folha:
   - `FolhaUiState` carrega `folhaId`.
   - `MainActivity` passa `folha.folhaId` para `syncScratch`, `syncAnswer` e `appendEvent`.
   - Submit usa o estado mais recente apenas se `latestFolhaState.folhaId == folha.folhaId`.

3. Reset entre exercicios:
   - A chave visual do canvas deve combinar `folhaId`, `exerciseId` e `fieldIndex`.
   - Como o Sprint roda um exercicio por vez, `fieldIndex` sozinho nao diferencia respostas.

4. Registro por exercicio:
   - `SessionViewModel` deve manter `exercisesPerPage = 1` no fluxo Sprint.
   - Cada enter corresponde a uma tentativa registrada.

5. Feedback:
   - Feedback e breve, discreto e nao vira tela permanente.
   - Correto: texto reconhecido em tinta/verde discreto.
   - Incorreto: texto reconhecido em vermelho.
   - Se nao leu: informar discretamente.

6. Sugestao sem decisao automatica:
   - 5 erros consecutivos mostram alerta de score.
   - `permanecer` nao altera nada.
   - `ajustar` abre scrolls secundarios.
   - Backend nao deve reduzir dificuldade sozinho em recovery.
   - 5 acertos seguidos podem sugerir proximo tema, mas nao trocar automaticamente.

7. Score e motor:
   - Score publico pode ultrapassar 1000 em exercicios dificeis.
   - Thresholds internos seguem escala 0..10; nao usar score bruto acima de 1000 diretamente para restart/adaptacao.
   - `python -m pytest` cobre o fluxo de maratona e-sports; nao validar apenas com `unittest`.

8. Aviso de score:
   - Depois que o aluno escolhe `permanecer`, o mesmo aviso nao deve reaparecer a cada novo erro da mesma sequencia.
   - O aviso volta somente depois que a sequencia quebra ou a sessao/configuracao muda.

9. Arena/ranked:
   - `ranked_mode` separa Sprint livre de sessao competitiva.
   - `competitive_score` nao substitui XP nem score livre.
   - Tentativas com `audit_flags` nao entram no ranking Arena.
   - Endpoint atual: `GET /api/ranking/arena/weekly`.

10. Simulacao de usuario:
   - `backend/scripts/simulate_user_flow.py` roda contra a API local e valida troca de temas com registro em historico, calendario/timeline e perfil.
   - O registro do enter deve acumular a sessao de estudo viva no app, mesmo quando trocar tema.
   - Com o roteiro 5 acertos/5 erros por tema em 4 temas, o acumulado final esperado e 40 feitos, 20 acertos e 20 erros.
   - O checkpoint de 25 exercicios deve marcar 10 acertos e 15 erros.
   - O Android deve persistir `student_id_v1`; reiniciar o app nao pode criar outro aluno.
   - O Painel/Perfil deve exibir o calendario compacto vindo de `/activity`, nao apenas validar isso no backend.

## Contrato de UX da Sprint

Na aba Sprint, manter apenas:

- scroll superior;
- enunciado;
- rascunho/resolucao;
- resposta;
- enter circular;
- linha/divisor com bolinha.

Nao adicionar tutorial, texto explicativo fixo, cards, menu pesado ou painel dentro da Sprint.

Gestos atuais:

- topo: scroll para mudar aba;
- enter toque: confirmar/avancar;
- enter segurado: registro da sessao;
- enter triplo toque: tela de scrolls secundarios;
- scrolls secundarios: tema, densidade, zoom e dificuldade;
- bolinha esquerda: ajustar altura do rascunho/resposta;
- dois dedos: avancar, se configurado;
- segurar na folha: borracha, se configurado.

## Teste manual obrigatorio

Nota offline deterministica: nao existe OCR/IA. Para resposta corrigida, tocar na area de resposta, usar o teclado matematico local e depois Enter. Escrever com caneta livre e apenas rascunho/telemetria.

Antes de declarar pronto:

1. Rodar `.\gradlew.bat :app:assembleDebug`.
2. Instalar em emulador ou tablet: `.\gradlew.bat :app:installDebug`.
3. Abrir app, tocar na resposta e usar o teclado matematico local:
   - ponto;
   - sinal de menos;
   - `1`;
   - `x`;
   - `=`;
   - uma resposta curta de equacao.
4. Confirmar que o rascunho com caneta aparece imediatamente e que a resposta estruturada aparece no campo de resposta.
5. Apertar enter.
6. Confirmar feedback certo/errado e texto interpretado.
7. Confirmar que a proxima resposta vem limpa.
8. Fazer 3 a 5 exercicios e abrir Painel.
9. Confirmar que Historico/Perfil mudaram e que o Calendario do Perfil aparece.
10. Ir para Arvore, trocar skill e confirmar que a Sprint muda de tema.
11. Testar especificamente `funcao_modular`, `trig_razoes`, `trig_seno_cosseno_tangente`, `trig_identidades` e `trig_equacoes`: a Sprint nao deve manter a folha antiga durante o carregamento.

## USB/tablet

Para testar em tablet Android real:

1. Ativar Opcoes do desenvolvedor no tablet.
2. Ativar Depuracao USB.
3. Conectar via USB e aceitar a chave RSA.
4. Rodar:

```powershell
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
& $adb devices
.\gradlew.bat :app:installDebug
```

Se aparecer `unauthorized`, desbloquear o tablet e aceitar a permissao. Se nao aparecer, trocar cabo/porta e confirmar modo USB.

## Riscos para Claude observar

- O enter circular pode interceptar escrita na borda direita.
- `sprintGestureInput` no pai pode reagir a multi-touch acidental.
- A area de rascunho nao e corrigida; somente a resposta estruturada pelo teclado local valida.
- Nao reintroduzir OCR, ML Kit, MyScript ou backend de correcao sem decisao explicita do usuario.
- Historico do Sprint central depende do Room local, nao do backend.
- A arvore pode iniciar skill nova, mas o usuario precisa ver isso refletido no Sprint imediatamente.
- Seeds e geracao Gemini devem ser aditivos; nunca apagar attempts, pen_events ou historico.
- Se alterar biblioteca, preservar `template_id`, `difficulty_vector` e metadados de progressao.
- Para ampliar modular/trig, preferir `backend/seed/expand_modular_trig.py`; ele e aditivo/idempotente e usa `source_library = sprint_parametric_modular_trig_v1`.

## Proxima tarefa recomendada

Fazer QA funcional no emulador/tablet e corrigir o primeiro ponto real que falhar, nesta ordem:

1. escrita aparece;
2. resposta reseta;
3. enter envia;
4. feedback mostra interpretacao;
5. Painel registra;
6. Arvore troca Sprint.
7. 5 erros seguidos mostram alerta de score sem alterar a dificuldade sozinho.
