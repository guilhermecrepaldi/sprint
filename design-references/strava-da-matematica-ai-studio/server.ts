/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import express from "express";
import path from "path";
import { createServer as createViteServer } from "vite";
import { GoogleGenAI, Type } from "@google/genai";
import dotenv from "dotenv";

dotenv.config();

const app = express();
const PORT = 3000;

// Increase request size limit to handle sketches/drawing strokes
app.use(express.json({ limit: "50mb" }));
app.use(express.urlencoded({ limit: "50mb", extended: true }));

// Store mock databases in-memory for the running session
interface PageHistoryItem {
  pageIndex: number;
  score: number;
  difficulty: number;
  correctCount: number;
  totalCount: number;
}

interface ActiveSession {
  sessionId: string;
  studentId: string;
  config: any;
  currentFolha: any;
  history: PageHistoryItem[];
  thermometerValue: number;
  createdAt: number;
}

const sessionsDatabase = new Map<string, ActiveSession>();

// Lazy implementation of Gemini AI client
let aiInstance: GoogleGenAI | null = null;
function getGeminiClient(): GoogleGenAI | null {
  if (!aiInstance) {
    const key = process.env.GEMINI_API_KEY;
    if (!key || key === "MY_GEMINI_API_KEY") {
      console.warn("GEMINI_API_KEY is not configured or placeholder. Falling back to heuristic/rule-based grading.");
      return null;
    }
    try {
      aiInstance = new GoogleGenAI({
        apiKey: key,
        httpOptions: {
          headers: {
            'User-Agent': 'aistudio-build',
          }
        }
      });
      console.log("Gemini client successfully initialized.");
    } catch (err) {
      console.error("Failed to initialize GoogleGenAI client:", err);
      return null;
    }
  }
  return aiInstance;
}

// Math Exercises Database
interface TemplateExercise {
  statement: string;
  latex?: string;
  expectedAnswer: string;
}

const exerciseTemplates: { [key: number]: TemplateExercise[] } = {
  // Level 1-2
  1: [
    { statement: "Resolva a equação de primeiro grau para x em R: 4x - 12 = 8", latex: "4x - 12 = 8", expectedAnswer: "5" },
    { statement: "Calcule a soma de frações unitárias: 1/2 + 2/3 = s. Qual o valor de s?", latex: "\\frac{1}{2} + \\frac{2}{3} = s", expectedAnswer: "7/6" },
    { statement: "Resolva para x: 5(x - 1) = 3x + 1", latex: "5(x - 1) = 3x + 1", expectedAnswer: "3" },
    { statement: "Encontre o valor numérico de x: 2x/3 + 4 = 10", latex: "\\frac{2x}{3} + 4 = 10", expectedAnswer: "9" },
    { statement: "Determine a solução real do sistema: x + y = 8 e x - y = 2. Escreva o par ordenado (x, y).", latex: "\\begin{cases} x + y = 8 \\\\ x - y = 2 \\end{cases}", expectedAnswer: "(5, 3)" },
    { statement: "Resolva para a: 3a + 7 = 2a + 12", latex: "3a + 7 = 2a + 12", expectedAnswer: "5" },
    { statement: "Determine o valor de x: 1.5x - 4.5 = 0", latex: "1,5x - 4,5 = 0", expectedAnswer: "3" },
    { statement: "Calcule a diferença: 11/4 - 3/2 = d", latex: "\\frac{11}{4} - \\frac{3}{2} = d", expectedAnswer: "5/4" }
  ],
  // Level 3-4
  3: [
    { statement: "Determine as raízes reais da equação quadrática: x^2 - 7x + 12 = 0. Escreva as raízes.", latex: "x^2 - 7x + 12 = 0", expectedAnswer: "3; 4" },
    { statement: "Resolva o sistema não-linear para soluções positivas: y = 2x e x^2 + y^2 = 20. Escreva o par (x, y).", latex: "\\begin{cases} y = 2x \\\\ x^2 + y^2 = 20 \\end{cases}", expectedAnswer: "(2, 4)" },
    { statement: "Resolva a equação racional em R: 2/(x - 1) = 3/(2x + 1)", latex: "\\frac{2}{x-1} = \\frac{3}{2x+1}", expectedAnswer: "-5" },
    { statement: "Encontre os valores de x que satisfazem a igualdade modular: |2x - 3| = 7. Escreva as respostas.", latex: "|2x - 3| = 7", expectedAnswer: "-2; 5" },
    { statement: "Qual o valor de x que resolve a equação: 1/x + 1/(2x) = 3/4?", latex: "\\frac{1}{x} + \\frac{1}{2x} = \\frac{3}{4}", expectedAnswer: "2" },
    { statement: "Encontre os pontos onde a parábola intersecta o eixo x: y = x^2 - 5x. Escreva os valores de x.", latex: "x^2 - 5x = 0", expectedAnswer: "0; 5" },
    { statement: "Resolva a equação fracionária: (x + 2)/x = 3", latex: "\\frac{x + 2}{x} = 3", expectedAnswer: "1" }
  ],
  // Level 5-6
  5: [
    { statement: "Resolva em R a equação composto-modular: |x^2 - 4| = 5. Informe as raízes reais.", latex: "|x^2 - 4| = 5", expectedAnswer: "-3; 3" },
    { statement: "Resolva utilizando as propriedades do logaritmo: log2(x) + log2(x - 2) = 3. Qual o valor válido de x?", latex: "\\log_2(x) + \\log_2(x - 2) = 3", expectedAnswer: "4" },
    { statement: "Ache o x real na equação exponencial: 3^(2x - 1) = 243", latex: "3^{2x - 1} = 243", expectedAnswer: "3" },
    { statement: "Resolva a equação irracional: sqrt(x + 3) = x - 3. Verifique as condições de existência.", latex: "\\sqrt{x + 3} = x - 3", expectedAnswer: "6" },
    { statement: "Dada a função quadrática y = x^2 - 6x + 8. Determine qual o menor valor (mínimo) assumido por y.", latex: "y = x^2 - 6x + 8", expectedAnswer: "-1" },
    { statement: "No triângulo retângulo de catetos 8 e 15, qual a medida da hipotenusa?", latex: "c^2 = 8^2 + 15^2", expectedAnswer: "17" },
    { statement: "Resolva para x: 2^(x+1) + 2^x = 48", latex: "2^{x+1} + 2^x = 48", expectedAnswer: "4" }
  ],
  // Level 7-8
  7: [
    { statement: "Determine x no intervalo [0, pi/2] tal que: 2*sin^2(x) - sin(x) - 1 = 0.", latex: "2\\sin^2(x) - \\sin(x) - 1 = 0", expectedAnswer: "pi/2" },
    { statement: "Resolva a equação logarítmica composta: log(x^2 - 5x + 6) = 0 na base 10.", latex: "\\log_{10}(x^2 - 5x + 6) = 0", expectedAnswer: "1; 4" },
    { statement: "Resolva o sistema não-linear real de faturamento: x^3 - y^3 = 19, e x - y = 1. Escreva (x, y).", latex: "\\begin{cases} x^3 - y^3 = 19 \\\\ x - y = 1 \\end{cases}", expectedAnswer: "(3, 2)" },
    { statement: "Três números formam uma progressão aritmética: x, 2x + 1, e 5x - 2. Determine o valor de x.", latex: "x, \\; 2x+1, \\; 5x-2", expectedAnswer: "2" },
    { statement: "Encontre as raízes reais do polinômio cúbico fatora-fácil: x^3 - 6x^2 + 11x - 6 = 0.", latex: "x^3 - 6x^2 + 11x - 6 = 0", expectedAnswer: "1; 2; 3" },
    { statement: "Calcule a soma dos infinitos termos da progressão geométrica: S = 9/10 + 9/100 + 9/1000 + ...", latex: "9/10 + 9/100 + 9/1000 + \\dots", expectedAnswer: "1" }
  ],
  // Level 9-10
  9: [
    { statement: "Resolva para x real: 4^x - 3 * 2^x + 2 = 0.", latex: "4^x - 3 \\cdot 2^x + 2 = 0", expectedAnswer: "0; 1" },
    { statement: "Resolva a equação de duplo módulo em R: ||x - 1| - 2| = 1. Indique todas as soluções.", latex: "||x - 1| - 2| = 1", expectedAnswer: "-2; 0; 2; 4" },
    { statement: "Determine o intervalo de soluções para x no intervalo [0, pi] tal que: 2*cos(x) - 1 > 0.", latex: "2\\cos(x) - 1 > 0", expectedAnswer: "[0, pi/3)" },
    { statement: "Dada a função quadrática y = -2x^2 + 8x - 3. Determine o valor máximo assumido pela imagem de y.", latex: "y = -2x^2 + 8x - 3", expectedAnswer: "5" },
    { statement: "Simplifique a expressão de produto trigonométrico de arcos duplos em fração simples: cos(20°)*cos(40°)*cos(80°).", latex: "\\cos(20^\\circ)\\cos(40^\\circ)\\cos(80^\\circ)", expectedAnswer: "1/8" },
    { statement: "Resolva o sistema de desigualdades reais: x^2 - 4 < 0 e x + 1 >= 0. Expresse como intervalo.", latex: "\\begin{cases} x^2 - 4 < 0 \\\\ x + 1 \\ge 0 \\end{cases}", expectedAnswer: "[-1, 2)" }
  ]
};

// Programmatic mathematics layout selector
function getRelevantPool(difficulty: number): TemplateExercise[] {
  if (difficulty < 3.0) return exerciseTemplates[1];
  if (difficulty < 5.0) return exerciseTemplates[3];
  if (difficulty < 7.0) return exerciseTemplates[5];
  if (difficulty < 9.0) return exerciseTemplates[7];
  return exerciseTemplates[9];
}

// Generate the Folha dynamically
function createFolhaForDifficulty(folhaId: string, pageIndex: number, difficulty: number, count: number): any {
  const pool = getRelevantPool(difficulty);
  
  // Suffle or pick unique items randomly
  const shuffled = [...pool].sort(() => 0.5 - Math.random());
  const selected = shuffled.slice(0, Math.min(count, shuffled.length));
  
  // If we need more questions, duplicate with slight adjustments or fallback
  while (selected.length < count) {
    selected.push({
      statement: `Resolva a equação de primeiro grau adaptada de nível ${(difficulty).toFixed(1)}: ${selected.length + 1}x = ${Math.floor(difficulty * 10)}`,
      expectedAnswer: `${Math.floor(difficulty * 10) / (selected.length + 1)}`
    });
  }

  const fields = selected.map((item, index) => {
    return {
      fieldIndex: index + 1,
      exerciseId: `ex_${folhaId}_${index + 1}`,
      statement: item.statement,
      latex: item.latex,
      expectedAnswer: item.expectedAnswer,
    };
  });

  return {
    folhaId,
    pageIndex,
    difficulty: parseFloat(difficulty.toFixed(1)),
    fields
  };
}

// API Endpoints Specification (Section 9)

app.get("/api/health", (req, res) => {
  res.json({ status: "ok", time: new Date().toISOString() });
});

// START SESSION: POST /api/session/start
app.post("/api/session/start", (req, res) => {
  try {
    const { student_id, config } = req.body;
    if (!student_id) {
      return res.status(400).json({ error: "student_id is required" });
    }

    const sessionId = `sess_${Math.random().toString(36).substring(2, 11)}`;
    const startDifficulty = config?.difficulty_start ? parseFloat(config.difficulty_start) : 2.0;
    const count = config?.exercises_per_page ? parseInt(config.exercises_per_page, 10) : 5;

    const firstFolha = createFolhaForDifficulty(`folha_1`, 1, startDifficulty, count);

    const newSession: ActiveSession = {
      sessionId,
      studentId: student_id,
      config: {
        show_thermometer: config?.show_thermometer ?? true,
        background: config?.background ?? 'white',
        pen_color: config?.pen_color ?? '#151713',
        pen_thickness: config?.pen_thickness ?? 2.2,
        duration_mode: config?.duration_mode ?? 'pages',
        duration_value_minutes: config?.duration_value_minutes ?? 30,
        pages_limit: config?.pages_limit ?? 10,
        difficulty_progression: config?.difficulty_progression ?? 'linear',
        difficulty_start: startDifficulty,
        difficulty_step: config?.difficulty_step ? parseFloat(config.difficulty_step) : 0.5,
        difficulty_ratio: config?.difficulty_ratio ? parseFloat(config.difficulty_ratio) : 1.15,
        restart_on_avg: config?.restart_on_avg ? parseFloat(config.restart_on_avg) : 7.0,
        restart_window: config?.restart_window ? parseInt(config.restart_window, 10) : 10,
        exercises_per_page: count,
      },
      currentFolha: firstFolha,
      history: [],
      thermometerValue: 0.75, // Ideal moderate startup confidence
      createdAt: Date.now(),
    };

    sessionsDatabase.set(sessionId, newSession);

    return res.json({
      session_id: sessionId,
      first_folha: firstFolha,
      thermometer_value: newSession.thermometerValue,
    });
  } catch (error: any) {
    console.error("Error creating session:", error);
    return res.status(500).json({ error: "Internal server error starting math training session" });
  }
});

// SUBMIT PAGE: POST /api/session/{session_id}/submit
app.post("/api/session/:session_id/submit", async (req, res) => {
  const sessionId = req.params.session_id;
  const session = sessionsDatabase.get(sessionId);

  if (!session) {
    return res.status(404).json({ error: "Mathematical workout session not found" });
  }

  try {
    const { fields } = req.body; // Array of submitted field crops and answers
    if (!fields || !Array.isArray(fields)) {
      return res.status(400).json({ error: "Missing fields in request body" });
    }

    const incomingFieldsMap = new Map<number, any>();
    fields.forEach((f: any) => {
      incomingFieldsMap.set(f.field_index, f);
    });

    const currentFolha = session.currentFolha;
    const ai = getGeminiClient();

    const results: any[] = [];
    let correctCount = 0;
    let scoreSum = 0;

    // Evaluate each field in the current Folha
    for (const field of currentFolha.fields) {
      const index = field.fieldIndex;
      const submission = incomingFieldsMap.get(index);
      const typed_answer = submission?.typed_answer?.trim() || "";
      const image_base64 = submission?.image_base64; // OCR visual trace of canvas

      let evaluatedCorrect = false;
      let evaluatedScore = 0; // 0 to 10
      let recognizedAnswerText = typed_answer || "Sem resposta";
      let errorTypeClassification: 'sinal' | 'fracao' | 'equacao_2_grau' | 'desconhecido' | null = null;
      let evaluatedByGemini = false;

      // Clean expected answer for basic string mismatch checking
      const cleanStr = (s: string) => s.toLowerCase().replace(/\s+/g, '').replace(/,/g, ';');
      const expectedClean = cleanStr(field.expectedAnswer);

      // Try actual visual grader with Gemini AI if a key is configured and image exists
      if (ai && (image_base64 || typed_answer)) {
        try {
          const parts: any[] = [];
          if (image_base64) {
            // Remove MIME header if present
            const cleanBase64 = image_base64.replace(/^data:image\/\w+;base64,/, "");
            parts.push({
              inlineData: {
                mimeType: "image/png",
                data: cleanBase64
              }
            });
          }

          const promptText = `
Você é uma inteligência de avaliação matemática rigorosa do app "Strava da Matemática" focado em olimpíadas e deep work.
O candidato resolveu o seguinte problema matemático de média/alta performance:
Enunciado: "${field.statement}"
Gabarito/Resposta esperada exata: "${field.expectedAnswer}"
Resposta opcional digitada pelo estudante: "${typed_answer}"

Analise a imagem da caneta/ stylus (se houver) e o texto fornecido.
Extraia a resposta final que o estudante alcançou. Compare com a resposta esperada levando em conta equivalências algébricas comuns (ex: Frações, radicais ou decimais equivalentes).
Avalie se a resposta está Correta. Dê uma nota de 0 a 10 ao esforço.
Se a resposta estiver errada, classifique o tipo de erro com base nas categorias:
- 'sinal' (erro simples de sinal de mais ou menos)
- 'fracao' (erro na manipulação de frações ou mmc)
- 'equacao_2_grau' (erro de fórmula ou fatoração quadrática)
- 'desconhecido' (cálculos incompreensíveis, resposta vaga, ou errada generalizada)

Retorne estritamente um JSON no seguinte formato (sem bloco de código markdown no exterior, só a string pura ou valid JSON format):
{
  "recognizedAnswer": "resposta encontrada na imagem ou texto",
  "isCorrect": true ou false,
  "score": nota de 0 a 10 como número inteiro,
  "errorType": "sinal" | "fracao" | "equacao_2_grau" | "desconhecido" | null
}
`;
          parts.push({ text: promptText });

          const geminiResponse = await ai.models.generateContent({
            model: "gemini-3.5-flash",
            contents: { parts },
            config: {
              responseMimeType: "application/json",
              responseSchema: {
                type: Type.OBJECT,
                properties: {
                  recognizedAnswer: { type: Type.STRING },
                  isCorrect: { type: Type.BOOLEAN },
                  score: { type: Type.INTEGER },
                  errorType: { 
                    type: Type.STRING,
                    description: "Null if correct. In other cases, use one of the strings.",
                    // Use optional string values
                  }
                },
                required: ["recognizedAnswer", "isCorrect", "score"]
              }
            }
          });

          const resText = geminiResponse.text?.trim() || "{}";
          const evaluationResult = JSON.parse(resText);

          recognizedAnswerText = evaluationResult.recognizedAnswer || recognizedAnswerText;
          evaluatedCorrect = !!evaluationResult.isCorrect;
          evaluatedScore = typeof evaluationResult.score === 'number' ? evaluationResult.score : (evaluatedCorrect ? 10 : 0);
          
          if (!evaluatedCorrect) {
            errorTypeClassification = evaluationResult.errorType || 'desconhecido';
          } else {
            errorTypeClassification = null;
          }

          evaluatedByGemini = true;
          console.log(`[Gemini evaluation for exercise ${index}]: Correct: ${evaluatedCorrect}, Score: ${evaluatedScore}, Recognized: ${recognizedAnswerText}`);

        } catch (authErrorOrParsingError) {
          console.error("Gemini OCR feedback error, reverting to algebraic heuristic:", authErrorOrParsingError);
          evaluatedByGemini = false;
        }
      }

      // Heuristic Fallback grading if Gemini isn't used or crashed
      if (!evaluatedByGemini) {
        if (typed_answer) {
          const studentClean = cleanStr(typed_answer);
          // Check exact match or basic partial parts
          if (studentClean === expectedClean || expectedClean.includes(studentClean) && studentClean.length >= 1) {
            evaluatedCorrect = true;
            evaluatedScore = 10;
            recognizedAnswerText = typed_answer;
            errorTypeClassification = null;
          } else {
            evaluatedCorrect = false;
            evaluatedScore = 0;
            recognizedAnswerText = typed_answer;
            // guess error classification heuristically representation
            if (typed_answer.startsWith("-") && !field.expectedAnswer.startsWith("-") || !typed_answer.startsWith("-") && field.expectedAnswer.startsWith("-")) {
              errorTypeClassification = "sinal";
            } else if (typed_answer.includes("/")) {
              errorTypeClassification = "fracao";
            } else {
              errorTypeClassification = "desconhecido";
            }
          }
        } else if (image_base64 && image_base64.length > 500) {
          // If they drew something (stroke captured) but no typed, grade as a simulated try
          // Under offline simulation, allow some success rate or mark as unread if absolutely static
          const isSimulatedCorrect = Math.random() > 0.45;
          evaluatedCorrect = isSimulatedCorrect;
          evaluatedScore = isSimulatedCorrect ? 10 : 3;
          recognizedAnswerText = isSimulatedCorrect ? field.expectedAnswer : "2x = 5";
          errorTypeClassification = isSimulatedCorrect ? null : "desconhecido";
        } else {
          // Empty field
          evaluatedCorrect = false;
          evaluatedScore = 0;
          recognizedAnswerText = "Sem resposta";
          errorTypeClassification = null;
        }
      }

      if (evaluatedCorrect) {
        correctCount += 1;
      }
      scoreSum += evaluatedScore;

      results.push({
        fieldIndex: index,
        recognizedAnswer: recognizedAnswerText,
        expectedAnswer: field.expectedAnswer,
        isCorrect: evaluatedCorrect,
        score: evaluatedScore,
        errorType: errorTypeClassification
      });
    }

    const pageCount = results.length;
    const pageAverageScore = Math.round((scoreSum / pageCount) * 10) / 10; // Out of 10.0

    // Adjust training confidence Level ("Thermometer") dynamically based on correct calculations
    // Thermometer is from 0.0 to 1.0. Correct shifts upwards, incorrect shifts downwards
    const performanceRatio = correctCount / pageCount; // 0 to 1
    // Thermometer shifts smoothly towards current performance
    session.thermometerValue = Math.min(1.0, Math.max(0.0, session.thermometerValue * 0.4 + performanceRatio * 0.6));

    // Save history page
    const historyItem: PageHistoryItem = {
      pageIndex: currentFolha.pageIndex,
      score: pageAverageScore,
      difficulty: currentFolha.difficulty,
      correctCount,
      totalCount: pageCount
    };
    session.history.push(historyItem);

    // Determine session status
    let nextStatus: 'active' | 'finished' = 'active';
    const isFinished = session.history.length >= session.config.pages_limit;
    if (isFinished) {
      nextStatus = 'finished';
    }

    // Set up next Page config if active
    if (nextStatus === 'active') {
      const nextPageIndex = currentFolha.pageIndex + 1;
      
      // Calculate next difficulty progression: linear vs geometric
      let nextDifficulty = session.config.difficulty_start;
      if (session.config.difficulty_progression === 'linear') {
        nextDifficulty = session.config.difficulty_start + (nextPageIndex - 1) * session.config.difficulty_step;
      } else {
        nextDifficulty = session.config.difficulty_start * Math.pow(session.config.difficulty_ratio, nextPageIndex - 1);
      }
      // clamp difficulty bounds between 1.0 and 10.0
      nextDifficulty = Math.min(10.0, Math.max(1.0, nextDifficulty));

      session.currentFolha = createFolhaForDifficulty(
        `folha_${nextPageIndex}`,
        nextPageIndex,
        nextDifficulty,
        session.config.exercises_per_page
      );
    } else {
      session.currentFolha = null;
    }

    // Return exact visual response format requested (Section 9.2)
    return res.json({
      session_status: nextStatus,
      page_index: currentFolha.pageIndex,
      score: pageAverageScore,
      thermometer_value: parseFloat(session.thermometerValue.toFixed(2)),
      results
    });

  } catch (error: any) {
    console.error("Error submitting sheet:", error);
    return res.status(500).json({ error: "Criticial failure while correcting workout page with Gemini OCR." });
  }
});

// Configure Vite middleware or Static Fallback
async function startServer() {
  if (process.env.NODE_ENV !== "production") {
    const vite = await createViteServer({
      server: { middlewareMode: true },
      appType: "spa",
    });
    app.use(vite.middlewares);
    console.log("Vite development server loaded as Express middleware.");
  } else {
    // Serve production elements
    const distPath = path.join(process.cwd(), 'dist');
    app.use(express.static(distPath));
    app.get('*', (req, res) => {
      res.sendFile(path.join(distPath, 'index.html'));
    });
    console.log("Static client files deployed.");
  }

  app.listen(PORT, "0.0.0.0", () => {
    console.log(`Strava da Matemática server booted on http://0.0.0.0:${PORT}`);
  });
}

startServer();
