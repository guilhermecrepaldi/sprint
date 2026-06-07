# SPRINT Handoff

## Estado atual

SPRINT e um app Android de treino continuo de matematica. A experiencia principal e a aba Sprint, com exercicio, rascunho, resposta, enter e scroll superior discreto.

Arquitetura: offline-first deterministico. Catalogo SQLite embarcado (16.576 exercicios), Room/KSP para runtime local, validador deterministico, teclado matematico local. Backend FastAPI permanece para seed/testes/historico mas nao e necessario para o Sprint central.

Ultima intervencao: crash fix (ML Kit invalid language tag) + ArenaManager fix + thread safety + dead code removal. App roda sem crash no emulador Medium_Tablet.

## Commits recentes

```
137cfbe fix: remove invalid 'default' ML Kit language tag that caused crash
81ac25a fix: MlKitRecognizer dual model fallback + numeric post-processing
47e8cb3 fix: ArenaManager daily seed, thread safety, dead code removal
20cd54f feat: JDK 21 setup, emulator QA, diagnostic report, handoff update
```

## Diagnostico resumido

| Area | Status |
|---|---|
| Build Android | OK — BUILD SUCCESSFUL (14s) |
| Backend | OK — 109 pytest, 105 unittest, 28 endpoints |
| Sprint loop core | OK |
| Motor procedural (14 geradores) | OK — 90+ skills, dead code removido |
| Room (2 databases) | OK — `fallbackToDestructiveMigration` (risco) |
| Elo + BKT adaptativo | OK |
| Canvas/Ink | OK |
| ML Kit recognizer | OK — math model + post-processamento numerico |
| ArenaManager | OK — seed diaria year*1000+dayOfYear |
| Thread safety | OK — synchronizedList em ambos |
| Dead code | OK — -295 linhas removidas |
| IinkRecognizer | STUB — retorna null |
| ChallengeSyncWorker | PARCIAL — sync simulado |
| Social login | TODO — so UI |
| StudyPlans | SO DADOS — sem UI |
| identifyTopic | VAZIO |
| Testes Android | NENHUM |

## Fixes desde o ultimo handoff

1. **JDK 21 instalado**: Microsoft OpenJDK 21.0.11.10-hotspot. JAVA_HOME configurado.
2. **ArenaManager**: seed diaria agora usa `year*1000+dayOfYear` (antes retornava sempre 42 por string vazia).
3. **Thread safety**: `ProceduralEngine.statementHistory` e `LocalSprintRepository.recentStatements` usam `Collections.synchronizedList`.
4. **Dead code removido**: -295 linhas em ProceduralAlgebra (generateSystems, generatePolynomials), ProceduralCalculus (6 metodos), ProceduralGeometry (generateProgressoes, generateCombinatoria, generateProbabilidade, generateTrigRazoes, generateTrigSenoCosseno, generateTrigIdentidades, generateTrigEquacoes, generateTrigonometry).
5. **ML Kit crash fix**: tag `"default"` nao e valida para Digital Ink Recognition. Removida. App usa apenas `zxx-Zsym-x-math`.
6. **MlKitRecognizer melhorado**: post-processamento que prioriza candidatos numericos quando a resposta esperada e numero, extrai digitos de texto lixo.

## O que falta para subir

**IMPORTANTE:**
1. QA funcional no emulador (11 passos do TRANSITION_SPEC) — testar escrita, enter, feedback, painel, arvore
2. Migrations reais em vez de `fallbackToDestructiveMigration`
3. Testes Android unitarios

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
- `app/src/main/java/com/strava_matematica/recognizer/MlKitRecognizer.kt`
- `app/src/main/java/com/strava_matematica/domain/procedural/ArenaManager.kt`
- `backend/engine/adaptive.py`
- `backend/api/submit.py`

## Ambiente de build

- JAVA_HOME: `C:\Program Files\Microsoft\jdk-21.0.11.10-hotspot`
- Android Studio JBR: `C:\Program Files\Android\Android Studio\jbr`
- Emuladores: `Medium_Tablet` (Android 15), `Pixel_Tablet`
- Build: `$env:JAVA_HOME = "C:\Program Files\Microsoft\jdk-21.0.11.10-hotspot"; .\gradlew.bat :app:assembleDebug`
- Install: `$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"; .\gradlew.bat :app:installDebug`
- Backend: PostgreSQL offline; `.\simular_android.ps1 -NoBackend` para QA sem backend

## Decisoes recentes

- Sprint central roda 100% offline-first; backend e opcional em runtime
- Resposta corrigida vem do teclado matematico local + ML Kit para escrita livre
- ML Kit usa apenas `zxx-Zsym-x-math`; tag `"default"` causa crash
- Caneta e rascunho/telemetria; ML Kit reconhece e preenche campo de resposta
- `exercisesPerPage = 1` no fluxo Sprint (cada enter = 1 tentativa)
- Engine sugere, nao decide (mudancas dependem do usuario)
- 5 erros consecutivos alertam; permanecer/ajustar sao opcoes do usuario
- Mastery e sugestao nao-bloqueante
- `skill_pin` e contrato forte: nao mudar de tema automaticamente

## Como continuar

1. Leia `docs/PROJECT_SPEC.md`, `docs/UX_SPEC.md`, `docs/ROADMAP.md`.
2. Veja `.sprint/session.md`.
3. Build: `$env:JAVA_HOME = "C:\Program Files\Microsoft\jdk-21.0.11.10-hotspot"; .\gradlew.bat :app:assembleDebug`
4. Install: `$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"; .\gradlew.bat :app:installDebug`
5. QA: 11 passos do `docs/TRANSITION_SPEC.md`.
6. Backend: `python -m pytest` dentro de `backend/`.

## Riscos conhecidos

- `fallbackToDestructiveMigration` em ambos databases: schema bump destroi dados do usuario
- ML Kit math model pode alucinar em numeros simples (post-processamento mitiga)
- `ExerciseField` usa reflection Java para acessar `PointerEvent.motionEvent` (fragil)
- `FolhaScreen` tem 1200+ linhas; `ExerciseField` tem 877 linhas (God-object risk)
- Sem testes Android unitarios
- Social login e StudyPlans sao UI-only ou data-only
