package com.backend.old_bicycle_project.service.impl;

import com.backend.old_bicycle_project.dto.request.ReviewReplyRequestDTO;
import com.backend.old_bicycle_project.dto.request.ReviewRequestDTO;
import com.backend.old_bicycle_project.dto.response.ReviewResponseDTO;
import com.backend.old_bicycle_project.entity.Order;
import com.backend.old_bicycle_project.entity.Review;
import com.backend.old_bicycle_project.entity.User;
import com.backend.old_bicycle_project.entity.enums.AppRole;
import com.backend.old_bicycle_project.entity.enums.OrderStatus;
import com.backend.old_bicycle_project.exception.AppException;
import com.backend.old_bicycle_project.repository.OrderRepository;
import com.backend.old_bicycle_project.repository.ReviewRepository;
import com.backend.old_bicycle_project.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewServiceImplTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ReviewServiceImpl reviewService;

    @Test
    void submitReviewCreatesReviewForCompletedBuyerOrder() {
        User buyer = user(AppRole.buyer, "buyer@test.dev");
        User seller = user(AppRole.seller, "seller@test.dev");
        Order order = Order.builder()
                .id(UUID.randomUUID())
                .buyer(buyer)
                .seller(seller)
                .status(OrderStatus.completed)
                .build();
        ReviewRequestDTO request = ReviewRequestDTO.builder()
                .rating(5)
                .comment("Xe đúng mô tả, giao dịch ổn.")
                .build();

        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));
        when(reviewRepository.existsByOrderId(order.getId())).thenReturn(false);
        when(reviewRepository.saveAndFlush(any(Review.class))).thenAnswer(invocation -> {
            Review review = invocation.getArgument(0);
            review.setId(UUID.randomUUID());
            return review;
        });
        when(reviewRepository.findWithDetailsById(any(UUID.class))).thenAnswer(invocation -> {
            UUID id = invocation.getArgument(0);
            Review hydratedReview = Review.builder()
                    .id(id)
                    .order(order)
                    .reviewer(buyer)
                    .reviewee(seller)
                    .rating(request.getRating())
                    .comment(request.getComment())
                    .build();
            return Optional.of(hydratedReview);
        });
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ReviewResponseDTO response = reviewService.submitReview(order.getId(), buyer.getId(), request);

        ArgumentCaptor<Review> reviewCaptor = ArgumentCaptor.forClass(Review.class);
        verify(reviewRepository).saveAndFlush(reviewCaptor.capture());

        assertThat(reviewCaptor.getValue().getReviewer()).isEqualTo(buyer);
        assertThat(reviewCaptor.getValue().getReviewee()).isEqualTo(seller);
        assertThat(response.getRating()).isEqualTo(5);
        assertThat(response.getComment()).isEqualTo("Xe đúng mô tả, giao dịch ổn.");
        assertThat(seller.getTotalReviews()).isEqualTo(1);
        assertThat(seller.getAverageRating()).isEqualTo(5.0);
    }

    @Test
    void replyToReviewAllowsSellerToCreateOrUpdateReply() {
        User buyer = user(AppRole.buyer, "buyer@test.dev");
        User seller = user(AppRole.seller, "seller@test.dev");
        Review review = Review.builder()
                .id(UUID.randomUUID())
                .order(Order.builder().id(UUID.randomUUID()).build())
                .reviewer(buyer)
                .reviewee(seller)
                .rating(4)
                .comment("Khung đẹp, xe đi ổn.")
                .build();

        when(reviewRepository.findWithDetailsById(review.getId())).thenReturn(Optional.of(review));
        when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ReviewResponseDTO response = reviewService.replyToReview(
                review.getId(),
                seller.getId(),
                ReviewReplyRequestDTO.builder().reply("Cảm ơn bạn, mình đã gửi thêm report kiểm định vào chat.").build()
        );

        assertThat(response.getSellerReply()).isEqualTo("Cảm ơn bạn, mình đã gửi thêm report kiểm định vào chat.");
        assertThat(response.getSellerRepliedAt()).isNotNull();
    }

    @Test
    void replyToReviewRejectsNonRevieweeSeller() {
        User buyer = user(AppRole.buyer, "buyer@test.dev");
        User seller = user(AppRole.seller, "seller@test.dev");
        User otherSeller = user(AppRole.seller, "other@test.dev");
        Review review = Review.builder()
                .id(UUID.randomUUID())
                .order(Order.builder().id(UUID.randomUUID()).build())
                .reviewer(buyer)
                .reviewee(seller)
                .rating(4)
                .comment("Khung đẹp, xe đi ổn.")
                .build();

        when(reviewRepository.findWithDetailsById(review.getId())).thenReturn(Optional.of(review));

        assertThatThrownBy(() -> reviewService.replyToReview(
                review.getId(),
                otherSeller.getId(),
                ReviewReplyRequestDTO.builder().reply("Không phải review của tôi.").build()
        )).isInstanceOf(AppException.class);
    }

    @Test
    void getSellerReviewsReturnsMappedReplyFields() {
        User buyer = user(AppRole.buyer, "buyer@test.dev");
        User seller = user(AppRole.seller, "seller@test.dev");
        Review review = Review.builder()
                .id(UUID.randomUUID())
                .order(Order.builder().id(UUID.randomUUID()).build())
                .reviewer(buyer)
                .reviewee(seller)
                .rating(5)
                .comment("Tốt")
                .sellerReply("Cảm ơn bạn")
                .build();

        when(userRepository.existsById(seller.getId())).thenReturn(true);
        when(reviewRepository.findByRevieweeIdOrderByCreatedAtDesc(seller.getId(), PageRequest.of(0, 10)))
                .thenReturn(new PageImpl<>(List.of(review)));

        ReviewResponseDTO response = reviewService.getSellerReviews(seller.getId(), PageRequest.of(0, 10))
                .getContent()
                .get(0);

        assertThat(response.getSellerReply()).isEqualTo("Cảm ơn bạn");
    }

    private User user(AppRole role, String email) {
        return User.builder()
                .id(UUID.randomUUID())
                .email(email)
                .firstName("Test")
                .lastName(role.name())
                .role(role)
                .build();
    }
}
