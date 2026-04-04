package com.payassure.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.payassure.dto.RegistrationDto.LoginRequest;
import com.payassure.dto.RegistrationDto.RegistrationResponse;
import com.payassure.dto.RegistrationDto.SendOtpRequest;
import com.payassure.dto.RegistrationDto.Step1Request;
import com.payassure.dto.RegistrationDto.Step2Request;
import com.payassure.dto.RegistrationDto.Step3Request;
import com.payassure.dto.RegistrationDto.Step4Request;
import com.payassure.dto.RegistrationDto.VerifyOtpRequest;
import com.payassure.service.AuthService;
import com.payassure.service.RegistrationService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class RegistrationController {

    private final RegistrationService registrationService;
    private final AuthService         authService;

    // ── OTP ──────────────────────────────────────────────────────────────────

    /** Send OTP to phone number */
    @PostMapping("/auth/otp/send")
    public ResponseEntity<?> sendOtp(@RequestBody SendOtpRequest req) {
        try {
            RegistrationResponse result = registrationService.sendOtp(req.getPhone());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(false, "Failed to send OTP: " + e.getMessage()));
        }
    }

    /** Verify OTP entered by user */
    @PostMapping("/auth/otp/verify")
    public ResponseEntity<?> verifyOtp(@RequestBody VerifyOtpRequest req) {
        try {
            RegistrationResponse result = registrationService.verifyOtp(req);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(false, "OTP verification failed: " + e.getMessage()));
        }
    }

    // ── REGISTRATION STEPS ───────────────────────────────────────────────────

    /**
     * Step 1 — Identity & Security
     * Phone + password + Aadhaar number + Aadhaar OTP + liveness selfie
     */
    @PostMapping("/register/step1")
    public ResponseEntity<?> step1(@RequestBody Step1Request req) {
        try {
            return ResponseEntity.ok(registrationService.completeStep1(req));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(false, e.getMessage()));
        }
    }

    @PostMapping("/register/step2")
    public ResponseEntity<?> step2(@RequestBody Step2Request req) {
        try {
            return ResponseEntity.ok(registrationService.completeStep2(req));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(false, e.getMessage()));
        }
    }

    @PostMapping("/register/step3")
    public ResponseEntity<?> step3(@RequestBody Step3Request req) {
        try {
            return ResponseEntity.ok(registrationService.completeStep3(req));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(false, e.getMessage()));
        }
    }

    @PostMapping("/register/step4")
    public ResponseEntity<?> step4(@RequestBody Step4Request req) {
        try {
            return ResponseEntity.ok(registrationService.completeStep4(req));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(false, e.getMessage()));
        }
    }

    @PostMapping("/auth/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest req) {
        try {
            return ResponseEntity.ok(authService.login(req));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(false, "Login failed: " + e.getMessage()));
        }
    }

    @PostMapping("/auth/aadhaar/otp")
    public ResponseEntity<?> requestAadhaarOtp(@RequestBody SendOtpRequest req) {
        try {
            boolean sent = registrationService.requestAadhaarOtp(req.getPhone());
            return ResponseEntity.ok(RegistrationResponse.builder()
                    .success(sent)
                    .message(sent
                            ? "OTP sent to your Aadhaar-linked mobile number"
                            : "Failed to send Aadhaar OTP. Check your Aadhaar number.")
                    .build());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(false, e.getMessage()));
        }
    }

    // ── RESPONSE CLASSES ─────────────────────────────────────────────────────

    public static class ErrorResponse {
        public boolean success;
        public String message;

        public ErrorResponse(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
    }
}
