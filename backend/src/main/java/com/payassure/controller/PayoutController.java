package com.payassure.controller;

import com.payassure.dto.RegistrationDto.*;
import com.payassure.service.RazorpayPayoutService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/payouts")
@RequiredArgsConstructor
public class PayoutController {

    @Autowired
    private RazorpayPayoutService razorpayPayoutService;

    @PostMapping("/initiate")
    public ResponseEntity<?> initiatePayout(
            @RequestParam String upiId,
            @RequestParam double amount,
            @RequestParam String claimId) {
        try {
            RazorpayPayoutService.PayoutResponse response = razorpayPayoutService
                    .initiateUpiPayout(upiId, amount, claimId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(RazorpayPayoutService.PayoutResponse.builder()
                            .success(false)
                            .status("FAILED")
                            .message(e.getMessage())
                            .build());
        }
    }

    @GetMapping("/{payoutId}/status")
    public ResponseEntity<?> getPayoutStatus(@PathVariable String payoutId) {
        try {
            RazorpayPayoutService.PayoutResponse response = razorpayPayoutService
                    .getPayoutStatus(payoutId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
