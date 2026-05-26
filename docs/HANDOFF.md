# SPRINT Handoff

## Estado atual

SPRINT e um app Android de treino continuo de matematica. A experiencia principal e a aba Sprint, com exercicio, rascunho, resposta, enter e scroll superior discreto.

Ultima intervencao: bug de escrita na resposta. O canvas agora registra o traco desde o primeiro toque/caneta, sem depender de `detectDragGestures`, e os callbacks salvam traços com `folhaId` explicito para evitar mistura entre exercicios quando `fieldIndex` repete.

## Decisoes recentes

- O topo troca abas por scroll, mas com atraso/estabilidade para evitar mudanca acidental.
- Triplo toque no enter abre scrolls secundarios de tema/densidade/zoom.
- Enter segurado mostra registro da sessao.
- Bolinha esquerda no divisor ajusta altura entre resolucao e resposta.
- Densidade pode ser por tema ou por exercicio exato.
- Zoom exato usa `template_pin` e `focus_source_exercise_id`.
- Backend consegue gerar pool semelhante para potencia/radiciacao e equacao linear simples.
- Painel/Perfil nao deve usar numeros mockados: deriva exercicios, acerto, tempo e tema mais praticado de `skill-progress` e `sessions`.
- Historico de sessoes inclui `density`, vindo de `SessionConfig.fixation_density`.
- Historico de sessoes inclui `template` quando ha `template_pin`; o Painel mostra apenas marcador discreto `zoom` para densidade exata.
- Painel abre direto no Perfil para mostrar exercicios feitos; lista Perfil/Historico continua acessivel pelo pill.
- Selecionar skill na Arvore deve iniciar sessao usando a skill tocada diretamente, limpando zoom exato anterior.
- Resultado da folha aparece por um instante na Sprint (`certo`/`erro`) antes de avancar.
- Sprint operacional usa `exercisesPerPage = 1` para registrar certo/errado a cada enter.
- Ao trocar/iniciar sessao, o estado local da folha deve ser limpo para nao carregar indice/traços antigos.
- Scroll superior esta maior e confirma troca apenas apos estabilizar o item no centro.
- Canvas principal usa captura bruta de ponteiro em `InkCanvas`: pontos, sinais e numeros pequenos devem aparecer no primeiro toque.
- O container da folha nao deve ter `clickable` vazio competindo com o canvas.
- `MainActivity` deve passar `folha.folhaId` para `syncScratch`, `syncAnswer` e `appendEvent`.
- Submit deve usar o estado mais recente de `FolhaViewModel` e apenas se `folhaId` bater com a folha atual.
- A chave visual do exercicio e `folhaId + exerciseId + fieldIndex`; nao usar apenas `fieldIndex`, porque no Sprint operacional ele tende a ser sempre 0.

## Arquivos mais relevantes

- `app/src/main/java/com/strava_matematica/MainActivity.kt`
- `app/src/main/java/com/strava_matematica/ui/folha/FolhaScreen.kt`
- `app/src/main/java/com/strava_matematica/ui/folha/ExerciseField.kt`
- `app/src/main/java/com/strava_matematica/viewmodel/SessionViewModel.kt`
- `app/src/main/java/com/strava_matematica/viewmodel/FolhaViewModel.kt`
- `backend/engine/adaptive.py`
- `backend/engine/focus_expansion.py`
- `backend/seed/generate_exercises.py`
- `backend/api/submit.py`
- `backend/api/activity.py`

## Como continuar

1. Leia `docs/PROJECT_SPEC.md` e `docs/UX_SPEC.md`.
2. Veja `.sprint/session.md`.
3. Rode `git status --short`.
4. Para UI Android, compile com `.\gradlew.bat :app:assembleDebug`.
5. Para backend, rode `python -m unittest` dentro de `backend/`.
6. Leia `docs/TRANSITION_SPEC.md` antes de continuar UX/QA da Sprint.

## Riscos conhecidos

- Varios arquivos ja estao modificados; nao reverter trabalho alheio.
- A UX precisa ser testada no tablet/emulador, nao apenas compilada.
- Geradores de zoom exato ainda cobrem poucos tipos.
- As migrations foram criadas, mas nem sempre aplicadas no banco local.
- `SUPER_SPEC.md` e handoffs antigos podem estar defasados; prefira `docs/` como fonte atual.
- Enter circular fica sobre a lateral direita; se o usuario escrever muito perto dele, pode parecer que o toque foi roubado. Validar no tablet antes de redesenhar.
- Gestos no pai (`sprintGestureInput`) usam `PointerEventPass.Initial`; 1 dedo deve passar para o canvas, mas multi-touch acidental pode avancar ou alternar borracha.
- A area de resposta e a unica enviada para OCR/correcao hoje; rascunho deve ser preservado para historico futuro, mas nao valida resposta.

## Proxima acao recomendada

Testar a Sprint no Android Studio/emulador e, se possivel, em tablet USB real. O ultimo teste local instalou no emulador `emulator-5554` e abriu sem crash fatal nos logs.

Checklist manual minimo:

- Escrever resposta curta: ponto, sinal de menos, `1`, `x`, `=`.
- Confirmar que o traco aparece imediatamente no primeiro toque.
- Apertar enter e ver feedback breve com a interpretacao reconhecida.
- Confirmar que o exercicio seguinte vem com resposta limpa.
- Errar de proposito e confirmar que contabiliza erro, mostra vermelho e avanca.
- Ir para Arvore, trocar skill e confirmar que a Sprint atualiza o tema.
- Abrir Painel e confirmar historico/progresso depois de alguns exercicios.

Ajustar sensibilidade de:

- Scroll superior.
- Triplo toque no enter.
- Arraste da bolinha divisoria.
- Confirmacao dos scrolls secundarios.
- Painel mostrando dados depois de submeter uma folha.
