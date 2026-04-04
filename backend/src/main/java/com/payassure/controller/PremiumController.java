package com.payassure.controller;

import com.payassure.dto.RegistrationDto.*;
import com.payassure.service.PremiumCalculationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/premium")
@RequiredArgsConstructor
public class PremiumController {

    @Autowired
    private PremiumCalculationService premiumCalculationService;

    @PostMapping("/calculate")
    public ResponseEntity<?> calculatePremium(
            @RequestParam String zoneId,
            @RequestParam double lat,
            @RequestParam double lon,
            @RequestParam double basePrice) {
        try {
            double dynamicPremium = premiumCalculationService.calculateDynamicPremium(
                    zoneId, lat, lon, basePrice);
            
            return ResponseEntity.ok(new RegistrationResponse(
                    true,
                    "Premium calculated",
                    null,
                    null,
                    new Object() {
                        public double premium = dynamicPremium;
                    }
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new RegistrationResponse(false, e.getMessage(), null, null, null));
        }
    }
}
