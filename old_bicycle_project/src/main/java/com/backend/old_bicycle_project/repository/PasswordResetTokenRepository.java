package com.backend.old_bicycle_project.repository;

import com.backend.old_bicycle_project.entity.PasswordResetToken;
import com.backend.old_bicycle_project.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {

    Optional<PasswordResetToken> findByToken(String token);

    @Modifying
    void deleteByUser(User user);
}
