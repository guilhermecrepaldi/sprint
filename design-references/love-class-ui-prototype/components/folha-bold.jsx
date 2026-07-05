// FolhaScreen — Bold variation.
// Editorial: huge mono numbers in left gutter, full-bleed ledger rules,
// right-side vertical ink rail, top page-strip progress.

function FieldRowBold({ ex, isActive, onActivate, ink, inkRef, penColor, penWidth, tool, enabled }) {
  return (
    <div
      data-field-bounds={ex.index}
      onPointerDown={onActivate}
      style={{
        position: 'relative',
        display: 'grid',
        gridTemplateColumns: '92px 1fr',
        gap: 0,
        borderBottom: '1px solid var(--hairline)',
        background: isActive ? 'var(--surface)' : 'transparent',
        transition: 'background 120ms',
      }}
    >
      {/* Active left accent rule */}
      {isActive && (
        <div style={{
          position: 'absolute', left: 0, top: 0, bottom: 0, width: 3,
          background: 'var(--progress)',
        }} />
      )}

      {/* Left gutter — huge number + label + statement */}
      <div style={{
        padding: '20px 14px 16px 18px',
        display: 'flex', flexDirection: 'column',
        gap: 4,
        borderRight: '1px solid var(--hairline-2)',
      }}>
        <span
          className="mono"
          style={{
            fontSize: 44, lineHeight: 1, fontWeight: 500,
            color: isActive ? 'var(--ink)' : 'var(--ink-3)',
            letterSpacing: '-0.02em',
          }}
        >
          {String(ex.index).padStart(2, '0')}
        </span>
        <span className="eyebrow" style={{ fontSize: 9 }}>{ex.label}</span>
      </div>

      {/* Right content */}
      <div style={{ padding: '14px 18px 14px 18px', display: 'flex', flexDirection: 'column', gap: 8, minHeight: 200 }}>
        {/* Statement */}
        <div className="mono" style={{
          fontSize: 18, color: 'var(--ink)', fontWeight: 500, letterSpacing: '-0.005em',
        }}>
          {ex.statement}
        </div>

        {/* Writing area — ruled */}
        <div style={{
          position: 'relative',
          flex: 1,
          minHeight: 110,
          background: `repeating-linear-gradient(to bottom, transparent, transparent 27px, var(--grid) 27px, var(--grid) 28px)`,
        }}>
          <InkCanvas
            id={`bold-${ex.index}`}
            ref={inkRef}
            color={penColor}
            width={penWidth}
            mode={tool}
            bg="plain"
            demoSeed={ink}
            enabled={enabled}
          />
        </div>

        {/* Resposta final */}
        <div style={{ display: 'flex', alignItems: 'center', gap: 12, paddingTop: 4 }}>
          <span className="eyebrow" style={{ fontSize: 9 }}>=</span>
          <div style={{ flex: 1, borderBottom: '1.5px solid var(--ink-3)', height: 18 }} />
        </div>
      </div>
    </div>
  );
}

function FolhaScreenBold({
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

  // Indicate which fields have ink (for the top progress strip)
  const fieldFilled = exercises.map(ex => {
    const seed = initialInk ? initialInk[ex.index] : null;
    return seed && seed.length > 0;
  });

  return (
    <div className="sm-screen" data-theme={theme} data-screen-label={`FolhaScreen Bold (${theme})`}>
      {/* Top training bar — taller, editorial */}
      <div style={{
        position: 'relative',
        padding: '12px 16px 10px 16px',
        background: 'var(--surface)',
        borderBottom: '1px solid var(--hairline)',
        display: 'flex', flexDirection: 'column', gap: 8,
      }}>
        {/* Row 1: page + difficulty + timer + send */}
        <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
          <IconButton title="Pausar" size={32}><SmIcon.Pause size={16} /></IconButton>

          <div style={{ display: 'flex', flexDirection: 'column', lineHeight: 1, marginLeft: 2 }}>
            <span className="eyebrow" style={{ fontSize: 9 }}>Página</span>
            <div style={{ display: 'flex', alignItems: 'baseline', gap: 4, marginTop: 2 }}>
              <span className="mono" style={{ fontSize: 22, fontWeight: 600, letterSpacing: '-0.02em' }}>
                {String(pageIndex).padStart(2, '0')}
              </span>
              <span className="mono" style={{ fontSize: 12, color: 'var(--ink-3)' }}>
                /{String(pagesTotal).padStart(2, '0')}
              </span>
            </div>
          </div>

          <div style={{ width: 1, height: 28, background: 'var(--hairline)', marginLeft: 6 }} />

          <div style={{ display: 'flex', flexDirection: 'column', lineHeight: 1, marginLeft: 4 }}>
            <span className="eyebrow" style={{ fontSize: 9 }}>Dificuldade</span>
            <span className="mono" style={{ fontSize: 18, fontWeight: 500, marginTop: 4 }}>
              {difficulty.toFixed(1)}
            </span>
          </div>

          <div style={{ flex: 1 }} />

          {showThermometer && (
            <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'flex-end', lineHeight: 1, gap: 4 }}>
              <span className="eyebrow" style={{ fontSize: 9 }}>Termômetro</span>
              <Thermometer value={thermometerValue} compact />
            </div>
          )}

          <div style={{ width: 1, height: 28, background: 'var(--hairline)' }} />

          <div style={{ display: 'flex', flexDirection: 'column', lineHeight: 1, alignItems: 'flex-end' }}>
            <span className="eyebrow" style={{ fontSize: 9 }}>Tempo</span>
            <span className="mono" style={{ fontSize: 18, fontWeight: 500, marginTop: 4, letterSpacing: '0.01em' }}>
              {elapsed}
            </span>
          </div>

          <button className="sm-btn sm-btn--primary" style={{ height: 40, padding: '0 16px', marginLeft: 4 }}>
            <SmIcon.Send size={16} />
            Enviar
          </button>
        </div>

        {/* Row 2: field progress dots */}
        <div style={{ display: 'flex', alignItems: 'center', gap: 6, marginTop: 2 }}>
          <span className="eyebrow" style={{ fontSize: 9, marginRight: 4 }}>Folha</span>
          {exercises.map((ex, i) => (
            <div key={ex.id} style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
              <button
                onClick={() => setActiveField(ex.index)}
                style={{
                  padding: 0, border: 0, background: 'transparent', cursor: 'pointer',
                  display: 'flex', alignItems: 'center', gap: 4,
                }}
              >
                <span className="mono" style={{
                  fontSize: 10,
                  color: activeField === ex.index ? 'var(--ink)' : 'var(--ink-3)',
                  fontWeight: activeField === ex.index ? 600 : 400,
                }}>
                  {String(ex.index).padStart(2, '0')}
                </span>
                <div style={{
                  width: 36, height: 4, borderRadius: 2,
                  background: activeField === ex.index
                    ? 'var(--ink)'
                    : (fieldFilled[i] ? 'var(--ink-3)' : 'var(--hairline)'),
                }} />
              </button>
            </div>
          ))}
        </div>
      </div>

      {/* Main area: scrollable fields + right rail */}
      <div style={{ position: 'absolute', top: 102, bottom: 0, left: 0, right: 0, display: 'flex' }}>
        <div className="no-scrollbar" style={{
          flex: 1, overflowY: 'auto',
        }}>
          {exercises.map(ex => (
            <FieldRowBold
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

        {/* Right vertical rail */}
        <div style={{
          width: 60,
          borderLeft: '1px solid var(--hairline)',
          background: 'var(--surface)',
          display: 'flex', flexDirection: 'column', alignItems: 'center', padding: '12px 0',
          gap: 4,
        }}>
          <IconButton title="Caneta" active={tool === 'pen'} onClick={() => setTool('pen')}>
            <SmIcon.Pen />
          </IconButton>
          <IconButton title="Borracha" active={tool === 'eraser'} onClick={() => setTool('eraser')}>
            <SmIcon.Eraser />
          </IconButton>

          <div style={{ width: 24, height: 1, background: 'var(--hairline)', margin: '6px 0' }} />

          <IconButton title="Desfazer" onClick={() => activeRef()?.undo()}><SmIcon.Undo /></IconButton>
          <IconButton title="Refazer" onClick={() => activeRef()?.redo()}><SmIcon.Redo /></IconButton>
          <IconButton title="Limpar" onClick={() => activeRef()?.clear()}><SmIcon.Trash /></IconButton>

          <div style={{ width: 24, height: 1, background: 'var(--hairline)', margin: '6px 0' }} />

          <div style={{ display: 'flex', flexDirection: 'column', gap: 6, padding: '4px 0' }}>
            {PEN_COLORS.map(c => (
              <button
                key={c.id}
                onClick={() => setPenColor(c.value)}
                title={c.label}
                style={{
                  width: 22, height: 22, borderRadius: 999,
                  background: c.value,
                  border: penColor === c.value ? '2px solid var(--ink)' : '1px solid var(--hairline)',
                  outline: penColor === c.value ? '2px solid var(--bg)' : 'none',
                  outlineOffset: -4,
                  cursor: 'pointer', padding: 0,
                }}
              />
            ))}
          </div>

          <div style={{ width: 24, height: 1, background: 'var(--hairline)', margin: '6px 0' }} />

          {[1.4, 2.2, 3.0, 4.0].map(w => (
            <button
              key={w}
              onClick={() => setPenWidth(w)}
              title={`${w}dp`}
              style={{
                width: 32, height: 22, borderRadius: 4,
                background: penWidth === w ? 'var(--field-hover)' : 'transparent',
                border: 0, padding: 0, cursor: 'pointer',
                display: 'flex', alignItems: 'center', justifyContent: 'center',
              }}
            >
              <div style={{ width: 18, height: w * 1.4, borderRadius: 999, background: 'var(--ink)' }} />
            </button>
          ))}

          <div style={{ flex: 1 }} />

          <div className="mono" style={{
            writingMode: 'vertical-rl', transform: 'rotate(180deg)',
            fontSize: 9, color: 'var(--ink-3)', letterSpacing: '0.15em', textTransform: 'uppercase',
            padding: '8px 0',
          }}>
            Campo {String(activeField).padStart(2, '0')}
          </div>
        </div>
      </div>
    </div>
  );
}

window.FolhaScreenBold = FolhaScreenBold;
