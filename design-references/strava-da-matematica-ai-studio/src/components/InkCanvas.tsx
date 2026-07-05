/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import React, { useRef, useState, useEffect, useImperativeHandle, forwardRef } from "react";
import { InkStroke, InkStrokePoint } from "../types";

interface InkCanvasProps {
  penColor: string;
  penThickness: number;
  isEraser: boolean;
  onInkChange?: (hasInk: boolean) => void;
  bgMode: 'white' | 'dark';
}

export interface InkCanvasRef {
  clear: () => void;
  getImgBase64: () => string;
  undo: () => void;
  redo: () => void;
}

export const InkCanvas = forwardRef<InkCanvasRef, InkCanvasProps>((props, ref) => {
  const { penColor, penThickness, isEraser, onInkChange, bgMode } = props;
  const canvasRef = useRef<HTMLCanvasElement | null>(null);
  const containerRef = useRef<HTMLDivElement | null>(null);

  const [strokes, setStrokes] = useState<InkStroke[]>([]);
  const [redoStack, setRedoStack] = useState<InkStroke[]>([]);
  const isDrawingRef = useRef<boolean>(false);
  const currentPointsRef = useRef<InkStrokePoint[]>([]);

  // Keep a local copy of props for pointer performance
  const penColorRef = useRef(penColor);
  const penThicknessRef = useRef(penThickness);
  const isEraserRef = useRef(isEraser);

  useEffect(() => {
    penColorRef.current = penColor;
    penThicknessRef.current = penThickness;
    isEraserRef.current = isEraser;
  }, [penColor, penThickness, isEraser]);

  // Canvas redrawing engine
  const redraw = () => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const ctx = canvas.getContext("2d");
    if (!ctx) return;

    // Clear background
    ctx.clearRect(0, 0, canvas.width, canvas.height);

    // Draw grid notebook background pattern (very subtle)
    ctx.strokeStyle = bgMode === 'white' ? 'rgba(217, 222, 214, 0.45)' : 'rgba(48, 54, 44, 0.35)';
    ctx.lineWidth = 1;
    const gridSize = 24;
    for (let x = gridSize; x < canvas.width; x += gridSize) {
      ctx.beginPath();
      ctx.moveTo(x, 0);
      ctx.lineTo(x, canvas.height);
      ctx.stroke();
    }
    for (let y = gridSize; y < canvas.height; y += gridSize) {
      ctx.beginPath();
      ctx.moveTo(0, y);
      ctx.lineTo(canvas.width, y);
      ctx.stroke();
    }

    // Render strokes
    strokes.forEach((stroke) => {
      if (stroke.points.length === 0) return;
      ctx.beginPath();
      ctx.strokeStyle = stroke.color;
      ctx.lineWidth = stroke.thickness;
      ctx.lineCap = "round";
      ctx.lineJoin = "round";

      ctx.moveTo(stroke.points[0].x, stroke.points[0].y);
      for (let i = 1; i < stroke.points.length; i++) {
        ctx.lineTo(stroke.points[i].x, stroke.points[i].y);
      }
      ctx.stroke();
    });

    // Render active drawing
    if (isDrawingRef.current && currentPointsRef.current.length > 0) {
      ctx.beginPath();
      ctx.strokeStyle = isEraserRef.current ? (bgMode === "white" ? "#FBFCFA" : "#1D221B") : penColorRef.current;
      ctx.lineWidth = isEraserRef.current ? penThicknessRef.current * 4 : penThicknessRef.current;
      ctx.lineCap = "round";
      ctx.lineJoin = "round";

      ctx.moveTo(currentPointsRef.current[0].x, currentPointsRef.current[0].y);
      for (let i = 1; i < currentPointsRef.current.length; i++) {
        ctx.lineTo(currentPointsRef.current[i].x, currentPointsRef.current[i].y);
      }
      ctx.stroke();
    }
  };

  useEffect(() => {
    redraw();
    if (onInkChange) {
      onInkChange(strokes.length > 0);
    }
  }, [strokes, bgMode]);

  // Fit canvas sizes based on bounds resize observer
  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas || !containerRef.current) return;

    const updateSize = () => {
      const parent = containerRef.current;
      if (!parent) return;
      const rect = parent.getBoundingClientRect();
      
      // Keep backing scale high quality
      canvas.width = rect.width;
      canvas.height = Math.max(180, rect.height || 220);
      redraw();
    };

    updateSize();

    const resizeObserver = new ResizeObserver(() => {
      // Small debounce
      window.requestAnimationFrame(() => {
        updateSize();
      });
    });

    resizeObserver.observe(containerRef.current);

    return () => {
      resizeObserver.disconnect();
    };
  }, []);

  const getCoordinates = (event: React.PointerEvent<HTMLCanvasElement>) => {
    const canvas = canvasRef.current;
    if (!canvas) return { x: 0, y: 0 };
    const rect = canvas.getBoundingClientRect();
    return {
      x: event.clientX - rect.left,
      y: event.clientY - rect.top,
    };
  };

  const handlePointerDown = (event: React.PointerEvent<HTMLCanvasElement>) => {
    event.preventDefault();
    canvasRef.current?.setPointerCapture(event.pointerId);
    isDrawingRef.current = true;
    const coords = getCoordinates(event);
    
    currentPointsRef.current = [{
      x: coords.x,
      y: coords.y,
      pressure: event.pressure || 0.5,
      tilt: event.tiltX || 0,
      time: Date.now()
    }];

    redraw();
  };

  const handlePointerMove = (event: React.PointerEvent<HTMLCanvasElement>) => {
    if (!isDrawingRef.current) return;
    event.preventDefault();
    const coords = getCoordinates(event);

    currentPointsRef.current.push({
      x: coords.x,
      y: coords.y,
      pressure: event.pressure || 0.5,
      tilt: event.tiltX || 0,
      time: Date.now()
    });

    // Draw active line immediately for zero rendering lag
    const canvas = canvasRef.current;
    if (canvas) {
      const ctx = canvas.getContext("2d");
      if (ctx && currentPointsRef.current.length > 1) {
        const p1 = currentPointsRef.current[currentPointsRef.current.length - 2];
        const p2 = currentPointsRef.current[currentPointsRef.current.length - 1];
        ctx.beginPath();
        ctx.strokeStyle = isEraserRef.current ? (bgMode === "white" ? "#FBFCFA" : "#1D221B") : penColorRef.current;
        ctx.lineWidth = isEraserRef.current ? penThicknessRef.current * 4 : penThicknessRef.current;
        ctx.lineCap = "round";
        ctx.lineJoin = "round";
        ctx.moveTo(p1.x, p1.y);
        ctx.lineTo(p2.x, p2.y);
        ctx.stroke();
      }
    }
  };

  const handlePointerUp = (event: React.PointerEvent<HTMLCanvasElement>) => {
    if (!isDrawingRef.current) return;
    event.preventDefault();
    canvasRef.current?.releasePointerCapture(event.pointerId);
    isDrawingRef.current = false;

    if (currentPointsRef.current.length > 1) {
      const newStroke: InkStroke = {
        points: currentPointsRef.current,
        color: isEraserRef.current ? (bgMode === "white" ? "#FBFCFA" : "#1D221B") : penColorRef.current,
        thickness: isEraserRef.current ? penThicknessRef.current * 4 : penThicknessRef.current
      };
      setStrokes((prev) => [...prev, newStroke]);
      setRedoStack([]); // Clear redo
    }
    
    currentPointsRef.current = [];
    redraw();
  };

  // Expose API methods to parents
  useImperativeHandle(ref, () => ({
    clear: () => {
      setStrokes([]);
      setRedoStack([]);
      if (onInkChange) onInkChange(false);
    },
    getImgBase64: () => {
      const canvas = canvasRef.current;
      if (!canvas) return "";
      // Create a temporary canvas with actual solid white/dark background matching theme
      const tempCanvas = document.createElement("canvas");
      tempCanvas.width = canvas.width;
      tempCanvas.height = canvas.height;
      const tempCtx = tempCanvas.getContext("2d");
      if (!tempCtx) return canvas.toDataURL("image/png");

      // Draw background color Solid so OCR models don't struggle with transparency artifacts
      tempCtx.fillStyle = bgMode === "white" ? "#FFFFFF" : "#171B15";
      tempCtx.fillRect(0, 0, tempCanvas.width, tempCanvas.height);

      // Copy actual drawn imagery
      tempCtx.drawImage(canvas, 0, 0);
      return tempCanvas.toDataURL("image/png");
    },
    undo: () => {
      if (strokes.length === 0) return;
      const latest = strokes[strokes.length - 1];
      setStrokes((prev) => prev.slice(0, prev.length - 1));
      setRedoStack((prev) => [...prev, latest]);
    },
    redo: () => {
      if (redoStack.length === 0) return;
      const next = redoStack[redoStack.length - 1];
      setRedoStack((prev) => prev.slice(0, prev.length - 1));
      setStrokes((prev) => [...prev, next]);
    }
  }));

  return (
    <div
      ref={containerRef}
      className={`relative w-full h-full min-h-[180px] rounded-md overflow-hidden cursor-crosshair border border-dashed ${
        bgMode === "white" 
          ? "border-emerald-100 bg-[#FBFCFA] hover:border-emerald-300" 
          : "border-emerald-950 bg-[#1D221B] hover:border-emerald-800"
      } transition-colors duration-200`}
    >
      <canvas
        ref={canvasRef}
        onPointerDown={handlePointerDown}
        onPointerMove={handlePointerMove}
        onPointerUp={handlePointerUp}
        className="absolute top-0 left-0 w-full h-full touch-none"
      />
      {strokes.length === 0 && !isDrawingRef.current && (
        <div className="absolute inset-0 flex items-center justify-center pointer-events-none select-none">
          <span className={`text-xs ${bgMode === "white" ? "text-[#5F665C]/35" : "text-[#A9B2A3]/25"} font-mono`}>
            [ Escreva a resolução aqui com mouse ou stylus ]
          </span>
        </div>
      )}
    </div>
  );
});

InkCanvas.displayName = "InkCanvas";
