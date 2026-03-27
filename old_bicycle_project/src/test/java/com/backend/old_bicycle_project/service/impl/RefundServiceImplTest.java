package com.backend.old_bicycle_project.service.impl;

import com.backend.old_bicycle_project.dto.request.RefundCreateRequestDTO;
import com.backend.old_bicycle_project.dto.request.RefundReviewRequestDTO;
import com.backend.old_bicycle_project.dto.response.AdminRefundResponseDTO;
import com.backend.old_bicycle_project.dto.response.OrderEvidenceSubmissionResponseDTO;
import com.backend.old_bicycle_project.dto.response.RefundResponseDTO;
import com.backend.old_bicycle_project.entity.Order;
import com.backend.old_bicycle_project.entity.Payment;
import com.backend.old_bicycle_project.entity.Payout;
import com.backend.old_bicycle_project.entity.Product;
import com.backend.old_bicycle_project.entity.RefundRequest;
import com.backend.old_bicycle_project.entity.User;
import com.backend.old_bicycle_project.entity.enums.AppRole;
import com.backend.old_bicycle_project.entity.enums.OrderEvidenceType;
import com.backend.old_bicycle_project.entity.enums.OrderFundingStatus;
import com.backend.old_bicycle_project.entity.enums.OrderStatus;
import com.backend.old_bicycle_project.entity.enums.PaymentMethod;
import com.backend.old_bicycle_project.entity.enums.PaymentPhase;
import com.backend.old_bicycle_project.entity.enums.PaymentStatus;
import com.backend.old_bicycle_project.entity.enums.PayoutStatus;
import com.backend.old_bicycle_project.entity.enums.PayoutType;
import com.backend.old_bicycle_project.entity.enums.RefundStatus;
import com.backend.old_bicycle_project.repository.InspectionRepository;
import com.backend.old_bicycle_project.repository.OrderRepository;
import com.backend.old_bicycle_project.repository.PaymentRepository;
import com.backend.old_bicycle_project.repository.RefundRequestRepository;
import com.backend.old_bicycle_project.repository.UserRepository;
import com.backend.old_bicycle_project.service.OrderEvidenceService;
import com.backend.old_bicycle_project.service.PayoutService;
import com.backend.old_bicycle_project.service.StorageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.mock.web.MockMultipartFile;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
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
    private InspectionRepository inspectionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private PayoutService payoutService;

    @Mock
    private OrderEvidenceService orderEvidenceService;

    @Mock
    private StorageService storageService;

    @InjectMocks
    private RefundServiceImpl refundService;

    @Test
    void requestRefundMovesOrderIntoRefundPending() {
        User buyer = user(AppRole.buyer, "buyer@test.dev");
        User admin = user(AppRole.admin, "admin@test.dev");
        Order order = depositedOrder(buyer);
        Payment payment = successfulUpfrontPayment(order);

        when(orderRepository.findByIdAndBuyerId(order.getId(), buyer.getId())).thenReturn(Optional.of(order));
        when(refundRequestRepository.findFirstByOrderIdAndStatusOrderByCreatedAtDesc(order.getId(), RefundStatus.pending))
                .thenReturn(Optional.empty());
        when(paymentRepository.findFirstByOrderIdAndPhaseOrderByCreatedAtDesc(order.getId(), PaymentPhase.upfront))
                .thenReturn(Optional.of(payment));
        when(userRepository.findByRole(AppRole.admin)).thenReturn(List.of(admin));
        when(refundRequestRepository.save(any(RefundRequest.class))).thenAnswer(invocation -> {
            RefundRequest refundRequest = invocation.getArgument(0);
            if (refundRequest.getId() == null) {
                refundRequest.setId(UUID.randomUUID());
            }
            return refundRequest;
        });
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RefundResponseDTO response = refundService.requestRefund(
                order.getId(),
                buyer,
                RefundCreateRequestDTO.builder()
                        .reason("Seller description does not match actual bicycle condition")
                        .build(),
                List.of()
        );

        assertThat(response.getStatus()).isEqualTo(RefundStatus.pending);
        assertThat(response.getAmount()).isEqualByComparingTo("2000000");
        assertThat(order.getFundingStatus()).isEqualTo(OrderFundingStatus.refund_pending);
        verify(eventPublisher, times(2)).publishEvent(any());
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
            if (refundRequest.getId() == null) {
                refundRequest.setId(UUID.randomUUID());
            }
            return refundRequest;
        });
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RefundResponseDTO response = refundService.requestRefund(
                order.getId(),
                buyer,
                RefundCreateRequestDTO.builder()
                        .reason("Xe nhận được không đúng tình trạng đã cam kết")
                        .build(),
                List.of()
        );

        assertThat(response.getStatus()).isEqualTo(RefundStatus.pending);
        assertThat(order.getFundingStatus()).isEqualTo(OrderFundingStatus.refund_pending);
    }

    @Test
    void requestRefundPersistsEvidenceFilesWhenBuyerUploadsImages() {
        User buyer = user(AppRole.buyer, "buyer@test.dev");
        User admin = user(AppRole.admin, "admin@test.dev");
        Order order = depositedOrder(buyer);
        Payment payment = successfulUpfrontPayment(order);
        MockMultipartFile evidence = new MockMultipartFile(
                "files",
                "frame-crack.jpg",
                "image/jpeg",
                "fake-image".getBytes()
        );

        when(orderRepository.findByIdAndBuyerId(order.getId(), buyer.getId())).thenReturn(Optional.of(order));
        when(refundRequestRepository.findFirstByOrderIdAndStatusOrderByCreatedAtDesc(order.getId(), RefundStatus.pending))
                .thenReturn(Optional.empty());
        when(paymentRepository.findFirstByOrderIdAndPhaseOrderByCreatedAtDesc(order.getId(), PaymentPhase.upfront))
                .thenReturn(Optional.of(payment));
        when(userRepository.findByRole(AppRole.admin)).thenReturn(List.of(admin));
        when(storageService.uploadFile(any(), anyString())).thenReturn("https://cdn.example/refunds/frame-crack.jpg");
        when(refundRequestRepository.save(any(RefundRequest.class))).thenAnswer(invocation -> {
            RefundRequest refundRequest = invocation.getArgument(0);
            if (refundRequest.getId() == null) {
                refundRequest.setId(UUID.randomUUID());
            }
            return refundRequest;
        });
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RefundResponseDTO response = refundService.requestRefund(
                order.getId(),
                buyer,
                RefundCreateRequestDTO.builder()
                        .reason("Buyer discovered a hidden frame crack")
                        .evidenceNote("Photo taken immediately after opening the package")
                        .build(),
                List.of(evidence)
        );

        assertThat(response.getEvidenceFiles()).hasSize(1);
        assertThat(response.getEvidenceFiles().getFirst().getFileName()).isEqualTo("frame-crack.jpg");
        verify(storageService).uploadFile(any(), anyString());
    }

    @Test
    void approveRefundMovesOrderIntoRefundPendingTransferAndCreatesPayout() {
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
                .status(RefundStatus.pending)
                .build();

        when(refundRequestRepository.findById(refundRequest.getId())).thenReturn(Optional.of(refundRequest));
        when(refundRequestRepository.save(any(RefundRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(payoutService.ensureRefundPayout(refundRequest)).thenReturn(Payout.builder()
                .id(UUID.randomUUID())
                .type(PayoutType.refund)
                .status(PayoutStatus.pending_transfer)
                .refundRequest(refundRequest)
                .recipient(buyer)
                .build());

        RefundResponseDTO response = refundService.reviewRefund(
                refundRequest.getId(),
                admin,
                RefundReviewRequestDTO.builder()
                        .status(RefundStatus.approved)
                        .adminNote("Approved and waiting for manual payout")
                        .build()
        );

        assertThat(response.getStatus()).isEqualTo(RefundStatus.approved);
        assertThat(order.getFundingStatus()).isEqualTo(OrderFundingStatus.refund_pending_transfer);
    }

    @Test
    void completeApprovedRefundDelegatesToPayoutCompletion() {
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
        Payout payout = Payout.builder()
                .id(UUID.randomUUID())
                .type(PayoutType.refund)
                .status(PayoutStatus.pending_transfer)
                .refundRequest(refundRequest)
                .recipient(buyer)
                .build();

        when(refundRequestRepository.findById(refundRequest.getId())).thenReturn(Optional.of(refundRequest));
        when(refundRequestRepository.save(any(RefundRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(payoutService.ensureRefundPayout(refundRequest)).thenReturn(payout);
        when(payoutService.completeRefundPayout(payout, admin, "RF-20260312-01", "Manual refund completed through bank transfer"))
                .thenAnswer(invocation -> {
                    refundRequest.setStatus(RefundStatus.completed);
                    refundRequest.setRefundReference("RF-20260312-01");
                    order.setStatus(OrderStatus.cancelled);
                    order.setFundingStatus(OrderFundingStatus.refunded);
                    payment.setStatus(PaymentStatus.refunded);
                    return payout;
                });

        RefundResponseDTO response = refundService.reviewRefund(
                refundRequest.getId(),
                admin,
                RefundReviewRequestDTO.builder()
                        .status(RefundStatus.completed)
                        .adminNote("Manual refund completed through bank transfer")
                        .refundReference("RF-20260312-01")
                        .build()
        );

        assertThat(response.getStatus()).isEqualTo(RefundStatus.completed);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.refunded);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.cancelled);
        assertThat(order.getFundingStatus()).isEqualTo(OrderFundingStatus.refunded);
    }

    @Test
    void getAdminRefundsReturnsPagedAdminView() {
        User buyer = user(AppRole.buyer, "buyer@test.dev");
        User seller = user(AppRole.seller, "seller@test.dev");
        Product product = Product.builder()
                .id(UUID.randomUUID())
                .title("Trek Domane SL6")
                .seller(seller)
                .build();
        Order order = depositedOrder(buyer);
        order.setProduct(product);
        Payment payment = successfulUpfrontPayment(order);
        RefundRequest refundRequest = RefundRequest.builder()
                .id(UUID.randomUUID())
                .order(order)
                .payment(payment)
                .requester(buyer)
                .amount(new BigDecimal("2000000"))
                .reason("Buyer discovered hidden frame damage")
                .status(RefundStatus.pending)
                .build();

        when(refundRequestRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(refundRequest)));
        when(inspectionRepository.findDistinctProductIdsWithInspection(anyCollection())).thenReturn(List.of(product.getId()));
        when(orderEvidenceService.getEvidenceByOrderIds(anyCollection())).thenReturn(
                java.util.Map.of(
                        order.getId(),
                        java.util.Map.of(
                                OrderEvidenceType.seller_handover,
                                OrderEvidenceSubmissionResponseDTO.builder()
                                        .id(UUID.randomUUID())
                                        .evidenceType(OrderEvidenceType.seller_handover)
                                        .build()
                        )
                )
        );

        org.springframework.data.domain.Page<AdminRefundResponseDTO> result = refundService.getAdminRefunds("trek", RefundStatus.pending, 0, 12);

        assertThat(result.getContent()).hasSize(1);
        AdminRefundResponseDTO firstItem = result.getContent().getFirst();
        assertThat(firstItem.getProductTitle()).isEqualTo("Trek Domane SL6");
        assertThat(firstItem.isHasInspection()).isTrue();
        assertThat(firstItem.getBuyerName()).isEqualTo(buyer.getFullName());
        assertThat(firstItem.getSellerName()).isEqualTo(seller.getFullName());
        assertThat(firstItem.getStatus()).isEqualTo(RefundStatus.pending);
        assertThat(firstItem.getSellerHandoverEvidence()).isNotNull();
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
