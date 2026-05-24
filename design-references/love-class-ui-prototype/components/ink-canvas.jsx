// InkCanvas — SVG-based stroke drawing.
// Strokes are smoothed with quadratic curves; pointer events captured.
// Exposes via window.InkCanvas. Stores its own state (strokes) and
// supports undo/redo + clear via an imperative handle on the props.
//
// Props:
//   color, width, mode ('pen' | 'eraser'), bg ('plain' | 'dot' | 'rule')
//   onStrokeStart(), onStrokeEnd()
//   demoSeed — optional initial strokes (array of {color, width, d})

const { useRef, useState, useEffect, useImperativeHandle, forwardRef, useCallback } = React;

function strokeToPath(points) {
  if (!points || points.length === 0) return '';
  if (points.length === 1) {
    const p = points[0];
    return `M${p.x},${p.y} l0.01,0`;
  }
  let d = `M${points[0].x},${points[0].y}`;
  for (let i = 1; i < points.length - 1; i++) {
    const p = points[i];
    const next = points[i + 1];
    const midX = (p.x + next.x) / 2;
    const midY = (p.y + next.y) / 2;
    d += ` Q${p.x},${p.y} ${midX},${midY}`;
  }
  const last = points[points.length - 1];
  d += ` L${last.x},${last.y}`;
  return d;
}

const InkCanvas = forwardRef(function InkCanvas(props, ref) {
  const {
    color = '#1a1a1a',
    width = 2.2,
    mode = 'pen',
    bg = 'plain',
    onStrokeStart,
    onStrokeEnd,
    demoSeed = null,
    enabled = true,
    style = {},
    className = '',
    eraserSize = 16,
  } = props;

  const [strokes, setStrokes] = useState(() => demoSeed ? [...demoSeed] : []);
  const [redoStack, setRedoStack] = useState([]);
  const [active, setActive] = useState(null); // {color, width, points}
  const svgRef = useRef(null);
  const isDrawing = useRef(false);

  useImperativeHandle(ref, () => ({
    undo: () => {
      setStrokes(prev => {
        if (prev.length === 0) return prev;
        const next = prev.slice(0, -1);
        setRedoStack(r => [...r, prev[prev.length - 1]]);
        return next;
      });
    },
    redo: () => {
      setRedoStack(r => {
        if (r.length === 0) return r;
        const last = r[r.length - 1];
        setStrokes(s => [...s, last]);
        return r.slice(0, -1);
      });
    },
    clear: () => { setStrokes([]); setRedoStack([]); },
    isEmpty: () => strokes.length === 0,
    strokeCount: () => strokes.length,
  }), [strokes]);

  const getPoint = useCallback((e) => {
    const svg = svgRef.current;
    if (!svg) return null;
    const r = svg.getBoundingClientRect();
    const t = e.touches ? e.touches[0] : e;
    return {
      x: ((t.clientX - r.left) / r.width) * 100,
      y: ((t.clientY - r.top) / r.height) * 100,
    };
  }, []);

  const handleDown = (e) => {
    if (!enabled) return;
    e.preventDefault();
    if (e.target.setPointerCapture && e.pointerId != null) {
      try { e.target.setPointerCapture(e.pointerId); } catch (_) {}
    }
    const pt = getPoint(e);
    if (!pt) return;
    isDrawing.current = true;
    setRedoStack([]);
    if (mode === 'eraser') {
      eraseAt(pt);
    } else {
      setActive({ color, width, points: [pt] });
      onStrokeStart && onStrokeStart();
    }
  };

  const handleMove = (e) => {
    if (!isDrawing.current || !enabled) return;
    const pt = getPoint(e);
    if (!pt) return;
    if (mode === 'eraser') {
      eraseAt(pt);
    } else {
      setActive(prev => prev ? { ...prev, points: [...prev.points, pt] } : prev);
    }
  };

  const handleUp = () => {
    if (!isDrawing.current) return;
    isDrawing.current = false;
    if (mode === 'pen' && active) {
      const finished = active;
      setActive(null);
      if (finished.points.length > 0) {
        setStrokes(prev => [...prev, finished]);
        onStrokeEnd && onStrokeEnd();
      }
    }
  };

  // Erase by intersection: drop any stroke whose any point is within eraserSize of pt.
  const eraseAt = (pt) => {
    setStrokes(prev => {
      const r = eraserSize / 2;
      const survivors = prev.filter(s => {
        return !s.points.some(p => {
          const dx = p.x - pt.x;
          const dy = p.y - pt.y;
          return Math.hypot(dx, dy) < r;
        });
      });
      return survivors;
    });
  };

  // Background pattern (very subtle)
  const bgPattern = bg === 'dot' ? (
    <defs>
      <pattern id={`dotgrid-${props.id || 'a'}`} width="2.5" height="2.5" patternUnits="userSpaceOnUse">
        <circle cx="1.25" cy="1.25" r="0.18" fill="var(--grid)" />
      </pattern>
    </defs>
  ) : bg === 'rule' ? (
    <defs>
      <pattern id={`rule-${props.id || 'a'}`} width="100" height="6" patternUnits="userSpaceOnUse">
        <line x1="0" y1="6" x2="100" y2="6" stroke="var(--grid)" strokeWidth="0.2" />
      </pattern>
    </defs>
  ) : null;

  return (
    <svg
      ref={svgRef}
      viewBox="0 0 100 100"
      preserveAspectRatio="none"
      className={className}
      style={{
        display: 'block', width: '100%', height: '100%',
        touchAction: 'none',
        cursor: enabled ? (mode === 'eraser' ? 'cell' : 'crosshair') : 'default',
        ...style
      }}
      onPointerDown={handleDown}
      onPointerMove={handleMove}
      onPointerUp={handleUp}
      onPointerLeave={handleUp}
      onPointerCancel={handleUp}
    >
      {bgPattern}
      {bg !== 'plain' && (
        <rect x="0" y="0" width="100" height="100" fill={`url(#${bg === 'dot' ? 'dotgrid' : 'rule'}-${props.id || 'a'})`} />
      )}
      {strokes.map((s, i) => (
        <path
          key={i}
          d={strokeToPath(s.points)}
          stroke={s.color}
          strokeWidth={s.width}
          fill="none"
          strokeLinecap="round"
          strokeLinejoin="round"
          vectorEffect="non-scaling-stroke"
        />
      ))}
      {active && (
        <path
          d={strokeToPath(active.points)}
          stroke={active.color}
          strokeWidth={active.width}
          fill="none"
          strokeLinecap="round"
          strokeLinejoin="round"
          vectorEffect="non-scaling-stroke"
        />
      )}
    </svg>
  );
});

window.InkCanvas = InkCanvas;
