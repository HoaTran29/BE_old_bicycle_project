package com.backend.old_bicycle_project.repository;

import com.backend.old_bicycle_project.entity.Review;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
public interface ReviewRepository extends JpaRepository<Review, UUID> {

    // Check if an order already has a review
    boolean existsByOrderId(UUID orderId);

    // Get all reviews for a specific seller (reviewee)
    @EntityGraph(attributePaths = {"order", "reviewer", "reviewee"})
    Page<Review> findByRevieweeIdOrderByCreatedAtDesc(UUID revieweeId, Pageable pageable);

    // Optional: Get a specific review by order
    @EntityGraph(attributePaths = {"order", "reviewer", "reviewee"})
    Optional<Review> findByOrderId(UUID orderId);

    @EntityGraph(attributePaths = {"order", "reviewer", "reviewee"})
    Optional<Review> findWithDetailsById(UUID id);

    @Query("select r.order.id from Review r where r.order.id in :orderIds")
    Set<UUID> findReviewedOrderIdsByOrderIds(@Param("orderIds") Collection<UUID> orderIds);
}
