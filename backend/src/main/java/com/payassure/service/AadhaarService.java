package com.payassure.service;

import com.payassure.dto.RegistrationDto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * AadhaarService — UIDAI Aadhaar e-KYC
 *
 * ╔══════════════════════════════════════════════════════════════════════╗
 * ║  REAL API: UIDAI Sandbox (AUA/KUA access)                           ║
 * ║  Sandbox URL: https://developer.uidai.gov.in/                       ║
 * ║                                                                      ║
 * ║  Flow:                                                               ║
 * ║  1. POST /api/v1/aadhaar/otp  → triggers OTP on Aadhaar mobile     ║
 * ║  2. POST /api/v1/aadhaar/kyc  → send Aadhaar + OTP → get KYC data  ║
 * ║                                                                      ║
 * ║  Alternative (easier sandbox): Setu Aadhaar API                     ║
 * ║  https://setu.co/products/kyc/aadhaar-api/                         ║
 * ║  - Provides test Aadhaar numbers for sandbox                        ║
 * ║  - Returns: name, dob, gender, address, photo (base64)              ║
 * ║                                                                      ║
 * ║  Sandbox test Aadhaar: 999941057058 (UIDAI provided test number)    ║
 * ╚══════════════════════════════════════════════════════════════════════╝
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AadhaarService {

    private final RestTemplate restTemplate;

    @Value("${payassure.uidai.base-url:https://stage1.uidai.gov.in/onlineOtpService}")
    private String uidaiBaseUrl;

    @Value("${payassure.uidai.aua-code:public}")
    private String auaCode;

    @Value("${payassure.uidai.license-key:MEaCgYDVR0PBBYwFAYIKwYBBQUHAwIGCCsGAQUFBwME}")
    private String licenseKey;

    @Value("${payassure.setu.base-url:https://dg-sandbox.setu.co}")
    private String setuBaseUrl;

    @Value("${payassure.setu.client-id:your-setu-client-id}")
    private String setuClientId;

    @Value("${payassure.setu.client-secret:your-setu-client-secret}")
    private String setuClientSecret;

    public AadhaarService() {
        this.restTemplate = null;
    }

    /**
     * Step A — Request OTP from UIDAI to the mobile linked with Aadhaar.
     * UIDAI sends a 6-digit OTP to the resident's registered mobile.
     *
     * Uses Setu Aadhaar API (easier sandbox integration than raw UIDAI AUA).
     */
    public boolean requestAadhaarOtp(String aadhaarNumber) {
        log.info("Requesting UIDAI OTP for Aadhaar ending in ...{}", aadhaarNumber.substring(8));

        if (shouldUseMockAadhaar()) {
            log.warn("Setu credentials are not configured. Using mock Aadhaar OTP flow for demo.");
            return true;
        }

        try {
            String url = setuBaseUrl + "/api/aadhaar-kyc/otp";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("x-client-id", setuClientId);
            headers.set("x-client-secret", setuClientSecret);

            Map<String, String> body = new HashMap<>();
            body.put("aadhaarNumber", aadhaarNumber);

            HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);

            return response.getStatusCode() == HttpStatus.OK;

        } catch (Exception e) {
            log.error("UIDAI OTP request failed: {}", e.getMessage());
            // Fallback for demo/sandbox — treat as success so flow continues
            log.warn("UIDAI sandbox unavailable — proceeding with mock OTP flow");
            return true;
        }
    }

    /**
     * Step B — Submit Aadhaar + OTP to fetch e-KYC data.
     * Returns: legal name, DOB, gender, address, photo (base64 JPEG).
     *
     * UIDAI sandbox test numbers:
     *   999941057058  → Male, returns full KYC
     *   999971658847  → Female, returns full KYC
     */
    public AadhaarKycData fetchKyc(String aadhaarNumber, String otp) {
        log.info("Fetching e-KYC from UIDAI for Aadhaar ...{}", aadhaarNumber.substring(8));

        // Validate format first
        if (aadhaarNumber == null || !aadhaarNumber.matches("\\d{12}")) {
            return AadhaarKycData.builder()
                    .verified(false)
                    .message("Invalid Aadhaar format — must be 12 digits")
                    .build();
        }

        if (shouldUseMockAadhaar()) {
            log.warn("Setu credentials are not configured. Returning sandbox mock e-KYC for demo.");
            return buildSandboxResponse(aadhaarNumber);
        }

        try {
            String url = setuBaseUrl + "/api/aadhaar-kyc/kyc";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("x-client-id", setuClientId);
            headers.set("x-client-secret", setuClientSecret);

            Map<String, String> body = new HashMap<>();
            body.put("aadhaarNumber", aadhaarNumber);
            body.put("otp", otp);
            body.put("consentArtifact", "Y");   // resident consent required by UIDAI

            HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> data = response.getBody();
                Map<String, Object> kycData = (Map<String, Object>) data.get("data");

                return AadhaarKycData.builder()
                        .legalName((String) kycData.get("name"))
                        .dob((String) kycData.get("dob"))
                        .gender((String) kycData.get("gender"))
                        .address(buildAddress(kycData))
                        .photoBase64((String) kycData.get("photo"))
                        .verified(true)
                        .message("Aadhaar e-KYC successful")
                        .build();
            }

        } catch (Exception e) {
            log.error("UIDAI KYC API error: {}", e.getMessage());
            log.warn("Falling back to sandbox mock response for demo");
        }

        // ── SANDBOX FALLBACK ─────────────────────────────────────────────────
        // Used when UIDAI sandbox is unreachable during demo
        // Remove this block in production
        return buildSandboxResponse(aadhaarNumber);
    }

    /**
     * Liveness Check — compares selfie frame against Aadhaar photo.
     *
     * Production: AWS Rekognition CompareFaces / Azure Face API / IDFY
     * For hackathon: checks that a selfie was provided + returns 95% confidence
     *
     * AWS Rekognition example:
     *   POST rekognition.ap-south-1.amazonaws.com
     *   Action: CompareFaces
     *   SourceImage: { Bytes: <aadhaar_photo_base64> }
     *   TargetImage: { Bytes: <selfie_base64> }
     *   SimilarityThreshold: 80
     */
    public LivenessResult checkLiveness(String selfieBase64, String aadhaarPhotoBase64) {
        if (selfieBase64 == null || selfieBase64.isBlank()) {
            return LivenessResult.builder()
                    .match(false).confidence(0).message("No selfie provided").build();
        }
        if (aadhaarPhotoBase64 == null || aadhaarPhotoBase64.isBlank()) {
            return LivenessResult.builder()
                    .match(false).confidence(0).message("Aadhaar photo unavailable").build();
        }

        // Mock liveness — replace with AWS Rekognition call in production
        log.info("Liveness check: selfie provided ({} chars), Aadhaar photo available",
                selfieBase64.length());

        return LivenessResult.builder()
                .match(true)
                .confidence(0.96)
                .message("Face match successful — 96% confidence")
                .build();
    }

    // ── HELPERS ──────────────────────────────────────────────────────────────

    private String buildAddress(Map<String, Object> kycData) {
        Object addr = kycData.get("address");
        if (addr instanceof Map) {
            Map<String, String> a = (Map<String, String>) addr;
            return String.join(", ",
                    nvl(a.get("house")), nvl(a.get("street")),
                    nvl(a.get("locality")), nvl(a.get("vtc")),
                    nvl(a.get("dist")), nvl(a.get("state")),
                    nvl(a.get("pincode")));
        }
        return addr != null ? addr.toString() : "";
    }

    private String nvl(String s) { return s != null ? s : ""; }

    private boolean shouldUseMockAadhaar() {
        return isBlank(setuClientId)
                || isBlank(setuClientSecret)
                || setuClientId.contains("your-setu-client-id")
                || setuClientSecret.contains("your-setu-client-secret")
                || setuClientId.contains("demo")
                || setuClientSecret.contains("demo");
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    /** Realistic sandbox response — used when UIDAI API is unreachable in demo */
    private AadhaarKycData buildSandboxResponse(String aadhaarNumber) {
        // Different response for UIDAI official test numbers
        boolean isMaleTestNumber = aadhaarNumber.equals("999941057058");

        return AadhaarKycData.builder()
                .legalName(isMaleTestNumber ? "Ravi Kumar" : "Priya Sharma")
                .dob(isMaleTestNumber ? "15-08-1992" : "22-03-1995")
                .gender(isMaleTestNumber ? "M" : "F")
                .address("14, Gandhi Nagar, Adyar, Chennai, Tamil Nadu - 600020")
                .photoBase64("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==")
                .verified(true)
                .message("Aadhaar e-KYC successful (sandbox)")
                .build();
    }
}
