package com.payassure.service;

import com.payassure.dto.RegistrationDto.ZoneDto;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ZoneService {

    /**
     * Returns available zones with risk index and estimated premium.
     * In production: fetch from PostGIS zones table.
     * Risk index: 0.7 (safe) → 1.5 (flood-prone)
     */
    public List<ZoneDto> getAvailableZones() {
        return List.of(
            ZoneDto.builder().id("z-01").name("Adyar").city("Chennai")
                .riskIndex(1.5).estimatedWeeklyPremium(68.0).build(),
            ZoneDto.builder().id("z-02").name("T-Nagar").city("Chennai")
                .riskIndex(0.9).estimatedWeeklyPremium(42.0).build(),
            ZoneDto.builder().id("z-03").name("Guindy").city("Chennai")
                .riskIndex(0.7).estimatedWeeklyPremium(29.0).build(),
            ZoneDto.builder().id("z-04").name("Velachery").city("Chennai")
                .riskIndex(1.4).estimatedWeeklyPremium(63.0).build(),
            ZoneDto.builder().id("z-05").name("HSR Layout").city("Bangalore")
                .riskIndex(1.0).estimatedWeeklyPremium(46.0).build(),
            ZoneDto.builder().id("z-06").name("Koramangala").city("Bangalore")
                .riskIndex(0.8).estimatedWeeklyPremium(35.0).build(),
            ZoneDto.builder().id("z-07").name("Indiranagar").city("Bangalore")
                .riskIndex(0.75).estimatedWeeklyPremium(32.0).build(),
            ZoneDto.builder().id("z-08").name("Andheri").city("Mumbai")
                .riskIndex(1.3).estimatedWeeklyPremium(58.0).build(),
            ZoneDto.builder().id("z-09").name("Bandra").city("Mumbai")
                .riskIndex(1.1).estimatedWeeklyPremium(50.0).build(),
            ZoneDto.builder().id("z-10").name("Connaught Place").city("Delhi")
                .riskIndex(0.85).estimatedWeeklyPremium(38.0).build()
        );
    }

    public ZoneDto getZoneById(String zoneId) {
        return getAvailableZones().stream()
                .filter(z -> z.getId().equals(zoneId))
                .findFirst()
                .orElse(null);
    }
}
