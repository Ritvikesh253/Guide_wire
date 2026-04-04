package com.payassure.controller;

import com.payassure.dto.RegistrationDto.*;
import com.payassure.service.ParametricAutomationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/claims")
@RequiredArgsConstructor
public class ClaimController {

    @Autowired
    private ParametricAutomationService parametricAutomationService;

    @PostMapping("/manual")
    public ResponseEntity<?> createManualClaim(
            @RequestParam String workerId,
            @RequestParam double amount,
            @RequestParam String reason) {
        try {
            return ResponseEntity.ok(new RegistrationResponse(
                    true,
                    "Manual claim created",
                    workerId,
                    null,
                    new Object() {
                        public double claimAmount = amount;
                        public String triggerReason = reason;
                    }
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new RegistrationResponse(false, e.getMessage(), null, null, null));
        }
    }

    @GetMapping("/trigger-disruption")
    public ResponseEntity<?> triggerDisruptionManually(
            @RequestParam String zone,
            @RequestParam double rainMm,
            @RequestParam String status) {
        try {
            // For testing: manually trigger the parametric automation
            parametricAutomationService.monitorDisruptionApi();
            
            return ResponseEntity.ok(new RegistrationResponse(
                    true,
                    "Disruption event processed",
                    null,
                    null,
                    new Object() {
                        public String affectedZone = zone;
                        public double rainfall = rainMm;
                    }
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new RegistrationResponse(false, e.getMessage(), null, null, null));
        }
    }
}
