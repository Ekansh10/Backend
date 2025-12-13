package com.gradia.repository;

import com.gradia.model.EmployerProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmployerProfileRepository extends JpaRepository<EmployerProfile, UUID> {
    Optional<EmployerProfile> findByUserId(UUID userId);
    boolean existsByUserId(UUID userId);
    Optional<EmployerProfile> findByEmail(String email);
}

