import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import Welcome      from './screens/Welcome';
import PhoneOtp     from './screens/PhoneOtp';
import Step1        from './screens/Step1';
import Step2        from './screens/Step2';
import Step3        from './screens/Step3';
import Step4        from './screens/Step4';
import Success      from './screens/Success';
import Login        from './screens/Login';

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/"          element={<Welcome />} />
        <Route path="/login"     element={<Login />} />
        <Route path="/otp"       element={<PhoneOtp />} />
        <Route path="/register/step1" element={<Step1 />} />
        <Route path="/register/step2" element={<Step2 />} />
        <Route path="/register/step3" element={<Step3 />} />
        <Route path="/register/step4" element={<Step4 />} />
        <Route path="/success"   element={<Success />} />
        <Route path="*"          element={<Navigate to="/" />} />
      </Routes>
    </BrowserRouter>
  );
}
