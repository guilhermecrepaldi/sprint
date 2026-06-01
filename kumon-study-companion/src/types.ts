/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

export interface Student {
  id: string;
  name: string;
  age: number;
  selectedSubject: 'math' | 'reading';
  currentMathLevel: string;
  currentReadingLevel: string;
  mathProgress: number; // 0 to 100% of current worksheets completed
  readingProgress: number; // 0 to 100%
  streak: number;
  lastActiveDate?: string; // YYYY-MM-DD
  history: HistorySession[];
  registeredDate: string;
}

export interface HistorySession {
  id: string;
  date: string; // YYYY-MM-DD
  subject: 'math' | 'reading';
  level: string;
  durationSeconds: number;
  sctSeconds: number; // Standard Completion Time
  wasSctAchieved: boolean; // true if durationSeconds <= sctSeconds
  totalQuestions: number;
  correctCount: number; // Final correct count (must be 100% after errors corrected)
  initialCorrectCount: number; // First attempt correct count (for accuracy rate tracking)
  attemptsToPerfect: number; // How many tries/corrections to get 100%
  pointsEarned: number;
  sheetIndex: number; // 1-10 sheets per level
}

export interface Question {
  id: string;
  questionText: string;
  secondaryText?: string; // e.g., equations solved hint or translation helper
  options?: string[]; // for reading multiple choice
  correctAnswer: string;
  studentAnswer?: string;
  isCorrect?: boolean; // evaluation state (graded or not)
  isMarkedForCorrection?: boolean; // Kumon model: wrong items flagged, student must change them
}

export interface Worksheet {
  id: string;
  subject: 'math' | 'reading';
  level: string;
  sheetIndex: number; // E.g., sheet 1 of 10 for level B
  title: string;
  questions: Question[];
  standardCompletionTimeSeconds: number; // SCT for this worksheet
  timerRunning: boolean;
  timeSpentSeconds: number;
  isGraded: boolean; // Whether the teacher (system) graded it and marked wrong items
  isPerfect: boolean; // Checked 100% correct, ready to finish
}

export interface LevelConfig {
  code: string;
  name: string;
  description: string;
  focus: string;
  sctSeconds: number; // Standard Completion Time for typical worksheet of 10-20 questions
  totalSheets: number; // How many unique sheet indices exist in this level
}

export const MATH_LEVELS: LevelConfig[] = [
  {
    code: '3A',
    name: 'Nível 3A (Adição Inicial)',
    description: 'Desenvolvimento das habilidades fundamentais de contagem e somas com +1, +2 e +3.',
    focus: 'Adições mentais simples com números de 1 a 10.',
    sctSeconds: 120, // 2 minutes
    totalSheets: 5
  },
  {
    code: '2A',
    name: 'Nível 2A (Adição de 1 a 10)',
    description: 'Dominando somas maiores até +10 de forma rápida e instintiva sem contar nos dedos.',
    focus: 'Somas mentais rápidas e fluidez de raciocínio lógico.',
    sctSeconds: 150, // 2.5 minutes
    totalSheets: 5
  },
  {
    code: 'A',
    name: 'Nível A (Adição & Subtração simples)',
    description: 'Introdução formal a contas de somas maiores e subtração básica de uma etapa.',
    focus: 'Subtrações exatas até 20 e somas horizontais de dois dígitos.',
    sctSeconds: 180, // 3 minutes
    totalSheets: 5
  },
  {
    code: 'B',
    name: 'Nível B (Subtração e Somas Complexas)',
    description: 'Cálculos avançados de adição e subtração por escrito com transporte e empréstimos.',
    focus: 'Subtrações com dois dígitos carregados e contas estruturadas.',
    sctSeconds: 240, // 4 minutes
    totalSheets: 5
  },
  {
    code: 'C',
    name: 'Nível C (Multiplicação e Divisão)',
    description: 'Aprendizado completo da tabuada e divisão básica de cabeça de números simples.',
    focus: 'Multiplicação rápida de 1 dígito e divisões curtas sem resto.',
    sctSeconds: 240, // 4 minutes
    totalSheets: 5
  },
  {
    code: 'D',
    name: 'Nível D (Divisão & Frações Iniciais)',
    description: 'Divisão avançada com restos e redução/introdução teórica às frações.',
    focus: 'Divisões por dois dígitos por estimativa de quociente e frações.',
    sctSeconds: 300, // 5 minutes
    totalSheets: 5
  },
  {
    code: 'E',
    name: 'Nível E (Operações com Frações)',
    description: 'Operações matemáticas complexas de soma e subtração de frações de denominadores opostos.',
    focus: 'Mínimo Múltiplo Comum (MMC), simplificação de frações impróprias.',
    sctSeconds: 360, // 6 minutes
    totalSheets: 5
  },
  {
    code: 'F',
    name: 'Nível F (Fração Avançada & Decimais)',
    description: 'Multiplicações, divisões e combinação de quatro operações com frações e decimais.',
    focus: 'Expressões aritméticas longas contendo frações e decimais.',
    sctSeconds: 420, // 7 minutes
    totalSheets: 5
  },
  {
    code: 'G',
    name: 'Nível G (Introdução à Álgebra)',
    description: 'Fundamentos de números negativos, equações de primeiro grau e simplificação literal.',
    focus: 'Resolver variáveis ocultas (X) sob múltiplas etapas algébricas.',
    sctSeconds: 480, // 8 minutes
    totalSheets: 5
  }
];

export const READING_LEVELS: LevelConfig[] = [
  {
    code: '3A',
    name: 'Nível 3A (Palavras & Imagens)',
    description: 'Conexão de termos fundamentais, associação visual de objetos e correspondência fonética.',
    focus: 'Reconhecimento de substantivos, verbos simples e ortografia elementar.',
    sctSeconds: 120, // 2 mins
    totalSheets: 5
  },
  {
    code: '2A',
    name: 'Nível 2A (Frases Curtas)',
    description: 'Construção mental de frases com sujeito e predicado, ampliando a fluência de leitura eletrônica.',
    focus: 'Unir artigos, adjetivos e ler de ponta a ponta com pontuação ideal.',
    sctSeconds: 150, // 2.5 mins
    totalSheets: 5
  },
  {
    code: 'A',
    name: 'Nível A (Gramática & Sintaxe)',
    description: 'Interpretação e remontagem estrutural de frases desorganizadas. Noção de tempo verbal.',
    focus: 'Remontar palavras soltas em orações corretas e conjugação básica.',
    sctSeconds: 180, // 3 mins
    totalSheets: 5
  },
  {
    code: 'B',
    name: 'Nível B (Textos Curtos)',
    description: 'Introdução à leitura ativa de parágrafos conectados e dedução lógica de contexto.',
    focus: 'Identificar personagens, locais de ações e sequências temporais.',
    sctSeconds: 210, // 3.5 mins
    totalSheets: 5
  },
  {
    code: 'C',
    name: 'Nível C (Interpretação & Vocabulário)',
    description: 'Interpretação de crônicas de média duração e fixação de sinônimos/antônimos profundos.',
    focus: 'Responder questionamentos diretos e deduzir palavras desconhecidas.',
    sctSeconds: 240, // 4 mins
    totalSheets: 5
  },
  {
    code: 'D',
    name: 'Nível D (Resumo de Parágrafos)',
    description: 'Extração da ideia central de múltiplos parágrafos estruturados e redação concisa.',
    focus: 'Suscitar pontos fortes do texto e ligar causa e efeito.',
    sctSeconds: 300, // 5 mins
    totalSheets: 5
  },
  {
    code: 'E',
    name: 'Nível E (Estilo Literário & Crítica)',
    description: 'Passagens opinativas, discernimento entre fatos e opiniões, refinamento gramatical.',
    focus: 'Avaliar o ponto de vista do autor e reescrever sentenças passivas de forma ativa.',
    sctSeconds: 360, // 6 mins
    totalSheets: 5
  }
];
