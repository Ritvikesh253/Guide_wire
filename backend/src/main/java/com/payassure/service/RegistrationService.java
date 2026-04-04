package com.payassure.service;

import com.payassure.dto.RegistrationDto.*;
import com.payassure.model.Worker;
import com.payassure.repository.WorkerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class RegistrationService {

    private final WorkerRepository      workerRepository;
    private final OtpService            otpService;
    private final AadhaarService        aadhaarService;
    private final OcrService            ocrService;
    private final PayoutVerificationService payoutService;
    private final ZoneService           zoneService;
    private final PasswordEncoder       passwordEncoder;

    // ── AADHAAR OTP (UIDAI) ───────────────────────────────────────────────────

    public boolean requestAadhaarOtp(String aadhaarNumber) {
        return aadhaarService.requestAadhaarOtp(aadhaarNumber);
    }

    // ── PHONE OTP ─────────────────────────────────────────────────────────────

    public RegistrationResponse sendOtp(String phone) {
        if (phone == null || !phone.matches("\\d{10}"))
            return fail("Enter a valid 10-digit mobile number.");

        String otp = otpService.generateAndSendOtp(phone);

        return RegistrationResponse.builder()
                .success(true)
                .message("OTP sent to +91-" + phone.substring(0,5) + "XXXXX")
                .data(otp)   // ← dev only; remove in production
                .build();
    }

    public RegistrationResponse verifyOtp(VerifyOtpRequest req) {
        if (!otpService.verifyOtp(req.getPhone(), req.getOtp()))
            return fail("Invalid or expired OTP. Please try again.");

        return RegistrationResponse.builder()
                .success(true)
                .message("Phone number verified")
                .nextStep(Worker.RegistrationStatus.STEP_1_PENDING)
                .build();
    }

    // ── STEP 1 — Identity & Security (Aadhaar + Liveness) ────────────────────

    @Transactional
    public RegistrationResponse completeStep1(Step1Request req) {

        if (workerRepository.existsByPhone(req.getPhone()))
            return fail("This phone number is already registered.");

        if (workerRepository.existsByAadhaarNumber(req.getAadhaarNumber()))
            return fail("This Aadhaar is already linked to another account.");

        // ── 1a. Aadhaar OTP + e-KYC via UIDAI / Setu ─────────────────────
        AadhaarKycData kyc = aadhaarService.fetchKyc(req.getAadhaarNumber(), req.getAadhaarOtp());
        if (!kyc.isVerified())
            return fail("Aadhaar verification failed: " + kyc.getMessage());

        // ── 1b. Liveness check — selfie vs Aadhaar photo ──────────────────
        LivenessResult liveness = aadhaarService.checkLiveness(
                req.getLivenessSelfieBase64(), kyc.getPhotoBase64());
        if (!liveness.isMatch())
            return fail("Liveness check failed — face does not match Aadhaar photo. " +
                        "Confidence: " + (int)(liveness.getConfidence() * 100) + "%");

        // ── Save worker after Step 1 ───────────────────────────────────────
        Worker worker = Worker.builder()
                .phone(req.getPhone())
                .passwordHash(passwordEncoder.encode(req.getPassword()))
                .aadhaarNumber(req.getAadhaarNumber())
                .legalName(kyc.getLegalName())
                .aadhaarAddress(kyc.getAddress())
                .aadhaarPhotoUrl(kyc.getPhotoBase64())
                .aadhaarDob(kyc.getDob())
                .aadhaarGender(kyc.getGender())
                .livenessVerified(true)
                .registrationStatus(Worker.RegistrationStatus.STEP_1_COMPLETE)
                .build();

        Worker saved = workerRepository.save(worker);
        log.info("Step 1 complete for worker {} ({})", kyc.getLegalName(), saved.getId());

        return RegistrationResponse.builder()
                .success(true)
                .message("Identity verified — Welcome, " + kyc.getLegalName() + "!")
                .workerId(saved.getId())
                .nextStep(Worker.RegistrationStatus.STEP_2_COMPLETE)
                .data(AadhaarKycData.builder()
                        .legalName(kyc.getLegalName())
                        .dob(kyc.getDob())
                        .gender(kyc.getGender())
                        .address(kyc.getAddress())
                        .verified(true).build())
                .build();
    }

    // ── STEP 2 — Work Verification (OCR + Name Match) ─────────────────────────

    @Transactional
    public RegistrationResponse completeStep2(Step2Request req) {
        Worker worker = findWorker(req.getWorkerId());

        // OCR: parse fake delivery ID card image
        OcrResult ocr = ocrService.processDeliveryId(
                req.getPlatform(),
                req.getIdCardImageBase64(),   // raw OCR text from ML Kit
                worker.getLegalName());

        if (!ocr.isNameMatchesAadhaar())
            return fail(ocr.getMessage());

        if (workerRepository.existsByPartnerId(ocr.getExtractedPartnerId()))
            return fail("This Partner ID (" + ocr.getExtractedPartnerId() +
                        ") is already registered with another account.");

        worker.setPlatform(Worker.DeliveryPlatform.valueOf(req.getPlatform().toUpperCase()));
        worker.setPartnerId(ocr.getExtractedPartnerId());
        worker.setPlatformNameOnId(ocr.getExtractedPlatform());
        worker.setWorkVerified(true);
        worker.setRegistrationStatus(Worker.RegistrationStatus.STEP_2_COMPLETE);
        workerRepository.save(worker);

        log.info("Step 2 complete — Partner ID: {}", ocr.getExtractedPartnerId());

        return RegistrationResponse.builder()
                .success(true)
                .message("Work ID verified — " + ocr.getExtractedPlatform() +
                         " Partner ID: " + ocr.getExtractedPartnerId())
                .workerId(worker.getId())
                .nextStep(Worker.RegistrationStatus.STEP_3_COMPLETE)
                .data(ocr)
                .build();
    }

    // ── STEP 3 — Zone & GPS ───────────────────────────────────────────────────

    @Transactional
    public RegistrationResponse completeStep3(Step3Request req) {
        Worker worker = findWorker(req.getWorkerId());

        if (!req.isGpsPermissionGranted())
            return fail("GPS access is required. Without it, we cannot verify your " +
                        "location during a disruption event to trigger payouts.");

        worker.setZoneId(req.getZoneId());
        worker.setZoneName(req.getZoneName());
        worker.setPrimaryLat(req.getLat());
        worker.setPrimaryLon(req.getLon());
        worker.setGpsPermissionGranted(true);
        if (req.getEShramUan() != null && !req.getEShramUan().isBlank())
            worker.setEShramUan(req.getEShramUan());
        worker.setRegistrationStatus(Worker.RegistrationStatus.STEP_3_COMPLETE);
        workerRepository.save(worker);

        ZoneDto zone = zoneService.getZoneById(req.getZoneId());
        log.info("Step 3 complete — Zone: {}", req.getZoneName());

        return RegistrationResponse.builder()
                .success(true)
                .message("Zone set: " + req.getZoneName() +
                         " | Estimated premium: ₹" +
                         (zone != null ? zone.getEstimatedWeeklyPremium() : "—") + "/week")
                .workerId(worker.getId())
                .nextStep(Worker.RegistrationStatus.ACTIVE)
                .data(zone)
                .build();
    }

    // ── STEP 4 — Payout + Activation ─────────────────────────────────────────

    @Transactional
    public RegistrationResponse completeStep4(Step4Request req) {
        Worker worker = findWorker(req.getWorkerId());

        if (!req.isPolicyAgreed())
            return fail("You must agree to the policy terms to activate coverage.");

        // Penny drop — mock
        PennyDropResult penny = payoutService.verifyUpi(req.getUpiId(), worker.getLegalName());
        if (!penny.isSuccess())
            return fail("UPI verification failed: " + penny.getMessage());

        worker.setUpiId(req.getUpiId());
        worker.setPennyDropVerified(true);
        worker.setNomineeName(req.getNomineeName());
        worker.setNomineePhone(req.getNomineePhone());
        worker.setPolicyAgreed(true);
        worker.setRegistrationStatus(Worker.RegistrationStatus.ACTIVE);
        worker.setActive(true);
        worker.setActivatedAt(LocalDateTime.now());
        workerRepository.save(worker);

        log.info("Worker {} is now ACTIVE", worker.getLegalName());

        return RegistrationResponse.builder()
                .success(true)
                .message("Coverage is now ACTIVE — " + worker.getLegalName() +
                         ", you're protected by PayAssure!")
                .workerId(worker.getId())
                .nextStep(Worker.RegistrationStatus.ACTIVE)
                .data(penny)
                .build();
    }

    // ── HELPERS ──────────────────────────────────────────────────────────────

    private Worker findWorker(String id) {
        return workerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Worker session not found. Please restart."));
    }

    private RegistrationResponse fail(String msg) {
        return RegistrationResponse.builder().success(false).message(msg).build();
    }
}
