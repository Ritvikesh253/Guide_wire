package com.payassure.service;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.RestTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

@Service
@Slf4j
public class RazorpayPayoutService {

    @Value("${payassure.razorpay.key-id:rzp_test_fake_key}")
    private String razorpayKeyId;

    @Value("${payassure.razorpay.key-secret:fake_secret}")
    private String razorpayKeySecret;

    @Autowired(required = false)
    private RestTemplate restTemplate;

    /**
     * Process instant UPI payout via RazorpayX API
     *
     * @param workerUpiId UPI ID of worker (e.g., "worker@bank")
     * @param amount Amount in rupees
     * @param claimId Claim ID for reference
     * @return Payout response
     */
    public PayoutResponse initiateUpiPayout(String workerUpiId, double amount, String claimId) {
        try {
            log.info("Initiating payout: upiId={}, amount={}, claimId={}", workerUpiId, amount, claimId);

            // Step 1: Create contact in Razorpay
            String contactId = createContact(workerUpiId);

            // Step 2: Create fund account
            String fundAccountId = createFundAccount(contactId, workerUpiId);

            // Step 3: Initiate payout
            String payoutId = createPayout(fundAccountId, (long) (amount * 100), claimId);  // Amount in paise

            return PayoutResponse.builder()
                    .success(true)
                    .payoutId(payoutId)
                    .amount(amount)
                    .status("INITIATED")
                    .message("Payout initiated successfully")
                    .build();

        } catch (Exception e) {
            log.error("Error initiating payout", e);
            return PayoutResponse.builder()
                    .success(false)
                    .status("FAILED")
                    .message("Payout failed: " + e.getMessage())
                    .build();
        }
    }

    private String createContact(String upiId) {
        // POST /api/v1/contacts
        // Contact represents a recipient
        try {
            // Simulated: In production, call Razorpay API
            return "cont_" + UUID.randomUUID().toString().substring(0, 10);
        } catch (Exception e) {
            throw new RuntimeException("Contact creation failed", e);
        }
    }

    private String createFundAccount(String contactId, String upiId) {
        // POST /api/v1/fund_accounts
        // Fund account links contact to their UPI/bank account
        try {
            // Simulated: In production, call Razorpay API
            return "fa_" + UUID.randomUUID().toString().substring(0, 10);
        } catch (Exception e) {
            throw new RuntimeException("Fund account creation failed", e);
        }
    }

    private String createPayout(String fundAccountId, long amountInPaise, String claimId) {
        // POST /api/v1/payouts
        // Sends money to the fund account
        try {
            // Simulated: In production, call Razorpay API
            PayoutRequest request = new PayoutRequest();
            request.setAccountNumber("2121021200061746");  // Razorpay X account
            request.setFundAccountId(fundAccountId);
            request.setAmount(amountInPaise);
            request.setCurrency("INR");
            request.setMode("NEFT");  // or "UPI"
            request.setReference(claimId);
            request.setNarrative("PayAssure claim payout");

            // Call Razorpay API would happen here
            String payoutId = "pout_" + UUID.randomUUID().toString().substring(0, 10);
            log.info("Payout created: {}", payoutId);
            return payoutId;

        } catch (Exception e) {
            throw new RuntimeException("Payout creation failed", e);
        }
    }

    public PayoutResponse getPayoutStatus(String payoutId) {
        try {
            // GET /api/v1/payouts/{payoutId}
            // Returns: { id, status, amount, created_at, ... }
            return PayoutResponse.builder()
                    .payoutId(payoutId)
                    .status("PROCESSED")
                    .message("Payout processed successfully")
                    .build();
        } catch (Exception e) {
            log.error("Error fetching payout status", e);
            return null;
        }
    }

    // ── Data Classes ───────────────────────────────────────────────────────────
    public static class PayoutRequest {
        private String accountNumber;
        private String fundAccountId;
        private long amount;
        private String currency;
        private String mode;
        private String reference;
        private String narrative;

        // Getters and Setters
        public String getAccountNumber() { return accountNumber; }
        public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }

        public String getFundAccountId() { return fundAccountId; }
        public void setFundAccountId(String fundAccountId) { this.fundAccountId = fundAccountId; }

        public long getAmount() { return amount; }
        public void setAmount(long amount) { this.amount = amount; }

        public String getCurrency() { return currency; }
        public void setCurrency(String currency) { this.currency = currency; }

        public String getMode() { return mode; }
        public void setMode(String mode) { this.mode = mode; }

        public String getReference() { return reference; }
        public void setReference(String reference) { this.reference = reference; }

        public String getNarrative() { return narrative; }
        public void setNarrative(String narrative) { this.narrative = narrative; }
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class PayoutResponse {
        private boolean success;
        private String payoutId;
        private double amount;
        private String status;
        private String message;
        private Long timestamp = System.currentTimeMillis();
    }
}
