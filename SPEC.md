# LOVE CLASS (Strava Matemática) - Software Specification

## 1. Visão Geral
**LOVE CLASS** é um aplicativo Android nativo construído com Jetpack Compose focado no aprendizado adaptativo de matemática e gamificação do estudo, agindo como um "Strava para Matemática". O sistema utiliza motores procedurais para geração infinita de exercícios e algoritmos de rastreamento de conhecimento (BKT) e Elo para ajustar dinamicamente a dificuldade.

## 2. Stack Tecnológica
- **Linguagem**: Kotlin
- **UI Framework**: Jetpack Compose (Material Design 3 adaptado para um estilo *Tailwind-like*)
- **Persistência**: Room Database (LocalRepository)
- **Arquitetura**: MVVM (Model-View-ViewModel)

## 3. UX/UI e Design System
A interface foi projetada para ter altíssima densidade de informações sem sobrecarregar cognitivamente o usuário, seguindo um padrão estético minimalista (*Tailwind-like*):
- **Cores**: Paleta *Slate* / Tons de cinza e azul-acinzentado, fugindo de cores puras ou genéricas. Foco em *glassmorphism* leve (fundos translúcidos).
- **Tipografia**: Fontes reduzidas (Inter/Roboto), pesos finos para legendas e semi-bold para destaques (ex: 11sp a 13sp para subtítulos).
- **Shapes**: Bordas extremamente arredondadas (16dp a 24dp) para *Cards* e botões.
- **Gestos**: Interações touch avançadas (Canvas com Zoom, toque de 2 dedos para avançar/maratona).

## 4. Funcionalidades Core (Features)

### 4.1 Motor Procedural de Matemática (`ProceduralEngine`)
- **Geração de Conteúdo**: Não utiliza banco de questões fixo. Gera questões proceduralmente no momento do acesso.
- **Escopo**: Abrange desde o Ensino Fundamental (Soma Básica, Frações) até o Ensino Superior (Limites, Derivadas, Integrais).
- **Formatação**: Equações renderizadas em sintaxe Math (LaTeX-like).

### 4.2 Inteligência e Matchmaking (BKT & Elo)
- **Bayesian Knowledge Tracing (BKT)**: Motor que rastreia a probabilidade do aluno dominar uma habilidade (`MathBktEngine`). Define Ineficácia Crítica (Quarentena) ou *Mastery*.
- **Elo Matchmaker**: Converte a fluência BKT em uma pontuação MMR (Matchmaking Rating), garantindo que o algoritmo de geração procedural crie exercícios na exata zona de desenvolvimento proximal do usuário.
- **Painel Cognitivo Premium**: Interface na configuração do Sprints que exibe visualmente o *Status* do aluno, alertando sobre ineficácia, masterização ou necessidade de fixação.

### 4.3 Experiência de Sessão (FolhaScreen & Canvas)
- **Simulado Tab Integrada**: No scroll horizontal principal de navegação, o aluno possui acesso à aba "SIMULADO". Nela, é exibida uma tabela (`SimuladoConfigPage`) onde o próprio aluno agenda/constrói sua rotina de questões (ex: 5 de Álgebra, 10 de Geometria).
- **Piloto Automático (Autopilot)**: Diferente do Simulado, o Piloto Automático define dinamicamente os temas com base na fraqueza/força do usuário lidos pelo BKT.
- **Campo de Resposta Inteligente (`ExerciseField`)**:
  - Contém Canvas de desenho vetorial acoplado a OCR.
  - **Borracha Local**: Botão interno para limpar apenas a área de resposta de uma questão específica, sem apagar o restante da folha.

### 4.4 Dashboard & Calendário Interativo de Rotina
- **Perfil do Aluno (`ProfileScreen` & `DashboardScreen`)**: Exibe o *Streak* (dias consecutivos) e Total de Exercícios.
- **Activity Heatmap (Histórico de Estudo)**:
  - Um gráfico estilo contribuição do GitHub.
  - **Interatividade**: Ao clicar em qualquer dia, exibe-se um card abaixo ("Resumo do Dia") revelando detalhes quantitativos dos exercícios concluídos naquele dia específico, dividido em turnos (Manhã, Tarde, Noite).

## 5. Fluxo Operacional de Uso
1. **Entrada**: Usuário visualiza o *Dashboard* com seu Heatmap Interativo. Pode clicar em dias passados para avaliar sua rotina.
2. **Setup**: Seleciona uma trilha de estudo no Menu Horizontal (Scroll) ou entra na aba "SIMULADO" para montar um bloco específico.
3. **Sessão**: Entra na `FolhaScreen`, resolve no Canvas e insere a resposta no `EnterSquare`.
4. **Avaliação**: O `BKT Engine` atualiza a fluência. Se atingir *Mastery*, o botão verde acende. Se cair em *Quarentena*, o painel vermelho alerta.

## 6. Próximos Passos (Roadmap)
- Integrar geração de Simulado Automático (baseado no MMR).
- Implementar OCR avançado para reconhecer fórmulas e etapas (*steps*) desenhadas no Canvas.
- Exportação de Gabaritos de Simulado para `.txt` ou `.md`.
- Adicionar propagandas obrigatórias (Ads de pausa de 2 min) caso a sessão do usuário dure mais de 20 minutos contínuos.
