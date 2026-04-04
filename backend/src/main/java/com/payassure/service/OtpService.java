package com.payassure.service;

import java.time.LocalDateTime;
import java.util.Random;

import org.springframework.stereotype.Service;

import com.payassure.model.OtpRecord;
import com.payassure.repository.OtpRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class OtpService {

    private final OtpRepository otpRepository;

    /**
     * Generates a 6-digit OTP and persists it.
     * In production: integrate Twilio / MSG91 / Fast2SMS to send via SMS.
     * For hackathon: OTP is logged and returned in response for testing.
     */
    public String generateAndSendOtp(String phone) {
        try {
            String otp = String.format("%06d", new Random().nextInt(999999));

            OtpRecord record = OtpRecord.builder()
                    .phone(phone)
                    .otp(otp)
                    .expiresAt(LocalDateTime.now().plusMinutes(5))
                    .used(false)
                    .build();
            otpRepository.save(record);

            log.info("OTP Generated for phone {}: {}", phone, otp);
            return otp;
        } catch (Exception e) {
            log.error("Failed to generate OTP: {}", e.getMessage());
            throw new RuntimeException("OTP generation failed", e);
        }
    }

    public boolean verifyOtp(String phone, String otp) {
        try {
            return otpRepository
                    .findTopByPhoneAndUsedFalseOrderByExpiresAtDesc(phone)
                    .map(record -> {
                        if (record.isExpired()) {
                            log.warn("OTP expired for phone: {}", phone);
                            return false;
                        }
                        if (!record.getOtp().equals(otp)) {
                            log.warn("OTP mismatch for phone: {}", phone);
                            return false;
                        }
                        record.setUsed(true);
                        otpRepository.save(record);
                        log.info("OTP verified successfully for phone: {}", phone);
                        return true;
                    })
                    .orElse(false);
        } catch (Exception e) {
            log.error("OTP verification failed: {}", e.getMessage());
            return false;
        }
    }
}
