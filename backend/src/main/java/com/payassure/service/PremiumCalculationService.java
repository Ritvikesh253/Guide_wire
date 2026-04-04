package com.payassure.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class PremiumCalculationService {

    @Value("${payassure.openweather.api-key:demo-key}")
    private String openWeatherApiKey;

    @Value("${payassure.openelevation.base-url:https://api.open-elevation.com}")
    private String openElevationUrl;

    @Value("${payassure.python.ml-service-url:http://localhost:5000}")
    private String pythonMlServiceUrl;

    @Autowired(required = false)
    private RestTemplate restTemplate;

    /**
     * Calculate dynamic premium based on:
     * 1. Zone risk index
     * 2. Elevation (from Open-Elevation API)
     * 3. Weather forecast (from OpenWeatherMap API)
     * 4. Flood history from database
     *
     * @param zoneId Zone ID
     * @param lat Latitude
     * @param lon Longitude
     * @param basePrice Base premium from zone
     * @return Calculated premium
     */
    public double calculateDynamicPremium(String zoneId, double lat, double lon, double basePrice) {
        try {
            double elevationRisk = fetchElevationRisk(lat, lon);
            double weatherRisk = fetchWeatherRisk(lat, lon);
            double floodHistoryRisk = getFloodHistoryRisk(zoneId);

            // ML Model: Random Forest Regressor
            double mlAdjustment = callPythonMlService(elevationRisk, weatherRisk, floodHistoryRisk);

            double finalPremium = basePrice * mlAdjustment;

            log.info("Premium calculated: base={}, elevation={}, weather={}, floodHistory={}, mlAdj={}, final={}",
                    basePrice, elevationRisk, weatherRisk, floodHistoryRisk, mlAdjustment, finalPremium);

            return Math.round(finalPremium * 100) / 100.0;  // Round to 2 decimals
        } catch (Exception e) {
            log.warn("Error calculating dynamic premium, returning base price", e);
            return basePrice;
        }
    }

    private double fetchElevationRisk(double lat, double lon) {
        // Call Open-Elevation API: https://api.open-elevation.com/api/v1/lookup?locations=lat,lon
        // Low elevation (near sea level) = High flood risk
        // Returns risk multiplier: 0.8 (safe) to 1.5 (high-risk)
        try {
            // Simulated: In production, call actual API
            if (lat < 13 && lon > 80) {  // Chennai floodplain
                return 1.3;  // Higher risk
            }
            return 0.9;  // Lower risk
        } catch (Exception e) {
            log.warn("Elevation API error", e);
            return 1.0;
        }
    }

    private double fetchWeatherRisk(double lat, double lon) {
        // Call OpenWeatherMap 7-day forecast
        // Rain forecast > 50mm in 7 days = increase premium
        // Returns multiplier: 0.7 (forecast clear) to 1.6 (monsoon expected)
        try {
            // Simulated: In production, call actual API
            return 1.0;  // Normal conditions
        } catch (Exception e) {
            log.warn("Weather API error", e);
            return 1.0;
        }
    }

    private double getFloodHistoryRisk(String zoneId) {
        // Query from flood_history table
        // Frequency of floods in past 5 years
        // Returns risk multiplier: 0.8 to 1.4
        try {
            // Simulated data
            if ("z-01".equals(zoneId) || "z-04".equals(zoneId)) {
                return 1.3;  // High flood zones (Adyar, Velachery)
            }
            return 0.9;
        } catch (Exception e) {
            log.warn("Flood history error", e);
            return 1.0;
        }
    }

    private double callPythonMlService(double elevation, double weather, double floodHistory) {
        // Call Python Random Forest model via REST API or native call
        // Input: [elevation_risk, weather_risk, flood_history_risk]
        // Output: premium_multiplier (1.0 = no change, 1.2 = 20% increase)
        try {
            // Simulated: In production, call Python service
            // For now, simple heuristic: average of inputs
            double avgRisk = (elevation + weather + floodHistory) / 3;
            return Math.min(avgRisk / 1.0, 1.5);  // Cap at 1.5x increase
        } catch (Exception e) {
            log.warn("ML service error", e);
            return 1.0;
        }
    }
}
