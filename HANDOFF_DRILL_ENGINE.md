# HANDOFF — Arquitetura Drill Engine & Proof of Work

Projeto: LOVE CLASS ("Strava da Matemática")
Workspace: `D:\LOVE CLASS`
Destino: Claude (Próximo Agente / Dev)

> STATUS: HISTORICO. Para prioridades atuais, leia `AGENTS.md`, `docs/README.md` e `docs/ROADMAP.md`.

---

## 1. Contexto Atual e Descobertas
A infraestrutura de telemetria já existe e é extremamente robusta. Durante a auditoria em `backend/models/`, constatamos que o sistema já possui tabelas para captura de traços via Stylus/Tablet (`PenEvent`, `ExerciseAttempt`, pressão média, OCR, etc.).

A diretriz atual **muda radicalmente o design do Produto (MVP 2.1)**: o sistema agora é um Motor de Treinamento de Alta Frequência (Drill Engine), sem Inteligência Artificial provendo "tutoriais" ou "explicações". O foco é **Performance, Repetição e Pressão de Volume**.

---

## 2. Decisões Arquiteturais Consolidadas (O que você deve implementar)

1. **Marathon Mode Nativo:** 
   O sistema operará 100% em lotes (Maratonas). O Backend deve fornecer "Batches" de exercícios (via `GET /api/drill/batch` ou similar) contendo múltiplos desafios. O Android armazena o lote localmente, e o usuário os resolve sem requisições HTTP entre um exercício e outro (Latência Zero).
   
2. **UX de "Deep Work" (Minimalismo Absoluto):**
   - Nada de vermelhos agressivos ou sirenes.
   - O UI/UX deve simular o silêncio de uma folha de papel.
   - A Folha (App Kotlin) suportará paginação flexível: desde 1 único cálculo complexo centralizado até 30 operações rápidas empilhadas na vertical.
   
3. **Sprints Aritméticos Base (Hiperagilidade):**
   - O treinamento começa na base (`5+2`, `-5+8`, `5-8`).
   - Meta: Resolver 100 em 50 segundos.
   - **Técnica:** NÃO usar IA ou banco de dados para gerar isso. Crie um `arithmetic_drill.py` que gera os números aleatoriamente (On-The-Fly) para treinar jogo de sinais de forma infinita e determinística.
   - O Input no Android deve possuir *Auto-Submit* ao bater o comprimento da string (ex: digitou "7" em `5+2`, o cursor pula automático).

4. **Public Profile & Proof of Work (O Dashboard):**
   - O Dashboard de resultados evoluirá para uma Tela Pública Compartilhável (`/u/slug`).
   - Deverá exibir um **Heatmap** de dedicação.
   - As centenas de skills (`generated_v1`) devem ser empacotadas em **Tracks (Trilhas)** (Ex: "Trilha de Equações Quadráticas", "Trilha de Fundamentos").
   - Foco em expor a estamina e constância (Prova de Esforço), e não expor os erros pontuais do aluno.

5. **Preparação Modular Multi-Tenant (Futuro):**
   - O código deve isolar o módulo "Core Engine do Aluno" de qualquer lógica de "Professor/Responsável".
   - Adicione suporte embrionário no banco para `roles_types` (B2B/B2C, Licenciamento de Professores), mas NÃO perca tempo desenvolvendo as telas de Professor agora. Foco 100% no aluno.

---

## 3. Estado do Banco de Dados
A meta primária de exercícios preenchidos no DB via Gemini foi atingida:
- O banco Postgres `strava_math_postgres` possui mais de **6.400 itens** em `generated_v1` abarcando do básico à geometria analítica e cálculo.
- Um último script de nivelamento (`task-149`) foi enviado para background rodando as 3 últimas skills faltantes com `batch-size 10`. 

---

## 4. Próximos Passos (Para Claude)
Seu ponto de partida:
1. Revise o `SUPER_SPEC.md` atualizado com as adições no capítulo 17.
2. Inicie a codificação no Backend (FastAPI): Crie os endpoints assíncronos orientados a lote (`Batch`).
3. Modele as tabelas de rastreio de Trilha (`Tracks`) e adicione as chaves de compartilhamento social (`slug`, `public`) na entidade `Student/User`.
4. Garanta que o App Android comunique-se via *Flush* (enviando a telemetria do lote todo apenas ao final do treino).

Boa sorte, Claude. Mantenha o código estritamente modular.
