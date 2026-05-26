# SPRINT UX Spec

## Direcao visual

Light, slim, fit. A Sprint deve parecer uma folha viva de estudo, nao um dashboard. O foco e resolucao.

Paleta atual: fundo claro/parchment, tinta discreta, linhas e pontos com baixa opacidade.

## Abas por scroll superior

O topo tem um scroll discreto e largo o suficiente para o dedo. Ao mover o dedo, a aba muda quando o item fica estavel no centro. A mudanca de pagina deve ser calma; o objetivo nao e ficar trocando de pagina.

Regras:

- Scroll sempre presente no topo.
- Texto das abas deve ser discreto.
- Troca visual com fade suave.
- Evitar troca acidental por micro movimento.
- Area de toque deve ser grande o suficiente para dedo/tablet, mesmo com visual leve.
- Futuro: vibracao leve somente quando a aba realmente troca.

## Sprint limpa

Na aba Sprint, nao colocar UI fixa alem do necessario. O aluno deve ver:

- Enunciado.
- Rascunho/resolucao.
- Resposta.
- Enter circular.
- Bolinha esquerda no divisor.

Sem textos de tutorial por enquanto.

## Enter circular

Toque normal:

- Avanca o exercicio ou confirma a tela secundaria.

Segurar:

- Mostra registro leve da sessao: acertos, total, percentual, tempo e exercicio atual.

Triplo toque:

- Abre uma pagina secundaria de scrolls.

## Scrolls secundarios

Triplo toque no enter abre uma tela temporaria minimalista com scrolls:

- Tema.
- Densidade.
- Zoom: tema ou exato.

Ao confirmar no enter, aplica a escolha e volta para a Sprint.

## Densidade

Nomes atuais:

- Leve: menos fixacao, sobe mais rapido.
- Fixa: padrao.
- Densa: mais repeticao, sobe devagar.
- Exata: usa o exercicio atual como semente e fixa no `template_id`.

## Bolinha do divisor

A bolinha pequena no lado esquerdo da divisao entre resolucao e resposta controla a altura do rascunho/resolucao.

Comportamento:

- Arrastar para cima/baixo altera proporcao da area.
- Exercicios com mais texto podem iniciar com proporcao diferente.
- A mudanca deve ser discreta e nao virar painel.
- Futuro: persistir por exercicio/sessao e vibrar ao iniciar o arraste.

## Escrita e registro

A area de resposta e a principal entrada para OCR/correcao. A area de rascunho e parte do historico de aprendizagem e deve ser salva para analise futura quando possivel.

O canvas de escrita deve começar o traço no primeiro toque/caneta. Nao usar detector com limiar de arrasto para a escrita principal, pois matematica tem sinais pequenos: ponto, menos, expoente, virgula, `1`, `x` e `=`.

O registro deve preservar:

- Traços.
- Tempo ate primeiro traço.
- Tempo total.
- Apagamentos.
- Pausas.
- Resposta reconhecida.
- Confianca do reconhecimento.
- Se a analise foi confiavel.

## Painel

O Painel deve refletir exercicios realmente feitos. Perfil e Historico nao devem depender de numeros mockados.

Ao entrar no Painel, mostrar Perfil primeiro, pois ele responde imediatamente "o que ja fiz?". A lista Perfil/Historico pode existir como navegacao secundaria.

Fontes atuais:

- `GET /api/student/{student_id}/sessions`
- `GET /api/student/{student_id}/skill-progress`

Depois de iniciar ou submeter sessao, o app deve atualizar historico e progresso.

O Historico deve mostrar tambem a densidade usada na sessao: leve, fixa, densa ou exata.

Quando a densidade for exata e houver template registrado, o Painel pode mostrar apenas um marcador discreto de zoom. Nao exibir IDs longos de template na interface principal.

## Feedback De Acerto

Depois de enviar um exercicio, a Sprint deve mostrar um sinal breve antes de trocar para o proximo:

- Mostrar o que o sistema interpretou da escrita.
- Verde/escuro quando correto.
- Vermelho quando incorreto.
- Se nao conseguiu ler, mostrar isso discretamente.
- Em folhas com varios itens, pode mostrar tambem `corretas/total`.

Esse feedback deve ser curto, discreto e nao virar tela de resultado permanente.

Na experiencia Sprint, a folha operacional deve ter 1 exercicio por vez. Assim cada enter registra uma tentativa, retorna certo/erro e so entao avanca para o proximo exercicio.

## Transicao Entre Exercicios

Ao apertar enter:

- coletar somente os traços da resposta para OCR/correcao;
- preservar rascunho e eventos como historico/telemetria quando possivel;
- enviar a tentativa da folha atual;
- mostrar feedback breve;
- avancar;
- abrir a proxima folha/exercicio com canvas de resposta limpo.

A chave de estado visual deve incluir `folhaId`, `exerciseId` e `fieldIndex`. Nao usar somente `fieldIndex`.
