/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import React, { useState, useEffect, useRef } from 'react';
import { 
  Play, Pause, RefreshCw, Send, CheckCircle2, AlertCircle, 
  Printer, ArrowRight, HelpCircle, Sparkles, BookOpen, Clock, RotateCcw
} from 'lucide-react';
import { motion, AnimatePresence } from 'motion/react';
import { Student, Worksheet, Question, LevelConfig, MATH_LEVELS, READING_LEVELS } from '../types';
import { generateWorksheet } from '../utils/generator';

interface WorksheetSectionProps {
  student: Student;
  onWorksheetComplete: (
    sessionHistoryItem: {
      id: string;
      date: string;
      subject: 'math' | 'reading';
      level: string;
      durationSeconds: number;
      sctSeconds: number;
      wasSctAchieved: boolean;
      totalQuestions: number;
      correctCount: number;
      initialCorrectCount: number;
      attemptsToPerfect: number;
      pointsEarned: number;
      sheetIndex: number;
    },
    updatedProgress: number
  ) => void;
}

export function WorksheetSection({
  student,
  onWorksheetComplete
}: WorksheetSectionProps) {
  const currentLevelCode = student.selectedSubject === 'math' ? student.currentMathLevel : student.currentReadingLevel;
  const currentProgress = student.selectedSubject === 'math' ? student.mathProgress : student.readingProgress;
  
  // Choose the next unfinished worksheet index: completed sheets = progress / 20. Sheet range is 1-5.
  const currentSheetIndex = Math.min(Math.floor(currentProgress / 20) + 1, 5);

  const [worksheet, setWorksheet] = useState<Worksheet | null>(null);
  const [answers, setAnswers] = useState<Record<string, string>>({});
  const [attempts, setAttempts] = useState(1);
  const [firstAttemptCorrect, setFirstAttemptCorrect] = useState<number | null>(null);
  const [isPrintMode, setIsPrintMode] = useState(false);
  const [showCelebration, setShowCelebration] = useState(false);

  // Timer Ref & state
  const timerRef = useRef<NodeJS.Timeout | null>(null);

  // Initialize worksheet when sheet index or level code changes
  useEffect(() => {
    initNewWorksheet();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [student.selectedSubject, currentLevelCode, currentSheetIndex]);

  const initNewWorksheet = () => {
    const sheet = generateWorksheet(student.selectedSubject, currentLevelCode, currentSheetIndex);
    setWorksheet(sheet);
    setAnswers({});
    setAttempts(1);
    setFirstAttemptCorrect(null);
    setShowCelebration(false);
    setIsPrintMode(false);
    
    // Stop any existing timers
    if (timerRef.current) clearInterval(timerRef.current);
  };

  useEffect(() => {
    if (worksheet && worksheet.timerRunning && !worksheet.isPerfect) {
      timerRef.current = setInterval(() => {
        setWorksheet((prev) => {
          if (!prev) return null;
          return {
            ...prev,
            timeSpentSeconds: prev.timeSpentSeconds + 1
          };
        });
      }, 1000);
    } else {
      if (timerRef.current) clearInterval(timerRef.current);
    }

    return () => {
      if (timerRef.current) clearInterval(timerRef.current);
    };
  }, [worksheet?.timerRunning, worksheet?.isPerfect]);

  if (!worksheet) return null;

  const handleStartTimer = () => {
    setWorksheet((prev) => {
      if (!prev) return null;
      return { ...prev, timerRunning: true };
    });
  };

  const handlePauseTimer = () => {
    setWorksheet((prev) => {
      if (!prev) return null;
      return { ...prev, timerRunning: false };
    });
  };

  const handleAnswerChange = (qId: string, value: string) => {
    setAnswers((prev) => ({
      ...prev,
      [qId]: value
    }));
  };

  // Grading algorithm mimicking Kumon "red marker" approach
  const handleGradeSheet = () => {
    if (!worksheet.timerRunning) return; // Must start timer to grade

    let correctedQuestionsCount = 0;
    const gradedQuestions = worksheet.questions.map((q) => {
      const studentAnsRaw = answers[q.id] || '';
      const studentAnsClean = studentAnsRaw.trim().replace(/\s+/g, ' ');
      const isCorrect = studentAnsClean.toLowerCase() === q.correctAnswer.trim().toLowerCase();
      
      if (isCorrect) {
        correctedQuestionsCount++;
      }

      return {
        ...q,
        studentAnswer: studentAnsRaw,
        isCorrect,
        isMarkedForCorrection: !isCorrect // flags wrong questions for pupil
      };
    });

    const isPerfect = correctedQuestionsCount === worksheet.questions.length;

    // Track first-attempt grade metrics
    let newFirstAttemptCorrect = firstAttemptCorrect;
    if (firstAttemptCorrect === null) {
      newFirstAttemptCorrect = correctedQuestionsCount;
      setFirstAttemptCorrect(correctedQuestionsCount);
    }

    setWorksheet((prev) => {
      if (!prev) return null;
      return {
        ...prev,
        questions: gradedQuestions,
        isGraded: true,
        isPerfect
      };
    });

    if (isPerfect) {
      // 100% correct! Complete the sheet
      stopTimer();
      setShowCelebration(true);
    } else {
      // Needs review
      setAttempts((prev) => prev + 1);
    }
  };

  const stopTimer = () => {
    setWorksheet((prev) => {
      if (!prev) return null;
      return { ...prev, timerRunning: false };
    });
    if (timerRef.current) clearInterval(timerRef.current);
  };

  const handleFinishSheet = () => {
    // Generate historical record
    const points = calculatePoints();
    const finalCorrect = worksheet.questions.length;
    const initialCorrect = firstAttemptCorrect !== null ? firstAttemptCorrect : finalCorrect;
    const wasSctAchieved = worksheet.timeSpentSeconds <= worksheet.standardCompletionTimeSeconds;

    const historyRecord = {
      id: `${worksheet.level}-S${worksheet.sheetIndex}-${Date.now()}`,
      date: new Date().toISOString().split('T')[0],
      subject: worksheet.subject,
      level: worksheet.level,
      durationSeconds: worksheet.timeSpentSeconds,
      sctSeconds: worksheet.standardCompletionTimeSeconds,
      wasSctAchieved,
      totalQuestions: worksheet.questions.length,
      correctCount: finalCorrect,
      initialCorrectCount: initialCorrect,
      attemptsToPerfect: attempts,
      pointsEarned: points,
      sheetIndex: worksheet.sheetIndex
    };

    // Calculate progression increment. 1 sheet completed = 20%
    const increment = 20;
    const newProgress = Math.min(currentProgress + increment, 100);

    onWorksheetComplete(historyRecord, newProgress);
  };

  const calculatePoints = () => {
    const isUnderSct = worksheet.timeSpentSeconds <= worksheet.standardCompletionTimeSeconds;
    const basePoints = worksheet.questions.length * 10;
    const sctBonus = isUnderSct ? 50 : 0;
    const speedBonus = isUnderSct ? Math.max(10, Math.floor((worksheet.standardCompletionTimeSeconds - worksheet.timeSpentSeconds) / 2)) : 0;
    const singleAttemptBonus = attempts === 1 ? 30 : 0;
    return basePoints + sctBonus + speedBonus + singleAttemptBonus;
  };

  // Render questions with vertical formatting if multi-line is contained
  const renderMathQuestion = (q: Question) => {
    const isVerticalLayout = q.questionText.includes('\n');
    
    if (isVerticalLayout) {
      const parts = q.questionText.split('\n');
      const val1 = parts[0]?.trim() || '';
      const val2 = parts[1]?.trim() || '';
      return (
        <div className="flex flex-col items-center justify-center p-4 bg-slate-50 border border-slate-200 rounded-xl relative overflow-hidden h-full min-h-[160px]">
          {/* Kumon Red Ink check */}
          {worksheet.isGraded && (
            <div className="absolute right-3 top-3">
              {q.isCorrect ? (
                <svg className="w-16 h-16 text-red-500 animate-[drawCircle_0.5s_ease-out_forwards]" viewBox="0 0 100 100" fill="none">
                  <circle cx="50" cy="50" r="38" stroke="currentColor" strokeWidth="6" strokeDasharray="240" strokeDashoffset="240" style={{ strokeDashoffset: 0, transition: 'stroke-dashoffset 0.5s ease-out' }} />
                </svg>
              ) : (
                <svg className="w-12 h-12 text-red-500" viewBox="0 0 100 100" fill="none" stroke="currentColor" strokeWidth="8">
                  <line x1="15" y1="15" x2="85" y2="85" />
                </svg>
              )}
            </div>
          )}

          <div className="font-mono font-bold text-2xl text-right max-w-[80px] w-full border-b-2 border-slate-900 pb-1 pr-1 space-y-1">
            <div>{val1}</div>
            <div>{val2}</div>
          </div>

          <div className="mt-3 w-24">
            <input
              type="text"
              disabled={!worksheet.timerRunning || q.isCorrect}
              value={answers[q.id] || ''}
              onChange={(e) => handleAnswerChange(q.id, e.target.value)}
              placeholder="?"
              className={`w-full p-2 text-center text-xl font-mono font-bold rounded-lg border-2 focus:outline-none focus:ring-2 ${
                q.isCorrect 
                  ? 'bg-emerald-50 border-emerald-300 text-emerald-800' 
                  : q.isMarkedForCorrection
                  ? 'bg-red-50 border-red-300 text-red-900 focus:ring-red-500' 
                  : 'bg-white border-slate-300 text-slate-800 focus:ring-blue-500 focus:border-blue-500'
              }`}
            />
          </div>
          {q.secondaryText && (
            <span className="text-[10px] text-gray-400 mt-2 text-center leading-tight">{q.secondaryText}</span>
          )}
        </div>
      );
    }

    // Default horizontal row layout
    return (
      <div className="flex flex-col p-4 bg-slate-50 border border-slate-200 rounded-xl relative overflow-hidden h-full min-h-[140px] justify-between">
        {/* Kumon Red Marks */}
        {worksheet.isGraded && (
          <div className="absolute right-3 top-2">
            {q.isCorrect ? (
              <svg className="w-12 h-12 text-red-500" viewBox="0 0 100 100" fill="none">
                <circle cx="50" cy="50" r="35" stroke="currentColor" strokeWidth="6" strokeDasharray="220" strokeDashoffset="0" />
              </svg>
            ) : (
              <svg className="w-10 h-10 text-red-500" viewBox="0 0 100 100" stroke="currentColor" strokeWidth="8">
                <line x1="20" y1="20" x2="80" y2="80" />
              </svg>
            )}
          </div>
        )}

        <div className="font-sans font-bold text-lg text-slate-800 pt-2">
          {q.questionText}
        </div>

        <div className="mt-2">
          <input
            type="text"
            disabled={!worksheet.timerRunning || q.isCorrect}
            value={answers[q.id] || ''}
            onChange={(e) => handleAnswerChange(q.id, e.target.value)}
            placeholder="?"
            className={`w-full p-2 text-center font-mono font-bold text-lg rounded-lg border-2 focus:outline-none focus:ring-2 ${
              q.isCorrect 
                ? 'bg-emerald-50 border-emerald-300 text-emerald-800 font-bold' 
                : q.isMarkedForCorrection
                ? 'bg-red-50 border-red-300 text-red-900 focus:ring-red-500' 
                : 'bg-white border-slate-300 text-slate-800 focus:ring-blue-500 focus:border-blue-500'
            }`}
          />
        </div>

        {q.secondaryText && (
          <span className="text-[10px] text-gray-400 mt-2 block leading-snug">{q.secondaryText}</span>
        )}
      </div>
    );
  };

  const renderReadingQuestion = (q: Question) => {
    const isMultipleChoice = q.options && q.options.length > 0;

    return (
      <div className="flex flex-col p-5 bg-slate-50 border border-slate-200 rounded-xl relative overflow-hidden h-full min-h-[180px] justify-between space-y-3">
        {/* Kumon style grading mark overlays */}
        {worksheet.isGraded && (
          <div className="absolute right-3 top-3">
            {q.isCorrect ? (
              <svg className="w-12 h-12 text-red-500" viewBox="0 0 100 100" fill="none">
                <circle cx="50" cy="50" r="35" stroke="currentColor" strokeWidth="6" />
              </svg>
            ) : (
              <svg className="w-10 h-10 text-red-500" viewBox="0 0 100 100" stroke="currentColor" strokeWidth="8">
                <line x1="20" y1="20" x2="80" y2="80" />
              </svg>
            )}
          </div>
        )}

        <div className="space-y-1.5 pt-2">
          <div className="text-sm font-sans text-gray-800 whitespace-pre-line leading-relaxed font-medium">
            {q.questionText}
          </div>
          {q.secondaryText && (
            <div className="text-xs text-gray-500 italic pb-1">
              Dica: {q.secondaryText}
            </div>
          )}
        </div>

        {isMultipleChoice ? (
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-2 pt-2">
            {q.options?.map((opt) => {
              const isSelected = answers[q.id] === opt;
              return (
                <button
                  key={opt}
                  type="button"
                  disabled={!worksheet.timerRunning || q.isCorrect}
                  onClick={() => handleAnswerChange(q.id, opt)}
                  className={`px-3 py-2 text-xs font-semibold rounded-lg border text-left transition ${
                    q.isCorrect && isSelected
                      ? 'bg-emerald-50 border-emerald-300 text-emerald-800'
                      : isSelected
                      ? 'bg-slate-900 border-slate-900 text-white'
                      : 'bg-white border-gray-300 text-gray-700 hover:bg-gray-100'
                  }`}
                >
                  {opt}
                </button>
              );
            })}
          </div>
        ) : (
          <div className="pt-2">
            <input
              type="text"
              disabled={!worksheet.timerRunning || q.isCorrect}
              value={answers[q.id] || ''}
              onChange={(e) => handleAnswerChange(q.id, e.target.value)}
              placeholder="Digite sua resposta estruturada..."
              className={`w-full px-3 py-2 text-sm rounded-lg border focus:outline-none focus:ring-2 ${
                q.isCorrect 
                  ? 'bg-emerald-50 border-emerald-300 text-emerald-800 font-medium' 
                  : q.isMarkedForCorrection
                  ? 'bg-red-50 border-red-300 text-red-900 focus:ring-red-500' 
                  : 'bg-white border-slate-300 text-slate-800 focus:ring-blue-500 focus:border-blue-500'
              }`}
            />
          </div>
        )}
      </div>
    );
  };

  const getFormatSctTime = (seconds: number) => {
    const mins = Math.floor(seconds / 60);
    const secs = seconds % 60;
    return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
  };

  const activeSct = worksheet.standardCompletionTimeSeconds;
  const isTimeOverSct = worksheet.timeSpentSeconds > activeSct;

  return (
    <div className="space-y-6">
      {/* Printable template */}
      {isPrintMode ? (
        <div className="bg-white p-8 border border-gray-300 rounded-lg max-w-4xl mx-auto shadow-sm printable-section space-y-6 relative overflow-hidden" id="printable-worksheet">
          {/* Print controls */}
          <div className="print:hidden flex justify-between items-center border-b border-gray-200 pb-3 mb-6">
            <div className="flex items-center gap-2 text-sm font-semibold text-gray-700">
              <Printer className="w-5 h-5 text-gray-400" />
              <span>Visualização de Impressão de Ficha</span>
            </div>
            <div className="flex gap-2">
              <button
                onClick={() => setIsPrintMode(false)}
                className="px-3 py-1.5 border border-gray-300 text-gray-700 text-xs rounded hover:bg-gray-50 font-medium"
              >
                Voltar ao Digital
              </button>
              <button
                onClick={() => window.print()}
                className="px-3 py-1.5 bg-red-600 text-white text-xs rounded hover:bg-red-700 font-semibold"
              >
                Imprimir Agora
              </button>
            </div>
          </div>

          {/* Worksheet Header Box */}
          <div className="grid grid-cols-3 border border-gray-900 text-center text-sm font-serif p-4 rounded bg-slate-50/50">
            <div className="border-r border-gray-900 p-2 space-y-1.5 text-left">
              <div className="font-bold">Método Kumon</div>
              <div className="text-xs">Estudante: <span className="underline">{student.name}</span></div>
              <div className="text-xs">Assunto: {worksheet.subject === 'math' ? 'Matemática' : 'Português'}</div>
            </div>
            <div className="border-r border-gray-900 p-2 flex flex-col justify-center items-center">
              <div className="font-mono text-xl font-bold tracking-wider">{worksheet.level}</div>
              <div className="text-xs">Ficha {worksheet.sheetIndex}</div>
            </div>
            <div className="p-2 space-y-1.5 text-left text-xs">
              <div>Data: ____/____/20__</div>
              <div>Hora Início: ____:____</div>
              <div>Hora Término: ____:____</div>
            </div>
          </div>

          <p className="text-xs text-center text-gray-400 font-mono">
            * Para o correto acompanhamento, registre os tempos com cronômetro físico. Tempo Objetivo (SCT): {getFormatSctTime(activeSct)}
          </p>

          {/* Questions Grid printed */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-x-8 gap-y-6 pt-4">
            {worksheet.questions.map((q, idx) => (
              <div key={q.id} className="border-b border-gray-200 pb-3 flex justify-between items-start">
                <div className="space-y-1.5 pr-2 w-full">
                  <div className="font-mono font-semibold text-xs text-slate-400">({idx + 1})</div>
                  <div className="font-medium whitespace-pre-line text-sm text-slate-800">
                    {q.questionText}
                  </div>
                  {q.secondaryText && (
                    <span className="text-[10px] text-gray-400 block italic">{q.secondaryText}</span>
                  )}
                </div>
                <div className="w-24 h-10 border border-gray-400 rounded bg-slate-50 flex items-end justify-center pb-1 text-slate-300 font-serif text-xs">
                  Assinatura
                </div>
              </div>
            ))}
          </div>

          {/* Footer warning */}
          <div className="text-center pt-8 text-xs text-gray-400 font-mono">
            Ficha gerada dinamicamente pelo Kumon Self-Study Companion. Impresso por {student.name}.
          </div>
        </div>
      ) : (
        /* DIGITAL STUDY SCREEN */
        <div className="space-y-6" id="digital-study-panel">
          {/* Toolbar and metrics bar */}
          <div className="bg-slate-50 border border-gray-200 rounded-2xl p-5 flex flex-col sm:flex-row items-center justify-between gap-4">
            <div className="flex items-center gap-4">
              <div className="w-12 h-12 bg-red-500 rounded-xl flex flex-col justify-center items-center text-white font-mono shadow-sm">
                <span className="text-[10px] leading-tight font-semibold">FICHA</span>
                <span className="text-xl font-bold leading-none">{worksheet.sheetIndex}</span>
              </div>

              <div>
                <h3 className="font-sans font-bold text-gray-900 text-base">{worksheet.title}</h3>
                <p className="text-xs text-gray-500 flex items-center gap-1.5 mt-0.5">
                  <BookOpen className="w-3.5 h-3.5 text-gray-450" />
                  Nível {worksheet.level} • {worksheet.questions.length} questões estruturadas
                </p>
              </div>
            </div>

            {/* Timers & Play tools */}
            <div className="flex flex-wrap items-center gap-3">
              {/* Objective meter */}
              <div className="text-right">
                <div className="text-[10px] font-mono font-bold text-gray-500 uppercase tracking-widest">Alvo SCT</div>
                <div className="text-sm font-semibold text-sky-700">{getFormatSctTime(activeSct)}</div>
              </div>

              {/* Digital count chronometer */}
              <div className={`px-4 py-2 rounded-xl border flex items-center gap-2 font-mono text-lg font-bold min-w-[110px] justify-center ${
                worksheet.isPerfect 
                  ? 'bg-emerald-50 border-emerald-250 text-emerald-700' 
                  : isTimeOverSct
                  ? 'bg-amber-50 border-amber-250 text-amber-700 animate-pulse'
                  : 'bg-slate-900 text-white border-transparent'
              }`}>
                <Clock className="w-5 h-5 text-red-500 shrink-0" />
                <span>{getFormatSctTime(worksheet.timeSpentSeconds)}</span>
              </div>

              {/* Action buttons */}
              {!worksheet.isPerfect ? (
                <>
                  {!worksheet.timerRunning ? (
                    <button
                      onClick={handleStartTimer}
                      id="play-timer-btn"
                      className="px-4 py-2 bg-red-600 text-white rounded-xl text-sm font-bold shadow-md hover:bg-red-700 hover:shadow-lg transition flex items-center gap-1.5"
                    >
                      <Play className="w-4 h-4 fill-white" />
                      <span>Iniciar Ficha</span>
                    </button>
                  ) : (
                    <button
                      onClick={handlePauseTimer}
                      id="pause-timer-btn"
                      className="px-4 py-2 bg-slate-300 text-slate-800 rounded-xl text-sm font-bold hover:bg-slate-400 transition flex items-center gap-1.5"
                    >
                      <Pause className="w-4 h-4" />
                      <span>Pausar</span>
                    </button>
                  )}
                </>
              ) : null}

              {/* Print worksheet Trigger */}
              <button
                onClick={() => setIsPrintMode(true)}
                id="print-sheet-btn"
                className="p-2.5 bg-white border border-gray-300 text-gray-700 transition rounded-xl hover:bg-gray-100"
                title="Imprimir Ficha de Estudo"
              >
                <Printer className="w-5 h-5" />
              </button>
            </div>
          </div>

          {/* Worksheet State / Guidelines overlays */}
          {!worksheet.timerRunning && !worksheet.isPerfect && (
            <div className="bg-red-50 border border-red-200 text-red-900 rounded-2xl p-6 text-center space-y-3">
              <AlertCircle className="w-10 h-10 text-red-500 mx-auto" />
              <h4 className="font-sans font-bold text-lg">Pronto para Resolver?</h4>
              <p className="text-sm text-red-800 max-w-lg mx-auto">
                No método Kumon, você resolve os problemas de forma sequencial com o cronômetro ligado para mensurar sua fluência lógica. Pressione <strong>"Iniciar Ficha"</strong> para exibir e destravar os problemas.
              </p>
              <button
                onClick={handleStartTimer}
                className="px-6 py-2.5 bg-red-600 text-white font-bold rounded-xl text-sm hover:bg-red-700 shadow-md inline-block"
              >
                Ativar Cronômetro e Começar
              </button>
            </div>
          )}

          {/* ACTIVE QUESTIONS AREA */}
          <div className={`relative ${!worksheet.timerRunning && !worksheet.isPerfect ? 'blur-sm select-none pointer-events-none' : ''}`}>
            
            {worksheet.isGraded && !worksheet.isPerfect && (
              <div className="mb-4 p-4 bg-yellow-50 border border-yellow-200 text-yellow-900 text-sm rounded-xl flex items-center gap-2">
                <AlertCircle className="w-5 h-5 text-yellow-600 shrink-0" />
                <div>
                  <span className="font-bold">Correções Exigidas:</span> Você cometeu alguns erros (marcados com traço vermelho). Ajuste os campos incorretos e envie novamente para buscar a nota 100%!
                </div>
              </div>
            )}

            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
              {worksheet.questions.map((q) => (
                <div key={q.id}>
                  {worksheet.subject === 'math' ? renderMathQuestion(q) : renderReadingQuestion(q)}
                </div>
              ))}
            </div>

            {/* GRADING SUBMISSION TOOLBAR */}
            {worksheet.timerRunning && !worksheet.isPerfect && (
              <div className="mt-8 flex justify-center pb-6">
                <button
                  onClick={handleGradeSheet}
                  id="submit-for-grading-btn"
                  className="px-8 py-4 bg-slate-900 text-white rounded-2xl text-base font-bold shadow-xl hover:bg-slate-800 transition transform hover:-translate-y-0.5 flex items-center gap-2"
                >
                  <Send className="w-5 h-5 text-red-500" />
                  <span>{worksheet.isGraded ? 'Re-enviar Correções' : 'Enviar para Correção'}</span>
                </button>
              </div>
            )}
          </div>

          {/* CELEBRATION MODAL FOR PERFECT SUBMISSION */}
          <AnimatePresence>
            {showCelebration && (
              <motion.div 
                className="fixed inset-0 bg-slate-900/60 backdrop-blur-sm flex items-center justify-center p-4 z-50"
                initial={{ opacity: 0 }}
                animate={{ opacity: 1 }}
                exit={{ opacity: 0 }}
              >
                <motion.div 
                  className="bg-white rounded-3xl p-8 max-w-md w-full text-center space-y-6 shadow-2xl border border-gray-100 relative overflow-hidden"
                  initial={{ scale: 0.9, y: 20 }}
                  animate={{ scale: 1, y: 0 }}
                  exit={{ scale: 0.9, y: 20 }}
                  id="perfect-sheet-modal"
                >
                  {/* Confetti Sparkles effect */}
                  <div className="absolute top-0 left-0 right-0 h-2 bg-gradient-to-r from-red-500 via-amber-500 to-green-500" />
                  
                  <div className="w-20 h-20 bg-green-50 border border-green-200 text-green-600 rounded-full flex items-center justify-center mx-auto text-4xl font-bold animate-[bounce_1s_infinite]">
                    <Sparkles className="w-10 h-10 text-emerald-500 fill-emerald-500" />
                  </div>

                  <div className="space-y-2">
                    <h3 className="font-sans font-bold text-2xl text-slate-900">Estudo 100% Concluído!</h3>
                    <p className="text-sm text-gray-500">
                      Excelente dedicação! No melhor modelo Kumon de aprendizagem, você continuou corrigindo até que todas as respostas alcançassem a nota ideal.
                    </p>
                  </div>

                  {/* Summary Box */}
                  <div className="bg-slate-50 p-4 rounded-2xl border border-gray-200 text-left text-sm font-mono space-y-2">
                    <div className="flex justify-between">
                      <span className="text-gray-500">Tempo Final:</span>
                      <span className={`font-bold ${isTimeOverSct ? 'text-gray-900' : 'text-emerald-600'}`}>
                        {getFormatSctTime(worksheet.timeSpentSeconds)}
                        {!isTimeOverSct && ' (Meta Batida! ⚡)'}
                      </span>
                    </div>
                    <div className="flex justify-between">
                      <span className="text-gray-500">Correções Exigidas:</span>
                      <span className="font-bold">
                        {attempts === 1 ? 'Nenhuma (Ficha Perfeita!)' : `${attempts - 1} rodadas de correção`}
                      </span>
                    </div>
                    <div className="flex justify-between border-t border-gray-200 pt-2 font-sans font-bold text-base">
                      <span className="text-slate-900">Pontos Conquistados:</span>
                      <span className="text-red-600 text-lg">+{calculatePoints()} Pts</span>
                    </div>
                  </div>

                  <button
                    onClick={handleFinishSheet}
                    id="confirm-finish-worksheet"
                    className="w-full py-3 bg-red-600 text-white rounded-xl text-sm font-bold shadow-md hover:bg-red-700 transition flex items-center justify-center gap-1.5"
                  >
                    <span>Gravar Pontuação e Seguir</span>
                    <ArrowRight className="w-4 h-4" />
                  </button>
                </motion.div>
              </motion.div>
            )}
          </AnimatePresence>
        </div>
      )}
    </div>
  );
}
