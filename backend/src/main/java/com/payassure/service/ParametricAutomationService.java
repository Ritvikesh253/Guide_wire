package com.payassure.service;

import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.client.RestTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import lombok.extern.slf4j.Slf4j;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
@Slf4j
public class ParametricAutomationService {

    @Autowired(required = false)
    private RestTemplate restTemplate;

    @Autowired(required = false)
    private RazorpayPayoutService razorpayPayoutService;

    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Runs every 60 seconds to fetch disruption data from Mock API
     * and trigger automatic claims and payouts
     */
    @Scheduled(fixedRate = 60000)  // 60 seconds
    public void monitorDisruptionApi() {
        try {
            log.info("Monitoring disruption API for new events...");
            
            // Call Mock API: GET /api/disruptionEvents
            // Response: { zone: "Velachery", rain_mm: 95, status: "RED_ALERT", strike: "TRUE" }
            DisruptionEvent event = fetchDisruptionEvent();
            
            if (event != null && isClaimTriggered(event)) {
                log.info("Disruption detected: {}", event);
                
                // Step 1: Find all active policies in affected zone
                // Step 2: Validate worker GPS location
                // Step 3: Initiate claims
                // Step 4: Schedule instant payout
                processDisruptionClaim(event);
            }
        } catch (Exception e) {
            log.error("Error monitoring disruption API", e);
        }
    }

    private DisruptionEvent fetchDisruptionEvent() {
        try {
            // Mock API configuration
            String mockApiUrl = "http://localhost:8090/api/disruptionEvents";
            
            // In production: Call actual API
            // String response = restTemplate.getForObject(mockApiUrl, String.class);
            
            // Simulated response for demo
            DisruptionEvent event = new DisruptionEvent();
            event.setZone("Velachery");
            event.setRainMm(95);
            event.setStatus("RED_ALERT");
            event.setStrike("TRUE");
            return event;
        } catch (Exception e) {
            log.warn("Could not fetch disruption event", e);
            return null;
        }
    }

    private boolean isClaimTriggered(DisruptionEvent event) {
        // Claim is triggered if:
        // - rain_mm > 70 OR
        // - status == "RED_ALERT" OR
        // - strike == "TRUE"
        return (event.getRainMm() > 70) || 
               "RED_ALERT".equalsIgnoreCase(event.getStatus()) ||
               "TRUE".equalsIgnoreCase(event.getStrike());
    }

    private void processDisruptionClaim(DisruptionEvent event) {
        try {
            log.info("Processing claim for zone: {}", event.getZone());
            
            // TODO: Implement claim processing:
            // 1. Find workers in affected zone
            // 2. Verify their GPS location at claim time
            // 3. Create claim record
            // 4. Trigger Razorpay payout
            
            if (razorpayPayoutService != null) {
                // razorpayPayoutService.initiatePayoutForZone(event.getZone(), event);
            }
        } catch (Exception e) {
            log.error("Error processing disruption claim", e);
        }
    }

    // ── Data Class ─────────────────────────────────────────────────────────────
    public static class DisruptionEvent {
        private String zone;
        private double rainMm;
        private String status;
        private String strike;

        // Getters and Setters
        public String getZone() { return zone; }
        public void setZone(String zone) { this.zone = zone; }

        public double getRainMm() { return rainMm; }
        public void setRainMm(double rainMm) { this.rainMm = rainMm; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public String getStrike() { return strike; }
        public void setStrike(String strike) { this.strike = strike; }

        @Override
        public String toString() {
            return "DisruptionEvent{" +
                    "zone='" + zone + '\'' +
                    ", rainMm=" + rainMm +
                    ", status='" + status + '\'' +
                    ", strike='" + strike + '\'' +
                    '}';
        }
    }
}
