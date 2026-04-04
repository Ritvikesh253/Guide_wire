import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Shell, SectionHead, Field, Input, Btn, Alert } from '../components/UI';
import { AuthAPI } from '../services/api';

export default function Login() {
  const nav = useNavigate();
  const [form, setForm]   = useState({ phone: '', password: '' });
  const [loading, setLoading] = useState(false);
  const [error, setError]     = useState('');
  const set = (k, v) => setForm(p => ({ ...p, [k]: v }));

  const handleLogin = async () => {
    if (!form.phone || !form.password) { setError('Enter phone and password'); return; }
    setError(''); setLoading(true);
    try {
      const res = await AuthAPI.login(form.phone, form.password);
      if (!res.data.success) { setError(res.data.message); setLoading(false); return; }
      localStorage.setItem('gs_token', res.data.token);
      sessionStorage.setItem('gs_workerId', res.data.workerId);
      // Redirect based on registration status
      const status = res.data.registrationStatus;
      if (status === 'ACTIVE')           nav('/success');
      else if (status === 'STEP_1_COMPLETE') nav('/register/step2');
      else if (status === 'STEP_2_COMPLETE') nav('/register/step3');
      else if (status === 'STEP_3_COMPLETE') nav('/register/step4');
      else nav('/register/step1');
    } catch (e) {
      setError(e.response?.data?.message || 'Login failed. Try again.');
      setLoading(false);
    }
  };

  return (
    <Shell>
      <SectionHead icon="👋" title="Welcome back" sub="Sign in to your PayAssure account" />
      {error && <Alert type="error">{error}</Alert>}
      <Field label="Mobile number">
        <Input prefix="+91" type="tel" maxLength={10} placeholder="98XXXXXXXX"
          value={form.phone} onChange={e => set('phone', e.target.value.replace(/\D/g,''))} />
      </Field>
      <Field label="Password">
        <Input type="password" placeholder="Your password"
          value={form.password} onChange={e => set('password', e.target.value)} />
      </Field>
      <Btn loading={loading} onClick={handleLogin}>Sign In</Btn>
      <div style={{ textAlign: 'center', marginTop: 20, fontSize: 14, color: '#8896A5' }}>
        New to PayAssure?{' '}
        <span style={{ color: '#1B3A6B', fontWeight: 600, cursor: 'pointer' }} onClick={() => nav('/otp')}>
          Register now
        </span>
      </div>
    </Shell>
  );
}
