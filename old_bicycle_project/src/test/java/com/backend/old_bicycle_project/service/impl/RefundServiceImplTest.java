package com.backend.old_bicycle_project.service.impl;

import com.backend.old_bicycle_project.dto.request.RefundCreateRequestDTO;
import com.backend.old_bicycle_project.dto.request.RefundReviewRequestDTO;
import com.backend.old_bicycle_project.dto.response.RefundResponseDTO;
import com.backend.old_bicycle_project.entity.Order;
import com.backend.old_bicycle_project.entity.Payment;
import com.backend.old_bicycle_project.entity.RefundRequest;
import com.backend.old_bicycle_project.entity.User;
import com.backend.old_bicycle_project.entity.enums.AppRole;
import com.backend.old_bicycle_project.entity.enums.OrderFundingStatus;
import com.backend.old_bicycle_project.entity.enums.OrderStatus;
import com.backend.old_bicycle_project.entity.enums.PaymentMethod;
import com.backend.old_bicycle_project.entity.enums.PaymentPhase;
import com.backend.old_bicycle_project.entity.enums.PaymentStatus;
import com.backend.old_bicycle_project.entity.enums.RefundStatus;
import com.backend.old_bicycle_project.repository.OrderRepository;
import com.backend.old_bicycle_project.repository.PaymentRepository;
import com.backend.old_bicycle_project.repository.RefundRequestRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefundServiceImplTest {

    @Mock
    private RefundRequestRepository refundRequestRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private RefundServiceImpl refundService;

    @Test
    void requestRefundMovesOrderIntoRefundPending() {
        User buyer = user(AppRole.buyer, "buyer@test.dev");
        Order order = depositedOrder(buyer);
        Payment payment = successfulUpfrontPayment(order);

        when(orderRepository.findByIdAndBuyerId(order.getId(), buyer.getId())).thenReturn(Optional.of(order));
        when(refundRequestRepository.findFirstByOrderIdAndStatusOrderByCreatedAtDesc(order.getId(), RefundStatus.pending))
                .thenReturn(Optional.empty());
        when(paymentRepository.findFirstByOrderIdAndPhaseOrderByCreatedAtDesc(order.getId(), PaymentPhase.upfront))
                .thenReturn(Optional.of(payment));
        when(refundRequestRepository.save(any(RefundRequest.class))).thenAnswer(invocation -> {
            RefundRequest refundRequest = invocation.getArgument(0);
            refundRequest.setId(UUID.randomUUID());
            return refundRequest;
        });
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RefundResponseDTO response = refundService.requestRefund(order.getId(), buyer, RefundCreateRequestDTO.builder()
                .reason("Seller description does not match actual bicycle condition")
                .build());

        assertThat(response.getStatus()).isEqualTo(RefundStatus.pending);
        assertThat(response.getAmount()).isEqualByComparingTo("2000000");
        assertThat(order.getFundingStatus()).isEqualTo(OrderFundingStatus.refund_pending);
    }

    @Test
    void requestRefundIsStillAllowedWhileWaitingForBuyerConfirmation() {
        User buyer = user(AppRole.buyer, "buyer@test.dev");
        Order order = depositedOrder(buyer);
        order.setStatus(OrderStatus.awaiting_buyer_confirmation);
        Payment payment = successfulUpfrontPayment(order);

        when(orderRepository.findByIdAndBuyerId(order.getId(), buyer.getId())).thenReturn(Optional.of(order));
        when(refundRequestRepository.findFirstByOrderIdAndStatusOrderByCreatedAtDesc(order.getId(), RefundStatus.pending))
                .thenReturn(Optional.empty());
        when(paymentRepository.findFirstByOrderIdAndPhaseOrderByCreatedAtDesc(order.getId(), PaymentPhase.upfront))
                .thenReturn(Optional.of(payment));
        when(refundRequestRepository.save(any(RefundRequest.class))).thenAnswer(invocation -> {
            RefundRequest refundRequest = invocation.getArgument(0);
            refundRequest.setId(UUID.randomUUID());
            return refundRequest;
        });
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RefundResponseDTO response = refundService.requestRefund(order.getId(), buyer, RefundCreateRequestDTO.builder()
                .reason("Xe nhận được không đúng tình trạng đã cam kết")
                .build());

        assertThat(response.getStatus()).isEqualTo(RefundStatus.pending);
        assertThat(order.getFundingStatus()).isEqualTo(OrderFundingStatus.refund_pending);
    }

    @Test
    void completeApprovedRefundCancelsOrderAndMarksPaymentRefunded() {
        User buyer = user(AppRole.buyer, "buyer@test.dev");
        User admin = user(AppRole.admin, "admin@test.dev");
        Order order = depositedOrder(buyer);
        Payment payment = successfulUpfrontPayment(order);
        RefundRequest refundRequest = RefundRequest.builder()
                .id(UUID.randomUUID())
                .order(order)
                .payment(payment)
                .requester(buyer)
                .amount(new BigDecimal("2000000"))
                .reason("Seller violated listing commitment")
                .status(RefundStatus.approved)
                .build();

        when(refundRequestRepository.findById(refundRequest.getId())).thenReturn(Optional.of(refundRequest));
        when(refundRequestRepository.save(any(RefundRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RefundResponseDTO response = refundService.reviewRefund(refundRequest.getId(), admin, RefundReviewRequestDTO.builder()
                .status(RefundStatus.completed)
                .adminNote("Manual refund completed through bank transfer")
                .refundReference("RF-20260312-01")
                .build());

        assertThat(response.getStatus()).isEqualTo(RefundStatus.completed);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.refunded);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.cancelled);
        assertThat(order.getFundingStatus()).isEqualTo(OrderFundingStatus.refunded);
        assertThat(order.getPaidAmount()).isEqualByComparingTo("0");
        assertThat(order.getRemainingAmount()).isEqualByComparingTo("10000000");
    }

    private Order depositedOrder(User buyer) {
        User seller = user(AppRole.seller, "seller@test.dev");
        return Order.builder()
                .id(UUID.randomUUID())
                .buyer(buyer)
                .seller(seller)
                .totalAmount(new BigDecimal("10000000"))
                .depositAmount(new BigDecimal("2000000"))
                .requiredUpfrontAmount(new BigDecimal("2000000"))
                .paidAmount(new BigDecimal("2000000"))
                .remainingAmount(new BigDecimal("8000000"))
                .paymentMethod(PaymentMethod.transfer)
                .status(OrderStatus.deposited)
                .fundingStatus(OrderFundingStatus.held)
                .build();
    }

    private Payment successfulUpfrontPayment(Order order) {
        return Payment.builder()
                .id(UUID.randomUUID())
                .order(order)
                .amount(new BigDecimal("2000000"))
                .method(PaymentMethod.transfer)
                .phase(PaymentPhase.upfront)
                .status(PaymentStatus.success)
                .build();
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
