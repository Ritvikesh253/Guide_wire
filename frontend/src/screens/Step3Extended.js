import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Shell, SectionHead, Field, Btn, Alert } from '../components/UI';
import { RegisterAPI, ZoneAPI } from '../services/api';
import 'leaflet/dist/leaflet.css';
import L from 'leaflet';

export default function Step3Zones() {
  const nav = useNavigate();
  const workerId = sessionStorage.getItem('gs_workerId');
  
  const [selectedZone, setSelectedZone] = useState(null);
  const [zones, setZones] = useState([]);
  const [map, setMap] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [coordinates, setCoordinates] = useState({ lat: 13.0827, lon: 80.2707 });  // Chennai

  // Load available zones from backend
  useEffect(() => {
    const loadZones = async () => {
      try {
        const response = await ZoneAPI.list();
        const payload = Array.isArray(response.data) ? response.data : (response.data?.data || []);
        setZones(payload);
      } catch (e) {
        setError('Could not load zones: ' + (e.response?.data?.message || e.message));
      }
    };
    loadZones();
  }, []);

  // Initialize Leaflet map
  useEffect(() => {
    if (!map) {
      const mapInstance = L.map('map').setView([coordinates.lat, coordinates.lon], 11);
      
      L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
        attribution: '© OpenStreetMap contributors',
        maxZoom: 19,
      }).addTo(mapInstance);

      // Add zone markers
      zones.forEach((zone) => {
        if (zone.centerLat && zone.centerLon) {
          const color = zone.riskIndex < 0.9 ? 'green' : zone.riskIndex < 1.2 ? 'orange' : 'red';
          const circleMarker = L.circleMarker([zone.centerLat, zone.centerLon], {
            radius: 12,
            fillColor: color,
            color: color,
            weight: 2,
            opacity: 1,
            fillOpacity: 0.7,
            className: selectedZone?.id === zone.id ? 'selected' : '',
          });

          circleMarker.bindPopup(`
            <strong>${zone.name}</strong><br/>
            Risk: ${zone.riskIndex}<br/>
            Premium: ₹${zone.estimatedWeeklyPremium}/week
          `);

          circleMarker.on('click', () => {
            setSelectedZone(zone);
            setCoordinates({ lat: zone.centerLat, lon: zone.centerLon });
          });

          circleMarker.addTo(mapInstance);
        }
      });

      // Add click listener to map to select coordinates
      mapInstance.on('click', (e) => {
        setCoordinates({ lat: e.latlng.lat, lon: e.latlng.lng });
      });

      setMap(mapInstance);
    }

    return () => {
      // Cleanup on unmount
    };
  }, [zones, selectedZone, map]);

  const handleSubmit = async () => {
    if (!selectedZone) {
      setError('Please select a zone on the map');
      return;
    }

    setError('');
    setLoading(true);

    try {
      const response = await RegisterAPI.step3({
        workerId,
        zoneId: selectedZone.id,
        zoneName: selectedZone.name,
        lat: coordinates.lat,
        lon: coordinates.lon,
        gpsPermissionGranted: true,
        eShramUan: sessionStorage.getItem('pa_eshram') || '',
      });

      if (response.data.success) {
        sessionStorage.setItem('pa_zoneId', selectedZone.id);
        setTimeout(() => nav('/register/step4'), 1500);
      } else {
        setError(response.data.message);
      }
    } catch (e) {
      setError(e.response?.data?.message || 'Zone selection failed');
    } finally {
      setLoading(false);
    }
  };

  return (
    <Shell step={3} totalSteps={4}>
      <SectionHead
        icon="🗺️"
        title="Select Your Working Zone"
        sub="Choose your primary delivery area. Risk-based insurance premiums calculated in real-time."
      />

      {error && <Alert type="error">{error}</Alert>}

      {/* Map Container */}
      <div style={s.mapContainer} id="map"></div>

      {/* Zone Stats */}
      <div style={s.stats}>
        <div style={s.stat}>
          <span style={s.statLabel}>Coordinates:</span>
          <span style={s.statValue}>{coordinates.lat.toFixed(4)}, {coordinates.lon.toFixed(4)}</span>
        </div>
        {selectedZone && (
          <>
            <div style={s.stat}>
              <span style={s.statLabel}>Zone:</span>
              <span style={s.statValue}>{selectedZone.name}</span>
            </div>
            <div style={s.stat}>
              <span style={s.statLabel}>Risk Index:</span>
              <span style={{ ...s.statValue, color: selectedZone.riskIndex > 1.2 ? '#E74C3C' : '#27AE60' }}>
                {selectedZone.riskIndex} ({selectedZone.riskIndex < 0.9 ? 'Low' : selectedZone.riskIndex < 1.2 ? 'Moderate' : 'High'})
              </span>
            </div>
            <div style={s.stat}>
              <span style={s.statLabel}>Premium:</span>
              <span style={s.statValue}>₹{selectedZone.estimatedWeeklyPremium}/week</span>
            </div>
          </>
        )}
      </div>

      {/* Zone List */}
      <div style={s.zoneList}>
        <h3 style={s.zoneListTitle}>Available Zones</h3>
        {zones.map((zone) => (
          <div
            key={zone.id}
            style={{
              ...s.zoneItem,
              background: selectedZone?.id === zone.id ? '#EBF0FA' : '#fff',
              borderLeft: `4px solid ${zone.riskIndex > 1.2 ? '#E74C3C' : zone.riskIndex > 0.9 ? '#F39C12' : '#27AE60'}`,
            }}
            onClick={() => setSelectedZone(zone)}
          >
            <div style={s.zoneName}>{zone.name}</div>
            <div style={s.zoneDetails}>
              Risk: {zone.riskIndex} · ₹{zone.estimatedWeeklyPremium}/week
            </div>
          </div>
        ))}
      </div>

      <Btn loading={loading} onClick={handleSubmit}>
        Confirm Zone Selection
      </Btn>
    </Shell>
  );
}

const s = {
  mapContainer: { width: '100%', height: 280, borderRadius: 12, marginBottom: 20, overflow: 'hidden' },
  stats: { background: '#F7F9FC', padding: 16, borderRadius: 8, marginBottom: 20 },
  stat: { display: 'flex', justifyContent: 'space-between', padding: '8px 0', borderBottom: '1px solid #E8ECEF', fontSize: 13 },
  statLabel: { color: '#8896A5', fontWeight: 500 },
  statValue: { color: '#0D1B2A', fontWeight: 600, fontFamily: 'monospace' },
  zoneList: { marginBottom: 20 },
  zoneListTitle: { fontSize: 14, fontWeight: 600, color: '#0D1B2A', marginBottom: 12 },
  zoneItem: { padding: 12, borderRadius: 8, marginBottom: 8, cursor: 'pointer', border: '1px solid #E8ECEF', transition: 'all 0.2s' },
  zoneName: { fontWeight: 600, fontSize: 14, color: '#0D1B2A', marginBottom: 4 },
  zoneDetails: { fontSize: 12, color: '#8896A5' },
};
