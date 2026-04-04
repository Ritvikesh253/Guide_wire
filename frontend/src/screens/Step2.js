import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Shell, SectionHead, Field, Btn, Alert, PlatformCard, InfoCard } from '../components/UI';
import { RegisterAPI } from '../services/api';

const PLATFORMS = [
  { id: 'RUSHDASH',   name: 'RushDash',   color: '#E8471A', desc: 'Food delivery' },
  { id: 'QUICKBITE',  name: 'QuickBite',  color: '#C0392B', desc: 'Restaurant delivery' },
  { id: 'ZIPDELIVER', name: 'ZipDeliver', color: '#6B3FA0', desc: 'Quick commerce' },
];

// ── Fake ID card preview — rendered in browser ──────────────────────────────
function FakeIdCard({ platform }) {
  const p = PLATFORMS.find(x => x.id === platform);
  if (!p) return null;

  const idFormats = {
    RUSHDASH:   { prefix: 'RSD', format: 'RSD-2024-084521', city: 'Chennai' },
    QUICKBITE:  { prefix: 'QBT', format: 'QBT-100293847',   city: 'Chennai' },
    ZIPDELIVER: { prefix: 'ZPD', format: 'ZPD/CHN/0039281', city: 'Chennai' },
  };
  const meta = idFormats[platform];

  return (
    <div style={{ border: `2px solid ${p.color}`, borderRadius: 14, overflow: 'hidden', marginBottom: 20 }}>
      {/* Header bar */}
      <div style={{ background: p.color, padding: '10px 16px', display: 'flex', alignItems: 'center', gap: 10 }}>
        <div style={{ width: 32, height: 32, background: 'rgba(255,255,255,0.2)', borderRadius: 8, display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 16 }}>🛵</div>
        <div>
          <div style={{ color: '#fff', fontWeight: 700, fontSize: 14 }}>{p.name}</div>
          <div style={{ color: 'rgba(255,255,255,0.75)', fontSize: 11 }}>DELIVERY PARTNER ID</div>
        </div>
      </div>
      {/* Card body */}
      <div style={{ padding: '14px 16px', background: '#fff' }}>
        <table style={{ width: '100%', fontSize: 12, borderCollapse: 'collapse' }}>
          {[
            ['Name',       'Ravi Kumar'],
            ['Partner ID', meta.format],
            ['City',       meta.city],
            ['Joining',    '12-Jan-2024'],
            ['Valid Until','11-Jan-2025'],
          ].map(([k, v]) => (
            <tr key={k} style={{ borderBottom: '1px solid #F0F4FA' }}>
              <td style={{ padding: '6px 0', color: '#8896A5', width: 90 }}>{k}</td>
              <td style={{ padding: '6px 0', color: '#0D1B2A', fontWeight: k === 'Partner ID' ? 700 : 400, fontFamily: k === 'Partner ID' ? 'monospace' : 'inherit' }}>
                {v}
              </td>
            </tr>
          ))}
        </table>
        <div style={{ marginTop: 10, fontSize: 10, color: '#B0BCC8', textAlign: 'center' }}>
          ▪ This is a demo card for PayAssure hackathon ▪
        </div>
      </div>
    </div>
  );
}

export default function Step2() {
  const nav = useNavigate();
  const workerId = sessionStorage.getItem('gs_workerId');

  const [platform, setPlatform] = useState('');
  const [scanDone, setScanDone] = useState(false);
  const [ocrText, setOcrText]   = useState('');
  const [loading, setLoading]   = useState(false);
  const [scanning, setScanning] = useState(false);
  const [result, setResult]     = useState(null);
  const [error, setError]       = useState('');

  // Simulate OCR scan — in production: Google ML Kit on mobile sends extracted text
  const handleScan = () => {
    if (!platform) { setError('Select your delivery platform first'); return; }
    setScanning(true); setError('');

    // Simulate 1.5s OCR processing
    setTimeout(() => {
      const idFormats = {
        RUSHDASH:   `Name : Ravi Kumar\nPartner ID : RSD-2024-084521\nPlatform : RushDash\nCity : Chennai\nValid Until : 11-Jan-2025`,
        QUICKBITE:  `Name : Ravi Kumar\nPartner ID : QBT-100293847\nPlatform : QuickBite\nCity : Chennai\nValid Until : 11-Jan-2025`,
        ZIPDELIVER: `Name : Ravi Kumar\nPartner ID : ZPD/CHN/0039281\nPlatform : ZipDeliver\nCity : Chennai\nValid Until : 11-Jan-2025`,
      };
      setOcrText(idFormats[platform]);
      setScanDone(true);
      setScanning(false);
    }, 1500);
  };

  const handleSubmit = async () => {
    if (!platform)  { setError('Select your delivery platform'); return; }
    if (!scanDone)  { setError('Scan your delivery ID card first'); return; }
    setError(''); setLoading(true);

    try {
      const res = await RegisterAPI.step2({
        workerId,
        platform,
        idCardImageBase64: ocrText,   // sending extracted OCR text as "image" for demo
      });

      if (!res.data.success) { setError(res.data.message); setLoading(false); return; }
      setResult(res.data);
      setTimeout(() => nav('/register/step3'), 1800);

    } catch (e) {
      setError(e.response?.data?.message || 'Verification failed. Try again.');
      setLoading(false);
    }
  };

  return (
    <Shell step={2}>
      <SectionHead
        icon="🛵"
        title="Work Verification"
        sub="Prove you're an active delivery partner — we scan your ID card"
      />

      {error  && <Alert type="error">{error}</Alert>}
      {result && <Alert type="success">✅ {result.message} — moving to next step…</Alert>}

      {!result && <>
        {/* Platform selection */}
        <Field label="Select your delivery platform">
          {PLATFORMS.map(p => (
            <PlatformCard key={p.id} name={p.name} color={p.color}
              selected={platform === p.id}
              onClick={() => { setPlatform(p.id); setScanDone(false); setOcrText(''); }}
            />
          ))}
        </Field>

        {/* Show fake ID card */}
        {platform && <FakeIdCard platform={platform} />}

        {/* OCR scan button */}
        {platform && (
          <Field label="Scan your Partner ID card"
            hint="Your app camera will read the card using OCR and match your name with Aadhaar">
            <button onClick={handleScan} disabled={scanning || scanDone} style={{
              width: '100%', padding: '28px 16px',
              border: `2px dashed ${scanDone ? '#1A8A5A' : '#C8D5E8'}`,
              borderRadius: 12,
              background: scanDone ? '#E6F7F0' : scanning ? '#F7F9FC' : '#fff',
              color: scanDone ? '#1A8A5A' : '#4A5568',
              fontSize: 14, fontWeight: 500, cursor: 'pointer', fontFamily: 'inherit',
              transition: 'all 0.2s',
            }}>
              {scanDone   ? '✅  ID scanned — name matched with Aadhaar'
               : scanning ? '🔍  Scanning ID card…'
               : '📷  Tap to scan your Partner ID card'}
            </button>
          </Field>
        )}

        {/* OCR result preview */}
        {scanDone && ocrText && (
          <div style={{ background: '#F7F9FC', border: '1px solid #E2E8F0', borderRadius: 10, padding: '12px 14px', marginBottom: 16 }}>
            <div style={{ fontSize: 11, fontWeight: 600, color: '#8896A5', marginBottom: 8, textTransform: 'uppercase', letterSpacing: '0.5px' }}>OCR extracted text</div>
            <pre style={{ fontSize: 12, color: '#0D1B2A', lineHeight: 1.7, whiteSpace: 'pre-wrap', fontFamily: 'monospace' }}>{ocrText}</pre>
          </div>
        )}

        <Btn loading={loading} onClick={handleSubmit}>
          Verify Work ID →
        </Btn>
      </>}
    </Shell>
  );
}
