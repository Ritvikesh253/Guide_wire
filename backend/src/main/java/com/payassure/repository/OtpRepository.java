package com.payassure.repository;

import com.payassure.model.OtpRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface OtpRepository extends JpaRepository<OtpRecord, String> {
    Optional<OtpRecord> findTopByPhoneAndUsedFalseOrderByExpiresAtDesc(String phone);
}
