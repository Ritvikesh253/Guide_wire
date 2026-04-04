import axios from 'axios';

const api = axios.create({ baseURL: '/api' });

// Attach JWT on every request if available
api.interceptors.request.use(cfg => {
  const token =
    localStorage.getItem('gs_token') ||
    sessionStorage.getItem('gs_token') ||
    localStorage.getItem('pa_token') ||
    sessionStorage.getItem('pa_token');
  if (token) cfg.headers.Authorization = `Bearer ${token}`;
  return cfg;
});

// Error handling
api.interceptors.response.use(
  res => res,
  err => {
    if (err.response?.status === 401) {
      localStorage.removeItem('gs_token');
      sessionStorage.removeItem('gs_token');
      localStorage.removeItem('pa_token');
      sessionStorage.removeItem('pa_token');
      window.location.href = '/login';
    }
    return Promise.reject(err);
  }
);

export const AuthAPI = {
  sendOtp:        (phone)       => api.post('/auth/otp/send',    { phone }),
  verifyOtp:      (phone, otp)  => api.post('/auth/otp/verify',  { phone, otp }),
  sendAadhaarOtp: (aadhaarNum)  => api.post('/auth/aadhaar/otp', { phone: aadhaarNum }),
  login:          (phone, pass) => api.post('/auth/login',        { phone, password: pass }),
};

export const RegisterAPI = {
  step1: (data) => api.post('/register/step1', data),
  step2: (data) => api.post('/register/step2', data),
  step3: (data) => api.post('/register/step3', data),
  step4: (data) => api.post('/register/step4', data),
};

// Feature 1: Zone Selection with Leaflet.js + OpenStreetMap
export const ZoneAPI = {
  list:     ()        => api.get('/zones'),
  getById:  (zoneId)  => api.get(`/zones/${zoneId}`),
};

// Feature 2: ML-Driven Dynamic Premium Calculation (Random Forest)
export const PremiumAPI = {
  calculate: (zoneId, lat, lon, basePrice) => 
    api.post('/premium/calculate', null, { params: { zoneId, lat, lon, basePrice } }),
};

// Feature 3: Parametric Automation (60s polling, auto-claims on disruption)
export const ClaimAPI = {
  createManual: (workerId, amount, reason) => 
    api.post('/claims/manual', null, { params: { workerId, amount, reason } }),
  triggerDisruption: (zone, rainMm, status) => 
    api.get('/claims/trigger-disruption', { params: { zone, rainMm, status } }),
};

// Feature 4: Instant Payouts (RazorpayX UPI)
export const PayoutAPI = {
  initiate: (upiId, amount, claimId) => 
    api.post('/payouts/initiate', null, { params: { upiId, amount, claimId } }),
  status:   (payoutId) => 
    api.get(`/payouts/${payoutId}/status`),
};

// Feature 5: Aadhaar ID Matching + Real Selfie Validation
export const AadhaarAPI = {
  validate:     (aadhaarNumber) => api.post('/aadhaar/validate', { aadhaarNumber }),
  verifyFace:   (selfieBase64, referencePhotoBase64) => 
    api.post('/aadhaar/verify-face', { selfieBase64, referencePhotoBase64 }),
};

export default api;
