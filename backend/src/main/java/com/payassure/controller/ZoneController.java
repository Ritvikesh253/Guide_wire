package com.payassure.controller;

import com.payassure.dto.RegistrationDto.*;
import com.payassure.service.ZoneService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/zones")
@RequiredArgsConstructor
public class ZoneController {

    @Autowired
    private ZoneService zoneService;

    @GetMapping
    public ResponseEntity<?> getAvailableZones() {
        try {
            List<ZoneDto> zones = zoneService.getAvailableZones();
            return ResponseEntity.ok(new RegistrationResponse(true, "Zones retrieved", null, null, zones));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new RegistrationResponse(false, e.getMessage(), null, null, null));
        }
    }

    @GetMapping("/{zoneId}")
    public ResponseEntity<?> getZoneById(@PathVariable String zoneId) {
        try {
            ZoneDto zone = zoneService.getZoneById(zoneId);
            if (zone == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(zone);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
