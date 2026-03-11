package com.backend.old_bicycle_project.service.impl;

import com.backend.old_bicycle_project.dto.request.ReviewRequestDTO;
import com.backend.old_bicycle_project.dto.response.ReviewResponseDTO;
import com.backend.old_bicycle_project.entity.Order;
import com.backend.old_bicycle_project.entity.Review;
import com.backend.old_bicycle_project.entity.User;
import com.backend.old_bicycle_project.entity.enums.OrderStatus;
import com.backend.old_bicycle_project.exception.AppException;
import com.backend.old_bicycle_project.exception.ErrorCode;
import com.backend.old_bicycle_project.repository.OrderRepository;
import com.backend.old_bicycle_project.repository.ReviewRepository;
import com.backend.old_bicycle_project.repository.UserRepository;
import com.backend.old_bicycle_project.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public ReviewResponseDTO submitReview(UUID orderId, ReviewRequestDTO requestDTO) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.RECORD_NOT_EXISTS)); // Should be ORDER_NOT_FOUND but using closest

        // Ensure order is COMPLETED before allowing review
        if (order.getStatus() != OrderStatus.COMPLETED) {
            throw new AppException(ErrorCode.INVALID_STATUS);
        }

        // Validate reviewer is the buyer
        if (!order.getBuyer().getId().equals(requestDTO.getReviewerId())) {
            throw new AppException(ErrorCode.FORBIDDEN);
        }

        // Validate if review already exists
        if (reviewRepository.existsByOrderId(orderId)) {
            throw new AppException(ErrorCode.RECORD_ALREADY_EXISTS);
        }

        User reviewer = order.getBuyer();
        User reviewee = order.getSeller();

        Review review = Review.builder()
                .order(order)
                .reviewer(reviewer)
                .reviewee(reviewee)
                .rating(requestDTO.getRating())
                .comment(requestDTO.getComment())
                .build();

        review = reviewRepository.save(review);

        // Update Seller's average rating
        updateSellerAverageRating(reviewee, requestDTO.getRating());

        return mapToDTO(review);
    }

    @Override
    public Page<ReviewResponseDTO> getSellerReviews(UUID sellerId, Pageable pageable) {
        if (!userRepository.existsById(sellerId)) {
            throw new AppException(ErrorCode.USER_NOT_EXISTED);
        }

        Page<Review> reviews = reviewRepository.findByRevieweeIdOrderByCreatedAtDesc(sellerId, pageable);
        return reviews.map(this::mapToDTO);
    }

    private void updateSellerAverageRating(User seller, int newRating) {
        int currentCount = seller.getTotalReviews() != null ? seller.getTotalReviews() : 0;
        double currentAverage = seller.getAverageRating() != null ? seller.getAverageRating() : 0.0;

        double totalScore = (currentAverage * currentCount) + newRating;
        int newCount = currentCount + 1;
        double newAverage = totalScore / newCount;

        // Round to 1 decimal place
        newAverage = Math.round(newAverage * 10.0) / 10.0;

        seller.setTotalReviews(newCount);
        seller.setAverageRating(newAverage);
        userRepository.save(seller);
    }

    private ReviewResponseDTO mapToDTO(Review review) {
        return ReviewResponseDTO.builder()
                .id(review.getId())
                .orderId(review.getOrder().getId())
                .reviewerId(review.getReviewer().getId())
                .reviewerName(review.getReviewer().getFullName())
                .revieweeId(review.getReviewee().getId())
                .revieweeName(review.getReviewee().getFullName())
                .rating(review.getRating())
                .comment(review.getComment())
                .createdAt(review.getCreatedAt())
                .build();
    }
}
