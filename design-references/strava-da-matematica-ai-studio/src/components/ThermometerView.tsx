/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import React from "react";

interface ThermometerViewProps {
  value: number; // 0.0 to 1.0
  bgMode: 'white' | 'dark';
  compact?: boolean;
}

export function ThermometerView({ value, bgMode, compact = false }: ThermometerViewProps) {
  // Determine color based on specified bounds
  let color = "";
  let label = "";

  if (value < 0.45) {
    color = bgMode === 'white' ? 'bg-[#C2413A]' : 'bg-[#E05D55]';
    label = "Instável";
  } else if (value < 0.70) {
    color = bgMode === 'white' ? 'bg-[#B7791F]' : 'bg-[#D6A13D]';
    label = "Hesitante";
  } else {
    color = bgMode === 'white' ? 'bg-[#1E7F5C]' : 'bg-[#4CC38A]';
    label = "Consolidação";
  }

  const percent = Math.round(value * 100);

  if (compact) {
    return (
      <div className="flex items-center gap-2 max-w-[120px] w-full" title={`Confiança Cognitiva: ${percent}%`}>
        <div className="flex-1 h-1.5 rounded-full bg-slate-200 dark:bg-zinc-800 overflow-hidden">
          <div 
            className={`h-full ${color} transition-all duration-300 rounded-full`}
            style={{ width: `${percent}%` }}
          />
        </div>
        <span className="font-mono text-[10px] tabular-nums font-semibold tracking-wider">
          {percent}%
        </span>
      </div>
    );
  }

  return (
    <div className={`p-3 rounded-lg border ${
      bgMode === 'white' 
        ? 'border-[#D9DED6] bg-[#FBFCFA]' 
        : 'border-[#30362C] bg-[#1D221B]'
    } flex flex-col gap-2`}>
      <div className="flex justify-between items-center">
        <span className="text-[11px] font-semibold uppercase tracking-wider opacity-70">
          Rendimento Cognitivo
        </span>
        <span className="font-mono text-xs font-bold tabular-nums">
          {percent}%
        </span>
      </div>
      <div className="h-2 rounded-full bg-slate-200 dark:bg-zinc-800 overflow-hidden relative">
        <div 
          className={`h-full ${color} transition-all duration-300 rounded-full`}
          style={{ width: `${percent}%` }}
        />
      </div>
      <div className="flex justify-between text-[10px] font-mono opacity-60">
        <span>Foco Puro</span>
        <span>Estado: <b className="font-semibold">{label}</b></span>
      </div>
    </div>
  );
}
