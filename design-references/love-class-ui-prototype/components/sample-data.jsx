// Shared sample data: exercises, results, session state.
// These power the static screens AND the live prototype.

window.SAMPLE_EXERCISES = [
  { id: 'ex1', index: 1, statement: '3x + 7 = 22', label: 'Resolva', expected: 'x = 5', skill: 'Equação 1º grau' },
  { id: 'ex2', index: 2, statement: '(2 + 3)² − 5', label: 'Calcule', expected: '20', skill: 'Potência' },
  { id: 'ex3', index: 3, statement: '1/4 + 2/3', label: 'Some as frações', expected: '11/12', skill: 'Fração' },
  { id: 'ex4', index: 4, statement: 'x² − 9', label: 'Fatore', expected: '(x − 3)(x + 3)', skill: 'Fatoração' },
  { id: 'ex5', index: 5, statement: '2x − 5 = 3x + 1', label: 'Resolva', expected: 'x = −6', skill: 'Equação 1º grau' },
];

window.SAMPLE_RESULTS = [
  { fieldIndex: 1, recognized: 'x = 5', expected: 'x = 5', isCorrect: true, score: 1.0, errorType: null, timeMs: 24300 },
  { fieldIndex: 2, recognized: '20', expected: '20', isCorrect: true, score: 1.0, errorType: null, timeMs: 18700 },
  { fieldIndex: 3, recognized: '11/12', expected: '11/12', isCorrect: true, score: 1.0, errorType: null, timeMs: 42100 },
  { fieldIndex: 4, recognized: '(x + 3)²', expected: '(x − 3)(x + 3)', isCorrect: false, score: 0.0, errorType: 'fatoracao', timeMs: 51800 },
  { fieldIndex: 5, recognized: null, expected: 'x = −6', isCorrect: false, score: 0.0, errorType: 'desconhecido', timeMs: 39600, ocrFailed: true },
];

// Pre-rendered demo ink strokes (in % coordinates) for the static design canvas.
// Stroke point arrays approximate handwriting "x = 5" etc.
function mkStroke(color, width, ...pts) {
  return { color, width, points: pts.map(([x, y]) => ({ x, y })) };
}

// Each field has its own ink seed for visual variety.
window.DEMO_INK = {
  1: [
    // "x = 5"
    mkStroke('#1a1a1a', 2.2, [12, 40], [16, 60], [20, 40]),
    mkStroke('#1a1a1a', 2.2, [12, 60], [22, 40]),
    mkStroke('#1a1a1a', 2.2, [30, 48], [42, 48]),
    mkStroke('#1a1a1a', 2.2, [30, 56], [42, 56]),
    mkStroke('#1a1a1a', 2.2, [52, 38], [60, 38], [60, 50], [54, 56], [60, 60], [56, 68], [52, 64]),
  ],
  2: [
    // Quick calc scribble
    mkStroke('#1a1a1a', 2.2, [10, 30], [22, 30]),
    mkStroke('#1a1a1a', 2.2, [16, 24], [16, 36]),
    mkStroke('#1a1a1a', 2.2, [30, 30], [42, 30]),
    mkStroke('#1a1a1a', 2.2, [12, 60], [22, 50], [20, 70]),
    mkStroke('#1a1a1a', 2.2, [30, 65], [42, 65]),
  ],
  3: [
    mkStroke('#1a1a1a', 2.2, [12, 30], [12, 70]),
    mkStroke('#1a1a1a', 2.2, [10, 30], [14, 30]),
    mkStroke('#1a1a1a', 2.2, [10, 70], [14, 70]),
  ],
  4: [
    // wrong answer scribble: "(x+3)²"
    mkStroke('#1a1a1a', 2.2, [10, 30], [8, 50], [10, 70]),
    mkStroke('#1a1a1a', 2.2, [14, 40], [22, 60]),
    mkStroke('#1a1a1a', 2.2, [14, 60], [22, 40]),
    mkStroke('#1a1a1a', 2.2, [26, 50], [34, 50]),
    mkStroke('#1a1a1a', 2.2, [30, 46], [30, 54]),
    mkStroke('#1a1a1a', 2.2, [38, 40], [44, 40], [44, 50], [38, 50], [44, 60], [38, 60]),
    mkStroke('#1a1a1a', 2.2, [48, 30], [56, 30], [48, 70], [56, 70]),
  ],
  5: [],
};
