/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

export interface SessionConfig {
  show_thermometer: boolean;
  background: 'white' | 'dark';
  pen_color: string;
  pen_thickness: number;
  duration_mode: 'free' | 'time' | 'pages';
  duration_value_minutes?: number; // e.g., 30, 60, 120
  pages_limit: number;
  difficulty_progression: 'linear' | 'geometric';
  difficulty_start: number; // 1.0 to 10.0
  difficulty_step: number; // progression step for linear (e.g., 0.5)
  difficulty_ratio: number; // progression ratio for geometric (e.g., 1.15)
  restart_on_avg: number;
  restart_window: number;
  exercises_per_page: number;
}

export interface FolhaField {
  fieldIndex: number; // 1-indexed (e.g., 1 to 5)
  exerciseId: string;
  statement: string;
  expectedAnswer: string;
  latex?: string;
  recognizedAnswer?: string;
  isCorrect?: boolean;
  score?: number; // 0.0 to 1.0 or 0 to 10
  errorType?: 'sinal' | 'fracao' | 'equacao_2_grau' | 'desconhecido' | null;
  hasInk?: boolean;
}

export interface Folha {
  folhaId: string;
  pageIndex: number;
  difficulty: number;
  fields: FolhaField[];
}

export interface SessionUiState {
  studentId: string;
  sessionId: string | null;
  config: SessionConfig;
  currentFolha: Folha | null;
  status: 'start' | 'active' | 'page_result' | 'finished';
  apiStatus: 'ok' | 'connecting' | 'offline';
  errorMessage: string | null;
  thermometerValue: number; // 0 to 1
  history: {
    pageIndex: number;
    score: number;
    difficulty: number;
    correctCount: number;
    totalCount: number;
  }[];
}

export interface InkStrokePoint {
  x: number;
  y: number;
  pressure?: number;
  tilt?: number;
  time: number;
}

export interface InkStroke {
  points: InkStrokePoint[];
  color: string;
  thickness: number;
}

export interface InkLayer {
  strokes: InkStroke[];
}

export interface FieldTiming {
  startedAtMs: number | null;
  firstStrokeAtMs: number | null;
  totalTimeMs: number;
}

export interface SessionStartRequestPayload {
  student_id: string;
  config: SessionConfig;
}

export interface FieldSubmitPayload {
  field_index: number;
  exercise_id: string;
  image_base64?: string; // OCR input of the canvas crop
  timed_out?: boolean;
  first_stroke_ms?: number;
  started_ms?: number;
  total_time_ms?: number;
  ink_data?: string; // JSON representation of Strokes
  typed_answer?: string; // Optional manual typed fallback fallback
}

export interface SessionSubmitRequestPayload {
  fields: FieldSubmitPayload[];
}

export interface FieldResult {
  fieldIndex: number;
  recognizedAnswer: string;
  expectedAnswer: string;
  isCorrect: boolean;
  score: number;
  errorType: 'sinal' | 'fracao' | 'equacao_2_grau' | 'desconhecido' | null;
}

export interface SessionSubmitResponsePayload {
  session_status: 'active' | 'finished';
  page_index: number;
  score: number; // Average score of page (0 to 10)
  thermometer_value: number; // Updated confidence level
  results: FieldResult[];
}
