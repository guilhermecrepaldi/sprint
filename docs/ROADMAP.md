# SPRINT Roadmap

## Agora

- Manter a Sprint limpa e testavel no Android Studio.
- Consolidar fluxo de exercicio continuo.
- Garantir registro confiavel de cada tentativa.
- Expandir biblioteca parametrica de exercicios por tema/template.
- Validar densidade por tema e zoom exato por exercicio.
- Garantir que a engine apenas sugira; mudancas de Sprint dependem do usuario.

## Proximo bloco

- Ligar gestos finais no tablet:
  - Triplo toque no enter.
  - Segurar enter para registro.
  - Arraste da bolinha divisoria.
  - Scroll superior com troca calma.
- Persistir altura do divisor por exercicio/sessao.
- Registrar no backend quando a sessao usa densidade leve/fixa/densa/exata.
- Expor no historico: tema, template, densidade, tempo, acertos, erros, OCR e confianca.
- Validar aviso de score apos 5 erros consecutivos.
- Validar que `permanecer` nao altera dificuldade/tema/zoom.
- Validar que `ajustar` abre scrolls secundarios.
- Manter score e-sports sem teto publico, mas com thresholds internos normalizados em 0..10.
- Separar XP/atividade de Arena/ranked competitivo.

## Arena e e-sports

- Manter Sprint livre como treino infinito.
- Criar Arena no Painel, sem poluir Sprint.
- Usar `ranked_mode`, `rules_version`, `competitive_score` e `audit_flags`.
- Ranking competitivo deve usar apenas tentativas auditaveis.
- Proximos passos: seed fixo de pacote, replay, revisao de anti-cheat e ranking por tema/modo.

## Biblioteca de exercicios

- Cada tema deve ter familias parametrizadas.
- Cada familia deve ter variacoes semelhantes.
- Zoom exato deve gerar cerca de 200 variacoes muito proximas do exercicio atual.
- Prioridade de geradores:
  1. Potenciacao/radiciacao.
  2. Equacao linear.
  3. Quadratica.
  4. Fracoes.
  5. Fatoracao.
  6. Funcoes.
  7. Calculo.

## Adaptacao

- Usar acerto, velocidade, estabilidade da escrita e repeticao para sugerir foco.
- Sinalizacoes devem ser raras e leves.
- Depois de muita tentativa/erro, pode aparecer uma sugestao discreta em cor diferente.
- O app deve adaptar sem interromper o estado de estudo profundo.

## Qualidade

- Backend: manter `python -m unittest` verde.
- Android: manter `.\gradlew.bat :app:assembleDebug` verde.
- Testar em emulador/tablet antes de considerar UX concluida.
- Seeds e geracao de exercicios devem ser idempotentes/aditivos, sem apagar historico.

## Nao agora

- Tutorial completo.
- Rede social.
- Ranking global pesado fora da Arena minima.
- UI explicativa permanente na Sprint.
- Gamificacao infantil.
