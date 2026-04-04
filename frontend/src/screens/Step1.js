import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Shell, SectionHead, Field, Input, Btn, Alert, InfoCard } from '../components/UI';
import { AuthAPI, RegisterAPI } from '../services/api';

export default function Step1() {
  const nav = useNavigate();
  const phone = sessionStorage.getItem('gs_phone') || '';

  const [form, setForm]         = useState({ password: '', confirmPwd: '', aadhaar: '', aadhaarOtp: '', selfie: '' });
  const [aadhaarOtpSent, setAadhaarOtpSent] = useState(false);
  const [kycData, setKycData]   = useState(null);
  const [loading, setLoading]   = useState(false);
  const [otpLoading, setOtpLoading] = useState(false);
  const [error, setError]       = useState('');
  const [selfieLabel, setSelfieLabel] = useState('');

  const set = (k, v) => setForm(p => ({ ...p, [k]: v }));

  // Send OTP to Aadhaar-linked mobile via UIDAI/Setu
  const sendAadhaarOtp = async () => {
    if (!form.aadhaar.match(/^\d{12}$/)) { setError('Enter valid 12-digit Aadhaar number'); return; }
    setError(''); setOtpLoading(true);
    try {
      await AuthAPI.sendAadhaarOtp(form.aadhaar);
      setAadhaarOtpSent(true);
    } catch (e) {
      setError('Could not send Aadhaar OTP. Check your Aadhaar number.');
    } finally { setOtpLoading(false); }
  };

  // Capture selfie (mock — reads a file or uses camera in real app)
  const captureSelfie = async () => {
    // In production: open camera, capture 2-second video, extract frame as base64
    // Here we simulate with a placeholder base64 string
    const mockBase64 = 'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==';
    set('selfie', mockBase64);
    setSelfieLabel('Selfie captured ✓');
  };

  const handleSubmit = async () => {
    if (!form.password || form.password.length < 6) { setError('Password must be at least 6 characters'); return; }
    if (form.password !== form.confirmPwd)           { setError('Passwords do not match'); return; }
    if (!form.aadhaar.match(/^\d{12}$/))             { setError('Enter valid 12-digit Aadhaar number'); return; }
    if (!form.aadhaarOtp.match(/^\d{6}$/))           { setError('Enter 6-digit Aadhaar OTP'); return; }
    if (!form.selfie)                                { setError('Please capture your selfie for liveness check'); return; }

    setError(''); setLoading(true);
    try {
      const res = await RegisterAPI.step1({
        phone,
        password:           form.password,
        aadhaarNumber:      form.aadhaar,
        aadhaarOtp:         form.aadhaarOtp,
        livenessSelfieBase64: form.selfie,
      });

      if (!res.data.success) { setError(res.data.message); setLoading(false); return; }

      sessionStorage.setItem('gs_workerId', res.data.workerId);
      setKycData(res.data.data);

      // Short pause to show the verified card, then go to step 2
      setTimeout(() => nav('/register/step2'), 1800);

    } catch (e) {
      setError(e.response?.data?.message || 'Verification failed. Please try again.');
      setLoading(false);
    }
  };

  return (
    <Shell step={1}>
      <SectionHead
        icon="🪪"
        title="Identity & Security"
        sub="Verify your Aadhaar and create your account password"
      />

      {error && <Alert type="error">{error}</Alert>}

      {/* Verified state */}
      {kycData && (
        <Alert type="success">
          ✅ Identity verified — <strong>{kycData.legalName}</strong>. Moving to next step…
        </Alert>
      )}

      {!kycData && <>
        <Field label="Create password">
          <Input type="password" placeholder="Min 6 characters" value={form.password} onChange={e => set('password', e.target.value)} />
        </Field>

        <Field label="Confirm password">
          <Input type="password" placeholder="Re-enter password" value={form.confirmPwd} onChange={e => set('confirmPwd', e.target.value)} />
        </Field>

        <div style={{ height: 1, background: '#F0F4FA', margin: '20px 0' }} />

        <Field label="Aadhaar number" hint="12-digit Aadhaar — we fetch your name, address & photo">
          <div style={{ display: 'flex', gap: 8 }}>
            <Input
              type="tel" maxLength={12} placeholder="XXXX XXXX XXXX"
              value={form.aadhaar} onChange={e => set('aadhaar', e.target.value.replace(/\D/g,''))}
              style={{ flex: 1 }}
            />
            <button onClick={sendAadhaarOtp} disabled={otpLoading || aadhaarOtpSent}
              style={{ padding: '0 16px', background: aadhaarOtpSent ? '#E6F7F0' : '#1B3A6B', color: aadhaarOtpSent ? '#1A8A5A' : '#fff', border: 'none', borderRadius: 10, fontSize: 13, fontWeight: 600, cursor: 'pointer', whiteSpace: 'nowrap', fontFamily: 'inherit' }}>
              {aadhaarOtpSent ? 'Sent ✓' : otpLoading ? '…' : 'Get OTP'}
            </button>
          </div>
        </Field>

        {aadhaarOtpSent && (
          <Field label="Aadhaar OTP" hint="OTP sent to your Aadhaar-linked mobile by UIDAI">
            <Input type="tel" maxLength={6} placeholder="• • • • • •"
              value={form.aadhaarOtp} onChange={e => set('aadhaarOtp', e.target.value.replace(/\D/g,''))}
              style={{ letterSpacing: 8, fontSize: 20, textAlign: 'center' }}
            />
          </Field>
        )}

        <div style={{ height: 1, background: '#F0F4FA', margin: '20px 0' }} />

        {/* Liveness selfie */}
        <Field label="Liveness selfie" hint="A quick 2-second video selfie to match your Aadhaar photo">
          <button onClick={captureSelfie} style={{
            width: '100%', padding: '32px 16px', border: '2px dashed #C8D5E8',
            borderRadius: 12, background: form.selfie ? '#E6F7F0' : '#F7F9FC',
            color: form.selfie ? '#1A8A5A' : '#8896A5', fontSize: 14,
            cursor: 'pointer', fontFamily: 'inherit', fontWeight: 500,
          }}>
            {selfieLabel || '📷  Tap to open camera'}
          </button>
        </Field>

        <Btn loading={loading} onClick={handleSubmit}>
          Verify Identity →
        </Btn>
      </>}
    </Shell>
  );
}
