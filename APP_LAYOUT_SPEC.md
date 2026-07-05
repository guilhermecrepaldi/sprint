# APP LAYOUT SPEC v1.0 — Strava da Matemática
# Para IA Especializada em UI/UX Android

> STATUS: HISTORICO. Para UX atual, leia `AGENTS.md`, `docs/README.md` e `docs/UX_SPEC.md`.
> A navegacao atual prioriza Sprint limpa, scroll superior discreto e scrolls secundarios pelo enter.

> Objetivo: construir o layout completo do app Android do "Strava da Matemática" com estética premium, funcionalidade real e foco em adolescentes 13-17 treinando matemática de alta performance.
> Este documento é auto-suficiente para design e implementação visual. Para backend/API, ler `SUPER_SPEC.md`.

---

## 1. Produto em Uma Frase

Um app de treino matemático estilo "deep work" onde o aluno resolve folhas com caneta/stylus, recebe correção por página e vê sua evolução cognitiva sem gamificação infantil.

---

## 2. Direção de Design

### 2.1 Personalidade

- Sério, calmo, preciso.
- Mais "caderno de treino de atleta" do que "app escolar colorido".
- Deve parecer feito para foco, repetição e domínio.
- O usuário deve sentir: "estou treinando de verdade".

### 2.2 Não Fazer

- Não usar estética infantil, mascotes, confete, medalhas fofas ou ilustrações escolares genéricas.
- Não criar landing page dentro do app.
- Não usar hero marketing.
- Não usar paleta dominada por roxo, azul escuro, bege/creme ou laranja/marrom.
- Não encher a tela de cards decorativos.
- Não explicar a interface com textos longos.
- Não usar sombras pesadas, blobs, gradientes decorativos ou bokeh.

### 2.3 Referências de Sensação

- Notebook de matemática premium.
- Painel de treino de corrida/ciclismo, mas minimalista.
- Ambiente de estudo silencioso.
- "Strava" só como inspiração de treino e progresso, não como cópia visual.

---

## 3. Plataforma e Tecnologia Visual

### 3.1 Target

- Android tablet primeiro.
- Funcionar também em celular grande, mas a experiência principal é tablet com stylus.
- Orientação principal: portrait.
- Landscape deve ser suportado de forma aceitável para tablets.

### 3.2 Stack Recomendada

- Kotlin.
- Jetpack Compose.
- Material 3 como base, mas com componentes customizados para canvas/folha.
- Navigation Compose.
- ViewModel + StateFlow.
- Retrofit/OkHttp para API.

### 3.3 Densidade e Layout

- Interface deve ser densa, limpa e operacional.
- Priorizar leitura rápida e área de escrita.
- A tela de resolução deve reservar a maior parte da viewport para a folha/canvas.

---

## 4. Sistema Visual

### 4.1 Paleta

Criar duas aparências governadas por `SessionConfig.background`.

#### Tema White Focus

- App background: `#F7F8F6`
- Surface principal: `#FFFFFF`
- Ink/text primary: `#151713`
- Text secondary: `#5F665C`
- Hairline/border: `#D9DED6`
- Field fill: `#FBFCFA`
- Accent progress: `#1E7F5C`
- Accent warning/hesitation: `#B7791F`
- Accent error: `#C2413A`
- Accent info: `#2F6F9F`

#### Tema Dark Focus

- App background: `#11140F`
- Surface principal: `#171B15`
- Ink/text primary: `#F2F5EF`
- Text secondary: `#A9B2A3`
- Hairline/border: `#30362C`
- Field fill: `#1D221B`
- Accent progress: `#4CC38A`
- Accent warning/hesitation: `#D6A13D`
- Accent error: `#E05D55`
- Accent info: `#6EA8D8`

### 4.2 Tipografia

Usar uma família sans-serif moderna e legível:

- Preferência: Inter, Roboto Flex ou system font.
- Números tabulares para timer, score e métricas.
- Expressões matemáticas devem usar fonte com boa legibilidade de símbolos; se possível, usar renderização LaTeX/MathJax equivalente no Android ou fallback com texto monoespaçado apenas nos enunciados complexos.

Escala:

- Screen title: 22sp, weight 600.
- Section label: 13sp, weight 600, uppercase opcional com tracking leve.
- Body: 16sp.
- Field statement: 17sp a 19sp.
- Metrics compact: 12sp a 14sp.
- Timer: 18sp tabular.
- Botões: 15sp weight 600.

Não escalar fonte com largura da viewport. Usar breakpoints e wrapping.

### 4.3 Forma

- Cards só para itens repetidos, resultados e modais.
- Raio máximo padrão: 8dp.
- Botões icon-only para ações óbvias: voltar, pausar, enviar, apagar, trocar tema, abrir configurações.
- Tooltips/labels curtos para ícones menos óbvios.
- Borda fina é preferível a sombra.

### 4.4 Espaçamento

- Grid base: 4dp.
- Padding tela: 16dp celular, 24dp tablet.
- Gap entre grupos: 16dp.
- Gap compacto interno: 8dp.
- A folha deve ter margens internas suficientes para mão/stylus: mínimo 16dp.

---

## 5. Navegação do App

Fluxo principal:

```text
Launch
→ SessionConfigScreen
→ FolhaScreen
→ PageResultScreen
→ FolhaScreen próxima
→ SessionSummaryScreen quando finished
```

Fluxos auxiliares:

```text
FolhaScreen → PauseSheet
FolhaScreen → ConfirmSubmitSheet
FolhaScreen → ConnectionStatusSheet
PageResultScreen → SkillDetailSheet
SessionSummaryScreen → RestartSession / NewConfig
```

O app deve abrir direto na configuração ou na sessão ativa se houver sessão local em andamento.

---

## 6. Telas

## 6.1 SessionConfigScreen

### Objetivo

Permitir ao aluno configurar a sessão antes de começar, sem parecer formulário burocrático.

### Layout

Tablet portrait:

```text
Top bar:
  [Logo textual Strava Matemática]        [API status dot]

Main:
  Coluna esquerda: Configuração essencial
  Coluna direita: Preview da folha

Bottom:
  [Iniciar sessão]
```

Celular:

```text
Top bar
Scrollable config
Preview compacto
Sticky bottom bar com [Iniciar]
```

### Componentes

1. Header compacto
   - Título: `Treino`
   - Subtexto curto: `Monte a folha e comece.`
   - Sem texto explicativo longo.

2. Modo de feedback
   - Segmented control:
     - `Visível`
     - `Blind`
   - Mapeia `show_thermometer`.

3. Aparência
   - Segmented control com ícones:
     - Sol: white
     - Lua: dark
   - Mapeia `background`.

4. Caneta
   - Swatches circulares ou quadrados pequenos.
   - Opções:
     - Preto `#1a1a1a`
     - Verde foco `#1E7F5C`
     - Azul técnico `#2F6F9F`
     - Vermelho correção `#C2413A`
   - Campo custom opcional só se simples.

5. Duração
   - Segmented control:
     - `Livre`
     - `Tempo`
     - `Folhas`
   - Se `Tempo`: stepper/chips `30m`, `1h`, `2h`.
   - Se `Folhas`: stepper de 1 a 20.

6. Dificuldade
   - Slider `difficulty_start` 1.0 a 10.0.
   - Mostrar valor como badge discreto: `2.0`.
   - Segmented control:
     - `Linear`
     - `Geométrica`
   - Se linear: stepper `difficulty_step`.
   - Se geométrica: stepper `difficulty_ratio`.

7. Reinício automático
   - Toggle.
   - Se ligado:
     - Slider/meta `restart_on_avg`, default 7.0.
     - Stepper `restart_window`, default 10.

8. Exercícios por folha
   - Stepper: 3, 5, 8, 10.
   - Default 5.

9. Preview da folha
   - Mini folha com campos numerados.
   - Refletir background e cor da caneta.
   - Preview não deve parecer card dentro de card.

### Estado de API

Mostrar status pequeno:

- Verde: API ok.
- Amarelo: conectando.
- Vermelho: offline.

Não bloquear edição se offline, mas botão iniciar deve mostrar erro claro se API falhar.

### Botão Primário

Texto: `Iniciar treino`
Ícone: play/arrow.
Altura: 48dp.
Fixo no bottom em celular.

---

## 6.2 FolhaScreen

### Objetivo

Ser a tela principal de resolução. Máxima área de escrita, mínima distração.

### Layout

Tablet portrait:

```text
Top training bar (56dp):
  [Voltar/Pausar] [Página 1] [Dificuldade 2.0] [Timer] [Termômetro opcional] [Enviar]

Body:
  Folha full-width, scroll se necessário
  Campo 1
  Campo 2
  Campo 3
  Campo 4
  Campo 5

Bottom mini toolbar:
  [Caneta] [Borracha] [Undo] [Redo] [Limpar campo] [Cor] [Espessura]
```

Celular:

```text
Top compact bar
Scrollable folha
Bottom toolbar horizontal
Enviar como FAB ou sticky button
```

### Folha

Cada campo deve ter:

- Número do campo no canto superior esquerdo.
- Enunciado.
- Área demarcada para resposta final.
- Espaço suficiente para rascunho dentro do campo, se necessário.
- Linha/base sutil para resposta final.
- Estado ativo com borda accent.
- Estado preenchido com pequena marca discreta.

Campo padrão:

```text
[01] Resolva: 3x + 7 = 22

Área de escrita livre

Resposta final: ______________________
```

Como o backend faz OCR apenas do crop de campo, o layout deve permitir capturar o crop por campo. Cada campo precisa ter coordenadas e bounds bem definidos.

### Canvas/Stylus

Requisitos visuais:

- Escrita precisa acompanhar stylus com baixa latência.
- Stroke deve respeitar `pen_color`.
- Espessura default: 2.2dp.
- Borracha deve remover traços por interseção ou apagar stroke inteiro.
- Não usar animações que atrasem a escrita.

Eventos capturados:

- `stroke_start`
- `stroke_move`
- `stroke_end`
- `erase`

Telemetria:

- `ts`, `x`, `y`, `pressure`, `tilt`, `velocity`, `event_type`.

### Termômetro

Se `show_thermometer=true`:

- Aparece na top bar como barra fina ou medidor compacto.
- Valor de 0 a 1.
- Cores:
  - 0.0-0.45: error
  - 0.45-0.70: warning
  - 0.70-1.0: progress
- Nunca ocupar mais que 120dp de largura.

Se `show_thermometer=false`:

- Não mostrar placeholder.
- Não dizer "modo blind" o tempo todo.

### Submit

Botão enviar:

- Ícone check/send.
- Desabilitado se nenhum campo teve stroke.
- Ao tocar:
  - Abrir bottom sheet de confirmação se houver campos vazios.
  - Caso todos preenchidos, enviar direto ou confirmar com sheet compacto.

Loading:

- Overlay leve: `Corrigindo...`
- Mostrar progresso por campo: 1/5, 2/5 se possível.
- Não bloquear com modal agressivo se a rede estiver lenta; mas impedir edição durante upload.

Erros:

- API offline: bottom sheet com `Tentar novamente`.
- OCR falhou para campo: resultado mostra "Não consegui ler" e permite refazer página futuramente.

---

## 6.3 PageResultScreen

### Objetivo

Mostrar correção da folha e preparar a próxima, sem quebrar o ritmo.

### Layout

Tablet:

```text
Top:
  [Página 1 concluída] [Score da página] [Termômetro]

Main:
  Lista de resultados por campo
  Painel lateral: resumo cognitivo

Bottom:
  [Próxima folha] [Revisar erros]
```

Celular:

```text
Header compacto
Lista vertical de resultados
Sticky bottom: Próxima folha
```

### Resultado por Campo

Card simples por exercício:

- Número do campo.
- Enunciado curto.
- Resposta reconhecida.
- Resposta esperada.
- Status:
  - Correto: check verde.
  - Errado: x vermelho.
  - OCR inválido: ícone alert.
- Score.
- Error type se houver:
  - `sinal`
  - `fracao`
  - `equacao_2_grau`
  - `desconhecido`

Não usar textos motivacionais genéricos. A informação deve ser direta.

### Painel Cognitivo

Mostrar no máximo 4 métricas:

- Acerto
- Velocidade
- Fluidez
- Hesitação

Visual:

- Barras horizontais finas.
- Valores discretos.
- Sem gráficos complexos no MVP.

### Ação Principal

- Se `session_status=active`: botão `Próxima folha`.
- Se `session_status=finished`: botão `Ver resumo`.

---

## 6.4 SessionSummaryScreen

### Objetivo

Fechar a sessão com visão de treino, não com festa.

### Conteúdo

- Tempo total.
- Folhas concluídas.
- Exercícios resolvidos.
- Score médio.
- Skills mais fortes.
- Skills para revisar.
- Evolução de dificuldade.

### Layout

- Header com `Treino concluído`.
- Métricas em grid compacto.
- Lista de skills.
- Botões:
  - `Novo treino`
  - `Repetir configuração`
  - `Ajustar configuração`

### Visual

Usar um gráfico simples de linha para dificuldade/score se viável.
Se não, usar lista temporal simples.

---

## 6.5 PauseSheet

Bottom sheet compacto:

- `Pausado`
- Timer congelado.
- Botões:
  - `Continuar`
  - `Encerrar`
  - `Descartar folha` (secundário/perigoso)

---

## 6.6 ConnectionStatusSheet

Quando API falhar:

- Título: `Sem conexão com o treino`
- Texto curto: `Não consegui falar com o backend.`
- Mostrar URL/base API se estiver em modo dev.
- Botões:
  - `Tentar novamente`
  - `Voltar`

---

## 7. Componentes Globais

### 7.1 TopTrainingBar

Props:

- `pageIndex`
- `difficulty`
- `elapsedTime`
- `showThermometer`
- `thermometerValue`
- `onPause`
- `onSubmit`

Altura:

- Tablet: 56dp.
- Celular: 52dp.

### 7.2 InkToolbar

Controles:

- Pen.
- Eraser.
- Undo.
- Redo.
- Clear active field.
- Color swatch.
- Thickness slider/stepper.

Usar ícones conhecidos, não botões de texto longos.

### 7.3 ExerciseField

Props:

- `fieldIndex`
- `statement`
- `isActive`
- `hasInk`
- `bounds`
- `backgroundMode`

Responsabilidades:

- Renderizar enunciado.
- Renderizar área de escrita.
- Informar bounds para crop.

### 7.4 ThermometerView

Props:

- `value`
- `trend`
- `compact`

Não exibir se blind mode.

### 7.5 ResultRow

Props:

- `fieldIndex`
- `statement`
- `recognizedAnswer`
- `expectedAnswer`
- `isCorrect`
- `score`
- `errorType`

---

## 8. Estados Obrigatórios

Cada tela precisa cobrir:

- Loading inicial.
- Empty state.
- Offline/API error.
- Backend validation error.
- Submit loading.
- Submit success.
- Session finished.
- Dark/white background.
- Blind mode.
- Tablet/celular.

### Empty States

Session start sem exercícios:

- `Ainda não há exercícios disponíveis.`
- Ação dev: `Rodar seed`.

Submit sem campos:

- Não deve acontecer via UI; botão fica desabilitado.

---

## 9. Contratos de API para UI

### 9.1 Start

Endpoint:

```text
POST /api/session/start
```

UI envia:

```json
{
  "student_id": "uuid",
  "config": {
    "show_thermometer": true,
    "background": "white",
    "pen_color": "#1a1a1a",
    "duration_mode": "pages",
    "pages_limit": 10,
    "difficulty_progression": "arithmetic",
    "difficulty_start": 2.0,
    "difficulty_step": 0.5,
    "difficulty_ratio": 1.15,
    "restart_on_avg": 7.0,
    "restart_window": 10,
    "exercises_per_page": 5
  }
}
```

UI recebe `first_folha`.

### 9.2 Submit

Endpoint:

```text
POST /api/session/{session_id}/submit
```

Regra importante:

- Enviar exatamente todos os fields da folha.
- Não reenviar a mesma folha.
- Cada field precisa mandar `field_index`, `exercise_id`, `image_base64`, tempos e eventos.

### 9.3 Health

Endpoint:

```text
GET /api/health
```

Usar para status de API.

---

## 10. Modelos de Estado no App

### SessionUiState

```kotlin
data class SessionUiState(
    val studentId: String,
    val sessionId: String?,
    val config: SessionConfig,
    val currentFolha: Folha?,
    val status: SessionStatus,
    val apiStatus: ApiStatus,
    val errorMessage: String?
)
```

### FolhaUiState

```kotlin
data class FolhaUiState(
    val folhaId: String,
    val pageIndex: Int,
    val difficulty: Double,
    val fields: List<FolhaField>,
    val activeFieldIndex: Int?,
    val fieldInk: Map<Int, InkLayer>,
    val fieldTiming: Map<Int, FieldTiming>,
    val isSubmitting: Boolean,
    val elapsedMs: Long
)
```

### FieldTiming

```kotlin
data class FieldTiming(
    val startedAtMs: Long?,
    val firstStrokeAtMs: Long?,
    val totalTimeMs: Long
)
```

---

## 11. Layout Responsivo

### Tablet >= 840dp width

- Config screen usa duas colunas.
- Result screen usa lista + painel lateral.
- Folha usa largura central limitada, sem virar cartão flutuante exagerado.
- Toolbar pode ser lateral se houver espaço, mas bottom toolbar é aceitável.

### Celular < 840dp width

- Uma coluna.
- Bottom actions sticky.
- Folha scroll vertical.
- Top bar compacta.
- Reduzir métricas simultâneas.

### Regras Anti-Quebra

- Enunciado nunca pode sobrepor área de escrita.
- Botões devem manter texto dentro.
- Campos têm altura mínima de 160dp no celular e 190dp no tablet.
- Result rows devem quebrar resposta reconhecida/esperada em múltiplas linhas.
- Nenhum texto deve depender de viewport-scaled font.

---

## 12. Acessibilidade

- Contraste AA mínimo.
- Touch targets >= 48dp.
- Ícones com contentDescription.
- Estado ativo do campo não depender só de cor; usar borda + leve marcador.
- Erros com ícone + texto.
- Timer e score com labels acessíveis.

---

## 13. Motion

Usar movimento com parcimônia:

- Transição de folha: slide/fade curto, 160-220ms.
- Loading submit: progress linear suave.
- Termômetro: animação de valor 250ms.
- Resultado: rows aparecem com stagger muito leve ou sem stagger.

Não animar strokes já escritos.
Não usar animações decorativas constantes.

---

## 14. Conteúdo/Textos

Tom:

- Direto.
- Adulto.
- Sem hype.

Exemplos:

- `Treino`
- `Iniciar treino`
- `Página 1`
- `Corrigindo...`
- `Próxima folha`
- `Treino concluído`
- `Não consegui ler`
- `Resposta esperada`
- `Resposta reconhecida`
- `Revisar erros`

Evitar:

- `Parabéns, campeão!`
- `Você arrasou!`
- `Vamos brincar!`

---

## 15. Critérios de Aceite Visual

Uma IA/dev especializada termina o layout quando:

1. O app abre em `SessionConfigScreen`, não em landing page.
2. Configuração cobre todos os campos de `SessionConfig`.
3. Preview da folha reflete background, caneta e quantidade de exercícios.
4. `FolhaScreen` permite escrever em campos separados.
5. Cada campo tem bounds/crop recuperável.
6. Submit monta payload compatível com backend.
7. `PageResultScreen` mostra score, correções e termômetro.
8. `SessionSummaryScreen` aparece ao terminar.
9. White e dark funcionam.
10. Blind mode remove termômetro.
11. Layout não quebra em celular nem tablet.
12. Não há textos sobrepostos.
13. A interface parece premium, focada e não infantil.

---

## 16. Ordem de Implementação Recomendada

1. Criar design tokens: cores, type, spacing, shapes.
2. Criar modelos Kotlin para API.
3. Criar navegação.
4. Implementar `SessionConfigScreen` com preview estático.
5. Implementar `FolhaScreen` com campos e InkCanvas local.
6. Implementar captura de bounds/crops por campo.
7. Implementar Retrofit e `POST /session/start`.
8. Implementar submit com fallback visual/loading.
9. Implementar `PageResultScreen`.
10. Implementar `SessionSummaryScreen`.
11. Refinar responsividade tablet/celular.
12. Rodar QA visual em white/dark/blind.

---

## 17. Prompt Curto para IA de UI

Use este prompt ao entregar o spec para uma IA de design/implementação:

```text
Você é uma IA especialista em UI/UX Android com Jetpack Compose.
Construa o layout completo do app "Strava da Matemática" seguindo APP_LAYOUT_SPEC.md.
Priorize uma experiência premium, minimalista, focada, para treino matemático com stylus em tablet.
Não crie landing page. A primeira tela é SessionConfigScreen.
Implemente telas reais, estados reais e componentes reutilizáveis.
Não use estética infantil, gradientes decorativos, mascotes ou cards desnecessários.
Garanta que a FolhaScreen tenha campos com bounds/crop por exercício e uma área de escrita confortável.
Verifique visualmente white mode, dark mode, blind mode, celular e tablet.
```

---

## 18. Arquivos Esperados no Android

```text
app/src/main/java/com/strava_matematica/
├── MainActivity.kt
├── design/
│   ├── Color.kt
│   ├── Type.kt
│   ├── Spacing.kt
│   └── Theme.kt
├── model/
│   ├── SessionConfig.kt
│   ├── Folha.kt
│   ├── PenEvent.kt
│   └── SubmitResult.kt
├── network/
│   ├── ApiClient.kt
│   ├── StravaMathApi.kt
│   └── TelemetrySocket.kt
├── ui/
│   ├── config/SessionConfigScreen.kt
│   ├── folha/FolhaScreen.kt
│   ├── folha/InkCanvas.kt
│   ├── folha/ExerciseField.kt
│   ├── folha/InkToolbar.kt
│   ├── result/PageResultScreen.kt
│   ├── summary/SessionSummaryScreen.kt
│   └── components/ThermometerView.kt
└── viewmodel/
    ├── SessionViewModel.kt
    └── FolhaViewModel.kt
```

---

## 19. Perguntas que a IA de UI Não Deve Fazer

Decidir sem perguntar:

- Usar Jetpack Compose.
- Usar Material 3 com tema customizado.
- Começar pela tela de config.
- Usar tablet portrait como target primário.
- Usar White Focus como default.
- Implementar dark mode.
- Implementar blind mode.
- Não implementar Ghost Racing no MVP.

Perguntar apenas se precisar de:

- Logo/brand final.
- URL base de backend em ambiente de dev.
- Suporte obrigatório a versão mínima específica de Android.

---

*Documento criado em 2026-05-24 para orientar uma IA especializada em layout/UI Android.*
