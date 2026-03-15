package com.backend.old_bicycle_project.repository;

import com.backend.old_bicycle_project.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReviewRepository extends JpaRepository<Review, UUID> {

    // Check if an order already has a review
    boolean existsByOrderId(UUID orderId);

    // Get all reviews for a specific seller (reviewee)
    Page<Review> findByRevieweeIdOrderByCreatedAtDesc(UUID revieweeId, Pageable pageable);

    // Optional: Get a specific review by order
    Optional<Review> findByOrderId(UUID orderId);
}
