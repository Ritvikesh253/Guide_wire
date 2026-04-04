package com.payassure.controller;

import com.payassure.dto.RegistrationDto.*;
import com.payassure.service.AadhaarService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * AadhaarController — Feature 5: Aadhaar ID Matching & Real Selfie Validation
 *
 * Endpoints:
 * - POST /api/aadhaar/otp          → Request OTP from UIDAI
 * - POST /api/aadhaar/verify       → Verify Aadhaar + OTP → fetch e-KYC
 * - POST /api/aadhaar/verify-face  → Compare selfie vs Aadhaar photo (liveness check)
 */
@RestController
@RequestMapping("/api/aadhaar")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Feature 5: Aadhaar Validation", description = "ID matching & real selfie validation endpoints")
public class AadhaarController {

    private final AadhaarService aadhaarService;

    /**
     * Feature 5.1: Request OTP from UIDAI
     * Triggers OTP send to the mobile number linked with Aadhaar.
     *
     * Request:  { "aadhaarNumber": "999941057058" }
     * Response: { "success": true, "message": "OTP sent to registered mobile" }
     */
    @PostMapping("/otp")
    @Operation(summary = "Request OTP from UIDAI", description = "Send OTP to Aadhaar-linked mobile")
    public ResponseEntity<?> requestOtp(@RequestBody OtpRequest request) {
        log.info("OTP request for Aadhaar ending in ...{}", request.getAadhaarNumber().substring(8));

        boolean success = aadhaarService.requestAadhaarOtp(request.getAadhaarNumber());
        return success
                ? ResponseEntity.ok(new ApiResponse(true, "OTP sent to registered mobile"))
                : ResponseEntity.status(400).body(new ApiResponse(false, "OTP request failed"));
    }

    /**
     * Feature 5.2: Verify Aadhaar + OTP → Fetch e-KYC
     * Validates the OTP and returns name, DOB, gender, address, photo (base64).
     *
     * Request:  { "aadhaarNumber": "999941057058", "otp": "123456" }
     * Response: {
     *   "verified": true,
     *   "legalName": "Arun Kumar",
     *   "dob": "15/01/1990",
     *   "gender": "M",
     *   "address": "...",
     *   "photoBase64": "iVBORw0KGgoA..."
     * }
     */
    @PostMapping("/verify")
    @Operation(summary = "Verify Aadhaar & fetch e-KYC", description = "Submit Aadhaar + OTP to get KYC data")
    public ResponseEntity<AadhaarKycData> verifyAadhaar(
            @RequestParam String aadhaarNumber,
            @RequestParam String otp) {

        log.info("Verifying Aadhaar ...{}", aadhaarNumber.substring(8));
        AadhaarKycData kycData = aadhaarService.fetchKyc(aadhaarNumber, otp);

        return ResponseEntity.ok(kycData);
    }

    /**
     * Feature 5.3: Liveness Check — Compare Selfie vs Aadhaar Photo
     * Uses AWS Rekognition / Google Vision / Local ML to verify the person
     * is the same as Aadhaar photo (real person, not a printed photo).
     *
     * Request:  {
     *   "selfieBase64": "iVBORw0KGgoA...",
     *   "referencePhotoBase64": "iVBORw0KGgoA..." (from Aadhaar KYC)
     * }
     * Response: {
     *   "match": true,
     *   "confidence": 0.96,
     *   "message": "Face match successful"
     * }
     */
    @PostMapping("/verify-face")
    @Operation(summary = "Verify face liveness", description = "Compare selfie against Aadhaar photo")
    public ResponseEntity<LivenessResult> verifyFace(
            @RequestParam String selfieBase64,
            @RequestParam String referencePhotoBase64) {

        log.info("Verifying face liveness — selfie length: {} chars", selfieBase64.length());
        LivenessResult result = aadhaarService.checkLiveness(selfieBase64, referencePhotoBase64);

        return ResponseEntity.ok(result);
    }

    // ── DTOs for Request/Response ─────────────────────────────────────────────

    public static class OtpRequest {
        public String aadhaarNumber;

        public String getAadhaarNumber() { return aadhaarNumber; }
        public void setAadhaarNumber(String aadhaarNumber) { this.aadhaarNumber = aadhaarNumber; }
    }

    public static class ApiResponse {
        public boolean success;
        public String message;

        public ApiResponse(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
    }
}
