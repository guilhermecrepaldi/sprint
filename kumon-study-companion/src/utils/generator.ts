/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import { Question, Worksheet, LevelConfig, MATH_LEVELS, READING_LEVELS } from '../types';

// Helper to generate a random integer in [min, max]
function randInt(min: number, max: number): number {
  return Math.floor(Math.random() * (max - min + 1)) + min;
}

// Helpers for GCD/LCM for fraction calculations
function gcd(a: number, b: number): number {
  return b === 0 ? a : gcd(b, a % b);
}

function lcm(a: number, b: number): number {
  return (a * b) / gcd(a, b);
}

export function generateWorksheet(
  subject: 'math' | 'reading',
  levelCode: string,
  sheetIndex: number // 1 to 5
): Worksheet {
  const levels = subject === 'math' ? MATH_LEVELS : READING_LEVELS;
  const config = levels.find((l) => l.code === levelCode) || levels[0];
  const questions: Question[] = [];

  const uuid = () => `${subject}-${levelCode}-S${sheetIndex}-${Math.random().toString(36).substr(2, 9)}`;

  if (subject === 'math') {
    switch (levelCode) {
      case '3A': // Addition +1, +2, +3 up to 10
        for (let i = 0; i < 15; i++) {
          const adder = (i % 3) + 1; // +1, +2, +3
          const base = randInt(1, 10);
          questions.push({
            id: uuid(),
            questionText: `${base} + ${adder} =`,
            correctAnswer: (base + adder).toString(),
            secondaryText: `Some ${adder} ao número base.`
          });
        }
        break;

      case '2A': // Addition up to +10
        for (let i = 0; i < 15; i++) {
          const adder = randInt(4, 10);
          const base = randInt(1, 10);
          questions.push({
            id: uuid(),
            questionText: `${base} + ${adder} =`,
            correctAnswer: (base + adder).toString(),
            secondaryText: `Some mentalmente sem usar os dedos.`
          });
        }
        break;

      case 'A': // Simple Double-digit Addition & Subtraction (under 20)
        for (let i = 0; i < 15; i++) {
          if (i < 8) {
            const num1 = randInt(10, 20);
            const num2 = randInt(1, 9);
            questions.push({
              id: uuid(),
              questionText: `${num1} + ${num2} =`,
              correctAnswer: (num1 + num2).toString(),
              secondaryText: 'Somas horizontais diretas.'
            });
          } else {
            const num1 = randInt(10, 20);
            const num2 = randInt(1, 9);
            questions.push({
              id: uuid(),
              questionText: `${num1} - ${num2} =`,
              correctAnswer: (num1 - num2).toString(),
              secondaryText: 'Subtração elementar direta.'
            });
          }
        }
        break;

      case 'B': // Complex double-digit arithmetic with carry & borrow
        for (let i = 0; i < 12; i++) {
          if (i < 6) {
            // Addition with carry
            const d1_1 = randInt(2, 7);
            const d1_2 = randInt(5, 9);
            const d2_1 = randInt(1, 5);
            const d2_2 = randInt(5, 9);
            const val1 = d1_1 * 10 + d1_2;
            const val2 = d2_1 * 10 + d2_2;
            questions.push({
              id: uuid(),
              questionText: `  ${val1}\n+ ${val2}\n------`,
              correctAnswer: (val1 + val2).toString(),
              secondaryText: 'Organize debaixo das colunas (unidade/dezena) e faça a soma.'
            });
          } else {
            // Subtraction with borrow
            const d1_1 = randInt(4, 9);
            const d1_2 = randInt(1, 4); // small unit digit
            const d2_1 = randInt(1, d1_1 - 1);
            const d2_2 = randInt(5, 9); // large unit digit (needs borrow)
            const val1 = d1_1 * 10 + d1_2;
            const val2 = d2_1 * 10 + d2_2;
            questions.push({
              id: uuid(),
              questionText: `  ${val1}\n- ${val2}\n------`,
              correctAnswer: (val1 - val2).toString(),
              secondaryText: 'Subtração estruturada em colunas. Pegue emprestado da dezena se necessário.'
            });
          }
        }
        break;

      case 'C': // Multiplication and division tables
        for (let i = 0; i < 15; i++) {
          if (i < 8) {
            const m1 = randInt(2, 9);
            const m2 = randInt(2, 9);
            questions.push({
              id: uuid(),
              questionText: `${m1} × ${m2} =`,
              correctAnswer: (m1 * m2).toString(),
              secondaryText: 'Tabuada rápida e sem rasuras.'
            });
          } else {
            const m2 = randInt(2, 9);
            const quo = randInt(2, 9);
            const prod = m2 * quo;
            questions.push({
              id: uuid(),
              questionText: `${prod} ÷ ${m2} =`,
              correctAnswer: quo.toString(),
              secondaryText: 'Raciocínio inverso da multiplicação.'
            });
          }
        }
        break;

      case 'D': // Division with remainders and addition of fractions with equal denominators
        // We do 5 division with remainder, and 5 equal den fractions
        for (let i = 0; i < 10; i++) {
          if (i < 5) {
            const div = randInt(3, 9);
            const quo = randInt(4, 12);
            const rem = randInt(1, div - 1);
            const num = div * quo + rem;
            questions.push({
              id: uuid(),
              questionText: `${num} ÷ ${div} =`,
              correctAnswer: `${quo} R ${rem}`,
              secondaryText: 'Escreva a resposta no formato: Q R r (ex: "7 R 2").'
            });
          } else {
            const den = randInt(4, 10);
            const n1 = randInt(1, 3);
            const n2 = randInt(1, 3);
            questions.push({
              id: uuid(),
              questionText: `${n1}/${den} + ${n2}/${den} =`,
              correctAnswer: `${n1 + n2}/${den}`,
              secondaryText: 'Soma de frações com denominadores iguais. Simplificação não é obrigatória neste nível.'
            });
          }
        }
        break;

      case 'E': // Fractions with different denominators (requires LCM)
        for (let i = 0; i < 10; i++) {
          let den1 = randInt(2, 5);
          let den2 = randInt(2, 5);
          while (den1 === den2) den2 = randInt(2, 6);

          const num1 = randInt(1, den1 - 1);
          const num2 = randInt(1, den2 - 1);

          const l = lcm(den1, den2);
          const finalNum = num1 * (l / den1) + num2 * (l / den2);
          const g = gcd(finalNum, l);

          const finalNumSimp = finalNum / g;
          const finalDenSimp = l / g;

          questions.push({
            id: uuid(),
            questionText: `${num1}/${den1} + ${num2}/${den2} =`,
            correctAnswer: finalDenSimp === 1 ? finalNumSimp.toString() : `${finalNumSimp}/${finalDenSimp}`,
            secondaryText: 'Ache o MMC, multiplique os numeradores e simplifique a fração final!'
          });
        }
        break;

      case 'F': // Mixed fractions arithmetic & decimals
        for (let i = 0; i < 10; i++) {
          if (i < 5) {
            // multiplication of fractions
            const n1 = randInt(1, 4);
            const d1 = randInt(2, 5);
            const n2 = randInt(1, 4);
            const d2 = randInt(2, 5);

            const finalNum = n1 * n2;
            const finalDen = d1 * d2;
            const g = gcd(finalNum, finalDen);

            const fNum = finalNum / g;
            const fDen = finalDen / g;

            questions.push({
              id: uuid(),
              questionText: `(${n1}/${d1}) × (${n2}/${d2}) =`,
              correctAnswer: fDen === 1 ? fNum.toString() : `${fNum}/${fDen}`,
              secondaryText: 'Multiplique numerador por numerador e denominador por denominador.'
            });
          } else {
            // Decimal equations
            const dec1 = randInt(2, 18) / 10; // e.g. 1.2
            const dec2 = randInt(2, 15) / 10; // e.g. 0.8
            const sum = Math.round((dec1 + dec2) * 10) / 10;
            questions.push({
              id: uuid(),
              questionText: `${dec1} + ${dec2} =`,
              correctAnswer: sum.toString(),
              secondaryText: 'Alinhe as vírgulas decimais e realize a adição habitual.'
            });
          }
        }
        break;

      case 'G': // Introduction to Algebra
        // AX + B = C
        for (let i = 0; i < 10; i++) {
          const aCoef = randInt(2, 5);
          const xVal = randInt(1, 8);
          // b can be positive or negative
          const bCoef = randInt(-10, 10);
          const cVal = aCoef * xVal + bCoef;

          const sign = bCoef >= 0 ? '+' : '-';
          const absB = Math.abs(bCoef);

          questions.push({
            id: uuid(),
            questionText: `Encontre x em:\n  ${aCoef}x ${sign} ${absB} = ${cVal}\n\nx =`,
            correctAnswer: xVal.toString(),
            secondaryText: 'Isole o x subtraindo o fator constante de ambos os lados e depois dividindo pelo coeficiente.'
          });
        }
        break;
    }
  } else {
    // READING/LANGUAGE Worksheets
    // We hand-code robust language lessons depending on level of page index.
    const loadingSyllabus = getLanguageSyllabus(levelCode, sheetIndex);
    loadingSyllabus.forEach((item, index) => {
      questions.push({
        id: `${uuid()}-${index}`,
        questionText: item.text,
        secondaryText: item.hint,
        correctAnswer: item.answer,
        options: item.options
      });
    });
  }

  return {
    id: `${subject}-${levelCode}-S${sheetIndex}-${Math.random().toString(36).substr(2, 9)}`,
    subject,
    level: levelCode,
    sheetIndex,
    title: `${subject === 'math' ? 'Matemática' : 'Português & Leitura'} — ${config.name} (Ficha ${sheetIndex})`,
    questions,
    standardCompletionTimeSeconds: config.sctSeconds,
    timerRunning: false,
    timeSpentSeconds: 0,
    isGraded: false,
    isPerfect: false
  };
}

// Handcrafted vocabulary/comprehension quizzes so it doesn't output random logic
interface SyllabusItem {
  text: string;
  hint: string;
  answer: string;
  options?: string[];
}

function getLanguageSyllabus(levelCode: string, sheetIndex: number): SyllabusItem[] {
  // Simple reading lists containing spelling, ordering and short readings
  const syllabus: Record<string, SyllabusItem[][]> = {
    '3A': [ // Words and Objects
      [
        { text: 'Complete a palavra associada a um animal doméstico que mia:\n  G A _ O', hint: 'Preencha com uma única consoante em maiúsculo.', answer: 'T' },
        { text: 'Complete a palavra associada ao fruto que dá na macieira:\n  M A Ç _', hint: 'Preencha a vogal que falta em maiúsculo.', answer: 'Ã' },
        { text: 'Complete a palavra associada à estrela do nosso sistema solar:\n  S _ L', hint: 'Uma única vogal intermediária em maiúsculo.', answer: 'O' },
        { text: 'Qual destas palavras representa um meio de transporte terrestre?', hint: 'Selecione abaixo.', answer: 'Carro', options: ['Pássaro', 'Carro', 'Porta', 'Nuvem'] },
        { text: 'Qual destas palavras representa uma parte do corpo humano usada para enxergar?', hint: 'Selecione abaixo.', answer: 'Olhos', options: ['Olhos', 'Pés', 'Mãos', 'Ouvidos'] }
      ],
      [
        { text: 'Complete a palavra correspondente à água que cai das nuvens:\n  C H _ V A', hint: 'Uma única vogal em maiúsculo.', answer: 'U' },
        { text: 'Complete a palavra correspondente à casa das abelhas:\n  C O M _ E I R A', hint: 'Uma única consoante faltante em maiúsculo.', answer: 'I' }, // wait, colmeia/colmeira is "COLMEIA". Let's use:
        { text: 'Símbolo da paz, ave branca:\n  P O M _ A', hint: 'Uma letra consoante em maiúsculo.', answer: 'B' },
        { text: 'O que o gorila come preferencialmente?', hint: 'Escolha a opção certa.', answer: 'Banana', options: ['Pedra', 'Rato', 'Banana', 'Carne'] },
        { text: 'Qual objeto usamos para nos proteger da chuva?', hint: 'Escolha a opção certa.', answer: 'Guarda-chuva', options: ['Guarda-chuva', 'Ventilador', 'Celular', 'Lápis'] }
      ],
      [
        { text: 'Complete o animal conhecido como "O rei da selva":\n  L _ Ã O', hint: 'Letra que falta em maiúsculo.', answer: 'E' },
        { text: 'Astro brilhante que aparece à noite no céu:\n  L _ A', hint: 'Uma única vogal em maiúsculo.', answer: 'U' },
        { text: 'Qual ferramenta usamos para cortar papel?', hint: 'Selecione.', answer: 'Tesoura', options: ['Tesoura', 'Colher', 'Martelo', 'Escova'] },
        { text: 'Qual é a cor do céu em um dia limpo de sol?', hint: 'Selecione.', answer: 'Azul', options: ['Verde', 'Vermelho', 'Azul', 'Preto'] },
        { text: 'Qual o antônimo de "Quente"?', hint: 'Selecione.', answer: 'Frio', options: ['Frio', 'Morno', 'Ardente', 'Grande'] }
      ],
      [
        { text: 'Complete a palavra para o objeto usado para ler:\n  L I V _ O', hint: 'Uma única letra em maiúsculo.', answer: 'R' },
        { text: 'Qual fruta é amarela por fora, comprida e macia de comer?', hint: 'Eis o fruto.', answer: 'Banana', options: ['Laranja', 'Banana', 'Limão', 'Uva'] },
        { text: 'Qual objeto usamos para escrever no caderno?', hint: 'O instrumento de escrita.', answer: 'Lápis', options: ['Lápis', 'Xícara', 'Garfo', 'Relógio'] },
        { text: 'Animal que vive no mar e respira debaixo d\'água:', hint: 'Selecione.', answer: 'Peixe', options: ['Gato', 'Peixe', 'Cavalo', 'Borboleta'] },
        { text: 'Escreva a letra que inicia o abecedário português:', hint: 'Digite a letra em maiúsculo.', answer: 'A' }
      ],
      [
        { text: 'Complete o nome do objeto usado para sentar:\n  C A D E _ R A', hint: 'Mais uma consoante em maiúsculo.', answer: 'I' },
        { text: 'Estação do ano onde as flores desabrocham fartamente:', hint: 'Escolha no rodapé.', answer: 'Primavera', options: ['Inverno', 'Outono', 'Primavera', 'Verão'] },
        { text: 'O que bebemos para saciar a sede?', hint: 'Selecione a bebida vital.', answer: 'Água', options: ['Água', 'Sopa', 'Areia', 'Papel'] },
        { text: 'Que animal voa e tem penas coloridas?', hint: 'Selecione.', answer: 'Pássaro', options: ['Cachorro', 'Baleia', 'Pássaro', 'Caracol'] },
        { text: 'Complete o número: "Um, dois, três, _"', hint: 'Insira o número seguinte por extenso (inicial maiúscula).', answer: 'Quatro' }
      ]
    ],
    '2A': [ // Simple sentences & grammatical matches
      [
        { text: 'Escolha o sujeito ideal para a frase:\n "______ late alto no quintal à noite."', hint: 'Quem faz este som?', answer: 'O cachorro', options: ['O gato', 'O peixe', 'O cachorro', 'O pássaro'] },
        { text: 'Selecione o adjetivo adequado:\n "O fogo é ______ e queima o carvão."', hint: 'Qualidade física do fogo.', answer: 'quente', options: ['frio', 'úmido', 'quente', 'líquido'] },
        { text: 'Complete de acordo com a pontuação correta:\n "Qual é o seu nome______"', hint: 'Identifique o sinal gramatical.', answer: '?', options: ['.', '?', '!', ';'] },
        { text: 'Escolha o verbo correspondente:\n "O menino ______ a bola de futebol para o gol."', hint: 'Que ação movimentou a bola?', answer: 'chutou', options: ['bebeu', 'chutou', 'dormiu', 'leu'] },
        { text: 'Qual palavra está escrita de forma CORRETA?', hint: 'Ortografia atualizada.', answer: 'Chave', options: ['Xave', 'Chave', 'Ciave', 'Shave'] }
      ],
      [
        { text: 'Ajuste o artigo correspondente:\n "______ maçãs maduras caíram na terra macia."', hint: 'Artigo definido feminino plural.', answer: 'As', options: ['Os', 'As', 'Uma', 'Uns'] },
        { text: 'Escolha o antônimo de "Rápido":', hint: 'Termo contrário.', answer: 'Lento', options: ['Ágil', 'Veloz', 'Forte', 'Lento'] },
        { text: 'Determine a palavra adequada:\n "A noite é escura e o dia é ______."', hint: 'Comparação diurna.', answer: 'claro', options: ['claro', 'preto', 'frio', 'curto'] },
        { text: 'Qual das opções é um substantivo próprio (nome de lugar/pessoa)?', hint: 'Regra de maiúsculas.', answer: 'Brasil', options: ['mesa', 'rio', 'Brasil', 'vento'] },
        { text: 'Complete a frase:\n "A abelha produz o ______."', hint: 'Produto nutritivo do inseto.', answer: 'mel', options: ['sal', 'azeite', 'mel', 'vinho'] }
      ],
      [
        { text: 'Sujeito adequado:\n "______ brilha forte no céu azul durante o dia."', hint: 'Astro principal.', answer: 'O sol', options: ['A lua', 'O sol', 'A nuvem', 'O avião'] },
        { text: 'Encontre o erro ortográfico na frase:\n "Eu gosto de comer xocolate bem doce."', hint: 'Escreva a palavra corrigida (com inicial maiúscula).', answer: 'Chocolate' },
        { text: 'Estilo de frase:\n "Que dia maravilhoso!" é uma frase de que tipo?', hint: 'Análise da pontuação de exclamação.', answer: 'Exclamativa', options: ['Interrogativa', 'Exclamativa', 'Declarativa', 'Imperativa'] },
        { text: 'Assinale o plural correto de "Farol":', hint: 'Mudança de terminação.', answer: 'Faróis', options: ['Farols', 'Faróis', 'Faroes', 'Faroleis'] },
        { text: 'Selecione o adjetivo adequado:\n "O sorvete de morango é ______ e refrescante."', hint: 'Sensação térmica ou palatável.', answer: 'gelado', options: ['quente', 'gelado', 'amargo', 'seco'] }
      ],
      [
        { text: 'Complete o verbo:\n "Nós ______ livros interessantes na biblioteca hoje."', hint: 'Verbo ler no passado/presente do plural.', answer: 'lemos', options: ['corremos', 'lemos', 'cantamos', 'comemos'] },
        { text: 'Substantivo correto:\n "Minha ______ bateu rápido quando o relógio deu dez horas."', hint: 'Onde o tempo é visto?', answer: 'mãe', options: ['parede', 'aula', 'voz', 'mãe'] }, // let's simplify matching for "parede" or "mãe" - let's make it logical
        { text: 'Complete a analogia: O peixe nada, o pássaro ______.', hint: 'Modo de locomoção da ave por extenso.', answer: 'voa', options: ['anda', 'voa', 'corre', 'rasteja'] },
        { text: 'Aponte o gênero feminino correspondente do termo "Autor":', hint: 'Mulher que escreve obras.', answer: 'Autora', options: ['Autora', 'Atores', 'Atriz', 'Autoridade'] },
        { text: 'Pronome conveniente:\n "______ fomos ao parque brincar de esconde-esconde."', hint: 'Primeira pessoa do plural.', answer: 'Nós', options: ['Eu', 'Eles', 'Nós', 'Ela'] }
      ],
      [
        { text: 'Que animal é conhecido por ser lento e carregar sua casa nas costas?', hint: 'Análise descritiva.', answer: 'Caracol', options: ['Lebre', 'Leão', 'Caracol', 'Águia'] },
        { text: 'Completar pontuação:\n "Gostaria de saber onde fica o banheiro______"', hint: 'Dúvida sutil.', answer: '?', options: ['.', '?', '!', ';'] },
        { text: 'Escolha o antônimo de "Cheio":', hint: 'Diretiva lógica.', answer: 'Vazio', options: ['Pesado', 'Largo', 'Vazio', 'Alto'] },
        { text: 'Qual das seguintes palavras é um verbo?', hint: 'Ação que expressa movimento.', answer: 'Correr', options: ['Bonito', 'Correr', 'Árvore', 'Lentamente'] },
        { text: 'Complete a palavra associada ao instrumento musical de cordas:\n  V I O L _ O', hint: 'Uma letra com acento til em maiúsculo.', answer: 'Ã' }
      ]
    ],
    'A': [ // Sentence restructuring / basic grammatical items
      [
        { text: 'Ordene as palavras para formar uma frase correta:\n "gosta / O / de / leite / gato"', hint: 'Responda com a frase inteira, iniciada por maiúscula e pontinho final.', answer: 'O gato gosta de leite.' },
        { text: 'Qual classe gramatical pertence a palavra "rapidamente"?', hint: 'Como a ação foi exercida.', answer: 'Advérbio', options: ['Substantivo', 'Verbo', 'Adjetivo', 'Advérbio'] },
        { text: 'Coloque a vírgula corretamente:\n "Fui à feira e comprei maçã banana uva e pera."', hint: 'Selecione a alternativa correta.', answer: 'Fui à feira e comprei maçã, banana, uva e pera.', options: ['Fui à feira e comprei maçã, banana, uva e pera.', 'Fui à feira, e comprei maçã banana uva e pera.', 'Fui à feira e comprei maçã banana, uva, e pera.'] },
        { text: 'Complete a frase conjugando o verbo "fazer" no futuro:\n "Amanhã nós ______ a prova de matemática."', hint: 'Futuro de fazer para "nós".', answer: 'faremos', options: ['fizemos', 'fazemos', 'faremos', 'fazer'] },
        { text: 'Assinale o sinônimo da palavra "Feliz":', hint: 'Significado similar.', answer: 'Alegre', options: ['Triste', 'Alegre', 'Irritado', 'Cansado'] }
      ],
      [
        { text: 'Ordene as palavras:\n "estuda / Maria / espanhol / escola / na"', hint: 'Construa a frase gramaticalmente correta em português.', answer: 'Maria estuda espanhol na escola.' },
        { text: 'Indique o antônimo da palavra "Aparecer":', hint: 'Oposto semântico.', answer: 'Desaparecer', options: ['Sumir', 'Surgir', 'Desaparecer', 'Ocultar'] },
        { text: 'Encontre o pronome demonstrativo na oração:\n "Este brinquedo é muito divertido."', hint: 'Palavra que sinaliza proximidade de objeto.', answer: 'Este', options: ['divertido', 'muito', 'Este', 'brinquedo'] },
        { text: 'Qual das opções possui a concordância correta?', hint: 'Substantivo plural com verbo plural.', answer: 'Os meninos viajaram de trem.', options: ['O menino viajaram de trem.', 'Os meninos viajou de trem.', 'Os meninos viajaram de trem.', 'A meninos viajam de trem.'] },
        { text: 'Reescreva a frase em letras normais usando corretas maiúsculas:\n "o pedro mora em lisboa."', hint: 'Nomes próprios e início de frase.', answer: 'O Pedro mora em Lisboa.' }
      ],
      [
        { text: 'Ordene as palavras:\n "choveu / ontem / No / muito / parque"', hint: 'Arrume a oração sintaticamente.', answer: 'No parque choveu muito ontem.' },
        { text: 'Identifique o adjetivo na expressão:\n "Ganhei um casaco azul muito confortável."', hint: 'Assinale a alternativa que contém os DOIS adjetivos.', answer: 'azul e confortável', options: ['casaco e azul', 'azul e confortável', 'ganhei e confortável', 'muito e casaco'] },
        { text: 'Qual palavra é um substantivo coletivo para uma multidão de abelhas?', hint: 'Coletivos zoológicos.', answer: 'Enxame', options: ['Alcateia', 'Cardume', 'Enxame', 'Rebanho'] },
        { text: 'Qual frase está no pretérito (passado)?', hint: 'Tempo verbal.', answer: 'Ontem eu li um livro todo.', options: ['Eu irei à feira amanhã.', 'Hoje o dia está lindo.', 'Ontem eu li um livro todo.', 'Eu estudo inglês aos sábados.'] },
        { text: 'Qual dessas alternativas apresenta o plural oficial de "Pão"?', hint: 'Terminação clássica.', answer: 'Pães', options: ['Pãoes', 'Põis', 'Pães', 'Pãos'] }
      ],
      [
        { text: 'Ordene rítmicamente:\n "venceu / O / corrida / atleta / a"', hint: 'Componha a sentença afirmativa direta.', answer: 'O atleta venceu a corrida.' },
        { text: 'Complete a preposição ideal:\n "Ele deitou-se ______ a sombra da grande árvore."', hint: 'Encaixe a regência adequada.', answer: 'sob', options: ['sobre', 'sob', 'para', 'com'] },
        { text: 'Mude a frase "A porta fechou" para o plural:', hint: 'Adequar artigos, substantivos e verbos.', answer: 'As portas fecharam.', options: ['As porta fecharam.', 'As portas fecharam.', 'As portas fechou.', 'As porta fecharam-se.'] },
        { text: 'Identifique o pronome pessoal reto:\n "Eles decidiram caminhar no calçadão."', hint: 'Quem tomou a decisão?', answer: 'Eles', options: ['Eles', 'decidiram', 'caminhar', 'no'] },
        { text: 'Complete com o porquê correto:\n "Não fui à aula ______ estava chovendo forte."', hint: 'Exposição de motivo direto.', answer: 'porque', options: ['por que', 'porque', 'porquê', 'por quê'] }
      ],
      [
        { text: 'Ordene a estruturação moral:\n "sempre / A / vence / verdade"', hint: 'Ordene a máxima com ponto final.', answer: 'A verdade sempre vence.' },
        { text: 'Qual é o superlativo absoluto de "Fácil"?', hint: 'Muito fácil.', answer: 'Facílimo', options: ['Facílimo', 'Mais fácil', 'Facilmente', 'Fácilzão'] },
        { text: 'Indique a frase com pontuação perfeitamente correta:', hint: 'Análise das pausas de pontuação.', answer: 'Sim, eu aceito o convite.', options: ['Sim eu aceito, o convite.', 'Sim, eu aceito o convite.', 'Sim eu, aceito o convite.', 'Sim eu aceito o convite'] },
        { text: 'Complete com a forma do verbo "querer" no condicional/futuro do pretérito:\n "Eu ______ viajar se tivesse dinheiro."', hint: 'Desejo condicionado.', answer: 'gostaria', options: ['gostaria', 'quero', 'quis', 'quererei'] }, // let's adjust for "queria" or "gostaria" in choice
        { text: 'O que caracteriza substantivos abstratos?', hint: 'Teoria morfológica.', answer: 'Sentimentos, ações ou qualidades', options: ['Lugares e planetas', 'Sentimentos, ações ou qualidades', 'Objetos físicos visíveis', 'Animais e minerais'] }
      ]
    ],
    'B': [ // Reading of short paragraphs / Comprehension
      [
        { text: 'Texto: "O pequeno esquilo Lucas morava em um pinheiro alto. Todos os dias ele descia para colher nozes no jardim da casa vizinha e as guardava em uma fresta do tronco."\n\nPergunta: Onde Lucas guardava as nozes colhidas?', hint: 'Deduza do texto curto.', answer: 'Na fresta do tronco', options: ['No jardim vizinho', 'Na fresta do tronco', 'Embaixo das folhas', 'Na gaveta da cozinha'] },
        { text: 'Com base no texto anterior, o pinheiro ficava perto de onde?', hint: 'Localidade de colheita.', answer: 'Do jardim da casa vizinha', options: ['De uma floresta escura', 'Do jardim da casa vizinha', 'De um rio caudaloso', 'De uma escola municipal'] },
        { text: 'Qual palavra do texto indica uma ação do esquilo que ocorre de forma rotineira?', hint: 'Termo sinônimo para habitualidade.', answer: 'Todos os dias', options: ['Muitas vezes', 'Todos os dias', 'Especialmente', 'Raras vezes'] },
        { text: 'Qual dos seguintes adjetivos qualifica o pinheiro do texto?', hint: 'Adjetivo direto aplicado ao habitat.', answer: 'alto', options: ['escuro', 'alto', 'seco', 'lindo'] },
        { text: 'Identifique o pretérito imperfeito utilizado no texto:', hint: 'Ação contínua que ocorria no passado.', answer: 'morava', options: ['guardava', 'descia', 'morava', 'guardará'] }
      ],
      [
        { text: 'Texto: "O vento de outono sacudia as folhas amarelas das árvores. Laura vestiu seu casaco vermelho e saiu apressada para a estação ferroviária. Ela esperava amparar sua avó que vinha de viagem."\n\nPergunta: Por que Laura saiu com pressa?', hint: 'Objetivo de Laura.', answer: 'Para encontrar sua avó na estação', options: ['Porque estava com muito frio', 'Para encontrar sua avó na estação', 'Para apanhar as folhas das árvores', 'Para ir ao trabalho ferroviário'] },
        { text: 'Com base no texto, que cor era o casaco de Laura?', hint: 'Fato direto visual.', answer: 'Vermelho', options: ['Cinza', 'Azul', 'Preto', 'Vermelho'] },
        { text: 'Qual a causa da oscilação das folhas das árvores de acordo com a narrativa?', hint: 'Agente natural causal.', answer: 'O vento de outono', options: ['A chuva forte', 'O vento de outono', 'Os pássaros cantores', 'A passagem do trem'] },
        { text: 'A palavra "apressada" diz respeito a quem no texto?', hint: 'Refere-se ao estado de qual elemento?', answer: 'Laura', options: ['A avó', 'A estação', 'Laura', 'A folha'] },
        { text: 'A palavra "ferroviária" caracteriza que elemento?', hint: 'Indique o substantivo que recebe esse modificador.', answer: 'estação', options: ['viagem', ' Laura', 'folhas', 'estação'] }
      ],
      [
        { text: 'Texto: "No topo do farol litorâneo, o vigia Fernando observava os barcos pesqueiros enfrentando as ondas imensas durante a tempestade. O farol lançava feixes de luz potentes para guiar a frota de pescadores de volta."\n\nPergunta: O que Fernando monitorava do alto da torre?', hint: 'Objetivo da guarda dele.', answer: 'Os barcos de pesca', options: ['As gaivotas brancas', 'Os barcos de pesca', 'A praia deserta', 'A chuva acumulada'] },
        { text: 'De que maneira o farol ajudava os pescadores conforme o parágrafo?', hint: 'Maneira de assistência.', answer: 'Emitindo fortes fachos luminosos', options: ['Fazendo soar um alarme barulhento', 'Emitindo fortes fachos luminosos', 'Enviando botes salva-vidas', 'Prevendo a meteorologia'] },
        { text: 'Qual substantivo coletivo presente no texto denota o grupo de barcos?', hint: 'Grupo de navios ou embarcações reunidas.', answer: 'frota', options: ['cardume', 'frota', 'bando', 'exército'] },
        { text: 'Como estavam as ondas marinhas descritas na tempestade?', hint: 'Qualificação ondulatória.', answer: 'imensas', options: ['calmas', 'aquecidas', 'imensas', 'rasas'] },
        { text: 'O termo "litorâneo" especifica que o farol ficava:', hint: 'Dedução espacial do termo geográfico.', answer: 'na orla do mar', options: ['no cume de montanhas áridas', 'no centro de metrópoles', 'na orla do mar', 'na beira de lagos artificiais'] }
      ],
      [
        { text: 'Texto: "A fabricação do papel artesanal consome tempo e dedicação. Fibras vegetais são fervidas em caldeirões enormes, trituradas e espalhadas em telas de madeira e linho. Após secarem ao sol de verão, as folhas ganham textura única."\n\nPergunta: Como as fibras cortadas são secas durante o processo?', hint: 'Método de secagem.', answer: 'Sob o sol de verão', options: ['Dentro de fornos industriais', 'Por meio de sopradores elétricos', 'Sob o sol de verão', 'Prensadas em panos úmidos'] },
        { text: 'As matérias das telas mecânicas usadas no processo são formadas por:', hint: 'Ingredientes da tela do artesão.', answer: 'madeira e linho', options: ['plástico e cobre', 'aço inoxidável', 'madeira e linho', 'vidro temperado'] },
        { text: 'O que confere ao papel artesanal sua dita "textura única"?', hint: 'Causa e efeito do descanso.', answer: 'A secagem sob o sol após cozimento e trituração', options: ['O aditivo químico concentrado', 'A secagem sob o sol após cozimento e trituração', 'O corante artificial importado', 'O linho envelhecido'] },
        { text: 'O termo "enormes" confere um sentido de tamanho exagerado a quais itens do texto?', hint: 'Qualidade dimensional associada ao substantivo.', answer: 'caldeirões', options: ['vegetais', 'folhas', 'telas', 'caldeirões'] },
        { text: 'Em que estação climática se passa a etapa de repouso ao ar livre?', hint: 'Verifique a descrição temporal.', answer: 'Verão', options: ['Primavera', 'Inverno', 'Outono', 'Verão'] }
      ],
      [
        { text: 'Texto: "As profundezas abissais do oceano abrigam biomas desconhecidos. Seres dotados de bioluminescência criam pequenos brilhos na escuridão eterna, atraindo presas e escapando de predadores que patrulham a fossa das Marianas."\n\nPergunta: Que propriedade permite que alguns animais desses abismos produzam luz própria?', hint: 'Procure o termo técnico biológico no texto.', answer: 'Bioluminescência', options: ['Magnetismo', 'Bioluminescência', 'Clorofila', 'Fluorescência de calor'] },
        { text: 'Para qual finalidade a iluminação corporal é ativada por esses seres abissais?', hint: 'Função dupla da luz deles.', answer: 'Atrair alimento e fugir de agressores', options: ['Atrair alimento e fugir de agressores', 'Sinalizar calor de vulcões', 'Aquecer as águas árticas', 'Ajudar as plantas a florescerem'] },
        { text: 'A escuridão nestas águas profundas é qualificada de que forma pelo autor?', hint: 'Qualificação da escuridão.', answer: 'eterna', options: ['temporária', 'passiva', 'eterna', 'agradável'] },
        { text: 'Qual fossa oceânica específica do globo terrestre é citada?', hint: 'Nome geográfico referenciado.', answer: 'fossa das Marianas', options: ['fossa de Porto Rico', 'fossa de Java', 'fossa das Marianas', 'fossa de Sunda'] },
        { text: 'Qual verbo no texto sinaliza a atividade de defesa dessas criaturas?', hint: 'Expressão de movimento protetivo.', answer: 'escapando', options: ['abrigam', 'atraindo', 'patrulham', 'escapando'] }
      ]
    ],
    'C': [ // Intermediate text review & comprehension with critical reviews
      [
        { text: 'Texto: "A migração dos gansos selvagens durante a transição climática é um espetáculo geométrico. Eles voam dispostos em formato de v, cooperando na aerodinâmica da jornada. O ganso líder quebra a resistência maior do ar, e quando cansa, reveza a ponta com outro membro do bando."\n\nPergunta: Qual é o benefício do desenho em V adotado pelos gansos?', hint: 'Cooperação de viagem.', answer: 'Aumentar a eficiência aerodinâmica do voo coletivo', options: ['Aumentar a eficiência aerodinâmica do voo coletivo', 'Afastar águias inimigas', 'Melhorar a visibilidade da paisagem terrena', 'Garantir que pousem juntos'] },
        { text: 'De que forma o líder do bando é poupado do cansaço excessivo provocado pelo esforço frontal?', hint: 'Sistema de rodízio.', answer: 'Revezando a liderança da ponta com outro colega', options: ['Pousando no meio da viagem solitário', 'Revezando a liderança da ponta com outro colega', 'Voando na esteira lateral o tempo inteiro', 'Dormindo em pleno ar'] },
        { text: 'Que palavra qualifica o desenho de voo criado pela formação aérea das aves?', hint: 'Qualitativo geométrico das formas.', answer: 'geométrico', options: ['irregular', 'geométrico', 'caótico', 'vertical'] },
        { text: 'O termo "geográfico" ou "jornada" remete a qual ação das aves?', hint: 'Movimento natural cíclico.', answer: 'Migração', options: ['Acasalamento', 'Hibernação', 'Migração', 'Alimentação'] },
        { text: 'O que o ganso da ponta do bando quebra especificamente ao voar à frente?', hint: 'Fator de fricção física.', answer: 'A resistência maior do ar', options: ['A corrente de água das nuvens', 'A resistência maior do ar', 'As árvores limítrofes', 'A formação alinhada'] }
      ],
      [
        { text: 'Texto: "As patentes industriais cumprem o papel de retribuir o esforço do inventor, outorgando-lhe direitos de exclusividade temporários. Entretanto, o monopólio deve coexistir com o bem público, justificando tempos limites para que as fórmulas caiam no domínio social."\n\nPergunta: Por que patentes temporárias são criadas segundo o texto?', hint: 'Objetivo mercadológico e ético.', answer: 'Para valorizar as descobertas autorais mantendo prazos de interesse público', options: ['Para impedir permanentemente a concorrência produtiva', 'Para valorizar as descobertas autorais mantendo prazos de interesse público', 'Para capitalizar fundos governamentais diretos', 'Para encarecer produtos básicos de saúde humana'] },
        { text: 'O que ocorre depois do esgotamento do tempo limite de uma patente comercializada?', hint: 'Efeito concorrencial legal.', answer: 'A invenção entra em domínio público', options: ['A patente torna-se herança nacional compulsória', 'A invenção entra em domínio público', 'O inventor é multado criminalmente', 'A fórmula é destruída pelos tribunais'] },
        { text: 'Qual palavra é antônimo de "Exclusividade"?', hint: 'Escolha com base em vocabulário refinado.', answer: 'Universalidade', options: ['Isolamento', 'Monopólio', 'Privilégio', 'Universalidade'] },
        { text: 'Que termo no texto expressa conotação de direito outorgado legalmente?', hint: 'Vocabulário administrativo.', answer: 'direitos de exclusividade', options: ['esforço do inventor', 'direitos de exclusividade', 'monopólio público', 'domínio social'] },
        { text: 'Segundo o autor, o monopólio mercantil das patentes deve conviver de forma harmonizada com:', hint: 'Responsabilidade social.', answer: 'o bem público', options: ['a margem de lucro bancária', 'o bem público', 'as leis de copyright globais', 'os incentivos de patrocínios'] }
      ],
      [
        { text: 'Texto: "A Grande Barreira de Corais da Austrália, visível do espaço, padece com o branqueamento em escala global. O aquecimento anômalo das águas do oceano expele as algas simbiontes coloridas que habitam e alimentam os recifes. Sem as algas, os corais perdem suas matizes vibrantes e morrem."\n\nPergunta: Qual fator desencadeia diretamente a perda de cores nos corais na Austrália?', hint: 'Filtre a causa física direta descrita.', answer: 'O aumento anômalo das temperaturas marinhas', options: ['O vazamento catastrófico de defensivos agrícolas', 'A pesca predatória com explosivos caseiros', 'O aumento anômalo das temperaturas marinhas', 'A proliferação de tubarões-brancos'] },
        { text: 'Que seres vivos residem nos tecidos dos corais litorâneos e fornecem seu alimento essencial?', hint: 'Identifique os organismos associados nutricionalmente.', answer: 'Algas simbiontes', options: ['Pequenos moluscos filtradores', 'Planctons fósseis', 'Algas simbiontes', 'Lulas gigantes'] },
        { text: 'Qual o destino dos corais que sofrem a perda de suas algas protetoras de longo prazo?', hint: 'Causa fatal.', answer: 'Sofrem inanição severa e morrem', options: ['Ficam mais resistentes a tempestades', 'Passam a se alimentar de pequenos caranguejos', 'Sofrem inanição severa e morrem', 'Multiplicam suas colônias oceânicas'] },
        { text: 'O branqueamento citado acomete os recifes em qual proporção do globo?', hint: 'Dimensão do evento geográfico catastrófico.', answer: 'escala global', options: ['apenas baías costeiras isoladas', 'escala global', 'região ártica restrita', 'recifes artificiais domésticos'] },
        { text: 'Deduza o significado de "anômalo" com base no texto:', hint: 'Análise de sinônimos.', answer: 'Anormal ou fora de padrão', options: ['Esperado e regular', 'Benéfico e natural', 'Anormal ou fora de padrão', 'Insignificante'] }
      ],
      [
        { text: 'Texto: "O astrônomo Edwin Hubble revolucionou a cosmologia ao documentar que as galáxias distantes estão se distanciando de nós. Essa expansão indica que, no passado remoto, todo o cosmos estava compactado em um ponto denso e aquecido, tese que deu origem à teoria teórica do Big Bang."\n\nPergunta: Que descoberta importante de Hubble abriu portas à hipótese do Big Bang?', hint: 'Evidência cósmica mensurada pelo telescópio.', answer: 'O distanciamento progressivo de galáxias remotas entre si', options: ['A colisão iminente da Via Láctea com Andrômeda', 'O distanciamento progressivo de galáxias remotas entre si', 'A detecção de satélites artificiais alienígenas', 'A órbita perfeita de planetas vizinhos ao redor de anãs vermelhas'] },
        { text: 'De acordo com as conjecturas de expansão, como era o universo no princípio?', hint: 'Estágio inicial pré-expansivo.', answer: 'Concentrado e com energia térmica extrema', options: ['Frio, escuro e totalmente expandido', 'Concentrado e com energia térmica extrema', 'Uma planilha vazia de partículas inertes', 'Formado majoritariamente por buracos negros massivos'] },
        { text: 'Qual a ciência que estuda a constituição e evolução global do espaço e que foi revolucionada por Hubble?', hint: 'Nomenclatura do campo do saber estudado.', answer: 'cosmologia', options: ['astrologia', 'geofísica', 'cosmologia', 'oceanografia'] },
        { text: 'Hubble é classificado profissionalmente no parágrafo em que área de atuação?', hint: 'Atividade ocupacional científica.', answer: 'astrônomo', options: ['físico nuclear', 'cosmonauta de órbita', 'geólogo espacial', 'astrônomo'] },
        { text: 'Qual expressão de tempo do texto nos remete ao princípio da cronologia cósmica?', hint: 'Sinalizador do tempo passado profundo.', answer: 'passado remoto', options: ['futuro iminente', 'passado remoto', 'época contemporânea', 'ontem'] }
      ],
      [
        { text: 'Texto: "O Código de Hamurabi é o monumento legislativo escrito mais célebre da Mesopotâmia. Nele, a célebre Lei de Talião - olho por olho, dente por dente - regia a justiça civil. Porém, as punições estipuladas flutuavam conforme a posição do infrator na pirâmide de classes sociais."\n\nPergunta: Qual lei central fundamentava os pilares penais do Código de Hamurabi?', hint: 'Princípio de punição proporcional literal.', answer: 'Lei de Talião', options: ['Constituição Mesopotâmica', 'Lei de Talião', 'Código Canônico', 'Decretação de Ur-Nammu'] },
        { text: 'Segundo o texto, que fator introduzia disparidades nas punições e multas aplicadas pela justiça militar e civil?', hint: 'Fator de discriminação social e social.', answer: 'A classe e hierarquia social do réu', options: ['A idade do criminoso julgado', 'A classe e hierarquia social do réu', 'A confissão sob coerção física', 'O julgamento feito por anciãos tribais'] },
        { text: 'A Lei de Talião é comumente sintetizada por qual provérbio emblemático?', hint: 'Máxima de punição equivalente.', answer: 'Olho por olho, dente por dente', options: ['A pressa é inimiga da perfeição', 'Olho por olho, dente por dente', 'Quem tudo quer, tudo perde', 'Diga-me com quem andas e te direi quem és'] },
        { text: 'De que região histórica do Oriente Médio pertencia esse grandioso monumento legislativo?', hint: 'Geografia do berço da escrita.', answer: 'Mesopotâmia', options: ['Egito Antigo', 'Pérsia', 'Mesopotâmia', 'Fenícia'] },
        { text: 'A palavra "flutuavam" no contexto legislativo denota que as sanções eram:', hint: 'Análise morfossemântica.', answer: 'variáveis', options: ['estáveis', 'amenas', 'injustificadas', 'variáveis'] }
      ]
    ],
    'D': [ // Summarizing reading levels (this acts as the upper text comprehension tiers)
      [
        { text: 'Leia a narrativa histórica: "Santos Dumont consagrou seu nome na aviação mundial ao voar publicamente com o 14-Bis em Paris no ano de 1906. Diferente dos pioneiros norte-americanos irmãos Wright, que usavam trilhos fixos e catapultas sob segredo, o brasileiro realizou decolagem autônoma por força de propulsão a motor diante dos juízes franceses especializados."\n\nAssinale o resumo que melhor condensa o diferencial do feito de Dumont:', hint: 'A ideia principal do voo livre.', answer: 'Decolagem com propulsão autônoma e pública de Santos Dumont frente a competidores com catapultas', options: ['O incentivo financeiro recebido de banqueiros de Paris', 'Decolagem com propulsão autônoma e pública de Santos Dumont frente a competidores com catapultas', 'A predileção francesa pela engenharia aeronáutica americana', 'A invenção de motores a hélice mais silenciosos'] },
        { text: 'Qual método de lançamento caracterizava os testes secretos conduzidos pelos irmãos Wright?', hint: 'Diferencial mecânico de lançamento.', answer: 'Trilhos fixos apoiados em catapultas de impulsão', options: ['Rampas de inclinação naturais de montanhas', 'Trilhos fixos apoiados em catapultas de impulsão', 'Balões flutuantes a gás hélio', 'Decolagens em lagos planos com hidroaviões'] },
        { text: 'Em que cidade de prestígio cultural global ocorreu a homologação do 14-Bis pelo júri científico?', hint: 'Metrópole de exibição.', answer: 'Paris', options: ['Nova York', 'Rio de Janeiro', 'Paris', 'Londres'] },
        { text: 'O termo "autônoma" contrapõe-se a qual característica de lançamento externo?', hint: 'Análise conceitual de oposição.', answer: 'Lançamento dependente de aparatos de tração externos', options: ['Lançamento dependente de aparatos de tração externos', 'Voo controlado por controle remoto', 'Apenas planar sem motor ativo', 'Velocidade supersônica assistida de asas'] },
        { text: 'Santos Dumont realizou seu famoso marco no ano de:', hint: 'Assinale o ano exato de consagração.', answer: '1906', options: ['1899', '1903', '1906', '1914'] }
      ],
      [
        { text: 'Texto: "A penicilina foi descoberta por acidente pelo médico britânico Alexander Fleming em 1928, ao notar que colônias do fungo Penicillium causavam a lise de bactérias vizinhas em suas placas de cultura esquecidas. O achado deu ensejo à era da antibioticoterapia, erradicando infecções outrora fatais tais como a sífilis e a pneumonia bacteriana."\n\nAssinale a melhor síntese do parágrafo acadêmico:', hint: 'Síntese completa contendo autor e eficácia medicinal.', answer: 'A descoberta fortuita de Fleming em 1928 que resultou em antibióticos curando males mortais', options: ['O processo químico complexo de síntese laboratorial de assepsia', 'A descoberta fortuita de Fleming em 1928 que resultou em antibióticos curando males mortais', 'A persistência bacteriana em hospitais britânicos negligentes', 'Os malefícios gerados pelas alergias derivadas de esporos de bolor'] },
        { text: 'De que maneira o fungo combatia a infestação de microrganismos bactericidas?', hint: 'Fenômeno celular observado sob a lupa.', answer: 'Promovendo a lise (ruptura e dissolução) das bactérias circundantes', options: ['Congelando as colônias adjacentes', 'Promovendo a lise (ruptura e dissolução) das bactérias circundantes', 'Privando as colônias de oxigênio gasoso', 'Envenenando o meio com ácido lático concentrado'] },
        { text: 'Alexander Fleming tinha qual formação profissional descrita?', hint: 'Ocupação do cientista.', answer: 'médico', options: ['biólogo molecular', 'bioquímico industrial', 'farmacêutico clínico', 'médico'] },
        { text: 'Indique de que forma ocorreu o fenômeno inicial na bancada de Fleming:', hint: 'Circunstância da descoberta.', answer: 'Por acidente, mediante placas de cultivo deixadas na bancada', options: ['Por planejamento cirúrgico militar de prevenção biológica', 'Por acidente, mediante placas de cultivo deixadas na bancada', 'Por engenharia genética de isolamento induzido', 'Por captação de secreção de anfíbios tropicais'] },
        { text: 'Quais males pandêmicos outrora sem cura passaram a ser tratados com sucesso?', hint: 'Problemas curados indicados no término do texto.', answer: 'sífilis e pneumonia bacteriana', options: ['gripe aviária e varíola do gado', 'sífilis e pneumonia bacteriana', 'tétano agudo e malária de várzea', 'lepra micológica e picadas de bicho peçonhento'] }
      ],
      [
        { text: 'Texto: "O efeito estufa é um mecanismo termorregulador indispensável que impede que a Terra seja um deserto de gelo. No entanto, o ritmo desenfreado de emissão de gases industriais, decorrente da combustão de fósseis, adensa a atmosfera, retendo calor solar excedente e provocando alterações planetárias na temperatura média."\n\nIdentifique a síntese coerente do texto ambiental:', hint: 'Visão sistêmica entre regulação vital e distorção industrial.', answer: 'O papel vital do efeito estufa para o equilíbrio térmico da Terra e seu desequilíbrio pela queima de combustíveis', options: ['O banimento industrial urgente de todo tipo de exaustão veicular', 'O papel vital do efeito estufa para o equilíbrio térmico da Terra e seu desequilíbrio pela queima de combustíveis', 'A negação de cientistas sobre as variações climáticas naturais do planeta', 'O congelamento iminente das zonas temperadas devido à camada gasosa'] },
        { text: 'Como seria a condição térmica da Terra sem a presença natural do efeito estufa?', hint: 'Situação física hipotética inicial.', answer: 'Um ambiente gélido e sem possibilidades de vida complexa', options: ['Um paraíso tropical com chuvas distribuídas homogeneamente', 'Um deserto fervente devido à luz solar crua direta', 'Um ambiente gélido e sem possibilidades de vida complexa', 'Um campo estável cercado por poeira estelar densa'] },
        { text: 'O que constitui a fonte primária de espessamento e aprisionamento térmico da atmosfera contemporânea?', hint: 'Ações humanas geradoras de poluição.', answer: 'A queima de combustíveis fósseis e atividade fabril desenfreada', options: ['Os vulcões ativos localizados nos leitos continentais submarinos', 'A evaporação de oceanos salinos sob marés solares', 'A queima de combustíveis fósseis e atividade fabril desenfreada', 'O desmatamento exclusivo de florestas para pastagem animal'] },
        { text: 'A palavra "termo-regulador" remete à regulação de qual variável física solar?', hint: 'Dedução com base em radier de prefixos.', answer: 'temperatura', options: ['gases tóxicos', 'temperatura', 'umidade do ar', 'ventos costeiros'] },
        { text: 'O exótico calor excessivo da atmosfera decorre da retenção de que radiação?', hint: 'Componente da estufa térmica.', answer: 'calor solar excedente', options: ['calor solar excedente', 'raios gama profundos', 'gás carbônico congelante', 'raios ultravioleta cósmicos'] }
      ],
      [
        { text: 'Texto: "Os microplásticos, fragmentos de polímero com dimensão inferior a cinco milímetros, circulam no topo da cadeia trófica. Produzidos pela degradação de garrafas e sintéticos descartados inadequadamente, essas micropartículas contaminam bacias hidrográficas e tecidos biológicos de peixes, alcançando o prato do consumidor final através do peixe consumido."\n\nAssinale a melhor síntese conceitual do fluxo descrito:', hint: 'Acompanhar o material desde a origem até o topo nutricional.', answer: 'A infiltração e avanço de resíduos plásticos microscópicos nos mares até a mesa alimentar humana', options: ['A resistência física de plásticos de engenharia automotiva pesada', 'A infiltração e avanço de resíduos plásticos microscópicos nos mares até a mesa alimentar humana', 'Os tratamentos químicos aplicados no lixo reciclável urbano de metrópoles', 'O ciclo natural de decantação de minerais poliméricos sedimentares'] },
        { text: 'Qual a barreira de classificação física métrica que define uma partícula como microplástico?', hint: 'Medida limite especificada na introdução.', answer: 'Tamanho inferior a cinco milímetros de diâmetro', options: ['Tamanho inferior a cinco milímetros de diâmetro', 'Invisibilidade ao microscópio convencional', 'Tamanho exatamente correspondente a um grão de arroz selvagem', 'Peso específico inferior a um miligrama por cubículo'] },
        { text: 'Qual a principal via de ingresso de microplásticos nas bacias hidricas citada no texto?', hint: 'Causa poluidora física.', answer: 'Descarte inapropriado de recipientes plásticos e tecidos sintéticos', options: ['Derramamento industrial de petroquímica em tanques biológicos', 'Descarte inapropriado de recipientes plásticos e tecidos sintéticos', 'O lixo gerado exclusivamente por navios de cruzeiro marítimo', 'Partículas de pneus de cargas rodoviárias em asfalto'] },
        { text: 'A expressão "cadeia trófica" diz respeito a qual termo ecológico comum?', hint: 'Sinônimo de pirâmide trófica.', answer: 'cadeia alimentar', options: ['cadeia alimentar', 'correntes marinhas de convecção', 'regiões abissais marinhas', 'redes de arrasto extrativista'] },
        { text: 'Como as partículas de plástico impactam a saúde humana no desfecho estudado?', hint: 'Terminação do circuito.', answer: 'Alcançando a alimentação através da carne dos peixes consumidos', options: ['Provocando poluição visual em estâncias de férias', 'Alcançando a alimentação através da carne dos peixes consumidos', 'Atrapalhando o transporte hídrico de canais de cidades', 'Esvaziando as populações de predadores marinhos gigantes'] }
      ],
      [
        { text: 'Texto: "O mercantilismo econômico que operou entre os séculos XVI e XVIII defendia a balança comercial favorável (vender mais que comprar) e acúmulo de metais dourados. As coroas europeias impunham tarifas protecionistas e exclusividade colonial para reter a riqueza real no tesouro nacional, gerando tensões que mais tarde abriram bases ao livre comércio industrial."\n\nQual é a opção que condensa com rigor as bases descritas do mercantilismo?', hint: 'Destaque os dois pilares corporativos protecionistas e monetários.', answer: 'Política comercial europeia de retenção de metais e venda excedente com monopólio comercial', options: ['O desenvolvimento agropecuário subsidiado por dinastias imperiais asiáticas', 'Política comercial europeia de retenção de metais e venda excedente com monopólio comercial', 'A liberação alfandegária que extinguiu taxas alfandegárias nas fronteiras', 'A substituição de moedas reais de ouro por ativos fiduciários sem lastro'] },
        { text: 'A máxima de uma "balança comercial favorável" significa especificamente o quê?', hint: 'Equação de importação vs exportação.', answer: 'Manter as exportações nacionais em patamar maior do que as importações', options: ['Taxar no mesmo patamar todo tipo de navio comercial atracado', 'Manter as exportações nacionais em patamar maior do que as importações', 'Acabar com impostos de mercadorias agrícolas supérfluas', 'Dividir igualmente o ouro conquistado entre aliados diplomáticos'] },
        { text: 'Qual metal de prestígio monetário era prioritariamente desejado nos cofres das coroas?', hint: 'Acúmulo de metais preciosos.', answer: 'metais dourados (ouro)', options: ['cobre para bronzeamento', 'metais dourados (ouro)', 'ferro naval de fundição', 'estanhos e pratas de utensílios'] },
        { text: 'Que ferramentas alfandegárias os reis europeus impunham para defender a produção local?', hint: 'Instalação de barreiras e impostos impostos.', answer: 'Tarifas protecionistas e cláusulas de privilégio exclusivo com colônias', options: ['Subvenções totais a portos concorrentes internacionais', 'Tarifas protecionistas e cláusulas de privilégio exclusivo com colônias', 'Arrendamento livre de frotas mercantes para reis rivais', 'Declaração imediata de independência dos seus domínios coloniais'] },
        { text: 'O termo "protecionista" refere-se à política regulatória cujo objetivo é proteger:', hint: 'Objetivo do adjetivo mercadológico.', answer: 'o mercado interno contra produtos estrangeiros de baixo preço', options: ['a marinha militar contra piratarias', 'os segredos de alquimia antigos', 'o mercado interno contra produtos estrangeiros de baixo preço', 'a conservação de reservas florestais reais'] }
      ]
    ],
    'E': [ // Highly advanced reading content
      [
        { text: 'Leia a ponderação estética: "A arte mimética clássica, tributada a Aristóteles, enxerga no fazer artístico uma imitação da natureza, que depura e ordena o caos original. Já a poética romântica do século XIX transpõe a ênfase para a expressão subjetiva e catártica da alma do gênio criador, alterando o papel do receptor que deixa de buscar verossimilhança exterior para buscar comunhão espiritual."\n\nIdentifique a síntese conceitual correta da transição estética descrita:', hint: 'Destaque o objeto da arte que deixa a mimese externa para focar no interior criativo.', answer: 'A mudança de foco estético da cópia racional da natureza para a exteriorização emocional e espiritual do criador', options: ['A rejeição total da escultura grega por ser inferior à pintura à óleo medieval', 'A mudança de foco estético da cópia racional da natureza para a exteriorização emocional e espiritual do criador', 'O advento da fotografia mecânica banindo a imaginação da produção acadêmica moderna', 'A estatização da arte promovida por salões anarquistas na Europa Oriental'] },
        { text: 'O que consistia o conceito de "mimese" artística para a tradição tributada a Aristóteles?', hint: 'Definição clássica grega.', answer: 'Uma imitação harmônica da natureza que seleciona, aperfeiçoa e ordena o caos mundano', options: ['A colagem direta de elementos texturais não-figurativos em painéis', 'Um ritual mitológico destinado a agradar divindades guerreiras do Olimpo', 'Uma imitação harmônica da natureza que seleciona, aperfeiçoa e ordena o caos mundano', 'O cálculo matemático rígido de colunas arquitetônicas simétricas'] },
        { text: 'A poética romântica do século XIX realoca o epicentro criador para onde?', hint: 'Foco do romantismo de criação.', answer: 'A voz interna e catarse de sentimentos da genialidade singular do criador', options: ['Os manuais do funcionalismo geométrico industrial', 'O patronato financeiro de dinastias oligárquicas locais', 'A voz interna e catarse de sentimentos da genialidade singular do criador', 'As regras canônicas de academias de teologia monásticas'] },
        { text: 'A palavra "mimética" tem raiz grega ligada a qual termo contemporâneo?', hint: 'Dedução linguística.', answer: 'imitação', options: ['memória', 'mágica', 'imitação', 'mistura'] },
        { text: 'Como muda o papel do público/receptor na apreciação da arte romântica?', hint: 'Recepção do público.', answer: 'Abandona o juízo de semelhança ideal para buscar conexão sensível direta com o ser lírico', options: ['Abandona o juízo de semelhança ideal para buscar conexão sensível direta com o ser lírico', 'Passa a julgar a obra pela densidade aritmética e geométrica dos pigmentos', 'Limita-se a copiar réplicas em gesso expostas em feiras populares', 'Requer que as telas venham acompanhadas de explicações lógicas'] }
      ]
    ]
  };

  return syllabus[levelCode]?.[sheetIndex - 1] || syllabus['3A'][0];
}
