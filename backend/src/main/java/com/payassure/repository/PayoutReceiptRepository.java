package com.payassure.repository;

import com.payassure.model.PayoutReceipt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PayoutReceiptRepository extends JpaRepository<PayoutReceipt, String> {
    List<PayoutReceipt> findByWorkerId(String workerId);
    List<PayoutReceipt> findByStatus(PayoutReceipt.PayoutStatus status);
    List<PayoutReceipt> findByClaimId(String claimId);
}
