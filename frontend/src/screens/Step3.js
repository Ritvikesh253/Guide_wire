import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { Shell, SectionHead, Field, Btn, Alert, InfoCard } from '../components/UI';
import { ZoneAPI, RegisterAPI } from '../services/api';

export default function Step3() {
  const nav = useNavigate();
  const workerId = sessionStorage.getItem('gs_workerId');

  const [zones, setZones]           = useState([]);
  const [selected, setSelected]     = useState(null);
  const [gpsGranted, setGpsGranted] = useState(false);
  const [uan, setUan]               = useState('');
  const [loading, setLoading]       = useState(false);
  const [gpsLoading, setGpsLoading] = useState(false);
  const [error, setError]           = useState('');
  const [result, setResult]         = useState(null);
  const [coords, setCoords]         = useState(null);
  const [search, setSearch]         = useState('');

  useEffect(() => {
    ZoneAPI.list()
      .then(r => {
        const payload = Array.isArray(r.data) ? r.data : (r.data?.data || []);
        setZones(payload);
      })
      .catch(() => {});
  }, []);

  const handleGps = () => {
    setGpsLoading(true); setError('');
    navigator.geolocation.getCurrentPosition(
      pos => {
        setCoords({ lat: pos.coords.latitude, lon: pos.coords.longitude });
        setGpsGranted(true);
        setGpsLoading(false);
      },
      () => {
        // Fallback for demo — mock coords for Chennai
        setCoords({ lat: 13.0827, lon: 80.2707 });
        setGpsGranted(true);
        setGpsLoading(false);
      },
      { timeout: 6000 }
    );
  };

  const riskColor = (r) => r >= 1.3 ? '#C0392B' : r >= 1.0 ? '#E8A020' : '#1A8A5A';
  const riskLabel = (r) => r >= 1.3 ? 'High risk' : r >= 1.0 ? 'Moderate' : 'Low risk';

  const filtered = zones.filter(z =>
    z.name.toLowerCase().includes(search.toLowerCase()) ||
    z.city.toLowerCase().includes(search.toLowerCase())
  );

  const handleSubmit = async () => {
    if (!selected)    { setError('Select your primary working zone'); return; }
    if (!gpsGranted)  { setError('GPS access is required to verify your location during disruptions'); return; }
    setError(''); setLoading(true);
    try {
      const res = await RegisterAPI.step3({
        workerId,
        zoneId:   selected.id,
        zoneName: `${selected.name}, ${selected.city}`,
        lat:      coords?.lat || 13.0827,
        lon:      coords?.lon || 80.2707,
        gpsPermissionGranted: true,
        eShramUan: uan,
      });
      if (!res.data.success) { setError(res.data.message); setLoading(false); return; }
      setResult(res.data);
      setTimeout(() => nav('/register/step4'), 1800);
    } catch (e) {
      setError(e.response?.data?.message || 'Failed. Try again.');
      setLoading(false);
    }
  };

  return (
    <Shell step={3}>
      <SectionHead
        icon="📍"
        title="Location & Risk Profile"
        sub="Your zone determines your coverage and weekly premium"
      />

      {error  && <Alert type="error">{error}</Alert>}
      {result && <Alert type="success">✅ {result.message}. Moving to next step…</Alert>}

      {!result && <>
        {/* GPS permission */}
        <div style={{ background: gpsGranted ? '#E6F7F0' : '#EBF0FA', border: `1px solid ${gpsGranted ? '#1A8A5A' : '#C8D5E8'}`, borderRadius: 14, padding: '16px', marginBottom: 24 }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
            <span style={{ fontSize: 28 }}>{gpsGranted ? '✅' : '📡'}</span>
            <div style={{ flex: 1 }}>
              <div style={{ fontSize: 14, fontWeight: 600, color: gpsGranted ? '#0D5C3A' : '#1B3A6B' }}>
                {gpsGranted ? 'GPS access granted' : 'Enable GPS access'}
              </div>
              <div style={{ fontSize: 12, color: '#8896A5', marginTop: 2 }}>
                {gpsGranted
                  ? `Location: ${coords?.lat?.toFixed(4)}, ${coords?.lon?.toFixed(4)}`
                  : 'Required to verify your presence during disruption events'}
              </div>
            </div>
            {!gpsGranted && (
              <button onClick={handleGps} disabled={gpsLoading}
                style={{ padding: '8px 16px', background: '#1B3A6B', color: '#fff', border: 'none', borderRadius: 8, fontSize: 13, fontWeight: 600, cursor: 'pointer', fontFamily: 'inherit' }}>
                {gpsLoading ? '…' : 'Allow'}
              </button>
            )}
          </div>
        </div>

        {/* Zone search */}
        <Field label="Select primary working zone">
          <input
            placeholder="Search city or zone…"
            value={search} onChange={e => setSearch(e.target.value)}
            style={{ width: '100%', padding: '12px 14px', border: '1.5px solid #E2E8F0', borderRadius: 10, fontSize: 14, marginBottom: 10, background: '#F7F9FC', fontFamily: 'inherit', outline: 'none' }}
          />
          <div style={{ maxHeight: 240, overflowY: 'auto', display: 'flex', flexDirection: 'column', gap: 8 }}>
            {filtered.map(z => (
              <div key={z.id} onClick={() => setSelected(z)} style={{
                padding: '12px 14px', borderRadius: 10, cursor: 'pointer',
                border: selected?.id === z.id ? '2px solid #1B3A6B' : '1.5px solid #E2E8F0',
                background: selected?.id === z.id ? '#EBF0FA' : '#fff',
                display: 'flex', alignItems: 'center', justifyContent: 'space-between',
                transition: 'all 0.15s',
              }}>
                <div>
                  <div style={{ fontSize: 14, fontWeight: selected?.id === z.id ? 600 : 400, color: '#0D1B2A' }}>{z.name}</div>
                  <div style={{ fontSize: 12, color: '#8896A5' }}>{z.city}</div>
                </div>
                <div style={{ textAlign: 'right' }}>
                  <div style={{ fontSize: 13, fontWeight: 700, color: '#1B3A6B' }}>₹{z.estimatedWeeklyPremium}/wk</div>
                  <div style={{ fontSize: 11, color: riskColor(z.riskIndex), fontWeight: 500 }}>{riskLabel(z.riskIndex)}</div>
                </div>
              </div>
            ))}
          </div>
        </Field>

        {/* Selected zone premium preview */}
        {selected && (
          <div style={{ background: '#EBF0FA', borderRadius: 12, padding: '14px', marginBottom: 16 }}>
            <div style={{ fontSize: 12, color: '#8896A5', marginBottom: 4 }}>Estimated weekly premium</div>
            <div style={{ fontSize: 28, fontWeight: 800, color: '#1B3A6B' }}>₹{selected.estimatedWeeklyPremium}</div>
            <div style={{ fontSize: 12, color: '#4A5568', marginTop: 4 }}>Zone risk: {selected.riskIndex}× · {riskLabel(selected.riskIndex)}</div>
          </div>
        )}

        {/* e-Shram UAN (optional) */}
        <Field label="e-Shram UAN (optional)" hint="Universal Account Number — gold standard for unorganised worker verification">
          <input
            placeholder="UAN-XX-XXXX-XXXX-XXXX"
            value={uan} onChange={e => setUan(e.target.value)}
            style={{ width: '100%', padding: '13px 14px', border: '1.5px solid #E2E8F0', borderRadius: 10, fontSize: 14, background: '#F7F9FC', fontFamily: 'inherit', outline: 'none' }}
          />
        </Field>

        <Btn loading={loading} onClick={handleSubmit}>
          Confirm Zone & Continue →
        </Btn>
      </>}
    </Shell>
  );
}
