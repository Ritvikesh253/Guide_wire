import React from 'react';
import { useNavigate } from 'react-router-dom';

export default function Success() {
  const nav = useNavigate();

  return (
    <div style={s.outer}>
      <div style={s.card}>
        {/* Animated shield */}
        <div style={s.shieldWrap}>
          <svg width="72" height="72" viewBox="0 0 24 24" fill="none">
            <path d="M12 2L3 6v6c0 5.25 3.75 10.15 9 11.25C17.25 22.15 21 17.25 21 12V6l-9-4z" fill="rgba(255,255,255,0.95)"/>
            <path d="M8 12l3 3 5-5" stroke="#1B3A6B" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round"/>
          </svg>
        </div>

        <h1 style={s.title}>You're protected!</h1>
        <p style={s.sub}>Your PayAssure coverage is now active. When disruptions happen — you get paid automatically.</p>

        {/* Coverage summary */}
        <div style={s.summary}>
          {[
            { icon: '🌧️', label: 'Rain & floods' },
            { icon: '🌪️', label: 'Cyclones' },
            { icon: '😷', label: 'Air pollution' },
            { icon: '🚫', label: 'Curfews & strikes' },
          ].map(c => (
            <div key={c.label} style={s.chip}>
              <span>{c.icon}</span>
              <span style={s.chipLabel}>{c.label}</span>
            </div>
          ))}
        </div>

        <div style={s.note}>
          📲 Keep the app active while working. GPS must be on for disruption payouts to trigger.
        </div>

        <button style={s.btn} onClick={() => nav('/login')}>
          Go to Dashboard
        </button>
      </div>
    </div>
  );
}

const s = {
  outer: { minHeight: '100vh', display: 'flex', alignItems: 'center', justifyContent: 'center', background: 'linear-gradient(150deg, #1B3A6B 0%, #2553A0 100%)', padding: 20 },
  card:  { width: '100%', maxWidth: 400, background: '#fff', borderRadius: 28, padding: '40px 28px', textAlign: 'center', boxShadow: '0 20px 60px rgba(0,0,0,0.25)' },
  shieldWrap: { width: 100, height: 100, background: 'linear-gradient(135deg, #1B3A6B, #2553A0)', borderRadius: 30, display: 'flex', alignItems: 'center', justifyContent: 'center', margin: '0 auto 24px', boxShadow: '0 8px 30px rgba(27,58,107,0.35)' },
  title: { fontSize: 26, fontWeight: 800, color: '#0D1B2A', letterSpacing: '-0.5px', margin: '0 0 10px' },
  sub:   { fontSize: 14, color: '#8896A5', lineHeight: 1.7, margin: '0 0 28px' },
  summary: { display: 'flex', flexWrap: 'wrap', gap: 8, justifyContent: 'center', marginBottom: 24 },
  chip:     { display: 'flex', alignItems: 'center', gap: 6, background: '#EBF0FA', borderRadius: 20, padding: '8px 14px' },
  chipLabel:{ fontSize: 13, fontWeight: 500, color: '#1B3A6B' },
  note:  { background: '#FFF4E0', border: '1px solid #E8A020', borderRadius: 12, padding: '12px 16px', fontSize: 13, color: '#7A4500', lineHeight: 1.5, marginBottom: 24, textAlign: 'left' },
  btn:   { width: '100%', padding: 16, background: 'linear-gradient(135deg, #1B3A6B, #2553A0)', color: '#fff', border: 'none', borderRadius: 14, fontSize: 15, fontWeight: 700, cursor: 'pointer', fontFamily: 'inherit', boxShadow: '0 4px 20px rgba(27,58,107,0.3)' },
};
