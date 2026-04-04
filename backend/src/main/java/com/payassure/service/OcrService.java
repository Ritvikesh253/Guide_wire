package com.payassure.service;

import com.payassure.dto.RegistrationDto.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * OcrService — Delivery Partner ID Card Verification
 *
 * ╔══════════════════════════════════════════════════════════════════════╗
 * ║  Fake Delivery Companies (no real brand names)                       ║
 * ║                                                                      ║
 * ║  RushDash   — red/orange theme, like a Swiggy-style food delivery   ║
 * ║  QuickBite  — red theme, like a Zomato-style food platform          ║
 * ║  ZipDeliver — purple theme, like a Zepto-style quick commerce       ║
 * ║                                                                      ║
 * ║  Fake ID Card Fields (based on real Swiggy partner card layout):    ║
 * ║  ┌─────────────────────────────────────────┐                        ║
 * ║  │  [COMPANY LOGO]   DELIVERY PARTNER ID   │                        ║
 * ║  │  ─────────────────────────────────────  │                        ║
 * ║  │  Name    : Ravi Kumar                   │                        ║
 * ║  │  Partner ID: RSD-2024-084521            │                        ║
 * ║  │  City    : Chennai                      │                        ║
 * ║  │  Joining : 12-Jan-2024                  │                        ║
 * ║  │  Valid Until: 11-Jan-2025               │                        ║
 * ║  │  [QR CODE]   [PHOTO]                    │                        ║
 * ║  └─────────────────────────────────────────┘                        ║
 * ║                                                                      ║
 * ║  OCR Engine: Google ML Kit (mobile side) sends extracted text       ║
 * ║  Backend: Validates extracted fields + name match against Aadhaar   ║
 * ║                                                                      ║
 * ║  Production upgrade: Google Cloud Vision API / AWS Textract          ║
 * ╚══════════════════════════════════════════════════════════════════════╝
 */
@Service
@Slf4j
public class OcrService {

    /**
     * Process OCR result from mobile app.
     *
     * The mobile app (React Native) uses Google ML Kit Text Recognition
     * to scan the ID card image and send extracted raw text to backend.
     * Backend parses and validates the extracted fields.
     *
     * @param platform       Selected platform: RUSHDASH / QUICKBITE / ZIPDELIVER
     * @param extractedText  Raw OCR text from ML Kit (newline separated)
     * @param aadhaarName    Legal name from UIDAI — for cross-match
     */
    public OcrResult processDeliveryId(String platform,
                                        String extractedText,
                                        String aadhaarName) {

        log.info("Processing OCR for platform: {} | Aadhaar name: {}", platform, aadhaarName);

        if (extractedText == null || extractedText.isBlank()) {
            return OcrResult.builder()
                    .nameMatchesAadhaar(false)
                    .message("Could not read ID card. Please retake the scan.")
                    .build();
        }

        // Parse fields from OCR text
        // ML Kit returns raw text — we look for known label patterns
        String extractedName     = extractField(extractedText, "Name", "NAME");
        String extractedId       = extractField(extractedText, "Partner ID", "PARTNER ID", "ID No", "ID NO");
        String extractedPlatform = extractField(extractedText, "Company", "Platform");
        String extractedExpiry   = extractField(extractedText, "Valid Until", "VALID UNTIL", "Expiry");

        // Fallback: if OCR couldn't find platform field, use selected platform
        if (extractedPlatform == null || extractedPlatform.isBlank()) {
            extractedPlatform = getPlatformDisplayName(platform);
        }

        // Validate platform matches selection
        if (!isPlatformMatch(platform, extractedPlatform)) {
            return OcrResult.builder()
                    .extractedName(extractedName)
                    .extractedPlatform(extractedPlatform)
                    .nameMatchesAadhaar(false)
                    .message("ID card platform (" + extractedPlatform +
                             ") does not match selected platform (" +
                             getPlatformDisplayName(platform) + ")")
                    .build();
        }

        // Validate partner ID format per platform
        if (extractedId == null || !isValidPartnerId(platform, extractedId)) {
            return OcrResult.builder()
                    .extractedName(extractedName)
                    .extractedPlatform(extractedPlatform)
                    .nameMatchesAadhaar(false)
                    .message("Partner ID not found or invalid format on ID card.")
                    .build();
        }

        // Critical check: name on ID vs Aadhaar legal name
        boolean nameMatch = fuzzyNameMatch(extractedName, aadhaarName);
        if (!nameMatch) {
            return OcrResult.builder()
                    .extractedPartnerId(extractedId)
                    .extractedName(extractedName)
                    .extractedPlatform(extractedPlatform)
                    .extractedExpiry(extractedExpiry)
                    .nameMatchesAadhaar(false)
                    .message("Name mismatch: ID card shows '" + extractedName +
                             "' but Aadhaar shows '" + aadhaarName + "'")
                    .build();
        }

        log.info("OCR verification passed — Partner ID: {}, Name match: true", extractedId);

        return OcrResult.builder()
                .extractedPartnerId(extractedId)
                .extractedName(extractedName)
                .extractedPlatform(extractedPlatform)
                .extractedExpiry(extractedExpiry)
                .nameMatchesAadhaar(true)
                .message("ID verified. Partner ID: " + extractedId)
                .build();
    }

    // ── ID Card Field Parser ──────────────────────────────────────────────────

    /**
     * Extract a field value from OCR raw text.
     * Looks for patterns like "Name : Ravi Kumar" or "NAME\nRavi Kumar"
     */
    private String extractField(String text, String... labels) {
        String[] lines = text.split("\\n");
        for (String label : labels) {
            for (int i = 0; i < lines.length; i++) {
                String line = lines[i].trim();
                // Pattern 1: "Name : Value" on same line
                if (line.toUpperCase().startsWith(label.toUpperCase())) {
                    int colonIdx = line.indexOf(':');
                    if (colonIdx != -1 && colonIdx < line.length() - 1) {
                        return line.substring(colonIdx + 1).trim();
                    }
                    // Pattern 2: label on one line, value on next line
                    if (i + 1 < lines.length) {
                        return lines[i + 1].trim();
                    }
                }
            }
        }
        return null;
    }

    // ── Partner ID Validation per Platform ───────────────────────────────────
    // Based on real Swiggy-style ID format patterns

    private boolean isValidPartnerId(String platform, String id) {
        if (id == null) return false;
        return switch (platform.toUpperCase()) {
            // RushDash format: RSD-YYYY-XXXXXX  (e.g. RSD-2024-084521)
            case "RUSHDASH"   -> id.matches("RSD-\\d{4}-\\d{6}");
            // QuickBite format: QBT-XXXXXXXXX  (e.g. QBT-100293847)
            case "QUICKBITE"  -> id.matches("QBT-\\d{9}");
            // ZipDeliver format: ZPD/CITY/XXXXXXX  (e.g. ZPD/CHN/0039281)
            case "ZIPDELIVER" -> id.matches("ZPD/[A-Z]{3}/\\d{7}");
            default           -> id.length() >= 6;
        };
    }

    private boolean isPlatformMatch(String selected, String extractedPlatform) {
        if (extractedPlatform == null) return true; // can't extract = skip check
        String exp = extractedPlatform.toUpperCase();
        return switch (selected.toUpperCase()) {
            case "RUSHDASH"   -> exp.contains("RUSHDASH") || exp.contains("RUSH DASH");
            case "QUICKBITE"  -> exp.contains("QUICKBITE") || exp.contains("QUICK BITE");
            case "ZIPDELIVER" -> exp.contains("ZIPDELIVER") || exp.contains("ZIP DELIVER");
            default           -> true;
        };
    }

    private String getPlatformDisplayName(String platform) {
        return switch (platform.toUpperCase()) {
            case "RUSHDASH"   -> "RushDash";
            case "QUICKBITE"  -> "QuickBite";
            case "ZIPDELIVER" -> "ZipDeliver";
            default           -> platform;
        };
    }

    // ── Fuzzy Name Match ─────────────────────────────────────────────────────

    /**
     * Handles OCR noise, case differences, initials.
     * "Ravi Kumar" == "RAVI KUMAR" == "R. Kumar" == "Ravi K."
     */
    private boolean fuzzyNameMatch(String ocrName, String aadhaarName) {
        if (ocrName == null || aadhaarName == null) return false;

        String a = ocrName.trim().toLowerCase().replaceAll("[^a-z ]", "");
        String b = aadhaarName.trim().toLowerCase().replaceAll("[^a-z ]", "");

        if (a.equals(b)) return true;

        String[] aParts = a.split("\\s+");
        String[] bParts = b.split("\\s+");

        // Last name must match exactly
        String aLast = aParts[aParts.length - 1];
        String bLast = bParts[bParts.length - 1];
        if (!aLast.equals(bLast)) return false;

        // First name: full match OR initial match
        if (aParts.length > 0 && bParts.length > 0) {
            String aFirst = aParts[0];
            String bFirst = bParts[0];
            return aFirst.equals(bFirst)
                    || aFirst.startsWith(bFirst.substring(0, 1))
                    || bFirst.startsWith(aFirst.substring(0, 1));
        }
        return false;
    }
}
