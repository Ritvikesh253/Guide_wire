package com.payassure.repository;

import com.payassure.model.Claim;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ClaimRepository extends JpaRepository<Claim, String> {
    List<Claim> findByWorkerId(String workerId);
    List<Claim> findByStatus(Claim.ClaimStatus status);
    List<Claim> findByDisruptionEventId(String disruptionEventId);
    Optional<Claim> findById(String id);
}
