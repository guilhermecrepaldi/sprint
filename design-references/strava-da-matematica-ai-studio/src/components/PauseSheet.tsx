/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import React from "react";
import { Play, LogOut, Trash2 } from "lucide-react";

interface PauseSheetProps {
  onContinue: () => void;
  onExit: () => void;
  onDiscard: () => void;
  bgMode: 'white' | 'dark';
}

export function PauseSheet({ onContinue, onExit, onDiscard, bgMode }: PauseSheetProps) {
  const isDark = bgMode === 'dark';

  return (
    <div className="fixed inset-0 z-50 flex items-end sm:items-center justify-center bg-black/60 p-4 animate-fade-in">
      <div 
        className={`w-full max-w-md rounded-t-xl sm:rounded-xl border p-6 shadow-2xl transition-all ${
          isDark 
            ? 'bg-[#171B15] border-[#30362C] text-[#F2F5EF]' 
            : 'bg-[#FFFFFF] border-[#D9DED6] text-[#151713]'
        }`}
      >
        <div className="flex flex-col items-center text-center gap-1 mb-6">
          <div className={`p-3 rounded-full mb-2 ${isDark ? 'bg-zinc-800' : 'bg-zinc-100'}`}>
            <span className="text-2xl font-mono tracking-widest font-bold">PAUSE</span>
          </div>
          <h3 className="text-lg font-semibold tracking-tight">Treinamento Suspenso</h3>
          <p className={`text-xs ${isDark ? 'text-[#A9B2A3]' : 'text-[#5F665C]'} max-w-xs`}>
            O cronômetro interno está congelado. Retome quando restabelecer o nível de concentração.
          </p>
        </div>

        <div className="flex flex-col gap-2">
          {/* Continue button */}
          <button
            onClick={onContinue}
            className={`w-full py-3 px-4 rounded-md font-semibold text-sm flex items-center justify-center gap-2 transition-all cursor-pointer ${
              isDark 
                ? 'bg-[#4CC38A] hover:bg-[#4CC38A]/90 text-[#11140F]' 
                : 'bg-[#1E7F5C] hover:bg-[#1E7F5C]/90 text-white'
            }`}
          >
            <Play size={14} />
            Continuar Treino
          </button>

          {/* Exit button */}
          <button
            onClick={onExit}
            className={`w-full py-3 px-4 rounded-md font-semibold text-sm flex items-center justify-center gap-2 border transition-all cursor-pointer ${
              isDark 
                ? 'border-[#30362C] hover:bg-zinc-800/65 text-[#F2F5EF]' 
                : 'border-[#D9DED6] hover:bg-zinc-50 text-[#151713]'
            }`}
          >
            <LogOut size={14} />
            Pausar e Encerrar de Vez
          </button>

          {/* Discard button */}
          <button
            onClick={onDiscard}
            className={`w-full py-2.5 px-4 rounded-md font-medium text-xs flex items-center justify-center gap-2 border border-red-200/40 hover:border-red-500/30 text-red-400 hover:text-red-500 transition-all cursor-pointer mt-2`}
          >
            <Trash2 size={12} />
            Limpar Todos os Traços
          </button>
        </div>
      </div>
    </div>
  );
}
