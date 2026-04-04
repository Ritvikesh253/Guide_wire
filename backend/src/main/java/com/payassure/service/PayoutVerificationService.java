package com.payassure.service;

import com.payassure.dto.RegistrationDto.PennyDropResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * PayoutVerificationService — UPI Penny Drop
 *
 * Production: Razorpay Payout API / Cashfree Account Verification
 * Hackathon:  Mock — always returns success with Aadhaar name
 *
 * Real API (Razorpay):
 *   POST https://api.razorpay.com/v1/payments/validate/vpa
 *   Body: { "vpa": "worker@upi" }
 *   Returns: { "vpa": "...", "success": true, "customer_name": "..." }
 */
@Service
@Slf4j
public class PayoutVerificationService {

    public PennyDropResult verifyUpi(String upiId, String aadhaarName) {
        log.info("Penny drop verification for UPI: {}", upiId);

        if (upiId == null || !upiId.contains("@")) {
            return PennyDropResult.builder()
                    .success(false)
                    .message("Invalid UPI ID. Format: yourname@upi")
                    .build();
        }

        // Mock — ₹1 sent, name returned matches Aadhaar
        return PennyDropResult.builder()
                .success(true)
                .accountHolderName(aadhaarName)
                .nameMatchesAadhaar(true)
                .message("₹1 sent successfully. Account holder: " + aadhaarName)
                .build();
    }
}
