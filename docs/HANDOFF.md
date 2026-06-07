# SPRINT Handoff

## Estado atual

SPRINT e um app Android de treino continuo de matematica. A experiencia principal e a aba Sprint, com exercicio, rascunho, resposta, enter e scroll superior discreto.

Arquitetura: offline-first deterministico. Catalogo SQLite embarcado (16.576 exercicios), Room/KSP para runtime local, validador deterministico, teclado matematico local. Backend FastAPI permanece para seed/testes/historico mas nao e necessario para o Sprint central.

Ultima intervencao: diagnostico completo + build + QA emulador. JDK 21 (Microsoft OpenJDK) instalado. `Medium_Tablet` emulator rodando. App instalou e abriu sem crash.

## Diagnostico resumido

| Area | Status |
|---|---|
| Build Android | OK — `assembleDebug` BUILD SUCCESSFUL (2s, 39 tasks) |
| Backend | OK — 109 pytest verdes, 105 unittest verdes, 28 endpoints |
| Sprint loop core | OK — sessao, submit, avanco, retry |
| Motor procedural (14 geradores) | OK — 90+ skills, dead code em topicos avancados |
| Room (2 databases) | OK — `fallbackToDestructiveMigration` (risco) |
| Elo + BKT adaptativo | OK |
| Canvas/Ink | OK — undo/redo, eraser, guide modes |
| ML Kit recognizer | OK |
| IinkRecognizer | STUB — retorna null |
| ArenaManager | QUEBRADO — seed sempre 42 |
| ChallengeSyncWorker | PARCIAL — sync simulado |
| Social login | TODO — so UI |
| StudyPlans | SO DADOS — sem UI |
| identifyTopic | VAZIO |
| Testes Android | NENHUM |
| Thread safety | CONCERNCIA — LinkedList sem sync em ProceduralEngine |
| Dead code | 12+ metodos privados nao chamados |

## O que foi feito e parou

Sprint lote 10 concluiu a migracao offline-first deterministica:
- Room/KSP adicionado
- `exercise_catalog.db` embarcado com 16.576 exercicios
- `LocalSprintRepository` substitui backend no fluxo central
- ML Kit virou stub sem IA
- Teclado matematico local para resposta corrigida
- Caneta como rascunho/telemetria
- `simular_android.ps1 -NoBackend` PASS

Parou no diagnostico: 16 arquivos modificados nao commitados, QA funcional incompleto.

## O que falta para subir

**BLOQUEANTE:**
1. Commit dos 16 arquivos modificados
2. QA funcional no emulador (11 passos do TRANSITION_SPEC)
3. Fix `ArenaManager` (seed diaria sempre retorna 42)

**IMPORTANTE:**
4. Thread safety em `ProceduralEngine.statementHistory` e `LocalSprintRepository.recentStatements`
5. Migrations reais em vez de `fallbackToDestructiveMigration`
6. Remover dead code (12+ metodos nao chamados)
7. Testes Android unitarios

**OPCIONAL:**
- Split ratio persistence
- Zoom exato QA
- Social login OAuth
- IinkRecognizer (MyScript SDK)
- `identifyTopic` implementacao
- `StudyPlanDao` integracao com UI

## Arquivos mais relevantes

- `app/src/main/java/com/strava_matematica/MainActivity.kt`
- `app/src/main/java/com/strava_matematica/ui/folha/FolhaScreen.kt`
- `app/src/main/java/com/strava_matematica/ui/folha/ExerciseField.kt`
- `app/src/main/java/com/strava_matematica/viewmodel/SessionViewModel.kt`
- `app/src/main/java/com/strava_matematica/viewmodel/FolhaViewModel.kt`
- `app/src/main/java/com/strava_matematica/data/local/repository/LocalSprintRepository.kt`
- `app/src/main/java/com/strava_matematica/domain/procedural/ProceduralEngine.kt`
- `backend/engine/adaptive.py`
- `backend/api/submit.py`
- `backend/api/activity.py`

## Ambiente de build

- JAVA_HOME: `C:\Program Files\Microsoft\jdk-21.0.11.10-hotspot` ( Microsoft OpenJDK 21)
- Android Studio JBR: `C:\Program Files\Android\Android Studio\jbr` (usado pelo simular_android.ps1)
- Emuladores: `Medium_Tablet` (Android 15), `Pixel_Tablet`
- Build: `.\gradlew.bat :app:assembleDebug` com `$env:JAVA_HOME = "C:\Program Files\Microsoft\jdk-21.0.11.10-hotspot"`
- Backend: PostgreSQL offline; `.\simular_android.ps1 -NoBackend` para QA sem backend

## Decisoes recentes

- Sprint central roda 100% offline-first; backend e opcional em runtime
- Resposta corrigida vem do teclado matematico local, nao de OCR
- Caneta e apenas rascunho/telemetria
- `exercisesPerPage = 1` no fluxo Sprint (cada enter = 1 tentativa)
- Engine sugere, nao decide (mudancas dependem do usuario)
- 5 erros consecutivos alertam; permanecer/ajustar sao opcoes do usuario
- Mastery e sugestao nao-bloqueante
- `skill_pin` e contrato forte: nao mudar de tema automaticamente

## Como continuar

1. Leia `docs/PROJECT_SPEC.md`, `docs/UX_SPEC.md`, `docs/ROADMAP.md`.
2. Veja `.sprint/session.md`.
3. Rode `.\gradlew.bat :app:assembleDebug` com JAVA_HOME setado.
4. Instale com `.\gradlew.bat :app:installDebug` ou `.\simular_android.ps1 -NoBackend`.
5. QA: 11 passos do `docs/TRANSITION_SPEC.md`.
6. Para backend: `python -m pytest` dentro de `backend/`.

## Riscos conhecidos

- `fallbackToDestructiveMigration` em ambos databases: schema bump destroi dados do usuario
- Thread safety: `statementHistory` (LinkedList) e `recentStatements` (mutableListOf) sem sincronizacao
- Dead code em ProceduralAlgebra, ProceduralCalculus, ProceduralGeometry
- `ArenaManager.getCurrentTournamentSeed()` sempre retorna 42
- `ExerciseField` usa reflection Java para acessar `PointerEvent.motionEvent` (fragil)
- `FolhaScreen` tem 1200+ linhas; `ExerciseField` tem 877 linhas (God-object risk)
- Sem testes Android unitarios
- Social login e StudyPlans sao UI-only ou data-only
