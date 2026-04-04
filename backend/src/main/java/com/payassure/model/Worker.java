package com.payassure.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "workers")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Worker {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    // ── Step 1 : Identity & Security ─────────────────────────────────────────
    @Column(nullable = false, unique = true)
    private String phone;

    @Column(nullable = false)
    private String passwordHash;

    @Column(unique = true)
    private String aadhaarNumber;           // masked after KYC

    private String legalName;               // from UIDAI
    private String aadhaarAddress;          // from UIDAI
    private String aadhaarPhotoUrl;         // from UIDAI (base64 or URL)
    private String aadhaarDob;              // from UIDAI
    private String aadhaarGender;           // from UIDAI
    private boolean livenessVerified;

    // ── Step 2 : Work Verification ───────────────────────────────────────────
    @Enumerated(EnumType.STRING)
    private DeliveryPlatform platform;      // RUSHDASH / QUICKBITE / ZIPDELIVER

    private String partnerId;               // extracted by OCR from fake ID
    private String platformNameOnId;        // extracted by OCR from fake ID
    private boolean workVerified;           // true when OCR name == Aadhaar name

    // ── Step 3 : Location & Risk Profile ─────────────────────────────────────
    private String zoneId;
    private String zoneName;
    private Double primaryLat;
    private Double primaryLon;
    private boolean gpsPermissionGranted;
    private String eShramUan;               // optional UAN

    // ── Step 4 : Payout Setup ────────────────────────────────────────────────
    private String upiId;
    private boolean pennyDropVerified;      // mock — always true for now
    private String nomineeName;
    private String nomineePhone;
    private boolean policyAgreed;

    // ── Status ───────────────────────────────────────────────────────────────
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private RegistrationStatus registrationStatus = RegistrationStatus.STEP_1_PENDING;

    @Builder.Default
    private boolean active = false;

    private LocalDateTime createdAt;
    private LocalDateTime activatedAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    // ── Enums ─────────────────────────────────────────────────────────────────
    public enum DeliveryPlatform {
        RUSHDASH,       // fake "Swiggy-like"
        QUICKBITE,      // fake "Zomato-like"
        ZIPDELIVER      // fake "Zepto-like"
    }

    public enum RegistrationStatus {
        STEP_1_PENDING,
        STEP_1_COMPLETE,   // phone + Aadhaar + liveness done
        STEP_2_COMPLETE,   // work ID verified
        STEP_3_COMPLETE,   // zone + GPS set
        ACTIVE             // payout set, policy signed
    }
}
