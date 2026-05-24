// Shared icons + small primitives for Strava Matemática screens.
// Strokes use currentColor so they pick up the theme.

const SmIcon = {
  Pause: (p) => (
    <svg viewBox="0 0 20 20" width={p.size || 18} height={p.size || 18} fill="none" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round">
      <line x1="7" y1="5" x2="7" y2="15" />
      <line x1="13" y1="5" x2="13" y2="15" />
    </svg>
  ),
  Back: (p) => (
    <svg viewBox="0 0 20 20" width={p.size || 18} height={p.size || 18} fill="none" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round">
      <polyline points="11,4 5,10 11,16" />
      <line x1="5" y1="10" x2="16" y2="10" />
    </svg>
  ),
  Send: (p) => (
    <svg viewBox="0 0 20 20" width={p.size || 18} height={p.size || 18} fill="none" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round">
      <polyline points="4,10 9,15 17,5" />
    </svg>
  ),
  Pen: (p) => (
    <svg viewBox="0 0 20 20" width={p.size || 18} height={p.size || 18} fill="none" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round">
      <path d="M3 17l4-1L17 6l-3-3L4 13z" />
      <line x1="12" y1="5" x2="15" y2="8" />
    </svg>
  ),
  Eraser: (p) => (
    <svg viewBox="0 0 20 20" width={p.size || 18} height={p.size || 18} fill="none" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round">
      <path d="M3 14l6 3 8-8-6-6-8 8z" />
      <line x1="8" y1="6" x2="14" y2="12" />
    </svg>
  ),
  Undo: (p) => (
    <svg viewBox="0 0 20 20" width={p.size || 18} height={p.size || 18} fill="none" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round">
      <polyline points="6,5 3,8 6,11" />
      <path d="M3 8h9a4 4 0 0 1 0 8H8" />
    </svg>
  ),
  Redo: (p) => (
    <svg viewBox="0 0 20 20" width={p.size || 18} height={p.size || 18} fill="none" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round">
      <polyline points="14,5 17,8 14,11" />
      <path d="M17 8H8a4 4 0 0 0 0 8h4" />
    </svg>
  ),
  Trash: (p) => (
    <svg viewBox="0 0 20 20" width={p.size || 18} height={p.size || 18} fill="none" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round">
      <polyline points="4,6 16,6" />
      <path d="M6 6v10a1 1 0 0 0 1 1h6a1 1 0 0 0 1-1V6" />
      <path d="M8 6V4a1 1 0 0 1 1-1h2a1 1 0 0 1 1 1v2" />
    </svg>
  ),
  Check: (p) => (
    <svg viewBox="0 0 20 20" width={p.size || 16} height={p.size || 16} fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <polyline points="4,10 8,14 16,6" />
    </svg>
  ),
  X: (p) => (
    <svg viewBox="0 0 20 20" width={p.size || 16} height={p.size || 16} fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round">
      <line x1="5" y1="5" x2="15" y2="15" />
      <line x1="15" y1="5" x2="5" y2="15" />
    </svg>
  ),
  Alert: (p) => (
    <svg viewBox="0 0 20 20" width={p.size || 16} height={p.size || 16} fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
      <line x1="10" y1="6" x2="10" y2="11" />
      <line x1="10" y1="14" x2="10" y2="14.1" />
      <circle cx="10" cy="10" r="7.5" />
    </svg>
  ),
  ArrowRight: (p) => (
    <svg viewBox="0 0 20 20" width={p.size || 18} height={p.size || 18} fill="none" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round">
      <line x1="4" y1="10" x2="15" y2="10" />
      <polyline points="11,6 15,10 11,14" />
    </svg>
  ),
  Dot: (p) => (
    <svg viewBox="0 0 8 8" width={p.size || 8} height={p.size || 8}>
      <circle cx="4" cy="4" r="3" fill="currentColor" />
    </svg>
  ),
};

// Thermometer — thin horizontal bar with a tick + value.
function Thermometer({ value = 0.6, compact = false, blind = false }) {
  if (blind) return null;
  const clamped = Math.max(0, Math.min(1, value));
  const color = clamped < 0.45 ? 'var(--error)' : clamped < 0.7 ? 'var(--warning)' : 'var(--progress)';
  return (
    <div style={{ display: 'flex', alignItems: 'center', gap: 8, width: compact ? 100 : 120 }}>
      <div style={{
        flex: 1, height: 6, background: 'var(--field)', border: '1px solid var(--hairline)',
        borderRadius: 999, overflow: 'hidden', position: 'relative'
      }}>
        <div style={{
          position: 'absolute', left: 0, top: 0, bottom: 0,
          width: `${clamped * 100}%`,
          background: color,
          transition: 'width 250ms ease',
        }} />
      </div>
      <span className="mono" style={{ fontSize: 11, color: 'var(--ink-2)', minWidth: 24, textAlign: 'right' }}>
        {(clamped * 100).toFixed(0)}
      </span>
    </div>
  );
}

// Icon button with hairline border
function IconButton({ children, onClick, active, title, style, size = 40 }) {
  return (
    <button
      onClick={onClick}
      title={title}
      aria-label={title}
      style={{
        width: size, height: size,
        background: active ? 'var(--ink)' : 'transparent',
        color: active ? 'var(--surface)' : 'var(--ink)',
        border: 0,
        borderRadius: 6,
        display: 'flex', alignItems: 'center', justifyContent: 'center',
        cursor: 'pointer',
        transition: 'background 0.12s, color 0.12s',
        ...style,
      }}
      onMouseEnter={(e) => { if (!active) e.currentTarget.style.background = 'var(--field-hover)'; }}
      onMouseLeave={(e) => { if (!active) e.currentTarget.style.background = 'transparent'; }}
    >
      {children}
    </button>
  );
}

window.SmIcon = SmIcon;
window.Thermometer = Thermometer;
window.IconButton = IconButton;
