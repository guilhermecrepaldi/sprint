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

Nao mexer agora na biblioteca ampla de exercicios, salvo se o usuario pedir explicitamente.

## Estado tecnico atual

Android:

- `MainActivity.kt` orquestra abas, Sprint, feedback e submit.
- `FolhaScreen.kt` monta a folha, enter, scrolls secundarios, gesto de Sprint e registro de sessao.
- `ExerciseField.kt` contem `InkCanvas`, divisor de rascunho/resposta e captura de escrita.
- `FolhaViewModel.kt` guarda traços, eventos, timing e `folhaId`.
- `SessionViewModel.kt` inicia sessoes, aplica tema/densidade/zoom, submete e atualiza historico/progresso.

Backend:

- `backend/api/submit.py` corrige e registra tentativas.
- `backend/api/activity.py` expoe historico/progresso.
- `backend/engine/adaptive.py` escolhe proximos exercicios.
- `backend/engine/focus_expansion.py` cria variacoes para zoom exato em alguns templates.

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
- bolinha esquerda: ajustar altura do rascunho/resposta;
- dois dedos: avancar, se configurado;
- segurar na folha: borracha, se configurado.

## Teste manual obrigatorio

Antes de declarar pronto:

1. Rodar `.\gradlew.bat :app:assembleDebug`.
2. Instalar em emulador ou tablet: `.\gradlew.bat :app:installDebug`.
3. Abrir app e escrever na resposta:
   - ponto;
   - sinal de menos;
   - `1`;
   - `x`;
   - `=`;
   - uma resposta curta de equacao.
4. Confirmar que o traco aparece imediatamente.
5. Apertar enter.
6. Confirmar feedback certo/errado e texto interpretado.
7. Confirmar que a proxima resposta vem limpa.
8. Fazer 3 a 5 exercicios e abrir Painel.
9. Confirmar que Historico/Perfil mudaram.
10. Ir para Arvore, trocar skill e confirmar que a Sprint muda de tema.

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
- A area de rascunho ainda nao e enviada para OCR; somente resposta valida.
- Backend/OCR pode interpretar errado mesmo quando o canvas funciona.
- Historico depende do backend ativo e do banco/migrations estarem coerentes.
- A arvore pode iniciar skill nova, mas o usuario precisa ver isso refletido no Sprint imediatamente.

## Proxima tarefa recomendada

Fazer QA funcional no emulador/tablet e corrigir o primeiro ponto real que falhar, nesta ordem:

1. escrita aparece;
2. resposta reseta;
3. enter envia;
4. feedback mostra interpretacao;
5. Painel registra;
6. Arvore troca Sprint.
