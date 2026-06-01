# SPEC — Motor de Exercícios Matemáticos Progressivos estilo Kumon

> Foco principal: interação contínua com exercícios, progressão lógica, listas acessíveis, treino repetitivo inteligente e construção matemática por microetapas.

## 1. Visão Geral

Este documento define a especificação de um sistema de aprendizado de matemática com foco total na interação com exercícios, inspirado principalmente no modelo Kumon: listas progressivas, repetição, autonomia, fluência e avanço por domínio.

A proposta principal não é criar uma plataforma cheia de aulas, vídeos ou explicações longas. O coração do produto é o exercício. O aluno aprende resolvendo, repetindo, corrigindo, melhorando tempo, estabilizando acertos e avançando por microetapas.

A árvore de matemática existe para organizar a sequência lógica dos exercícios. Ela não deve ser complicada para o aluno. A complexidade fica por trás. Na frente, o aluno vê apenas a próxima lista certa para ele.

O sistema deve combinar:

- Kumon, como referência principal de listas progressivas, repetição e fluência;
- BNCC, como cobertura oficial mínima;
- Khan Academy, como inspiração de mapa amplo e navegação clara;
- Singapore Math, como apoio para construção conceitual simples quando necessário;
- AoPS, como camada futura de desafios, não como foco inicial;
- Métricas próprias de domínio, para decidir avanço, repetição, revisão ou retorno a pré-requisitos.

A ideia central é: matemática deve ser treinada como uma sequência lógica de exercícios pequenos, acessíveis e cumulativos.

---

## 2. Objetivo do Produto

Criar um aplicativo/plataforma de treino matemático onde o aluno interage quase exclusivamente com exercícios.

Objetivos principais:

1. Criar listas progressivas de matemática, em microetapas fáceis de seguir.
2. Fazer o aluno resolver uma sequência de exercícios com pouca distração.
3. Corrigir cada resposta imediatamente ou ao final da lista, conforme modo escolhido.
4. Medir acerto, erro, tempo, repetição, estabilidade e retenção.
5. Liberar o próximo bloco somente quando houver domínio suficiente.
6. Detectar quando o aluno precisa repetir uma lista anterior.
7. Organizar os exercícios numa árvore lógica de pré-requisitos.
8. Tornar a criação de novas listas simples, acessível e escalável.
9. Evitar explicações longas, mantendo foco em prática.
10. Fazer a matemática parecer uma escada: um degrau claro de cada vez.

O produto não deve se comportar como uma escola digital cheia de conteúdo. Ele deve funcionar como um caderno inteligente de exercícios progressivos.

---

## 3. Princípio Central

A matemática deve ser estruturada em três camadas simultâneas, mas o aluno só deve sentir uma coisa: a próxima lista.

```text
Lista atual
→ correção
→ repetição ou avanço
→ próxima lista
```

Por trás disso, o sistema usa:

```text
Árvore de matéria
+ Linha de progressão
+ Motor de domínio
+ Gerador de listas
```

### 3.1 Árvore de matéria

A árvore define dependências.

Exemplo:

```text
Divisão de frações
├── Conceito de fração
├── Fração como divisão
├── Multiplicação de frações
├── Inverso multiplicativo
├── Simplificação
└── Divisão de inteiros
```

Se o aluno erra divisão de frações, o sistema não deve apenas oferecer mais exercícios iguais. Ele deve investigar qual galho anterior está fraco.

### 3.2 Linha de progressão

A linha define a sequência principal de avanço.

Exemplo:

```text
Contagem
→ Adição
→ Subtração
→ Multiplicação
→ Divisão
→ Múltiplos e divisores
→ Frações
→ Decimais
→ Porcentagem
→ Inteiros
→ Álgebra
→ Equações
→ Funções
→ Geometria
→ Trigonometria
→ Cálculo
```

### 3.3 Motor de domínio

O motor decide se o aluno:

- avança;
- repete;
- revisa;
- regride para pré-requisito;
- recebe exercício de erro comum;
- recebe desafio;
- entra em ciclo de recuperação.

---

## 4. Fontes Pedagógicas Adotadas

A hierarquia pedagógica do produto deve ser clara:

```text
1º Kumon — base principal de interação
2º BNCC — cobertura oficial
3º Khan Academy — organização visual da trilha
4º Singapore Math — apoio conceitual simples
5º AoPS — desafios futuros/opcionais
6º Métricas próprias — inteligência de domínio
```

O foco inicial é Kumon. Todo o resto apoia, mas não deve roubar o centro do produto.

## 4.1 BNCC — Cobertura Oficial

A BNCC deve ser usada como matriz mínima de cobertura curricular.

Função da BNCC dentro do sistema:

- garantir que os grandes blocos oficiais estejam presentes;
- organizar o conteúdo por unidades temáticas;
- permitir associação com anos escolares quando necessário;
- permitir relatórios escolares e familiares;
- evitar buracos curriculares importantes.

As grandes unidades temáticas adotadas serão:

1. Números
2. Álgebra
3. Geometria
4. Grandezas e Medidas
5. Probabilidade e Estatística

No sistema, a BNCC não deve mandar na ordem absoluta do aprendizado. Ela deve servir como mapa de cobertura, não como motor pedagógico único.

### Decisão de produto

Usar BNCC como camada de conformidade:

```text
Conteúdo do app → vinculado a habilidades BNCC → relatórios por habilidade
```

Mas a progressão real deve seguir pré-requisitos matemáticos.

---

## 4.2 Kumon — Referência Principal de Interação

O Kumon entra como referência central de treino incremental, listas curtas, repetição e autonomia.

Função do Kumon dentro do sistema:

- listas curtas;
- avanço em pequenos passos;
- repetição suficiente para automatizar cálculo;
- independência do aluno;
- aumento quase imperceptível de dificuldade;
- controle de tempo por folha ou bloco.

O sistema deve copiar a lógica didática, não necessariamente o formato fechado.

### Princípios aproveitados

1. Um conceito por vez.
2. Uma variação pequena entre listas.
3. Muito treino de fluência.
4. Tempo como indicador de domínio.
5. Correção como parte do exercício.
6. Avanço somente após estabilidade.

### Decisão de produto

Cada conteúdo deve ser quebrado em microlistas progressivas. A lista é a unidade central do sistema.

Exemplo:

```text
MAT-ADI-001 — Adição até 5 com apoio visual
MAT-ADI-002 — Adição até 10 sem reagrupamento
MAT-ADI-003 — Adição até 20 sem reagrupamento
MAT-ADI-004 — Adição até 20 com reagrupamento simples
MAT-ADI-005 — Adição de dezenas exatas
MAT-ADI-006 — Adição de dois algarismos sem reagrupamento
MAT-ADI-007 — Adição de dois algarismos com reagrupamento
```

Cada lista deve ter uma lógica fácil de entender:

```text
muda pouco → repete bastante → estabiliza → avança
```

---

## 4.3 Khan Academy — Trilha Ampla e Clara

A Khan Academy entra como referência de navegação, clareza e amplitude curricular.

Função da Khan dentro do sistema:

- organizar trilhas grandes;
- mostrar progresso visual;
- dividir matemática em cursos compreensíveis;
- permitir que o aluno saiba onde está;
- conectar teoria, prática e domínio por habilidade.

### Princípios aproveitados

1. Trilha visual clara.
2. Conteúdo dividido por curso/módulo/habilidade.
3. Prática por habilidade.
4. Revisão por domínio.
5. Cobertura de matemática básica até cálculo e estatística.

### Decisão de produto

O usuário deve visualizar a matemática como mapa.

Exemplo:

```text
Matemática Básica
├── Números naturais
├── Operações
├── Frações
├── Decimais
└── Porcentagem

Álgebra
├── Expressões
├── Equações
├── Inequações
├── Sistemas
└── Funções
```

---

## 4.4 Singapore Math — Progressão Conceitual/Espiral

Singapore Math entra como referência conceitual, especialmente pela abordagem concreto → pictórico → abstrato.

Função dentro do sistema:

- evitar que o aluno decore símbolos sem entender;
- usar representação visual antes da abstração;
- retornar periodicamente a conceitos antigos com maior profundidade;
- construir base conceitual forte;
- transformar problemas em modelos visuais.

### Estrutura CPA

```text
Concreto → Pictórico → Abstrato
```

Exemplo em frações:

1. Concreto: dividir uma barra, pizza ou conjunto de objetos.
2. Pictórico: representar a fração em desenho ou reta numérica.
3. Abstrato: operar simbolicamente com numerador e denominador.

### Decisão de produto

Cada conteúdo importante deve ter níveis de representação:

```text
Nível 1 — Manipulável/concreto
Nível 2 — Visual/pictórico
Nível 3 — Simbólico/abstrato
Nível 4 — Aplicado/problema
```

---

## 4.5 AoPS — Problemas Desafiadores

AoPS entra como referência para raciocínio profundo, criatividade matemática e desafios de alto nível.

Função dentro do sistema:

- impedir que o aluno vire apenas executor mecânico;
- desenvolver pensamento estratégico;
- criar problemas não óbvios;
- trabalhar demonstração, padrões, contagem, número e geometria de forma mais rica;
- criar trilhas avançadas para alunos acima da média.

### Princípios aproveitados

1. Problemas mais difíceis que o padrão escolar.
2. Valorização do raciocínio, não só do resultado.
3. Exploração de múltiplos caminhos.
4. Introdução precoce a ideias discretas, combinatórias e lógicas.
5. Desafios opcionais após domínio básico.

### Decisão de produto

Todo nó relevante deve ter uma camada de desafio:

```text
Básico → Fluência → Aplicação → Desafio → Olímpico/Avançado
```

Nem todo aluno precisa fazer a camada AoPS para avançar no currículo básico, mas alunos fortes devem ter esse caminho.

---

## 5. Arquitetura Curricular Geral

A árvore completa será organizada em macroáreas.

## 5.1 Macroáreas principais

1. Pré-matemática
2. Números naturais
3. Operações fundamentais
4. Expressões numéricas
5. Divisibilidade, múltiplos e divisores
6. Frações
7. Decimais
8. Porcentagem, razão e proporção
9. Números inteiros
10. Números racionais
11. Potenciação e radiciação
12. Álgebra inicial
13. Equações e inequações
14. Produtos notáveis e fatoração
15. Funções
16. Geometria plana
17. Geometria espacial
18. Grandezas e medidas
19. Trigonometria
20. Geometria analítica
21. Matrizes e sistemas lineares
22. Sequências e progressões
23. Análise combinatória
24. Probabilidade
25. Estatística
26. Matemática financeira
27. Lógica e conjuntos
28. Números reais e complexos
29. Pré-cálculo
30. Cálculo diferencial
31. Cálculo integral
32. Séries
33. Cálculo multivariável
34. Álgebra linear
35. Matemática discreta
36. Estatística inferencial
37. Equações diferenciais
38. Otimização e modelagem

---

## 6. Linha Mestra de Progressão

A linha mestra define a progressão padrão para a maioria dos alunos.

```text
Fase 0 — Alfabetização matemática
Fase 1 — Aritmética básica
Fase 2 — Estrutura dos números
Fase 3 — Racionais
Fase 4 — Sinais, potências e raízes
Fase 5 — Álgebra
Fase 6 — Funções
Fase 7 — Geometria
Fase 8 — Trigonometria e analítica
Fase 9 — Estatística, probabilidade e combinatória
Fase 10 — Ensino médio avançado
Fase 11 — Pré-cálculo
Fase 12 — Cálculo
Fase 13 — Matemática superior
```

---

## 7. Estrutura de Cada Nó da Árvore

Cada conteúdo deve ser representado como um nó.

### 7.1 Modelo de nó

```text
ID: MAT-FRA-003
Nome: Frações equivalentes
Macroárea: Frações
Fase: Racionais
Nível: Fundamental
Pré-requisitos:
  - MAT-FRA-001 — Conceito de fração
  - MAT-DIV-001 — Divisores
  - MAT-MUL-002 — Multiplicação básica
Habilidades:
  - reconhecer frações equivalentes
  - gerar frações equivalentes
  - simplificar frações
  - comparar equivalências visualmente
Representações:
  - concreto
  - pictórico
  - simbólico
Tipos de exercício:
  - reconhecimento
  - cálculo direto
  - variação controlada
  - erro comum
  - problema aplicado
  - desafio
Critério de domínio:
  - acerto mínimo direto: 90%
  - acerto mínimo aplicado: 80%
  - estabilidade: 2 sessões
  - revisão: 1 dia, 7 dias, 30 dias
```

---

## 8. Tipos de Exercício

Os tipos de exercício devem ser simples. A experiência do aluno deve lembrar um caderno progressivo, não um videogame cheio de firula. Firula demais mata aprendizado. Aqui o exercício manda.

Cada nó deve gerar exercícios de tipos diferentes.

## 8.1 Tipo A — Reconhecimento

Objetivo: identificar o conceito.

Exemplo:

```text
Qual fração representa metade?
A) 1/2
B) 1/3
C) 2/5
D) 3/4
```

## 8.2 Tipo B — Execução direta

Objetivo: treinar procedimento.

```text
3/4 + 1/4 = ?
```

## 8.3 Tipo C — Variação controlada

Objetivo: mudar uma variável por vez.

```text
3/4 + 1/4
3/4 + 2/4
3/5 + 1/5
3/5 + 2/5
```

## 8.4 Tipo D — Erro comum

Objetivo: detectar confusão previsível.

```text
1/2 + 1/3 = ?
```

Erro comum esperado:

```text
1/5
```

## 8.5 Tipo E — Problema contextualizado

Objetivo: aplicar em situação real.

```text
Ana comeu 1/4 de uma pizza e João comeu 2/4. Quanto comeram juntos?
```

## 8.6 Tipo F — Problema inverso

Objetivo: inverter raciocínio.

```text
Ana comeu 3/4 de uma pizza no total. Se antes ela havia comido 1/4, quanto comeu depois?
```

## 8.7 Tipo G — Problema misto

Objetivo: misturar conteúdos já dominados.

```text
Um produto custa R$ 80. Teve desconto de 25% e depois foi dividido em 2 parcelas. Qual o valor de cada parcela?
```

## 8.8 Tipo H — Desafio

Objetivo: raciocínio não mecânico.

```text
Encontre três frações diferentes entre 1/3 e 1/2.
```

---

## 9. Ciclo Didático de Cada Conteúdo

Cada conteúdo deve seguir um ciclo baseado em exercício.

```text
1. Diagnóstico curto
2. Lista muito fácil
3. Lista fácil
4. Lista padrão
5. Lista com pequena variação
6. Lista de consolidação
7. Lista de revisão
8. Mini teste de domínio
9. Liberação do próximo conteúdo
```

Explicação deve existir apenas quando destrava o exercício. Nada de aula longa no meio da prática.

### 9.1 Regra de ouro da interação

```text
O aluno deve passar mais tempo resolvendo do que lendo.
```

### 9.2 Modelo de sessão

```text
Abrir app
→ ver lista do dia
→ resolver exercícios
→ receber correção
→ corrigir erros
→ repetir se necessário
→ finalizar sessão
```

A sessão deve ser simples o bastante para uma criança usar e rigorosa o bastante para gerar dados reais de aprendizado.

---

## 10. Métricas Próprias de Domínio

O domínio não deve ser medido apenas por acerto.

### 10.1 Fórmula conceitual

```text
Domínio = acerto × velocidade × estabilidade × retenção × transferência
```

### 10.2 Variáveis medidas

1. Taxa de acerto
2. Tempo médio por questão
3. Número de tentativas
4. Uso de dica
5. Tipo de erro
6. Reincidência do erro
7. Retenção após intervalo
8. Desempenho em problema aplicado
9. Desempenho em problema misto
10. Desempenho em desafio

### 10.3 Estados possíveis do nó

```text
Bloqueado
Disponível
Em diagnóstico
Em treino
Em revisão
Dominado fraco
Dominado estável
Dominado avançado
Em recuperação
```

### 10.4 Critério inicial sugerido

```text
Acerto direto: mínimo 90%
Acerto aplicado: mínimo 80%
Tempo: dentro do padrão do nível
Retenção: acerto após revisão futura
Erro crítico: não pode persistir
```

---

## 11. Motor de Decisão

O motor deve tomar decisões automáticas.

## 11.1 Avançar

Avança quando:

- acerto alto;
- tempo adequado;
- baixa hesitação;
- erro não recorrente;
- bom resultado em revisão.

## 11.2 Repetir

Repete quando:

- acerto médio;
- erro procedural;
- tempo alto;
- instabilidade.

## 11.3 Regressão para pré-requisito

Regredir quando:

- erro revela falha de base;
- o aluno erra repetidamente o mesmo padrão;
- o erro pertence a nó anterior.

Exemplo:

```text
Erro em soma de frações com denominadores diferentes
→ verificar MMC
→ verificar múltiplos
→ verificar tabuada
```

## 11.4 Revisão espaçada

Todo nó dominado deve voltar em:

```text
1 dia
7 dias
30 dias
90 dias
```

O intervalo pode aumentar ou diminuir conforme desempenho.

---

## 12. Diagnóstico Inicial

O aluno não deve começar necessariamente do zero.

O sistema deve aplicar diagnóstico adaptativo.

### 12.1 Objetivo do diagnóstico

Descobrir:

- nível atual;
- buracos de base;
- velocidade de cálculo;
- domínio conceitual;
- tolerância a problemas aplicados;
- ansiedade ou lentidão excessiva;
- capacidade de transferência.

### 12.2 Modelo

O diagnóstico deve começar amplo e depois afunilar.

```text
Operações básicas
→ frações/decimais
→ porcentagem
→ inteiros
→ álgebra
→ equações
→ funções
→ geometria
```

Se o aluno falha em uma região, o sistema explora os pré-requisitos daquela região.

---

## 13. Perfil do Aluno

Cada aluno deve ter um perfil matemático dinâmico.

### 13.1 Dados registrados

- nós dominados;
- nós fracos;
- tempo médio por tipo de exercício;
- erros recorrentes;
- evolução por semana;
- retenção;
- dificuldade por macroárea;
- preferência de representação;
- comportamento diante de desafio;
- consistência.

### 13.2 Perfil matemático gerado

Exemplo:

```text
Aluno forte em cálculo direto, mas fraco em problemas aplicados.
Boa velocidade em operações.
Erro recorrente em frações equivalentes.
Precisa revisar MMC antes de avançar para soma de frações com denominadores diferentes.
```

---

## 14. Papel da IA

A IA não deve virar professora tagarela. Ela deve funcionar como observadora, tutora e avaliadora.

### 14.1 Funções principais da IA

1. Diagnosticar erro.
2. Classificar tipo de erro.
3. Sugerir nó de recuperação.
4. Gerar novas variações de exercício.
5. Criar problemas contextualizados.
6. Criar desafios.
7. Resumir evolução do aluno.
8. Sugerir intervenção humana quando necessário.

### 14.2 Limite didático

O sistema pode ter regra de pouca explicação:

```text
Errou → dica curta
Errou de novo → exemplo parecido
Errou de novo → volta para pré-requisito
```

Nada de despejar aula longa.

---

## 15. UX Principal

## 15.1 Para o aluno

O aluno deve ver:

- missão atual;
- progresso do nó;
- sequência diária;
- mapa simplificado;
- conquistas reais;
- revisões pendentes;
- desafios liberados.

Não deve ver complexidade excessiva da árvore inteira.

## 15.2 Para o professor/pai/admin

O adulto deve ver:

- árvore completa;
- nós dominados;
- nós fracos;
- relatório de erros;
- tempo de estudo;
- evolução semanal;
- recomendações;
- alinhamento BNCC;
- previsão de próximos conteúdos.

---

## 16. Macroarquitetura Técnica Conceitual

```text
Frontend do aluno
↓
Motor de sessão
↓
Gerador de exercícios
↓
Corretor
↓
Classificador de erro
↓
Motor de domínio
↓
Árvore curricular
↓
Perfil do aluno
↓
Relatórios
```

---

## 17. Entidades Principais

Como o foco é exercício, a entidade mais importante não é a aula. É a lista.

### 17.1 Student

```text
id
nome
idade
série opcional
perfil_matematico
histórico
```

### 17.2 MathNode

```text
id
nome
macroarea
fase
pre_requisitos
habilidades
bncc_refs
dificuldade
representacoes
tipos_exercicio
criterio_dominio
```

### 17.3 Exercise

```text
id
node_id
list_id
tipo
pergunta
resposta_correta
distratores
erro_alvo
dificuldade
representacao
ordem_na_lista
```

### 17.4 ExerciseList

```text
id
node_id
codigo
nome
objetivo
nivel_microprogressao
quantidade_questoes
tempo_estimado
tipo_lista
pre_requisitos
criterio_aprovacao
proxima_lista
lista_recuperacao
```

Tipos de lista:

```text
diagnostico
fluencia
repeticao
variacao_controlada
correcao_de_erro
revisao
mini_teste
desafio
```

### 17.4 Attempt

```text
student_id
exercise_id
resposta
data_hora
correto
tempo
usou_dica
tipo_erro
```

### 17.5 MasteryState

```text
student_id
node_id
status
score_acerto
score_tempo
score_retencao
score_transferencia
ultima_revisao
proxima_revisao
```

---

## 18. Árvore Curricular — Versão Resumida

```text
Matemática
├── Pré-matemática
├── Números
│   ├── Naturais
│   ├── Inteiros
│   ├── Racionais
│   ├── Reais
│   └── Complexos
├── Operações
│   ├── Adição
│   ├── Subtração
│   ├── Multiplicação
│   ├── Divisão
│   ├── Potenciação
│   └── Radiciação
├── Frações e Decimais
│   ├── Conceito
│   ├── Equivalência
│   ├── Comparação
│   ├── Operações
│   └── Aplicações
├── Proporcionalidade
│   ├── Razão
│   ├── Proporção
│   ├── Porcentagem
│   └── Regra de três
├── Álgebra
│   ├── Expressões
│   ├── Equações
│   ├── Inequações
│   ├── Sistemas
│   ├── Produtos notáveis
│   └── Fatoração
├── Funções
│   ├── Afim
│   ├── Quadrática
│   ├── Modular
│   ├── Exponencial
│   ├── Logarítmica
│   └── Trigonométrica
├── Geometria
│   ├── Plana
│   ├── Espacial
│   ├── Analítica
│   └── Vetorial
├── Medidas
│   ├── Comprimento
│   ├── Área
│   ├── Volume
│   ├── Massa
│   ├── Tempo
│   └── Velocidade
├── Estatística
│   ├── Dados
│   ├── Gráficos
│   ├── Média/mediana/moda
│   ├── Variância/desvio padrão
│   └── Inferência
├── Probabilidade
│   ├── Espaço amostral
│   ├── Eventos
│   ├── Condicional
│   ├── Independência
│   └── Distribuições
├── Combinatória
│   ├── Contagem
│   ├── Permutação
│   ├── Arranjo
│   ├── Combinação
│   └── Binômio de Newton
├── Matemática financeira
├── Lógica e conjuntos
├── Pré-cálculo
├── Cálculo
├── Álgebra linear
└── Matemática discreta
```

---

## 19. MVP Recomendado

O MVP deve ser radicalmente focado em listas de exercícios. Nada de construir uma plataforma gigante antes de provar que a sequência de listas funciona.

### 19.1 MVP 1 — Núcleo de listas progressivas de aritmética

Conteúdos iniciais:

1. Contagem
2. Comparação de quantidades
3. Valor posicional
4. Adição simples
5. Adição com reagrupamento
6. Subtração simples
7. Subtração com empréstimo
8. Tabuada
9. Multiplicação simples
10. Divisão simples

Funções obrigatórias:

- cadastro de árvore de conteúdos;
- cadastro de listas;
- exercícios ordenados dentro da lista;
- tela de resolução simples;
- correção;
- repetição de erros;
- registro de tempo;
- status da lista: não iniciada, em andamento, concluída, repetir, dominada;
- liberação da próxima lista;
- relatório básico por aluno.

O MVP deve responder a uma pergunta:

```text
Conseguimos criar uma sequência de exercícios tão clara que o aluno progride quase sem explicação?
```

### 19.2 MVP 2 — Frações, decimais e porcentagem

Adicionar:

- frações equivalentes;
- operações com frações;
- decimais;
- porcentagem;
- problemas contextualizados;
- identificação de erro comum.

### 19.3 MVP 3 — Álgebra inicial

Adicionar:

- expressões algébricas;
- equações do 1º grau;
- inequações;
- sistemas simples;
- problemas verbais.

---

## 20. Regras de Produto

As regras abaixo devem proteger o foco do produto: exercício, exercício, exercício. Todo recurso novo deve provar que melhora a resolução de listas.

1. O exercício é o centro do produto.
2. Nenhum conteúdo avançado deve ser liberado se pré-requisito crítico estiver fraco.
3. O aluno pode errar, mas o sistema precisa entender o tipo de erro.
4. Toda questão deve estar ligada a uma lista.
5. Toda lista deve estar ligada a um nó da árvore.
6. Toda lista deve ter objetivo pedagógico claro.
7. A dificuldade deve subir pouco por vez.
8. Tempo importa, mas não pode ser usado sozinho.
9. Explicação deve ser curta e acionável.
10. Problema aplicado só deve vir depois de fluência mínima.
11. Desafio deve ser usado para expansão, não punição.
12. Revisão espaçada é obrigatória.
13. BNCC é camada de cobertura, não prisão didática.
14. A criação de listas deve ser simples para humanos e IAs.
15. O aluno deve sempre saber qual é a próxima lista.

---

## 21. Resumo Executivo

O sistema será, antes de tudo, um motor de exercícios matemáticos progressivos no estilo Kumon.

A matemática será organizada como árvore de pré-requisitos, mas a interação principal será simples:

```text
resolver lista
corrigir
repetir
avançar
revisar
```

O aluno não precisa enxergar toda a complexidade curricular. Ele precisa receber a lista certa, no momento certo, com dificuldade certa.

O sistema não perguntará apenas “qual série você está?”, mas sim:

```text
Qual lista você domina?
Qual lista ainda oscila?
Qual erro se repete?
Qual pré-requisito precisa voltar?
Qual é a próxima lista ideal?
```

Essa é a base real do produto.

---

## 22. Frase de Direção do Projeto

Construir um motor de listas progressivas de matemática, inspirado no Kumon, organizado por árvore lógica de pré-requisitos, capaz de treinar, corrigir, repetir, revisar e avançar o aluno por domínio real.

## 23. Princípio Final

```text
Menos aula.
Mais exercício.
Menos explicação longa.
Mais sequência lógica.
Menos mapa confuso.
Mais próxima lista certa.
```

A matemática deve ficar fácil de praticar, não superficial. Simples na frente, rigorosa por trás.

