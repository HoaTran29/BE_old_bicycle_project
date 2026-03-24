package com.backend.old_bicycle_project.repository;

import com.backend.old_bicycle_project.entity.Wishlist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface WishlistRepository extends JpaRepository<Wishlist, Wishlist.WishlistId> {

    boolean existsByUserIdAndProductId(UUID userId, UUID productId);

    List<Wishlist> findByUserIdOrderByCreatedAtDesc(UUID userId);

    long countByUserId(UUID userId);

    void deleteByUserIdAndProductId(UUID userId, UUID productId);
}
