package com.payassure.repository;

import com.payassure.model.Worker;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface WorkerRepository extends JpaRepository<Worker, String> {
    Optional<Worker> findByPhone(String phone);
    boolean existsByPhone(String phone);
    boolean existsByAadhaarNumber(String aadhaarNumber);
    boolean existsByPartnerId(String partnerId);
    boolean existsByUpiId(String upiId);
}
