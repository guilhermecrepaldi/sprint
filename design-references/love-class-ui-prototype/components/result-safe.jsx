// PageResultScreen — Safe variation.
// Sober vertical list of result rows + cognitive metrics row + actions.

function MetricBar({ label, value, max = 1, color = 'var(--ink)', sub }) {
  const pct = Math.max(0, Math.min(1, value / max));
  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 6, minWidth: 0 }}>
      <div style={{ display: 'flex', alignItems: 'baseline', justifyContent: 'space-between', gap: 8 }}>
        <span className="eyebrow" style={{ fontSize: 9 }}>{label}</span>
        <span className="mono" style={{ fontSize: 14, fontWeight: 600, color: 'var(--ink)' }}>{sub || `${(value * 100).toFixed(0)}`}</span>
      </div>
      <div style={{
        height: 4, background: 'var(--field)', border: '1px solid var(--hairline)',
        borderRadius: 999, overflow: 'hidden',
      }}>
        <div style={{ width: `${pct * 100}%`, height: '100%', background: color }} />
      </div>
    </div>
  );
}

function ResultRowSafe({ r, ex }) {
  const isOcrFail = r.ocrFailed;
  return (
    <div style={{
      display: 'grid',
      gridTemplateColumns: '40px 1fr 110px 30px',
      gap: 12,
      alignItems: 'center',
      padding: '14px 16px',
      background: 'var(--surface)',
      border: '1px solid var(--hairline)',
      borderRadius: 8,
    }}>
      {/* Field number */}
      <span className="mono" style={{ fontSize: 13, fontWeight: 600, color: 'var(--ink-2)' }}>
        {String(r.fieldIndex).padStart(2, '0')}
      </span>

      {/* Statement + answers */}
      <div style={{ minWidth: 0 }}>
        <div className="mono" style={{ fontSize: 14, color: 'var(--ink)', marginBottom: 4 }}>
          {ex.statement}
        </div>
        <div style={{ display: 'flex', alignItems: 'baseline', gap: 12, flexWrap: 'wrap' }}>
          <div style={{ display: 'flex', alignItems: 'baseline', gap: 6 }}>
            <span className="eyebrow" style={{ fontSize: 9 }}>Resp.</span>
            <span className="mono" style={{
              fontSize: 13,
              color: isOcrFail ? 'var(--warning)' : r.isCorrect ? 'var(--ink)' : 'var(--error)',
              textDecoration: !r.isCorrect && !isOcrFail ? 'line-through' : 'none',
              textDecorationColor: 'var(--ink-3)',
            }}>
              {isOcrFail ? 'Não consegui ler' : r.recognized}
            </span>
          </div>
          {!r.isCorrect && (
            <div style={{ display: 'flex', alignItems: 'baseline', gap: 6 }}>
              <span className="eyebrow" style={{ fontSize: 9 }}>Esperada</span>
              <span className="mono" style={{ fontSize: 13, color: 'var(--progress)', fontWeight: 600 }}>
                {r.expected}
              </span>
            </div>
          )}
          {r.errorType && r.errorType !== 'desconhecido' && (
            <span className="mono" style={{
              fontSize: 10, padding: '2px 6px',
              background: 'var(--field)', border: '1px solid var(--hairline)',
              borderRadius: 3, color: 'var(--ink-2)',
              textTransform: 'uppercase', letterSpacing: '0.08em',
            }}>
              {r.errorType}
            </span>
          )}
        </div>
      </div>

      {/* Time */}
      <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'flex-end', gap: 2 }}>
        <span className="eyebrow" style={{ fontSize: 9 }}>Tempo</span>
        <span className="mono" style={{ fontSize: 13, color: 'var(--ink-2)' }}>
          {(r.timeMs / 1000).toFixed(1)}s
        </span>
      </div>

      {/* Status icon */}
      <div style={{ display: 'flex', justifyContent: 'flex-end' }}>
        {isOcrFail ? (
          <div style={{ color: 'var(--warning)' }}><SmIcon.Alert size={18} /></div>
        ) : r.isCorrect ? (
          <div style={{
            width: 24, height: 24, borderRadius: 999,
            background: 'var(--progress)', color: 'var(--surface)',
            display: 'flex', alignItems: 'center', justifyContent: 'center',
          }}>
            <SmIcon.Check size={14} />
          </div>
        ) : (
          <div style={{
            width: 24, height: 24, borderRadius: 999,
            background: 'var(--error)', color: 'var(--surface)',
            display: 'flex', alignItems: 'center', justifyContent: 'center',
          }}>
            <SmIcon.X size={12} />
          </div>
        )}
      </div>
    </div>
  );
}

function PageResultScreenSafe({
  theme = 'white',
  exercises = window.SAMPLE_EXERCISES,
  results = window.SAMPLE_RESULTS,
  pageIndex = 1,
  pagesTotal = 10,
  showThermometer = true,
  onNext,
}) {
  const correct = results.filter(r => r.isCorrect).length;
  const total = results.length;
  const score = correct / total;
  const totalTime = results.reduce((acc, r) => acc + r.timeMs, 0);
  const avgTimeS = totalTime / total / 1000;

  return (
    <div className="sm-screen" data-theme={theme} data-screen-label={`PageResultScreen Safe (${theme})`}>
      {/* Header */}
      <div style={{
        padding: '20px 24px 16px',
        background: 'var(--surface)',
        borderBottom: '1px solid var(--hairline)',
      }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginBottom: 12 }}>
          <IconButton title="Voltar" size={32}><SmIcon.Back size={16} /></IconButton>
          <div style={{ display: 'flex', alignItems: 'baseline', gap: 8 }}>
            <span className="eyebrow" style={{ fontSize: 10 }}>Página concluída</span>
            <span className="mono" style={{ fontSize: 14, fontWeight: 600 }}>
              {String(pageIndex).padStart(2, '0')} / {String(pagesTotal).padStart(2, '0')}
            </span>
          </div>
          <div style={{ flex: 1 }} />
          {showThermometer && <Thermometer value={score} compact />}
        </div>

        <div style={{ display: 'flex', alignItems: 'baseline', gap: 20 }}>
          <div style={{ display: 'flex', flexDirection: 'column', lineHeight: 1 }}>
            <span className="eyebrow" style={{ fontSize: 10, marginBottom: 4 }}>Acerto</span>
            <div style={{ display: 'flex', alignItems: 'baseline', gap: 4 }}>
              <span className="mono" style={{ fontSize: 36, fontWeight: 600, letterSpacing: '-0.02em' }}>
                {correct}
              </span>
              <span className="mono" style={{ fontSize: 18, color: 'var(--ink-3)' }}>
                /{total}
              </span>
            </div>
          </div>
          <div style={{ width: 1, height: 36, background: 'var(--hairline)' }} />
          <div style={{ display: 'flex', flexDirection: 'column', lineHeight: 1 }}>
            <span className="eyebrow" style={{ fontSize: 10, marginBottom: 4 }}>Score</span>
            <span className="mono" style={{ fontSize: 36, fontWeight: 600, letterSpacing: '-0.02em' }}>
              {(score * 100).toFixed(0)}<span style={{ fontSize: 18, color: 'var(--ink-3)' }}>%</span>
            </span>
          </div>
          <div style={{ width: 1, height: 36, background: 'var(--hairline)' }} />
          <div style={{ display: 'flex', flexDirection: 'column', lineHeight: 1 }}>
            <span className="eyebrow" style={{ fontSize: 10, marginBottom: 4 }}>Tempo médio</span>
            <span className="mono" style={{ fontSize: 36, fontWeight: 600, letterSpacing: '-0.02em' }}>
              {avgTimeS.toFixed(1)}<span style={{ fontSize: 18, color: 'var(--ink-3)' }}>s</span>
            </span>
          </div>
        </div>
      </div>

      {/* Body: result list + cognitive metrics */}
      <div className="no-scrollbar" style={{
        position: 'absolute', top: 170, bottom: 80, left: 0, right: 0,
        overflowY: 'auto',
        padding: '16px 20px',
        display: 'flex', flexDirection: 'column', gap: 16,
      }}>
        <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
          {results.map(r => (
            <ResultRowSafe key={r.fieldIndex} r={r} ex={exercises[r.fieldIndex - 1]} />
          ))}
        </div>

        {/* Cognitive metrics */}
        <div style={{
          marginTop: 4,
          padding: '14px 16px',
          background: 'var(--surface)',
          border: '1px solid var(--hairline)',
          borderRadius: 8,
        }}>
          <span className="eyebrow" style={{ fontSize: 10, display: 'block', marginBottom: 12 }}>
            Diagnóstico cognitivo
          </span>
          <div style={{
            display: 'grid', gridTemplateColumns: '1fr 1fr 1fr 1fr', gap: 24,
          }}>
            <MetricBar label="Acerto" value={score} color="var(--progress)" sub={`${(score * 100).toFixed(0)}%`} />
            <MetricBar label="Velocidade" value={0.72} color="var(--info)" />
            <MetricBar label="Fluidez" value={0.81} color="var(--ink)" />
            <MetricBar label="Hesitação" value={0.34} color="var(--warning)" />
          </div>
        </div>
      </div>

      {/* Bottom action bar */}
      <div style={{
        position: 'absolute', bottom: 0, left: 0, right: 0,
        height: 80, background: 'var(--surface)',
        borderTop: '1px solid var(--hairline)',
        padding: '0 20px',
        display: 'flex', alignItems: 'center', gap: 12,
      }}>
        <button className="sm-btn" style={{ height: 48 }}>
          Revisar erros
        </button>
        <div style={{ flex: 1 }} />
        <button className="sm-btn sm-btn--primary" style={{ height: 48, padding: '0 22px' }} onClick={onNext}>
          Próxima folha
          <SmIcon.ArrowRight size={16} />
        </button>
      </div>
    </div>
  );
}

window.PageResultScreenSafe = PageResultScreenSafe;
window.MetricBar = MetricBar;
