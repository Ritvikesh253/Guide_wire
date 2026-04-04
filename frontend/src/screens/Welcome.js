import React from 'react';
import { useNavigate } from 'react-router-dom';

export default function Welcome() {
  const nav = useNavigate();
  return (
    <div style={s.outer}>
      <div style={s.card}>

        {/* Hero */}
        <div style={s.hero}>
          <div style={s.shieldWrap}>
            <svg width="64" height="64" viewBox="0 0 24 24" fill="none">
              <path d="M12 2L3 6v6c0 5.25 3.75 10.15 9 11.25C17.25 22.15 21 17.25 21 12V6l-9-4z" fill="rgba(255,255,255,0.95)"/>
              <path d="M9 12l2 2 4-4" stroke="#1B3A6B" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/>
            </svg>
          </div>
          <h1 style={s.brand}>PayAssure</h1>
          <p style={s.tagline}>Income protection for<br/>delivery partners</p>
        </div>

        {/* Value props */}
        <div style={s.props}>
          {[
            { icon: '⚡', title: 'Auto payouts', sub: 'Money reaches you before you ask' },
            { icon: '🌧️', title: 'Rain, flood, curfew', sub: 'Covered when the city shuts down' },
            { icon: '🔒', title: 'Aadhaar verified', sub: 'Secure, paperless, 3 minutes' },
          ].map(p => (
            <div key={p.title} style={s.prop}>
              <span style={s.propIcon}>{p.icon}</span>
              <div>
                <div style={s.propTitle}>{p.title}</div>
                <div style={s.propSub}>{p.sub}</div>
              </div>
            </div>
          ))}
        </div>

        {/* CTA */}
        <div style={s.cta}>
          <button style={s.btnPrimary} onClick={() => nav('/otp')}>
            Get covered in 3 minutes
          </button>
          <button style={s.btnLink} onClick={() => nav('/login')}>
            Already a member? Sign in
          </button>
        </div>

        <p style={s.legal}>
          Parametric income insurance · IRDAI registered · ₹20–₹70/week
        </p>
      </div>
    </div>
  );
}

const s = {
  outer: { minHeight: '100vh', display: 'flex', alignItems: 'center', justifyContent: 'center', background: 'linear-gradient(160deg, #EBF0FA 0%, #F7F9FC 70%)', padding: 20 },
  card:  { width: '100%', maxWidth: 420, background: '#fff', borderRadius: 28, boxShadow: '0 8px 48px rgba(27,58,107,0.13)', overflow: 'hidden' },

  hero:      { background: 'linear-gradient(150deg, #1B3A6B 0%, #2553A0 100%)', padding: '48px 32px 40px', textAlign: 'center' },
  shieldWrap:{ width: 80, height: 80, background: 'rgba(255,255,255,0.15)', borderRadius: 24, display: 'flex', alignItems: 'center', justifyContent: 'center', margin: '0 auto 16px' },
  brand:     { fontSize: 32, fontWeight: 800, color: '#fff', letterSpacing: '-1px', fontFamily: "'DM Serif Display', serif", margin: '0 0 8px' },
  tagline:   { fontSize: 16, color: 'rgba(255,255,255,0.75)', lineHeight: 1.5 },

  props:    { padding: '24px 28px 8px' },
  prop:     { display: 'flex', alignItems: 'flex-start', gap: 14, marginBottom: 20 },
  propIcon: { fontSize: 24, lineHeight: 1, marginTop: 2 },
  propTitle:{ fontSize: 14, fontWeight: 600, color: '#0D1B2A' },
  propSub:  { fontSize: 13, color: '#8896A5', marginTop: 2 },

  cta:       { padding: '8px 28px 20px' },
  btnPrimary:{ width: '100%', padding: 16, background: 'linear-gradient(135deg, #1B3A6B, #2553A0)', color: '#fff', border: 'none', borderRadius: 14, fontSize: 16, fontWeight: 700, cursor: 'pointer', boxShadow: '0 4px 20px rgba(27,58,107,0.28)', marginBottom: 12, fontFamily: 'inherit' },
  btnLink:   { width: '100%', padding: 12, background: 'transparent', color: '#1B3A6B', border: 'none', fontSize: 14, fontWeight: 500, cursor: 'pointer', fontFamily: 'inherit' },

  legal: { textAlign: 'center', fontSize: 11, color: '#B0BCC8', padding: '0 28px 24px', lineHeight: 1.6 },
};
