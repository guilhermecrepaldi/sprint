// FolhaScreen — Safe variation.
// Hairline-bordered, calm, by-the-book interpretation of the spec.
// Top training bar, vertical list of ExerciseFields, bottom ink toolbar.

const PEN_COLORS = [
  { id: 'black', value: '#1a1a1a', label: 'Preto' },
  { id: 'green', value: '#1E7F5C', label: 'Verde' },
  { id: 'blue',  value: '#2F6F9F', label: 'Azul' },
  { id: 'red',   value: '#C2413A', label: 'Vermelho' },
];

function ExerciseFieldSafe({ ex, isActive, onActivate, ink, inkRef, penColor, penWidth, tool, enabled }) {
  return (
    <div
      data-field-bounds={ex.index}
      onPointerDown={onActivate}
      style={{
        position: 'relative',
        background: 'var(--surface)',
        border: `1px solid ${isActive ? 'var(--ink)' : 'var(--hairline)'}`,
        borderRadius: 8,
        padding: '14px 16px 12px',
        minHeight: 190,
        display: 'flex',
        flexDirection: 'column',
        gap: 8,
        transition: 'border-color 120ms',
      }}
    >
      {/* Header: number + label + statement */}
      <div style={{ display: 'flex', alignItems: 'baseline', gap: 12 }}>
        <span
          className="mono"
          style={{
            fontSize: 12, fontWeight: 600, color: 'var(--ink-2)',
            letterSpacing: '0.04em',
          }}
        >
          {String(ex.index).padStart(2, '0')}
        </span>
        <span className="eyebrow" style={{ fontSize: 10 }}>{ex.label}</span>
        <span className="mono" style={{ fontSize: 17, color: 'var(--ink)', marginLeft: 'auto' }}>
          {ex.statement}
        </span>
      </div>

      {/* Writing area */}
      <div style={{
        position: 'relative',
        flex: 1,
        background: 'var(--field)',
        border: '1px dashed var(--hairline)',
        borderRadius: 4,
        minHeight: 110,
        overflow: 'hidden',
      }}>
        <InkCanvas
          id={`safe-${ex.index}`}
          ref={inkRef}
          color={penColor}
          width={penWidth}
          mode={tool}
          bg="dot"
          demoSeed={ink}
          enabled={enabled}
        />
      </div>

      {/* Resposta final line */}
      <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
        <span className="eyebrow" style={{ fontSize: 10 }}>Resposta final</span>
        <div style={{ flex: 1, borderBottom: '1px solid var(--hairline)', height: 18 }} />
      </div>
    </div>
  );
}

function FolhaScreenSafe({
  theme = 'white',
  exercises = window.SAMPLE_EXERCISES,
  initialInk = null,
  showThermometer = true,
  thermometerValue = 0.62,
  pageIndex = 1,
  pagesTotal = 10,
  difficulty = 2.0,
  elapsed = '14:32',
  drawable = false,
}) {
  const [activeField, setActiveField] = useState(1);
  const [tool, setTool] = useState('pen');
  const [penColor, setPenColor] = useState('#1a1a1a');
  const [penWidth, setPenWidth] = useState(2.2);
  const inkRefs = useRef({});

  const activeRef = () => inkRefs.current[activeField];

  return (
    <div className="sm-screen" data-theme={theme} data-screen-label={`FolhaScreen Safe (${theme})`}>
      {/* Top training bar */}
      <div style={{
        height: 56, display: 'flex', alignItems: 'center',
        padding: '0 12px', gap: 8,
        borderBottom: '1px solid var(--hairline)',
        background: 'var(--surface)',
      }}>
        <IconButton title="Pausar"><SmIcon.Pause /></IconButton>

        <div style={{ display: 'flex', alignItems: 'baseline', gap: 8, marginLeft: 4 }}>
          <span className="eyebrow" style={{ fontSize: 10 }}>Página</span>
          <span className="mono" style={{ fontSize: 16, fontWeight: 600, letterSpacing: '0.02em' }}>
            {String(pageIndex).padStart(2, '0')}
          </span>
          <span className="mono" style={{ fontSize: 12, color: 'var(--ink-3)' }}>
            / {String(pagesTotal).padStart(2, '0')}
          </span>
        </div>

        <div style={{ width: 1, height: 20, background: 'var(--hairline)', margin: '0 6px' }} />

        <div style={{ display: 'flex', alignItems: 'baseline', gap: 6 }}>
          <span className="eyebrow" style={{ fontSize: 10 }}>Dif.</span>
          <span className="mono" style={{ fontSize: 14, fontWeight: 600 }}>{difficulty.toFixed(1)}</span>
        </div>

        <div style={{ flex: 1 }} />

        {showThermometer && (
          <Thermometer value={thermometerValue} compact />
        )}

        <div style={{ width: 1, height: 20, background: 'var(--hairline)', margin: '0 6px' }} />

        <span className="mono" style={{ fontSize: 16, fontWeight: 500, letterSpacing: '0.02em' }}>
          {elapsed}
        </span>

        <button className="sm-btn sm-btn--primary" style={{ marginLeft: 6, height: 36, padding: '0 14px' }}>
          <SmIcon.Send size={16} />
          Enviar
        </button>
      </div>

      {/* Body: scrollable list of fields */}
      <div className="no-scrollbar" style={{
        position: 'absolute', top: 56, bottom: 64, left: 0, right: 0,
        overflowY: 'auto',
        padding: '16px 20px',
        display: 'flex', flexDirection: 'column', gap: 12,
      }}>
        {exercises.map(ex => (
          <ExerciseFieldSafe
            key={ex.id}
            ex={ex}
            isActive={activeField === ex.index}
            onActivate={() => setActiveField(ex.index)}
            ink={initialInk ? initialInk[ex.index] : null}
            inkRef={(el) => { if (el) inkRefs.current[ex.index] = el; }}
            penColor={penColor}
            penWidth={penWidth}
            tool={tool}
            enabled={drawable && activeField === ex.index}
          />
        ))}
      </div>

      {/* Bottom ink toolbar */}
      <div style={{
        position: 'absolute', bottom: 0, left: 0, right: 0,
        height: 64,
        background: 'var(--surface)',
        borderTop: '1px solid var(--hairline)',
        display: 'flex', alignItems: 'center', padding: '0 12px', gap: 4,
      }}>
        <IconButton title="Caneta" active={tool === 'pen'} onClick={() => setTool('pen')}>
          <SmIcon.Pen />
        </IconButton>
        <IconButton title="Borracha" active={tool === 'eraser'} onClick={() => setTool('eraser')}>
          <SmIcon.Eraser />
        </IconButton>

        <div style={{ width: 1, height: 24, background: 'var(--hairline)', margin: '0 6px' }} />

        <IconButton title="Desfazer" onClick={() => activeRef()?.undo()}>
          <SmIcon.Undo />
        </IconButton>
        <IconButton title="Refazer" onClick={() => activeRef()?.redo()}>
          <SmIcon.Redo />
        </IconButton>
        <IconButton title="Limpar campo" onClick={() => activeRef()?.clear()}>
          <SmIcon.Trash />
        </IconButton>

        <div style={{ width: 1, height: 24, background: 'var(--hairline)', margin: '0 6px' }} />

        {/* Color swatches */}
        <div style={{ display: 'flex', gap: 4, padding: '0 4px' }}>
          {PEN_COLORS.map(c => (
            <button
              key={c.id}
              onClick={() => setPenColor(c.value)}
              title={c.label}
              style={{
                width: 22, height: 22, borderRadius: 999,
                background: c.value,
                border: penColor === c.value
                  ? `2px solid var(--ink)`
                  : `1px solid var(--hairline)`,
                outline: penColor === c.value ? '2px solid var(--bg)' : 'none',
                outlineOffset: -4,
                cursor: 'pointer',
                padding: 0,
              }}
            />
          ))}
        </div>

        <div style={{ width: 1, height: 24, background: 'var(--hairline)', margin: '0 6px' }} />

        {/* Thickness */}
        <div style={{ display: 'flex', alignItems: 'center', gap: 8, padding: '0 4px' }}>
          <span className="eyebrow" style={{ fontSize: 10 }}>Esp.</span>
          {[1.4, 2.2, 3.0, 4.0].map(w => (
            <button
              key={w}
              onClick={() => setPenWidth(w)}
              title={`${w}dp`}
              style={{
                width: 24, height: 24, borderRadius: 6,
                background: penWidth === w ? 'var(--field-hover)' : 'transparent',
                border: 0, padding: 0, cursor: 'pointer',
                display: 'flex', alignItems: 'center', justifyContent: 'center',
              }}
            >
              <div style={{
                width: w * 3, height: w * 1.2, borderRadius: 999, background: 'var(--ink)',
              }} />
            </button>
          ))}
        </div>

        <div style={{ flex: 1 }} />

        <span className="eyebrow" style={{ fontSize: 10, marginRight: 8 }}>
          Campo {String(activeField).padStart(2, '0')} ativo
        </span>
      </div>
    </div>
  );
}

window.FolhaScreenSafe = FolhaScreenSafe;
window.PEN_COLORS = PEN_COLORS;
