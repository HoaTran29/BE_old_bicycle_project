package com.backend.old_bicycle_project.repository;

import com.backend.old_bicycle_project.entity.PayoutProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PayoutProfileRepository extends JpaRepository<PayoutProfile, UUID> {
    Optional<PayoutProfile> findByUserId(UUID userId);
}
