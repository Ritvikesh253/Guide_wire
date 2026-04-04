package com.payassure.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "claims")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Claim {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne
    @JoinColumn(name = "worker_id", nullable = false)
    private Worker worker;

    @Column(name = "policy_id")
    private String policyId;

    @Column(name = "disruption_event_id")
    private String disruptionEventId;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ClaimType claimType;  // PARAMETRIC, MANUAL

    @Column(nullable = false)
    private String triggerReason;  // RAIN>70MM, STRIKE, CURFEW

    @Column(name = "worker_gps_lat")
    private Double workerGpsLat;

    @Column(name = "worker_gps_lon")
    private Double workerGpsLon;

    @Column(name = "gps_zone_match")
    private Boolean gpsZoneMatch;

    @Column(nullable = false)
    private BigDecimal claimAmount;

    @Column(name = "payout_id")
    private String payoutId;

    @Column(name = "payout_status")
    @Enumerated(EnumType.STRING)
    private PayoutStatus payoutStatus;

    @Column(name = "payout_timestamp")
    private LocalDateTime payoutTimestamp;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ClaimStatus status;  // PENDING, APPROVED, REJECTED, PAID

    private LocalDateTime createdAt;
    private LocalDateTime approvedAt;
    private LocalDateTime paidAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) status = ClaimStatus.PENDING;
    }

    public enum ClaimType {
        PARAMETRIC, MANUAL
    }

    public enum ClaimStatus {
        PENDING, APPROVED, REJECTED, PAID
    }

    public enum PayoutStatus {
        INITIATED, PROCESSING, COMPLETED, FAILED
    }
}
