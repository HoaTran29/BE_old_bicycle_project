package com.backend.old_bicycle_project.repository;

import com.backend.old_bicycle_project.entity.EmailVerification;
import com.backend.old_bicycle_project.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmailVerificationRepository extends JpaRepository<EmailVerification, UUID> {
    Optional<EmailVerification> findByToken(String token);

    @Modifying
    void deleteByUser(User user);
}
