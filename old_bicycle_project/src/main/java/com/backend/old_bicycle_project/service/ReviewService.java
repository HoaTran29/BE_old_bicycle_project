package com.backend.old_bicycle_project.service;

import com.backend.old_bicycle_project.dto.request.ReviewRequestDTO;
import com.backend.old_bicycle_project.dto.response.ReviewResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ReviewService {

    /**
     * Buyer submits a review for an order
     */
    ReviewResponseDTO submitReview(UUID orderId, ReviewRequestDTO requestDTO);

    /**
     * Get paginated reviews for a specific seller
     */
    Page<ReviewResponseDTO> getSellerReviews(UUID sellerId, Pageable pageable);
}
