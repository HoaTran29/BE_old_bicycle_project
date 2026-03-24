package com.backend.old_bicycle_project.service.impl;

import com.backend.old_bicycle_project.entity.Order;
import com.backend.old_bicycle_project.entity.Payout;
import com.backend.old_bicycle_project.entity.PayoutProfile;
import com.backend.old_bicycle_project.entity.Payment;
import com.backend.old_bicycle_project.entity.Product;
import com.backend.old_bicycle_project.entity.RefundRequest;
import com.backend.old_bicycle_project.entity.User;
import com.backend.old_bicycle_project.entity.enums.AppRole;
import com.backend.old_bicycle_project.entity.enums.NotificationType;
import com.backend.old_bicycle_project.entity.enums.OrderFundingStatus;
import com.backend.old_bicycle_project.entity.enums.OrderStatus;
import com.backend.old_bicycle_project.entity.enums.PaymentMethod;
import com.backend.old_bicycle_project.entity.enums.PaymentStatus;
import com.backend.old_bicycle_project.entity.enums.PayoutStatus;
import com.backend.old_bicycle_project.entity.enums.PayoutType;
import com.backend.old_bicycle_project.entity.enums.RefundStatus;
import com.backend.old_bicycle_project.repository.OrderRepository;
import com.backend.old_bicycle_project.repository.PayoutProfileRepository;
import com.backend.old_bicycle_project.repository.PayoutRepository;
import com.backend.old_bicycle_project.repository.PaymentRepository;
import com.backend.old_bicycle_project.repository.RefundRequestRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PayoutServiceImplTest {

    @Mock
    private PayoutProfileRepository payoutProfileRepository;

    @Mock
    private PayoutRepository payoutRepository;

    @Mock
    private RefundRequestRepository refundRequestRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private PayoutServiceImpl payoutService;

    @Test
    void upsertMyProfileHydratesExistingProfileRequiredPayouts() {
        User seller = user(AppRole.seller, "seller@test.dev");
        Payout payout = Payout.builder()
                .id(UUID.randomUUID())
                .recipient(seller)
                .type(PayoutType.seller_release)
                .status(PayoutStatus.profile_required)
                .amount(new BigDecimal("2000000"))
                .transferContent("SL-ABC123")
                .build();

        when(payoutProfileRepository.findByUserId(seller.getId())).thenReturn(Optional.empty());
        when(payoutProfileRepository.save(any(PayoutProfile.class))).thenAnswer(invocation -> {
            PayoutProfile profile = invocation.getArgument(0);
            profile.setId(UUID.randomUUID());
            return profile;
        });
        when(payoutRepository.findByRecipientIdAndStatusOrderByCreatedAtAsc(seller.getId(), PayoutStatus.profile_required))
                .thenReturn(List.of(payout));
        when(payoutRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var response = payoutService.upsertMyProfile(seller, com.backend.old_bicycle_project.dto.request.PayoutProfileUpsertRequestDTO.builder()
                .bankCode("TPBank")
                .bankBin("970423")
                .accountNumber("00000645722")
                .accountName("NGUYEN HOANG VIET DO")
                .build());

        assertThat(response.getBankCode()).isEqualTo("TPBank");
        assertThat(payout.getStatus()).isEqualTo(PayoutStatus.pending_transfer);
        assertThat(payout.getQrCodeUrl()).contains("970423-00000645722");
        verify(payoutRepository).saveAll(any());
    }

    @Test
    void ensureRefundPayoutWithoutProfileMarksProfileRequired() {
        User buyer = user(AppRole.buyer, "buyer@test.dev");
        RefundRequest refundRequest = refundRequest(buyer);

        when(payoutRepository.findByRefundRequestId(refundRequest.getId())).thenReturn(Optional.empty());
        when(payoutProfileRepository.findByUserId(buyer.getId())).thenReturn(Optional.empty());
        when(payoutRepository.save(any(Payout.class))).thenAnswer(invocation -> {
            Payout payout = invocation.getArgument(0);
            payout.setId(UUID.randomUUID());
            return payout;
        });

        Payout payout = payoutService.ensureRefundPayout(refundRequest);

        assertThat(payout.getStatus()).isEqualTo(PayoutStatus.profile_required);
        assertThat(payout.getQrCodeUrl()).isNull();
        assertThat(payout.getTransferContent()).startsWith("RF-");
        verify(eventPublisher).publishEvent(any());
    }

    @Test
    void ensureSellerReleasePayoutUsesProfileAndDepositAmount() {
        User seller = user(AppRole.seller, "seller@test.dev");
        Order order = completedOrderForSellerPayout(seller);
        PayoutProfile profile = PayoutProfile.builder()
                .id(UUID.randomUUID())
                .user(seller)
                .bankCode("TPBank")
                .bankBin("970423")
                .accountNumber("00000645722")
                .accountName("NGUYEN HOANG VIET DO")
                .build();

        when(payoutRepository.findByOrderIdAndType(order.getId(), PayoutType.seller_release)).thenReturn(Optional.empty());
        when(payoutProfileRepository.findByUserId(seller.getId())).thenReturn(Optional.of(profile));
        when(payoutRepository.save(any(Payout.class))).thenAnswer(invocation -> {
            Payout payout = invocation.getArgument(0);
            payout.setId(UUID.randomUUID());
            return payout;
        });

        Payout payout = payoutService.ensureSellerReleasePayout(order);

        assertThat(payout.getStatus()).isEqualTo(PayoutStatus.pending_transfer);
        assertThat(payout.getAmount()).isEqualByComparingTo("2000000");
        assertThat(payout.getQrCodeUrl()).contains("amount=2000000");
        assertThat(payout.getTransferContent()).startsWith("SL-");
    }

    @Test
    void completeRefundPayoutUpdatesRefundPaymentAndOrder() {
        User admin = user(AppRole.admin, "admin@test.dev");
        User buyer = user(AppRole.buyer, "buyer@test.dev");
        Order order = depositedOrder(buyer);
        Payment payment = Payment.builder()
                .id(UUID.randomUUID())
                .order(order)
                .amount(new BigDecimal("2000000"))
                .method(PaymentMethod.transfer)
                .status(PaymentStatus.success)
                .build();
        RefundRequest refundRequest = RefundRequest.builder()
                .id(UUID.randomUUID())
                .order(order)
                .payment(payment)
                .requester(buyer)
                .amount(new BigDecimal("2000000"))
                .status(RefundStatus.approved)
                .build();
        Payout payout = Payout.builder()
                .id(UUID.randomUUID())
                .type(PayoutType.refund)
                .status(PayoutStatus.pending_transfer)
                .refundRequest(refundRequest)
                .recipient(buyer)
                .amount(new BigDecimal("2000000"))
                .build();

        when(refundRequestRepository.save(any(RefundRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(payoutRepository.save(any(Payout.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Payout completed = payoutService.completeRefundPayout(payout, admin, "RF-20260319-01", "Refund transferred manually");

        assertThat(completed.getStatus()).isEqualTo(PayoutStatus.completed);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.refunded);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.cancelled);
        assertThat(order.getFundingStatus()).isEqualTo(OrderFundingStatus.refunded);
        assertThat(refundRequest.getStatus()).isEqualTo(RefundStatus.completed);
        assertThat(refundRequest.getRefundReference()).isEqualTo("RF-20260319-01");
        verify(eventPublisher).publishEvent(any());
    }

    @Test
    void completeSellerPayoutMarksFundingReleased() {
        User admin = user(AppRole.admin, "admin@test.dev");
        User seller = user(AppRole.seller, "seller@test.dev");
        Order order = completedOrderForSellerPayout(seller);
        Payout payout = Payout.builder()
                .id(UUID.randomUUID())
                .type(PayoutType.seller_release)
                .status(PayoutStatus.pending_transfer)
                .order(order)
                .recipient(seller)
                .amount(new BigDecimal("2000000"))
                .build();

        when(payoutRepository.findById(payout.getId())).thenReturn(Optional.of(payout));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(payoutRepository.save(any(Payout.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = payoutService.completePayout(
                payout.getId(),
                admin,
                com.backend.old_bicycle_project.dto.request.PayoutCompleteRequestDTO.builder()
                        .bankReference("SL-20260319-01")
                        .adminNote("Seller deposit released manually")
                        .build()
        );

        assertThat(response.getStatus()).isEqualTo(PayoutStatus.completed);
        assertThat(order.getFundingStatus()).isEqualTo(OrderFundingStatus.released);
        assertThat(response.getBankReference()).isEqualTo("SL-20260319-01");
        verify(eventPublisher).publishEvent(any());
    }

    private RefundRequest refundRequest(User buyer) {
        Order order = depositedOrder(buyer);
        Payment payment = Payment.builder()
                .id(UUID.randomUUID())
                .order(order)
                .amount(new BigDecimal("2000000"))
                .method(PaymentMethod.transfer)
                .status(PaymentStatus.success)
                .build();
        return RefundRequest.builder()
                .id(UUID.randomUUID())
                .order(order)
                .payment(payment)
                .requester(buyer)
                .amount(new BigDecimal("2000000"))
                .reason("Condition mismatch")
                .status(RefundStatus.approved)
                .reviewedAt(LocalDateTime.now())
                .build();
    }

    private Order depositedOrder(User buyer) {
        User seller = user(AppRole.seller, "seller@test.dev");
        Product product = Product.builder()
                .id(UUID.randomUUID())
                .title("Refund bike")
                .seller(seller)
                .build();
        return Order.builder()
                .id(UUID.randomUUID())
                .buyer(buyer)
                .seller(seller)
                .product(product)
                .status(OrderStatus.deposited)
                .fundingStatus(OrderFundingStatus.held)
                .totalAmount(new BigDecimal("10000000"))
                .requiredUpfrontAmount(new BigDecimal("2000000"))
                .depositAmount(new BigDecimal("2000000"))
                .paidAmount(new BigDecimal("2000000"))
                .remainingAmount(new BigDecimal("8000000"))
                .paymentMethod(PaymentMethod.transfer)
                .build();
    }

    private Order completedOrderForSellerPayout(User seller) {
        User buyer = user(AppRole.buyer, "buyer@test.dev");
        Product product = Product.builder()
                .id(UUID.randomUUID())
                .title("Seller release bike")
                .seller(seller)
                .build();
        return Order.builder()
                .id(UUID.randomUUID())
                .buyer(buyer)
                .seller(seller)
                .product(product)
                .status(OrderStatus.completed)
                .fundingStatus(OrderFundingStatus.seller_payout_pending)
                .totalAmount(new BigDecimal("10000000"))
                .requiredUpfrontAmount(new BigDecimal("2000000"))
                .depositAmount(new BigDecimal("2000000"))
                .paidAmount(new BigDecimal("10000000"))
                .remainingAmount(BigDecimal.ZERO)
                .paymentMethod(PaymentMethod.transfer)
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
