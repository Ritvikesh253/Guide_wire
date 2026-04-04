import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Shell, SectionHead, Field, Input, Btn, Alert } from '../components/UI';
import { AuthAPI } from '../services/api';

export default function PhoneOtp() {
  const nav = useNavigate();
  const [phone, setPhone]         = useState('');
  const [otp, setOtp]             = useState('');
  const [otpSent, setOtpSent]     = useState(false);
  const [timer, setTimer]         = useState(0);
  const [loading, setLoading]     = useState(false);
  const [error, setError]         = useState('');
  const [devOtp, setDevOtp]       = useState(''); // dev only

  const startTimer = () => {
    setTimer(30);
    const t = setInterval(() => setTimer(p => { if (p <= 1) { clearInterval(t); return 0; } return p - 1; }), 1000);
  };

  const handleSend = async () => {
    if (!phone.match(/^\d{10}$/)) { setError('Enter a valid 10-digit mobile number'); return; }
    setError(''); setLoading(true);
    try {
      const res = await AuthAPI.sendOtp(phone);
      setOtpSent(true);
      setDevOtp(res.data?.data || '');  // dev only — remove in prod
      startTimer();
    } catch (e) {
      setError(e.response?.data?.message || 'Failed to send OTP. Try again.');
    } finally { setLoading(false); }
  };

  const handleVerify = async () => {
    if (!otp.match(/^\d{6}$/)) { setError('Enter the 6-digit OTP'); return; }
    setError(''); setLoading(true);
    try {
      await AuthAPI.verifyOtp(phone, otp);
      sessionStorage.setItem('gs_phone', phone);
      nav('/register/step1');
    } catch (e) {
      setError(e.response?.data?.message || 'Invalid OTP. Try again.');
    } finally { setLoading(false); }
  };

  return (
    <Shell>
      <SectionHead
        icon="📱"
        title="Verify your mobile"
        sub="We'll send a one-time password to confirm your number"
      />

      {error && <Alert type="error">{error}</Alert>}

      <Field label="Mobile number" hint="Use the number linked to your delivery app">
        <Input
          prefix="+91"
          type="tel" maxLength={10}
          placeholder="98XXXXXXXX"
          value={phone} onChange={e => setPhone(e.target.value.replace(/\D/g,''))}
          disabled={otpSent}
        />
      </Field>

      {!otpSent ? (
        <Btn loading={loading} onClick={handleSend}>Send OTP</Btn>
      ) : (
        <>
          {/* Dev helper — shows OTP returned from backend */}
          {devOtp && (
            <div style={{ background: '#FFF4E0', border: '1px solid #E8A020', borderRadius: 10, padding: '10px 14px', marginBottom: 16, fontSize: 13, color: '#7A4500' }}>
              <strong>Dev mode OTP:</strong> {devOtp}
            </div>
          )}

          <Field label="Enter OTP" hint={`Sent to +91-${phone.substring(0,5)}XXXXX`}>
            <Input
              type="tel" maxLength={6}
              placeholder="• • • • • •"
              value={otp} onChange={e => setOtp(e.target.value.replace(/\D/,''))}
              style={{ letterSpacing: 8, fontSize: 20, textAlign: 'center' }}
            />
          </Field>

          <Btn loading={loading} onClick={handleVerify}>Verify & Continue</Btn>

          <div style={{ textAlign: 'center', marginTop: 16, fontSize: 13, color: '#8896A5' }}>
            {timer > 0
              ? `Resend OTP in ${timer}s`
              : <span style={{ color: '#1B3A6B', cursor: 'pointer', fontWeight: 500 }} onClick={handleSend}>Resend OTP</span>
            }
          </div>
        </>
      )}
    </Shell>
  );
}
