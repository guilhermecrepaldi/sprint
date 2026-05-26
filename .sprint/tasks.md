# Sprint Tasks — 2026-05-26

## Backlog

### Prioridade 1 — Build e integridade

- [ ] [QA] Rodar `.\gradlew assembleDebug` e confirmar BUILD SUCCESSFUL (S)
- [ ] [QA] Verificar imports não usados em `FolhaScreen.kt` após adição de `ResultHistoryRow` e `retrySignal` (S)
- [ ] [QA] Verificar imports não usados em `MainActivity.kt` após adição de haptic e D3 alert (S)
- [ ] [QA] Verificar imports não usados em `SessionViewModel.kt` após adição de `ResultMark`, `recentResults`, `showConsecutiveFailureAlert` (S)
- [ ] [QA] Confirmar que `ResultMark` é importado corretamente em `FolhaScreen.kt` (`import com.strava_matematica.viewmodel.ResultMark`) (S)
- [ ] [QA] Verificar `HapticFeedbackType.TextHandleMove` e `LongPress` — confirmar API level mínimo (API 23+) com anotação `@RequiresApi` se necessário em `MainActivity.kt` (S)
- [ ] [QA] Verificar que `exercisesPerPage = 1` está sendo enviado no `SessionStartRequest` e respeitado em `get_first_folha` no backend (S)

### Prioridade 2 — UX alta prioridade

- [ ] [CODE] A2: Adicionar watermark "resposta" na área de resposta do `InkCanvas` em `ExerciseField.kt` — texto fantasma centralizado com `alpha = 0.18f`, some ao primeiro stroke (`onPenEvent` recebido) (S)
- [ ] [CODE] A2: Passar estado `hasAnswerStrokes: Boolean` para `InkCanvas` para controlar visibilidade da watermark sem acoplamento ao `ExerciseField` (S)
- [ ] [CODE] B2: Adicionar botão inline de toggle "livre / precisa acertar" diretamente na `FolhaScreen` — ícone pequeno (lock/unlock) no canto superior direito do campo ativo, chama `onConfigChange` com novo `requireCorrectToAdvance` (M)
- [ ] [CODE] B2: Garantir que o toggle inline em `FolhaScreen` e o toggle em `FolhaSettingsSheet` permaneçam sincronizados via `SessionConfig.requireCorrectToAdvance` (S)
- [ ] [CODE] D3: Refinar mensagem do AlertDialog de falhas consecutivas em `MainActivity.kt` — mudar para "Você errou 3 vezes seguidas. Respira fundo — quer tentar de novo ou pular?" com botões "Tentar de novo" e "Pular" (S)

### Prioridade 3 — Painel e Árvore

- [ ] [CODE] D1: Adicionar seção "Velocidade" no `DashboardTab.kt` — gráfico de linha fina (Canvas nativo Compose) com tempo médio por exercício dos últimos 7 dias, dados vindos de `skillAccuracy` e `skillAttempts` já disponíveis na state (M)
- [ ] [CODE] D1: Criar função `averageTimePerSkill(history: List<SprintHistoryItem>): Map<String, Float>` em `DashboardTab.kt` ou arquivo utilitário separado para alimentar o gráfico D1 (S)
- [ ] [CODE] D2: Colorir nós da árvore em `MathTreeTab.kt` por accuracy — vermelho (`Color(0xFFD32F2F)`) para accuracy < 0.60, verde (`Color(0xFF388E3C)`) para accuracy > 0.85, neutro (cor atual) entre os dois — usar `skillAccuracy` já passado como parâmetro (S)
- [ ] [CODE] D2: Adicionar legenda de cores discreta abaixo do título "ÁRVORE" em `MathTreeTab.kt` — três pontos coloridos com labels "<60%", "60-85%", ">85%" (S)

### Prioridade 4 — Qualidade do produto

- [ ] [CODE] Split ratio persistence: salvar `scratchRatio` por `field.fieldIndex` no `SharedPreferences` (chave `split_ratio_field_{index}`) ao soltar o `SplitHeightHandle` em `ExerciseField.kt` — restaurar no `remember` inicial (M)
- [ ] [CODE] Split ratio persistence: criar helper `SplitRatioPrefs` em arquivo separado `SplitRatioPrefs.kt` com `save(context, fieldIndex, ratio)` e `load(context, fieldIndex, default): Float` — não espalhar lógica de prefs no composable (S)
- [ ] [QA] Canvas guia: verificar visualmente se `guideMode = "horizontal"` e `guideMode = "dots"` em `InkCanvas` estão desenhando linhas/pontos corretamente no `ZoomableCanvas.kt` — rastrear chamada de `userGuideMode` do `SprintScrollConfigPage` até `ExerciseField` (S)
- [ ] [QA] Feedback overlay posicionamento: verificar em `FolhaScreen.kt` que `SprintFeedbackOverlay` é renderizado em camada acima do `ResultHistoryRow` (z-order via `Box` com `zIndex` ou ordem de composição) (S)
- [ ] [QA] Confirmar que `clearFieldAndRetry()` em `FolhaViewModel.kt` reseta `retryCount` corretamente ao avançar para próxima questão (não acumula entre questões) (S)
- [ ] [QA] Testar fluxo completo: escrever resposta → Enter → OCR → feedback overlay → reset de campo → próxima questão — verificar que não há delay artificial e que o estado `recentResults` atualiza (S)

### Prioridade 5 — Backend

- [ ] [QA] Verificar `GET /api/student/{id}/skill-progress` — endpoint existe em `backend/api/`? Se não, criar stub em arquivo `skill_progress.py` retornando `skillAccuracy` e `skillAttempts` do banco (M)
- [ ] [QA] Verificar se `DashboardTab.kt` e/ou `SessionViewModel.kt` consomem `GET /api/student/{id}/skill-progress` — se `fetchSkillProgress()` está implementado e mapeado corretamente (S)
- [ ] [QA] Motor adaptativo `backend/engine/adaptive.py`: confirmar que `RECOVERY_CONSECUTIVE_ERRORS = 3` aciona redução de dificuldade via `RECOVERY_DIFFICULTY_FACTOR = 0.65` e que o próximo exercício retornado tem `difficulty` menor (S)
- [ ] [QA] Biblioteca de exercícios: verificar quantos templates por skill existem em `exercise_library.py` — se alguma skill tem < 3 templates, registrar como gap para a sessão de geração com Gemini (S)

### Prioridade 6 — Infra

- [ ] [CODE] Substituir placeholder `"SEU_SERVIDOR.com"` no `buildType` release de `build.gradle.kts` por URL de ambiente (`BASE_URL_RELEASE`) lida de `local.properties` — evitar credencial hardcoded (S)
- [ ] [GIT] Fazer commit das mudanças desta sessão (`MainActivity.kt`, `SessionConfig.kt`, `FolhaScreen.kt`, `FolhaSettingsSheet.kt`, `FolhaViewModel.kt`, `SessionViewModel.kt`) com mensagem descritiva (S)

---

## Dependências

- [CODE] A2 watermark depende de [QA] build verde (Prioridade 1)
- [CODE] B2 toggle inline depende de [QA] build verde (Prioridade 1)
- [CODE] D1 gráfico depende de [QA] build verde (Prioridade 1)
- [CODE] D1 `averageTimePerSkill` deve ser criado antes do gráfico D1 ser conectado
- [CODE] D2 coloração de nós depende de [QA] build verde (Prioridade 1)
- [CODE] Split ratio persistence depende de `SplitRatioPrefs` helper ser criado primeiro
- [QA] `skill-progress` endpoint depende de existir antes de `DashboardTab` consumir
- [GIT] Commit (Prioridade 6) deve ser feito após [QA] build verde confirmado
