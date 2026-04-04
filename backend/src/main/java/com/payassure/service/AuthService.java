package com.payassure.service;

import com.payassure.dto.RegistrationDto.*;
import com.payassure.model.Worker;
import com.payassure.repository.WorkerRepository;
import com.payassure.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final WorkerRepository workerRepository;
    private final PasswordEncoder  passwordEncoder;
    private final JwtUtil          jwtUtil;

    public LoginResponse login(LoginRequest req) {
        Worker worker = workerRepository.findByPhone(req.getPhone())
                .orElse(null);

        if (worker == null || !passwordEncoder.matches(req.getPassword(), worker.getPasswordHash())) {
            return LoginResponse.builder()
                    .success(false)
                    .message("Invalid phone number or password.")
                    .build();
        }

        String token = jwtUtil.generateToken(worker.getId(), worker.getPhone());

        return LoginResponse.builder()
                .success(true)
                .token(token)
                .workerId(worker.getId())
                .workerName(worker.getLegalName())
                .registrationStatus(worker.getRegistrationStatus())
                .message("Login successful")
                .build();
    }
}
