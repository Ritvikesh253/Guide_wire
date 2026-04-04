package com.payassure.dto;

import com.payassure.model.Worker;
import lombok.*;

public class RegistrationDto {

    // ── OTP ───────────────────────────────────────────────────────────────────
    @Data public static class SendOtpRequest   { private String phone; }
    @Data public static class VerifyOtpRequest { private String phone; private String otp; }

    // ── STEP 1 ────────────────────────────────────────────────────────────────
    @Data
    public static class Step1Request {
        private String phone;
        private String password;
        private String aadhaarNumber;       // 12-digit
        private String aadhaarOtp;          // OTP sent by UIDAI to Aadhaar-linked mobile
        private String livenessSelfieBase64;// 2-second selfie frame (base64)
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class AadhaarKycData {
        private String legalName;
        private String dob;
        private String gender;
        private String address;
        private String photoBase64;         // UIDAI returns base64 photo
        private boolean verified;
        private String message;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class LivenessResult {
        private boolean match;
        private double confidence;
        private String message;
    }

    // ── STEP 2 ────────────────────────────────────────────────────────────────
    @Data
    public static class Step2Request {
        private String workerId;
        // RUSHDASH / QUICKBITE / ZIPDELIVER
        private String platform;
        // base64 image of the fake delivery ID card — OCR runs on backend
        private String idCardImageBase64;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class OcrResult {
        private String extractedPartnerId;
        private String extractedName;
        private String extractedPlatform;
        private String extractedExpiry;
        private boolean nameMatchesAadhaar;
        private String message;
    }

    // ── STEP 3 ────────────────────────────────────────────────────────────────
    @Data
    public static class Step3Request {
        private String workerId;
        private String zoneId;
        private String zoneName;
        private Double lat;
        private Double lon;
        private boolean gpsPermissionGranted;
        private String eShramUan;   // optional
    }

    // ── STEP 4 ────────────────────────────────────────────────────────────────
    @Data
    public static class Step4Request {
        private String workerId;
        private String upiId;
        private String nomineeName;
        private String nomineePhone;
        private boolean policyAgreed;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class PennyDropResult {
        private boolean success;
        private String accountHolderName;
        private boolean nameMatchesAadhaar;
        private String message;
    }

    // ── LOGIN ─────────────────────────────────────────────────────────────────
    @Data public static class LoginRequest { private String phone; private String password; }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class LoginResponse {
        private boolean success;
        private String token;
        private String workerId;
        private String workerName;
        private Worker.RegistrationStatus registrationStatus;
        private String message;
    }

    // ── ZONE ──────────────────────────────────────────────────────────────────
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ZoneDto {
        private String id;
        private String name;
        private String city;
        private double riskIndex;
        private double estimatedWeeklyPremium;
        private String riskLabel;   // "Low" / "Moderate" / "High"
    }

    // ── COMMON RESPONSE ───────────────────────────────────────────────────────
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class RegistrationResponse {
        private boolean success;
        private String message;
        private String workerId;
        private Worker.RegistrationStatus nextStep;
        private Object data;
    }
}
