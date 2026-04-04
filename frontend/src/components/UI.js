import React from 'react';

/* ── Shell — phone-sized card centered on screen ──────────────────────────── */
export function Shell({ children, step, totalSteps }) {
  return (
    <div style={s.outer}>
      <div style={s.card}>
        {/* top bar */}
        <div style={s.topBar}>
          <div style={s.logo}>
            <ShieldIcon />
            <span style={s.logoText}>PayAssure</span>
          </div>
          {step && (
            <span style={s.stepBadge}>Step {step} of {totalSteps || 4}</span>
          )}
        </div>

        {/* progress bar */}
        {step && (
          <div style={s.progressTrack}>
            <div style={{ ...s.progressFill, width: `${(step / (totalSteps || 4)) * 100}%` }} />
          </div>
        )}

        <div style={s.body}>{children}</div>
      </div>
    </div>
  );
}

/* ── Section header ────────────────────────────────────────────────────────── */
export function SectionHead({ icon, title, sub }) {
  return (
    <div style={s.sectionHead}>
      {icon && <div style={s.sectionIcon}>{icon}</div>}
      <h2 style={s.sectionTitle}>{title}</h2>
      {sub && <p style={s.sectionSub}>{sub}</p>}
    </div>
  );
}

/* ── Field wrapper ─────────────────────────────────────────────────────────── */
export function Field({ label, hint, children, error }) {
  return (
    <div style={s.field}>
      <label style={s.label}>{label}</label>
      {children}
      {hint  && !error && <span style={s.hint}>{hint}</span>}
      {error && <span style={s.error}>{error}</span>}
    </div>
  );
}

/* ── Text input ────────────────────────────────────────────────────────────── */
export function Input({ prefix, suffix, ...props }) {
  if (!prefix && !suffix) {
    return <input style={s.input} {...props} />;
  }
  return (
    <div style={s.inputWrap}>
      {prefix && <span style={s.inputPrefix}>{prefix}</span>}
      <input style={{ ...s.input, ...s.inputInner, ...(prefix ? { borderLeft: 'none', borderRadius: '0 8px 8px 0' } : {}), ...(suffix ? { borderRight: 'none', borderRadius: '8px 0 0 8px' } : {}) }} {...props} />
      {suffix && <span style={s.inputSuffix}>{suffix}</span>}
    </div>
  );
}

/* ── Primary button ────────────────────────────────────────────────────────── */
export function Btn({ loading, children, variant = 'primary', ...props }) {
  const base = variant === 'primary' ? s.btnPrimary : s.btnOutline;
  return (
    <button style={{ ...s.btn, ...base, ...(loading ? s.btnLoading : {}) }} disabled={loading} {...props}>
      {loading ? <Spinner /> : children}
    </button>
  );
}

/* ── Alert box ─────────────────────────────────────────────────────────────── */
export function Alert({ type = 'error', children }) {
  const color = type === 'success'
    ? { bg: '#E6F7F0', border: '#1A8A5A', text: '#0D5C3A' }
    : { bg: '#FDECEA', border: '#C0392B', text: '#7B1A13' };
  return (
    <div style={{ background: color.bg, border: `1px solid ${color.border}`, color: color.text, padding: '12px 16px', borderRadius: 10, fontSize: 13, lineHeight: 1.5, marginBottom: 16 }}>
      {children}
    </div>
  );
}

/* ── Info card ──────────────────────────────────────────────────────────────── */
export function InfoCard({ label, value, icon }) {
  return (
    <div style={s.infoCard}>
      {icon && <span style={s.infoIcon}>{icon}</span>}
      <div>
        <div style={s.infoLabel}>{label}</div>
        <div style={s.infoValue}>{value}</div>
      </div>
    </div>
  );
}

/* ── Platform selector card ─────────────────────────────────────────────────── */
export function PlatformCard({ name, color, selected, onClick }) {
  return (
    <div onClick={onClick} style={{
      ...s.platformCard,
      border: selected ? `2px solid ${color}` : '2px solid #E2E8F0',
      background: selected ? `${color}12` : '#fff',
    }}>
      <div style={{ ...s.platformDot, background: color }} />
      <span style={{ ...s.platformName, color: selected ? color : '#0D1B2A', fontWeight: selected ? 600 : 400 }}>{name}</span>
      {selected && <span style={{ ...s.platformCheck, background: color }}>✓</span>}
    </div>
  );
}

/* ── Spinner ────────────────────────────────────────────────────────────────── */
function Spinner() {
  return (
    <span style={{ display: 'inline-block', width: 18, height: 18, border: '2px solid rgba(255,255,255,0.4)', borderTopColor: '#fff', borderRadius: '50%', animation: 'spin 0.7s linear infinite' }}>
      <style>{`@keyframes spin{to{transform:rotate(360deg)}}`}</style>
    </span>
  );
}

/* ── Shield icon ────────────────────────────────────────────────────────────── */
function ShieldIcon() {
  return (
    <svg width="22" height="22" viewBox="0 0 24 24" fill="none">
      <path d="M12 2L3 6v6c0 5.25 3.75 10.15 9 11.25C17.25 22.15 21 17.25 21 12V6l-9-4z" fill="#1B3A6B"/>
      <path d="M9 12l2 2 4-4" stroke="#fff" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round"/>
    </svg>
  );
}

/* ── Styles ─────────────────────────────────────────────────────────────────── */
const s = {
  outer: {
    minHeight: '100vh', display: 'flex', alignItems: 'center',
    justifyContent: 'center', background: 'linear-gradient(135deg, #EBF0FA 0%, #F7F9FC 60%, #FFF4E0 100%)',
    padding: '20px 16px',
  },
  card: {
    width: '100%', maxWidth: 440, background: '#fff',
    borderRadius: 24, boxShadow: '0 8px 40px rgba(27,58,107,0.13)',
    overflow: 'hidden',
  },
  topBar: {
    display: 'flex', alignItems: 'center', justifyContent: 'space-between',
    padding: '18px 24px 14px',
    borderBottom: '1px solid #F0F4FA',
  },
  logo:     { display: 'flex', alignItems: 'center', gap: 8 },
  logoText: { fontSize: 17, fontWeight: 700, color: '#1B3A6B', letterSpacing: '-0.3px' },
  stepBadge:{ fontSize: 12, fontWeight: 500, color: '#8896A5', background: '#F7F9FC', padding: '4px 10px', borderRadius: 20 },
  progressTrack: { height: 3, background: '#EBF0FA' },
  progressFill:  { height: '100%', background: 'linear-gradient(90deg, #1B3A6B, #2553A0)', transition: 'width 0.5s ease', borderRadius: 2 },
  body: { padding: '28px 24px 32px' },

  sectionHead: { marginBottom: 28 },
  sectionIcon: { fontSize: 32, marginBottom: 12 },
  sectionTitle:{ fontSize: 22, fontWeight: 700, color: '#0D1B2A', letterSpacing: '-0.5px', lineHeight: 1.2, marginBottom: 6 },
  sectionSub:  { fontSize: 14, color: '#8896A5', lineHeight: 1.6 },

  field:  { marginBottom: 18 },
  label:  { display: 'block', fontSize: 13, fontWeight: 500, color: '#4A5568', marginBottom: 6 },
  hint:   { display: 'block', fontSize: 12, color: '#8896A5', marginTop: 5 },
  error:  { display: 'block', fontSize: 12, color: '#C0392B', marginTop: 5 },

  input: {
    width: '100%', padding: '13px 14px', fontSize: 15, color: '#0D1B2A',
    border: '1.5px solid #E2E8F0', borderRadius: 10, background: '#F7F9FC',
    transition: 'border-color 0.2s, background 0.2s',
    outline: 'none',
  },
  inputWrap:   { display: 'flex', alignItems: 'stretch' },
  inputInner:  { flex: 1 },
  inputPrefix: { display: 'flex', alignItems: 'center', padding: '0 12px', background: '#F0F4FA', border: '1.5px solid #E2E8F0', borderRight: 'none', borderRadius: '10px 0 0 10px', fontSize: 14, color: '#4A5568', whiteSpace: 'nowrap' },
  inputSuffix: { display: 'flex', alignItems: 'center', padding: '0 12px', background: '#F0F4FA', border: '1.5px solid #E2E8F0', borderLeft: 'none', borderRadius: '0 10px 10px 0', fontSize: 14, color: '#4A5568' },

  btn: {
    width: '100%', padding: '15px', fontSize: 15, fontWeight: 600,
    borderRadius: 12, transition: 'all 0.2s', marginTop: 8,
    display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 8,
  },
  btnPrimary:  { background: 'linear-gradient(135deg, #1B3A6B, #2553A0)', color: '#fff', boxShadow: '0 4px 16px rgba(27,58,107,0.3)' },
  btnOutline:  { background: '#fff', color: '#1B3A6B', border: '1.5px solid #1B3A6B' },
  btnLoading:  { opacity: 0.75 },

  infoCard:  { display: 'flex', alignItems: 'center', gap: 12, padding: '14px', background: '#F7F9FC', borderRadius: 12, marginBottom: 10, border: '1px solid #E2E8F0' },
  infoIcon:  { fontSize: 22 },
  infoLabel: { fontSize: 11, color: '#8896A5', fontWeight: 500, textTransform: 'uppercase', letterSpacing: '0.5px' },
  infoValue: { fontSize: 14, fontWeight: 600, color: '#0D1B2A', marginTop: 2 },

  platformCard: { display: 'flex', alignItems: 'center', gap: 12, padding: '14px 16px', borderRadius: 12, cursor: 'pointer', marginBottom: 10, transition: 'all 0.15s' },
  platformDot:  { width: 10, height: 10, borderRadius: '50%' },
  platformName: { flex: 1, fontSize: 15 },
  platformCheck:{ width: 20, height: 20, borderRadius: '50%', color: '#fff', fontSize: 11, display: 'flex', alignItems: 'center', justifyContent: 'center', fontWeight: 700 },
};
