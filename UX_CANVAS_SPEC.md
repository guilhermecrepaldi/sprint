# UX Spec — Platform Canvas Navigation
## LOVE CLASS · "Strava da Matemática"

> STATUS: HISTORICO. Para UX atual, leia `AGENTS.md`, `docs/README.md` e `docs/UX_SPEC.md`.
> O canvas zoomavel foi substituido por Sprint limpa, scroll superior discreto e controles gestuais.

> Este documento descreve a linguagem visual e de interação do canvas zoomável que substitui
> toda a navegação por botões no app. Lido do início ao fim, ele deve revelar um sistema coerente.
> Cada seção pressupõe as anteriores.

---

## 1. Princípio Central

O aluno nunca deixa o canvas. Não existe "ir para outra tela". Existe apenas **zoom**.

Próximo de 1× — ele está resolvendo um exercício, em foco total.  
Afastando com dois dedos — a plataforma inteira se revela ao redor, como um mapa.  
Tocando em qualquer nó do mapa — o zoom retorna a 1× centrado naquele ponto.

Nada abre. Nada fecha. Tudo já está lá.

---

## 2. Paleta — Bege Claro Pastel

O canvas tem uma única cor de fundo: **`#F5EFE6`**, bege quente e claro, como papel de caderno antigo.
Todos os elementos — nós, linhas, texto — são variações desse mesmo tom, nunca contrastando em excesso.
O exercício em si (a folha) herda o tema do app (claro ou escuro), mas quando o mapa aparece,
o fundo sempre reverte para o bege, criando uma sensação de afastamento espacial, não de mudança de tela.

```
Background    #F5EFE6   base do canvas
Surface       #EDE5D8   superfície levemente mais densa (glass base)
NodeFill      #E8DDD0   nó preenchido padrão
NodeBorder    #B8A892   borda fina bege-tan
NodeActive    #C4956A   cobre pastel — nó da skill atual
NodeReview    #D4A574   âmbar suave — revisão urgente
NodeLocked    #DAD4CC   bloqueado — quase sem pigmento
Edge          #CEC3B4   linha de conexão, quase invisível
TextPrimary   #3D2E1E   marrom escuro — label do nó ativo
TextSecondary #8B7563   marrom médio — labels de nós adjacentes
```

Nenhuma cor viva. Nenhum azul, verde ou vermelho no canvas. A urgência de revisão se expressa
no âmbar `#D4A574`, que é discreto o suficiente para não gritar, mas diferente o suficiente para
ser notado.

---

## 3. Glassmorphism — A Camada de Profundidade

Sobre o bege pastel, certos elementos flutuam como vidro fosco. Isso cria a única sensação
de hierarquia visual sem usar sombras ou bordas rígidas — a profundidade vem da transparência.

O efeito segue três propriedades em conjunto:

**Blur de fundo:** `BlurEffect(radiusX = 24dp, radiusY = 24dp)` aplicado via `RenderEffect` (API 31+).
O elemento que o recebe "desfoca" o que está atrás dele, criando a sensação de vidro fosco.

**Superfície semi-transparente:** `Color(0x28F5EFE6)` — o próprio bege com ~16% de opacidade.
Por cima, uma camada branca adicional de `Color(0x12FFFFFF)` (~7%). O resultado é um painel
quase invisível que só existe para borrar o que está atrás.

**Borda de 1dp:** `Color(0x35B8A892)` — a mesma cor `NodeBorder` com ~21% de opacidade.
`cornerRadius = 14dp` em todos os cantos. Nunca nenhuma sombra (`elevation = 0`).

Esses três elementos juntos — blur + superfície translúcida + borda fina — constroem o mesmo
efeito dos tooltips e painéis do Android Studio: presença sem peso.

### Onde o glassmorphism aparece

**Painel de info do nó (NodeInfoCard):**
Quando o aluno toca em qualquer nó do mapa (não o nó ativo), um card de vidro aparece
próximo a ele. Mostra: nome da skill, status (instável / em desenvolvimento / automatizado),
acurácia como barra thin, e um prompt: _"Toque novamente para navegar"_.
Dimensões: largura máxima de 200dp, altura automática, padding 16dp horizontal / 12dp vertical.
Desaparece em 2.5s sem interação, ou imediatamente ao tocar em outro lugar.

**Barra de modos (ModeBar):**
Uma faixa horizontal de vidro, fixada no topo do mapa, visível apenas quando `scale < 0.55`.
Altura: 40dp. Os três modos — Fixação · Simulado · Maratona — aparecem como texto `FontWeight.Light`
de 12sp, espaçados por `·` em `TextSecondary`. O modo ativo tem seu label em `TextPrimary`.
A barra não tem divisores, ícones ou sublinhados — só os três labels no vidro.

**Banner de revisão urgente (ReviewBanner):**
Aparece imediatamente abaixo da ModeBar quando `reviewSkills.isNotEmpty()`.
Fundo de vidro. Texto: _"Revisão disponível · N skills"_ em 11sp `TextSecondary`.
Os nós de revisão no mapa já têm borda âmbar `NodeReview` — o banner é apenas um reforço textual,
não o único sinal.

---

## 4. Arquitetura Espacial — As Três Camadas

O canvas tem sempre três camadas. Elas coexistem; o que muda é a opacidade de cada uma.

```
┌─────────────────────────────────────────────────────┐
│  GlassLayer   — NodeInfoCard, ModeBar, ReviewBanner  │  sempre no topo, aparece por interação
├─────────────────────────────────────────────────────┤
│  MapLayer     — PlatformMap (Canvas API)             │  alpha = 1 - scale / 0.55 (até 0.55)
├─────────────────────────────────────────────────────┤
│  FocusLayer   — FolhaScreen / DrillScreen            │  alpha = (scale - 0.80) / 0.20 (de 0.80 a 1.0)
└─────────────────────────────────────────────────────┘
```

Entre `0.55` e `0.80` de scale, nenhuma das duas camadas principais tem opacidade cheia.
O aluno vê um estado intermediário: o exercício some, o mapa ainda não chegou.
Nessa zona, apenas o fundo bege `#F5EFE6` e as linhas de edge do mapa são visíveis,
criando uma "névoa de transição" que reforça o sentido de movimento espacial.

Thresholds:
```
MIN_SCALE               0.08   (mapa completamente expandido)
MAP_MODE_THRESHOLD      0.55   (mapa começa a ficar opaco ao recuar)
EXERCISE_MODE_THRESHOLD 0.80   (exercício começa a aparecer ao avançar)
MAX_SCALE               1.00   (exercício em foco total)
```

A sessão pausa automaticamente quando `scale < MAP_MODE_THRESHOLD`. Retoma ao ultrapassar o threshold.

---

## 5. Vocabulário de Gestos

Três gestos, zero botões.

**Um dedo:** escreve no exercício. Traço normal de caneta. Não interfere com nada.

**Dois dedos — movimento de pinça:** zoom out (separar) ou zoom in (aproximar).
É o gesto principal de navegação. O canvas responde ao movimento proporcional:
afastar lentamente revela o mapa gradualmente; afastar rápido vai direto ao mapa completo.
Pan com dois dedos funciona quando `scale < 0.80` — no mapa, o aluno pode arrastar e explorar.

**Dois dedos — toque sem movimento:** avança o exercício (equivale ao antigo 2-finger tap).
A distinção entre pinça e toque é feita por delta de escala acumulado: se `|Δscale - 1| < 0.02`
durante todo o tempo de contato, é um toque. Se ultrapassou esse limiar, é uma pinça.
O avanço só dispara quando em modo exercício (`scale > MAP_MODE_THRESHOLD`).

Não há gesto de "voltar". Não há swipe. Não há menu hamburguer.
O mapa é a navegação.

---

## 6. Mapa — Gramática dos Nós

O mapa é desenhado com a Canvas API do Compose: `drawCircle`, `drawLine`, `drawText`.
Nenhum composable filho. Nenhum layout. Só primitivas desenhadas.

### Layout centrado na skill atual

O nó da skill ativa ocupa o centro geométrico do canvas.
Todos os outros nós são posicionados relativamente a ele:

```
                    [ModeBar: Fixação · Simulado · Maratona]
                    [ReviewBanner] (se houver revisões)

              [review-A]   [review-B]              ← Y = -150dp (amarelo âmbar)

[prereq-1] ─────────── [SKILL ATUAL ●] ─────────── [unlock-1]
                              │                     [unlock-2]
                         [prereq-2]                 [unlock-3]
```

Pré-requisitos da skill atual aparecem abaixo do centro (Y = +150dp).
Skills desbloqueadas pela skill atual aparecem à direita (X = +210dp), distribuídas verticalmente.
Skills de revisão aparecem acima do centro (Y = -150dp).

Quando a skill atual não tem pré-requisitos (ex: `soma_subtracao`), a linha abaixo fica vazia.
Quando tem muitas skills desbloqueadas, mostram-se apenas as 3 primeiras.

### Nó ativo

Círculo preenchido com `NodeActive #C4956A`. Raio 28dp. Borda fina 1.2dp da mesma cor.
Label abaixo: `TextPrimary`, `FontWeight.Light`, 11sp.

### Nó disponível (não praticado ou em progresso)

Círculo vazado — só borda `NodeBorder #B8A892`, stroke 1.2dp.
Label: `TextSecondary`, `FontWeight.Light`, 11sp.

### Nó automatizado

Círculo com fill `NodeFill #E8DDD0`, borda `NodeBorder`.
Indica domínio ≥ 85%. O fill sutil diferencia de um nó vazio.

### Nó bloqueado

Círculo vazado com borda `NodeLocked #DAD4CC`, alpha 0.4 total.
Label também com alpha 0.4. Não responde a toque.

### Nó de revisão

Círculo vazado com borda `NodeReview #D4A574`, stroke 2dp (levemente mais grossa).
Posicionado acima do centro. Toque inicia sessão de revisão naquela skill.

### Arestas (edges)

`drawLine` com cor `Edge #CEC3B4`, strokeWidth 0.8dp.
Apenas conexões imediatas são desenhadas — sem linhas para nós de 2º grau.

---

## 7. Tipografia no Canvas

Toda a tipografia do mapa usa `FontWeight.Light`. Nunca Bold.

| Elemento          | Tamanho | Cor           |
|-------------------|---------|---------------|
| Label nó ativo    | 11sp    | TextPrimary   |
| Label nó adjacente| 11sp    | TextSecondary |
| Labels de modo    | 13sp    | TextSecondary |
| Modo ativo        | 13sp    | TextPrimary   |
| NodeInfoCard nome | 14sp    | TextPrimary   |
| NodeInfoCard info | 11sp    | TextSecondary |
| ReviewBanner      | 11sp    | TextSecondary |

Labels de nós com mais de 14 caracteres são truncados com `…`.
Texto posicionado centralizado abaixo do círculo, separado 10px do ponto mais baixo do raio.

---

## 8. Motion — Como o Canvas Se Move

As transições são animadas por `animateFloatAsState` com a curva padrão do Compose.

`MapLayer.alpha` e `FocusLayer.alpha` não são complementares diretos — há uma lacuna intencional
entre `0.55` e `0.80` onde ambos são 0. Isso cria a névoa de transição descrita na seção 4.

O pan do mapa (`offsetX`, `offsetY`) é atualizado em tempo real durante o gesto de pinça.
Quando o scale retorna acima de `0.80`, o offset é mantido — assim, ao fazer zoom in de volta
para um nó específico, o mapa "lembra" de onde estava.

Quando o aluno toca em um nó e seleciona uma nova skill, o offset é resetado para 0,0
e o scale anima de volta para `1.0`. Esse movimento — mapa sumindo, exercício surgindo,
posição centralizada — comunica a transição para um novo contexto de estudo.

Duração sugerida para animação de retorno ao scale 1.0: 300ms, `FastOutSlowInEasing`.

---

## 9. NodeInfoCard — Especificação Completa

Quando o aluno toca em um nó disponível ou de revisão no mapa:

1. Um `NodeInfoCard` aparece com `scaleIn` e `fadeIn` simultâneos (100ms), ancorado próximo ao nó tocado.
2. O card tem fundo glassmorphism (blur 24dp, superfície `0x28F5EFE6`, borda `0x35B8A892` 1dp).
3. Conteúdo:
   - Nome da skill em 14sp `TextPrimary Light`
   - Status como texto em 11sp `TextSecondary`: _"Em desenvolvimento"_ / _"Automatizado"_ / _"Instável"_
   - Barra de acurácia: linha fina (2dp) de 120dp de largura. Fill proporcional em `NodeActive`.
     Background da barra: `NodeBorder`. Aparece apenas se `attempt_count > 0`.
   - Linha de rodapé em 10sp `TextSecondary`: _"Toque novamente para estudar aqui"_ (se disponível)
     ou _"Bloqueado — complete os pré-requisitos"_ (se bloqueado).
4. O card desaparece após 2.5s sem interação (`LaunchedEffect` com delay), ou ao tocar fora.
5. Um segundo toque no mesmo nó (enquanto o card está visível) dispara `onSkillSelect(tag)`.

---

## 10. Estados Globais do Canvas

| scale             | O que o aluno vê                                               |
|-------------------|----------------------------------------------------------------|
| 1.00              | Exercício em foco total. Bege substituído pelo tema do app.    |
| 0.80 – 1.00       | Exercício desbotando. Fundo bege aparecendo nas bordas.        |
| 0.55 – 0.80       | Zona de transição. Apenas bege e edges do mapa. Névoa.         |
| 0.00 – 0.55       | Mapa completo. ModeBar e ReviewBanner visíveis. Sessão pausada.|

A distinção entre "mapa completo" e "zona de transição" é chave:
o aluno não pode interagir com o mapa (tocar nós) durante a névoa de transição.
`detectTapGestures` do `PlatformMap` deve ignorar eventos quando `scale > MAP_MODE_THRESHOLD`.

---

## 11. O que Nunca Existe

Para preservar a leveza do design, as seguintes coisas estão explicitamente proibidas no canvas:

- Sombra (`elevation`, `shadow`, `BlurMaskFilter` para sombra)
- Ícones de navegação (setas, hamburguer, chevron)
- Modais ou dialogs sobrepostos ao canvas
- Animação de bounce ou spring exagerado
- Fundo preto ou escuro no estado de mapa
- Texto em `FontWeight.Bold` ou `FontWeight.Medium` em labels do mapa
- Gradientes (o fundo é flat `#F5EFE6`)
- Mais de 3 skills de revisão visíveis simultaneamente
- Nó de skill com raio maior que 28dp ou menor que 20dp

---

## 12. Implementação do Glassmorphism em Compose

O efeito requer API 31+ (`android:minSdk 31` ou `BuildCompat.isAtLeastS()`).
Para dispositivos abaixo do API 31, o glass panel degrada graciosamente para
fundo `Surface #EDE5D8` com opacidade 0.92 e borda `NodeBorder` sem blur.

```kotlin
// Glass modifier — reutilizável em ModeBar, ReviewBanner e NodeInfoCard
fun Modifier.glassPanel(cornerRadius: Dp = 14.dp): Modifier = this
    .clip(RoundedCornerShape(cornerRadius))
    .background(Color(0x28F5EFE6))           // bege translúcido
    .then(
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Modifier.graphicsLayer {
                renderEffect = BlurEffect(
                    radiusX = 48f,   // px — ~24dp em 2×
                    radiusY = 48f,
                    edgeTreatment = TileMode.Clamp,
                )
            }
        } else Modifier
    )
    .border(1.dp, Color(0x35B8A892), RoundedCornerShape(cornerRadius))
```

O `BlurEffect` borra o conteúdo do próprio composable, não o background.
Para borrar o fundo, o glass panel deve ser um `Box` irmão (não filho) do conteúdo que ele cobre,
posicionado com `Modifier.matchParentSize()` ou via `Layout` customizado.

```kotlin
Box {
    // Conteúdo de fundo (mapa, exercício)
    PlatformMap(...)

    // Glass panel — float acima, borra o que está atrás
    AnimatedVisibility(visible = showNodeInfo, ...) {
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 48.dp)
                .width(200.dp)
                .glassPanel(),
        ) {
            NodeInfoCard(...)
        }
    }
}
```

---

*Este spec é a fonte de verdade para qualquer decisão visual ou de interação no canvas.
Se uma escolha não está descrita aqui, a regra padrão é: menos é mais, bege sempre, sem sombra.*
