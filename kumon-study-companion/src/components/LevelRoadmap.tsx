/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import React, { useState } from 'react';
import { 
  Award, ChevronRight, Compass, ShieldAlert, Check, 
  Clock, Lock, BookOpen, Star, HelpCircle, ArrowUpRight
} from 'lucide-react';
import { Student, LevelConfig, MATH_LEVELS, READING_LEVELS } from '../types';

interface LevelRoadmapProps {
  student: Student;
  onUpdateLevel: (levelCode: string) => void;
}

export function LevelRoadmap({
  student,
  onUpdateLevel
}: LevelRoadmapProps) {
  const isMath = student.selectedSubject === 'math';
  const levels = isMath ? MATH_LEVELS : READING_LEVELS;
  const currentStudentLevelCode = isMath ? student.currentMathLevel : student.currentReadingLevel;
  
  const currentIdx = levels.findIndex(l => l.code === currentStudentLevelCode);
  const [selectedLevelConfig, setSelectedLevelConfig] = useState<LevelConfig>(
    levels[currentIdx] || levels[0]
  );

  return (
    <div className="bg-white rounded-2xl shadow-xl border border-gray-100 overflow-hidden" id="roadmap-card">
      <div className="p-6 border-b border-gray-100 bg-slate-50 flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div className="flex items-center gap-3">
          <div className="p-2.5 bg-red-50 text-red-600 rounded-xl">
            <Compass className="w-5 h-5" />
          </div>
          <div>
            <h3 className="font-sans font-bold text-gray-900 text-base">Escalada de Aprendizado</h3>
            <p className="text-xs text-gray-500 mt-0.5">Veja seu progresso atual e os próximos passos do programa.</p>
          </div>
        </div>

        <div className="flex items-center gap-2 text-xs font-mono">
          <span className="flex items-center gap-1 text-slate-500">
            <span className="w-2.5 h-2.5 rounded-full bg-emerald-500 block" /> Concluído
          </span>
          <span className="flex items-center gap-1 text-slate-500">
            <span className="w-2.5 h-2.5 rounded-full bg-red-600 block" /> Atual
          </span>
          <span className="flex items-center gap-1 text-slate-500">
            <span className="w-2.5 h-2.5 rounded-full bg-slate-200 block" /> Futuro
          </span>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3">
        {/* LEFT LADDER COLUMN */}
        <div className="lg:col-span-1 border-r border-gray-100 p-6 space-y-3 max-h-[500px] overflow-y-auto">
          <h4 className="text-xs font-mono font-bold text-gray-400 uppercase tracking-wider mb-2">Linha do Tempo de Níveis</h4>
          
          <div className="relative pl-3 space-y-4">
            {/* Draw a vertical connection line */}
            <div className="absolute left-[23px] top-6 bottom-6 w-[2px] bg-gray-100" />

            {levels.map((lvl, index) => {
              const isCompleted = index < currentIdx;
              const isCurrent = index === currentIdx;
              const isSelected = selectedLevelConfig.code === lvl.code;

              let nodeBg = 'bg-gray-100 border-gray-200 text-gray-400';
              let borderCol = 'border-transparent';

              if (isCompleted) {
                nodeBg = 'bg-emerald-50 border-emerald-200 text-emerald-600';
              } else if (isCurrent) {
                nodeBg = 'bg-red-50 border-red-200 text-red-600 font-bold scale-105 ring-2 ring-red-500/10';
              }

              if (isSelected) {
                borderCol = 'border-red-500 bg-red-50/20';
              }

              return (
                <div 
                  key={lvl.code} 
                  onClick={() => setSelectedLevelConfig(lvl)}
                  id={`roadmap-node-${lvl.code}`}
                  className={`flex items-center gap-3 p-2.5 rounded-xl border-2 transition cursor-pointer hover:bg-slate-50 ${borderCol}`}
                >
                  {/* Circle number icon */}
                  <div className={`w-8 h-8 rounded-full border flex items-center justify-center text-xs font-mono shrink-0 transition-all ${nodeBg}`}>
                    {isCompleted ? <Check className="w-4 h-4" /> : lvl.code}
                  </div>

                  <div className="overflow-hidden">
                    <div className="text-xs font-bold font-sans text-gray-900 truncate">
                      {lvl.code} - {lvl.name.replace(/Nível \w+ \(/, '').replace(')', '')}
                    </div>
                    <div className="text-[10px] text-gray-500 truncate mt-0.5">{lvl.focus}</div>
                  </div>

                  <ChevronRight className={`w-3.5 h-3.5 text-gray-300 ml-auto shrink-0 transition ${isSelected ? 'translate-x-0.5 text-red-500' : ''}`} />
                </div>
              );
            })}
          </div>
        </div>

        {/* RIGHT DETAIL PREVIEW COLUMN */}
        <div className="lg:col-span-2 p-6 space-y-6 bg-slate-50/50">
          <div className="bg-white p-5 rounded-2xl border border-gray-200 shadow-sm space-y-4">
            <div className="flex justify-between items-start">
              <div>
                <span className="px-2.5 py-0.5 bg-red-50 text-red-650 rounded-full text-xs font-mono font-bold border border-red-100">
                  Código: {selectedLevelConfig.code}
                </span>
                <h3 className="font-sans font-bold text-gray-950 text-lg mt-2">
                  {selectedLevelConfig.name}
                </h3>
              </div>
              <div className="p-3 bg-slate-50 border border-gray-200 rounded-xl text-slate-800 text-center min-w-[70px] shrink-0">
                <div className="text-[10px] uppercase font-mono font-semibold tracking-wider text-gray-400">Objetivo</div>
                <div className="text-sm font-bold text-slate-900 mt-0.5">{(selectedLevelConfig.sctSeconds) / 60}m SCT</div>
              </div>
            </div>

            <div className="space-y-3 divide-y divide-gray-100 text-sm">
              <div className="pb-3">
                <span className="text-xs text-gray-400 uppercase font-mono block">Visão do Nível</span>
                <p className="text-gray-700 leading-relaxed mt-1 font-medium select-text">{selectedLevelConfig.description}</p>
              </div>

              <div className="py-3">
                <span className="text-xs text-gray-400 uppercase font-mono block">Foco Pedagógico das Planilhas</span>
                <p className="text-gray-700 font-semibold mt-1">{selectedLevelConfig.focus}</p>
              </div>

              <div className="py-3">
                <span className="text-xs text-gray-400 uppercase font-mono block">Requisito para Conclusão de Nível</span>
                <p className="text-gray-700 mt-1 flex items-center gap-1.5 font-medium">
                  <Star className="w-4 h-4 text-amber-500 fill-amber-500" />
                  Preencher e acertar {selectedLevelConfig.totalSheets} fichas de exercícios temporizadas sob o tempo padrão ideal.
                </p>
              </div>
            </div>

            {/* Level Action: Manual override if selected level is not active and student wants to jump or review level */}
            {selectedLevelConfig.code !== currentStudentLevelCode && (
              <div className="bg-slate-50 p-4 border border-gray-200 rounded-xl flex items-center justify-between gap-4 mt-6">
                <div>
                  <h5 className="text-xs font-bold text-slate-800">Deseja alterar para este nível?</h5>
                  <p className="text-[11px] text-gray-500 mt-0.5">
                    Atualize os exercícios sugeridos para praticar {isMath ? 'Matemática' : 'Português'} neste patamar.
                  </p>
                </div>
                <button
                  onClick={() => {
                    onUpdateLevel(selectedLevelConfig.code);
                    alert(`Nível atualizado com sucesso para ${selectedLevelConfig.code}!`);
                  }}
                  id={`activate-level-btn-${selectedLevelConfig.code}`}
                  className="px-4 py-2 bg-slate-900 text-white rounded-lg text-xs font-bold hover:bg-slate-800 transition shadow-sm"
                >
                  Ativar Nível
                </button>
              </div>
            )}
          </div>

          {/* Quick pedagogical tips block */}
          <div className="bg-red-50/50 border border-red-150 rounded-2xl p-5 space-y-2">
            <h5 className="font-sans font-bold text-red-900 text-xs uppercase tracking-wider flex items-center gap-1">
              <Compass className="w-4 h-4 text-red-650" />
              Filosofia Kumon: Estudo Autodidata
            </h5>
            <p className="text-xs text-red-800 leading-relaxed font-normal">
              No Kumon, não há aulas tradicionais expositivas. O aluno lê as breves instruções e exemplos no início ou rodapé de cada ficha, descobre as regras de funcionamento por dedução e treina exaustivamente para atingir o domínio completo de forma natural.
            </p>
          </div>
        </div>
      </div>
    </div>
  );
}
