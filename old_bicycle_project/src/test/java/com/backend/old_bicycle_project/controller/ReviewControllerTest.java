package com.backend.old_bicycle_project.controller;

import com.backend.old_bicycle_project.dto.request.ReviewReplyRequestDTO;
import com.backend.old_bicycle_project.dto.request.ReviewRequestDTO;
import com.backend.old_bicycle_project.dto.response.ApiResponse;
import com.backend.old_bicycle_project.dto.response.ReviewResponseDTO;
import com.backend.old_bicycle_project.entity.User;
import com.backend.old_bicycle_project.service.ReviewService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewControllerTest {

    @Mock
    private ReviewService reviewService;

    @InjectMocks
    private ReviewController reviewController;

    @Test
    void submitReviewReturnsWrappedReview() {
        UUID orderId = UUID.randomUUID();
        UUID buyerId = UUID.randomUUID();
        User buyer = User.builder().id(buyerId).build();
        ReviewRequestDTO request = ReviewRequestDTO.builder()
                .rating(5)
                .comment("Xe ổn.")
                .build();
        ReviewResponseDTO review = ReviewResponseDTO.builder()
                .id(UUID.randomUUID())
                .orderId(orderId)
                .rating(5)
                .comment("Xe ổn.")
                .build();

        when(reviewService.submitReview(orderId, buyerId, request)).thenReturn(review);

        ApiResponse<ReviewResponseDTO> response = reviewController.submitReview(orderId, buyer, request).getBody();

        assertThat(response).isNotNull();
        assertThat(response.getResult()).isSameAs(review);
        verify(reviewService).submitReview(orderId, buyerId, request);
    }

    @Test
    void replyToReviewReturnsWrappedReviewReply() {
        UUID reviewId = UUID.randomUUID();
        UUID sellerId = UUID.randomUUID();
        User seller = User.builder().id(sellerId).build();
        ReviewReplyRequestDTO request = ReviewReplyRequestDTO.builder()
                .reply("Cảm ơn bạn.")
                .build();
        ReviewResponseDTO review = ReviewResponseDTO.builder()
                .id(reviewId)
                .sellerReply("Cảm ơn bạn.")
                .build();

        when(reviewService.replyToReview(reviewId, sellerId, request)).thenReturn(review);

        ApiResponse<ReviewResponseDTO> response = reviewController.replyToReview(reviewId, seller, request).getBody();

        assertThat(response).isNotNull();
        assertThat(response.getResult()).isSameAs(review);
        verify(reviewService).replyToReview(reviewId, sellerId, request);
    }
}
