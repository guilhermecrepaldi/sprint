// PageResultScreen — Bold variation.
// Editorial ledger. Huge score moment, full-bleed result rows, cognitive panel below.

function ResultRowBold({ r, ex }) {
  const isOcrFail = r.ocrFailed;
  const statusColor = isOcrFail ? 'var(--warning)' : r.isCorrect ? 'var(--progress)' : 'var(--error)';
  const statusLabel = isOcrFail ? 'Ilegível' : r.isCorrect ? 'Correto' : 'Errado';
  return (
    <div style={{
      display: 'grid',
      gridTemplateColumns: '64px 1fr',
      borderBottom: '1px solid var(--hairline)',
      position: 'relative',
    }}>
      {/* Status accent bar */}
      <div style={{
        position: 'absolute', left: 0, top: 0, bottom: 0, width: 3,
        background: statusColor,
      }} />

      {/* Left gutter — number */}
      <div style={{
        padding: '18px 12px 18px 18px',
        borderRight: '1px solid var(--hairline-2)',
      }}>
        <span className="mono" style={{
          fontSize: 28, lineHeight: 1, fontWeight: 500,
          color: r.isCorrect ? 'var(--ink)' : 'var(--ink-3)',
          letterSpacing: '-0.02em',
        }}>
          {String(r.fieldIndex).padStart(2, '0')}
        </span>
      </div>

      {/* Right content */}
      <div style={{ padding: '14px 18px', display: 'flex', flexDirection: 'column', gap: 6 }}>
        <div style={{ display: 'flex', alignItems: 'baseline', justifyContent: 'space-between', gap: 12 }}>
          <span className="eyebrow" style={{ fontSize: 9 }}>{ex.label}</span>
          <span className="mono" style={{
            fontSize: 9, fontWeight: 600,
            color: statusColor, textTransform: 'uppercase', letterSpacing: '0.12em',
          }}>
            {statusLabel}
          </span>
        </div>

        <div className="mono" style={{ fontSize: 16, color: 'var(--ink)', fontWeight: 500 }}>
          {ex.statement}
        </div>

        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 80px', gap: 12, alignItems: 'baseline', marginTop: 4 }}>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
            <span className="eyebrow" style={{ fontSize: 9 }}>Você escreveu</span>
            <span className="mono" style={{
              fontSize: 14,
              color: isOcrFail ? 'var(--warning)' : r.isCorrect ? 'var(--ink)' : 'var(--error)',
            }}>
              {isOcrFail ? '— ilegível —' : r.recognized}
            </span>
          </div>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
            <span className="eyebrow" style={{ fontSize: 9 }}>Esperada</span>
            <span className="mono" style={{
              fontSize: 14,
              color: r.isCorrect ? 'var(--ink-2)' : 'var(--progress)',
              fontWeight: r.isCorrect ? 400 : 600,
            }}>
              {r.expected}
            </span>
          </div>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 2, alignItems: 'flex-end' }}>
            <span className="eyebrow" style={{ fontSize: 9 }}>Tempo</span>
            <span className="mono" style={{ fontSize: 14, color: 'var(--ink-2)' }}>
              {(r.timeMs / 1000).toFixed(1)}s
            </span>
          </div>
        </div>

        {/* Footer: skill chip + error type */}
        <div style={{ display: 'flex', gap: 6, marginTop: 6 }}>
          <span className="mono" style={{
            fontSize: 9, padding: '2px 7px',
            background: 'var(--field)', border: '1px solid var(--hairline)',
            borderRadius: 3, color: 'var(--ink-2)',
            textTransform: 'uppercase', letterSpacing: '0.1em',
          }}>
            {ex.skill}
          </span>
          {r.errorType && r.errorType !== 'desconhecido' && (
            <span className="mono" style={{
              fontSize: 9, padding: '2px 7px',
              background: 'var(--error)', color: 'var(--surface)',
              borderRadius: 3,
              textTransform: 'uppercase', letterSpacing: '0.1em', fontWeight: 600,
            }}>
              {r.errorType}
            </span>
          )}
        </div>
      </div>
    </div>
  );
}

// Score sparkline — last N pages
function ScoreSpark({ values = [0.4, 0.5, 0.6, 0.55, 0.7, 0.6, 0.65, 0.6], width = 140, height = 36 }) {
  const max = 1;
  const stepX = width / (values.length - 1);
  const pts = values.map((v, i) => `${i * stepX},${height - (v / max) * height * 0.85 - 2}`);
  return (
    <svg width={width} height={height} style={{ display: 'block' }}>
      <polyline
        points={pts.join(' ')}
        fill="none"
        stroke="var(--ink)"
        strokeWidth="1.5"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
      {pts.map((p, i) => {
        const [x, y] = p.split(',');
        const last = i === pts.length - 1;
        return <circle key={i} cx={x} cy={y} r={last ? 3 : 1.5} fill={last ? 'var(--progress)' : 'var(--ink-3)'} />;
      })}
    </svg>
  );
}

function PageResultScreenBold({
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

  return (
    <div className="sm-screen" data-theme={theme} data-screen-label={`PageResultScreen Bold (${theme})`}>
      {/* Header — editorial */}
      <div style={{
        padding: '16px 24px 18px',
        background: 'var(--surface)',
        borderBottom: '1px solid var(--hairline)',
      }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginBottom: 16 }}>
          <IconButton title="Voltar" size={32}><SmIcon.Back size={16} /></IconButton>
          <span className="eyebrow" style={{ fontSize: 10 }}>Folha concluída</span>
          <span className="mono" style={{ fontSize: 13, fontWeight: 600 }}>
            {String(pageIndex).padStart(2, '0')} / {String(pagesTotal).padStart(2, '0')}
          </span>
          <div style={{ flex: 1 }} />
          <span className="eyebrow" style={{ fontSize: 10 }}>Últimas folhas</span>
          <ScoreSpark />
        </div>

        {/* Huge score */}
        <div style={{ display: 'flex', alignItems: 'flex-end', gap: 28, marginTop: 8 }}>
          <div style={{ display: 'flex', flexDirection: 'column', lineHeight: 0.9 }}>
            <span className="eyebrow" style={{ fontSize: 10, marginBottom: 8 }}>Score da página</span>
            <div style={{ display: 'flex', alignItems: 'flex-end', gap: 6 }}>
              <span className="mono" style={{
                fontSize: 84, fontWeight: 500,
                letterSpacing: '-0.04em', color: 'var(--ink)',
                lineHeight: 0.85,
              }}>
                {(score * 100).toFixed(0)}
              </span>
              <span className="mono" style={{ fontSize: 28, color: 'var(--ink-3)', paddingBottom: 6 }}>%</span>
            </div>
          </div>

          <div style={{ flex: 1, display: 'flex', flexDirection: 'column', gap: 8, paddingBottom: 6 }}>
            <div style={{ display: 'flex', gap: 4, alignItems: 'center' }}>
              {results.map(r => (
                <div key={r.fieldIndex} style={{ display: 'flex', flexDirection: 'column', gap: 4, alignItems: 'center' }}>
                  <div style={{
                    width: 28, height: 8, borderRadius: 2,
                    background: r.ocrFailed ? 'var(--warning)' : r.isCorrect ? 'var(--progress)' : 'var(--error)',
                  }} />
                  <span className="mono" style={{ fontSize: 9, color: 'var(--ink-3)' }}>
                    {String(r.fieldIndex).padStart(2, '0')}
                  </span>
                </div>
              ))}
            </div>
            <div style={{ display: 'flex', gap: 18 }}>
              <div style={{ display: 'flex', flexDirection: 'column', lineHeight: 1 }}>
                <span className="eyebrow" style={{ fontSize: 9 }}>Acerto</span>
                <span className="mono" style={{ fontSize: 16, fontWeight: 600, marginTop: 3 }}>
                  {correct}/{total}
                </span>
              </div>
              <div style={{ display: 'flex', flexDirection: 'column', lineHeight: 1 }}>
                <span className="eyebrow" style={{ fontSize: 9 }}>Tempo total</span>
                <span className="mono" style={{ fontSize: 16, fontWeight: 600, marginTop: 3 }}>
                  {Math.floor(totalTime / 60000)}:{String(Math.floor((totalTime % 60000) / 1000)).padStart(2, '0')}
                </span>
              </div>
              <div style={{ display: 'flex', flexDirection: 'column', lineHeight: 1 }}>
                <span className="eyebrow" style={{ fontSize: 9 }}>Dif. próxima</span>
                <span className="mono" style={{ fontSize: 16, fontWeight: 600, marginTop: 3, color: 'var(--progress)' }}>
                  2.5 ↑
                </span>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Body: result list + cognitive */}
      <div className="no-scrollbar" style={{
        position: 'absolute', top: 232, bottom: 80, left: 0, right: 0,
        overflowY: 'auto',
      }}>
        {/* Section label */}
        <div style={{
          padding: '14px 24px 8px', display: 'flex', alignItems: 'center', gap: 8,
        }}>
          <span className="eyebrow" style={{ fontSize: 10 }}>Correção por campo</span>
          <div style={{ flex: 1, borderBottom: '1px solid var(--hairline)', height: 1 }} />
        </div>

        {results.map(r => (
          <ResultRowBold key={r.fieldIndex} r={r} ex={exercises[r.fieldIndex - 1]} />
        ))}

        {/* Cognitive panel */}
        <div style={{ padding: '20px 24px 24px' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 14 }}>
            <span className="eyebrow" style={{ fontSize: 10 }}>Diagnóstico cognitivo</span>
            <div style={{ flex: 1, borderBottom: '1px solid var(--hairline)', height: 1 }} />
          </div>
          <div style={{
            display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '14px 28px',
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
        padding: '0 24px',
        display: 'flex', alignItems: 'center', gap: 12,
      }}>
        <button className="sm-btn" style={{ height: 48 }}>
          Revisar erros
        </button>
        <div style={{ flex: 1 }} />
        <span className="eyebrow" style={{ fontSize: 10, marginRight: 8 }}>Próxima dificuldade 2.5</span>
        <button className="sm-btn sm-btn--primary" style={{ height: 48, padding: '0 22px' }} onClick={onNext}>
          Próxima folha
          <SmIcon.ArrowRight size={16} />
        </button>
      </div>
    </div>
  );
}

window.PageResultScreenBold = PageResultScreenBold;
