import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Shell, SectionHead, Field, Input, Btn, Alert } from '../components/UI';
import { RegisterAPI } from '../services/api';

export default function Step4() {
  const nav = useNavigate();
  const workerId = sessionStorage.getItem('gs_workerId');

  const [form, setForm]   = useState({ upi: '', nomineeName: '', nomineePhone: '', agreed: false });
  const [loading, setLoading] = useState(false);
  const [error, setError]     = useState('');
  const [pennyStatus, setPennyStatus] = useState(''); // '', 'checking', 'done'

  const set = (k, v) => setForm(p => ({ ...p, [k]: v }));

  const handleUpiBlur = () => {
    if (form.upi.includes('@')) {
      setPennyStatus('checking');
      setTimeout(() => setPennyStatus('done'), 1200); // mock penny drop delay
    }
  };

  const handleSubmit = async () => {
    if (!form.upi.includes('@'))          { setError('Enter a valid UPI ID (e.g. name@upi)'); return; }
    if (!form.nomineeName.trim())          { setError('Nominee name is required'); return; }
    if (!form.nomineePhone.match(/^\d{10}$/)) { setError('Enter valid 10-digit nominee mobile'); return; }
    if (!form.agreed)                     { setError('Please agree to the policy terms to activate coverage'); return; }

    setError(''); setLoading(true);
    try {
      const res = await RegisterAPI.step4({
        workerId,
        upiId:        form.upi,
        nomineeName:  form.nomineeName,
        nomineePhone: form.nomineePhone,
        policyAgreed: form.agreed,
      });
      if (!res.data.success) { setError(res.data.message); setLoading(false); return; }
      sessionStorage.setItem('gs_name', res.data.message);
      nav('/success');
    } catch (e) {
      setError(e.response?.data?.message || 'Activation failed. Try again.');
      setLoading(false);
    }
  };

  return (
    <Shell step={4}>
      <SectionHead
        icon="💳"
        title="Payout Setup"
        sub="Set up your UPI for instant payouts when disruptions hit"
      />

      {error && <Alert type="error">{error}</Alert>}

      {/* UPI field with penny drop indicator */}
      <Field label="UPI ID" hint="We'll send ₹1 to verify your account (penny drop)">
        <div style={{ position: 'relative' }}>
          <Input type="text" placeholder="yourname@upi" value={form.upi}
            onChange={e => set('upi', e.target.value)}
            onBlur={handleUpiBlur}
          />
          {pennyStatus === 'checking' && (
            <div style={{ marginTop: 6, fontSize: 12, color: '#E8A020' }}>⏳ Sending ₹1 to verify account…</div>
          )}
          {pennyStatus === 'done' && (
            <div style={{ marginTop: 6, fontSize: 12, color: '#1A8A5A', fontWeight: 500 }}>✅ Account verified — ₹1 sent successfully</div>
          )}
        </div>
      </Field>

      <div style={{ height: 1, background: '#F0F4FA', margin: '20px 0' }} />

      {/* Nominee */}
      <div style={{ background: '#F7F9FC', borderRadius: 14, padding: '16px', marginBottom: 20 }}>
        <div style={{ fontSize: 13, fontWeight: 600, color: '#4A5568', marginBottom: 14 }}>Nominee details</div>
        <Field label="Nominee full name">
          <Input type="text" placeholder="Family member's name"
            value={form.nomineeName} onChange={e => set('nomineeName', e.target.value)} />
        </Field>
        <Field label="Nominee mobile">
          <Input prefix="+91" type="tel" maxLength={10} placeholder="10-digit mobile"
            value={form.nomineePhone} onChange={e => set('nomineePhone', e.target.value.replace(/\D/g,''))} />
        </Field>
      </div>

      {/* Policy terms */}
      <div style={{ background: '#EBF0FA', borderRadius: 14, padding: '16px', marginBottom: 20 }}>
        <div style={{ fontSize: 13, fontWeight: 600, color: '#1B3A6B', marginBottom: 10 }}>Policy terms summary</div>
        {[
          'Payouts trigger automatically — no claim filing needed',
          'Coverage: rain, flood, cyclone, AQI, curfew, strikes',
          'Payout based on your personal 30-day earning average',
          '7-day clawback window for fraud prevention',
          'Weekly premium auto-deducted every Monday',
        ].map(t => (
          <div key={t} style={{ display: 'flex', gap: 8, marginBottom: 7, fontSize: 13, color: '#4A5568' }}>
            <span style={{ color: '#1A8A5A', fontWeight: 700, flexShrink: 0 }}>✓</span>
            <span>{t}</span>
          </div>
        ))}
      </div>

      {/* Agreement */}
      <label style={{ display: 'flex', alignItems: 'flex-start', gap: 12, cursor: 'pointer', marginBottom: 24 }}>
        <input type="checkbox" checked={form.agreed} onChange={e => set('agreed', e.target.checked)}
          style={{ width: 18, height: 18, accentColor: '#1B3A6B', marginTop: 2, flexShrink: 0 }} />
        <span style={{ fontSize: 13, color: '#4A5568', lineHeight: 1.5 }}>
          I agree to the PayAssure parametric insurance policy terms and consent to automatic payouts and premium deduction
        </span>
      </label>

      <Btn loading={loading} onClick={handleSubmit}>
        🛡️  Activate My Coverage
      </Btn>
    </Shell>
  );
}
