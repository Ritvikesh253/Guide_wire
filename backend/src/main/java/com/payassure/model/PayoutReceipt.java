package com.payassure.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payout_receipts")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PayoutReceipt {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne
    @JoinColumn(name = "claim_id", nullable = false)
    private Claim claim;

    @ManyToOne
    @JoinColumn(name = "worker_id", nullable = false)
    private Worker worker;

    @Column(name = "razorpay_payout_id")
    private String razorpayPayoutId;

    @Column(name = "razorpay_contact_id")
    private String razorpayContactId;

    @Column(name = "razorpay_fund_account_id")
    private String razorpayFundAccountId;

    @Column(name = "upi_id")
    private String upiId;

    @Column(name = "amount_rupees")
    private BigDecimal amountRupees;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private PayoutStatus status;

    @Column(columnDefinition = "TEXT")
    private String failureReason;

    private LocalDateTime initiatedAt;
    private LocalDateTime completedAt;

    @Column(name = "bank_reference")
    private String bankReference;

    public enum PayoutStatus {
        INITIATED, PROCESSING, PROCESSED, FAILED
    }

    @PrePersist
    protected void onCreate() {
        if (initiatedAt == null) initiatedAt = LocalDateTime.now();
        if (status == null) status = PayoutStatus.INITIATED;
    }
}
