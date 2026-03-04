package com.backend.old_bicycle_project.repository;

import com.backend.old_bicycle_project.entity.RefreshToken;
import com.backend.old_bicycle_project.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {
    Optional<RefreshToken> findByToken(String token);

    @Modifying
    void deleteAllByUser(User user);
}
