package com.backend.old_bicycle_project.service.impl;

import com.backend.old_bicycle_project.dto.request.OrderCreateRequestDTO;
import com.backend.old_bicycle_project.dto.response.OrderEvidenceSubmissionResponseDTO;
import com.backend.old_bicycle_project.dto.response.OrderResponseDTO;
import com.backend.old_bicycle_project.entity.Order;
import com.backend.old_bicycle_project.entity.Payout;
import com.backend.old_bicycle_project.entity.Product;
import com.backend.old_bicycle_project.entity.User;
import com.backend.old_bicycle_project.entity.enums.AppRole;
import com.backend.old_bicycle_project.entity.enums.OrderEvidenceType;
import com.backend.old_bicycle_project.entity.enums.OrderFundingStatus;
import com.backend.old_bicycle_project.entity.enums.OrderStatus;
import com.backend.old_bicycle_project.entity.enums.PayoutStatus;
import com.backend.old_bicycle_project.entity.enums.PayoutType;
import com.backend.old_bicycle_project.entity.enums.PaymentMethod;
import com.backend.old_bicycle_project.entity.enums.PaymentOption;
import com.backend.old_bicycle_project.entity.enums.ProductStatus;
import com.backend.old_bicycle_project.exception.AppException;
import com.backend.old_bicycle_project.repository.OrderRepository;
import com.backend.old_bicycle_project.repository.ProductRepository;
import com.backend.old_bicycle_project.repository.ReviewRepository;
import com.backend.old_bicycle_project.service.OrderEvidenceService;
import com.backend.old_bicycle_project.service.PayoutService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private PayoutService payoutService;

    @Mock
    private OrderEvidenceService orderEvidenceService;

    @InjectMocks
    private OrderServiceImpl orderService;

    @Test
    void createOrderWithFullPaymentSetsUpfrontAmountToTotalPrice() {
        User buyer = user(AppRole.buyer, "buyer@test.dev");
        User seller = user(AppRole.seller, "seller@test.dev");
        Product product = Product.builder()
                .id(UUID.randomUUID())
                .seller(seller)
                .title("Specialized Allez")
                .price(new BigDecimal("12000000"))
                .status(ProductStatus.active)
                .build();

        OrderCreateRequestDTO request = OrderCreateRequestDTO.builder()
                .productId(product.getId())
                .paymentOption(PaymentOption.full)
                .paymentMethod(PaymentMethod.transfer)
                .build();

        when(productRepository.findById(product.getId())).thenReturn(java.util.Optional.of(product));
        when(orderRepository.existsByProductIdAndStatusIn(eq(product.getId()), any(List.class))).thenReturn(false);
        when(reviewRepository.existsByOrderId(any(UUID.class))).thenReturn(false);
        when(orderEvidenceService.getEvidenceByOrderId(any(UUID.class))).thenReturn(java.util.Collections.emptyMap());
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(UUID.randomUUID());
            return order;
        });

        OrderResponseDTO response = orderService.createOrder(buyer, request);

        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(orderCaptor.capture());
        Order savedOrder = orderCaptor.getValue();

        assertThat(savedOrder.getPaymentOption()).isEqualTo(PaymentOption.full);
        assertThat(savedOrder.getRequiredUpfrontAmount()).isEqualByComparingTo("12000000");
        assertThat(savedOrder.getDepositAmount()).isEqualByComparingTo("12000000");
        assertThat(savedOrder.getPaidAmount()).isEqualByComparingTo("0");
        assertThat(savedOrder.getRemainingAmount()).isEqualByComparingTo("12000000");

        assertThat(response.getRequiredUpfrontAmount()).isEqualByComparingTo("12000000");
        assertThat(response.getRemainingAmount()).isEqualByComparingTo("12000000");
        assertThat(response.getPaymentMethod()).isEqualTo(PaymentMethod.transfer);
    }

    @Test
    void sellerCompleteMovesOrderToAwaitingBuyerConfirmation() {
        User seller = user(AppRole.seller, "seller@test.dev");
        User buyer = user(AppRole.buyer, "buyer@test.dev");
        Product product = Product.builder()
                .id(UUID.randomUUID())
                .seller(seller)
                .title("Trek Domane")
                .status(ProductStatus.active)
                .build();
        Order order = Order.builder()
                .id(UUID.randomUUID())
                .buyer(buyer)
                .seller(seller)
                .product(product)
                .totalAmount(new BigDecimal("15000000"))
                .requiredUpfrontAmount(new BigDecimal("3000000"))
                .depositAmount(new BigDecimal("3000000"))
                .paidAmount(new BigDecimal("3000000"))
                .remainingAmount(new BigDecimal("12000000"))
                .status(OrderStatus.deposited)
                .fundingStatus(OrderFundingStatus.held)
                .paymentMethod(PaymentMethod.transfer)
                .build();

        when(orderRepository.findById(order.getId())).thenReturn(java.util.Optional.of(order));
        when(reviewRepository.existsByOrderId(order.getId())).thenReturn(false);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderEvidenceSubmissionResponseDTO evidence = OrderEvidenceSubmissionResponseDTO.builder()
                .id(UUID.randomUUID())
                .evidenceType(OrderEvidenceType.seller_handover)
                .build();
        MultipartFile handoverPhoto = new MockMultipartFile("files", "handover.jpg", "image/jpeg", new byte[]{1, 2, 3});

        when(orderEvidenceService.createSellerHandoverEvidence(eq(order), eq(seller), eq("Đã bàn giao tại cửa hàng"), any(List.class)))
                .thenReturn(evidence);
        OrderResponseDTO response = orderService.completeOrder(
                order.getId(),
                seller,
                "Đã bàn giao tại cửa hàng",
                List.of(handoverPhoto)
        );

        assertThat(response.getStatus()).isEqualTo(OrderStatus.awaiting_buyer_confirmation);
        assertThat(response.getFundingStatus()).isEqualTo(OrderFundingStatus.held);
        assertThat(product.getStatus()).isEqualTo(ProductStatus.active);
        assertThat(response.getSellerHandoverEvidence()).isEqualTo(evidence);
        verify(orderEvidenceService, never()).getEvidenceByOrderId(order.getId());
    }

    @Test
    void buyerConfirmReceivedCompletesOrderAndMarksProductSold() {
        User seller = user(AppRole.seller, "seller@test.dev");
        User buyer = user(AppRole.buyer, "buyer@test.dev");
        Product product = Product.builder()
                .id(UUID.randomUUID())
                .seller(seller)
                .title("Cannondale CAAD")
                .status(ProductStatus.active)
                .build();
        Order order = Order.builder()
                .id(UUID.randomUUID())
                .buyer(buyer)
                .seller(seller)
                .product(product)
                .totalAmount(new BigDecimal("18000000"))
                .requiredUpfrontAmount(new BigDecimal("4000000"))
                .depositAmount(new BigDecimal("4000000"))
                .paidAmount(new BigDecimal("4000000"))
                .remainingAmount(new BigDecimal("14000000"))
                .status(OrderStatus.awaiting_buyer_confirmation)
                .fundingStatus(OrderFundingStatus.held)
                .paymentMethod(PaymentMethod.transfer)
                .build();

        when(orderRepository.findById(order.getId())).thenReturn(java.util.Optional.of(order));
        OrderEvidenceSubmissionResponseDTO sellerEvidence = OrderEvidenceSubmissionResponseDTO.builder()
                .id(UUID.randomUUID())
                .evidenceType(OrderEvidenceType.seller_handover)
                .build();
        OrderEvidenceSubmissionResponseDTO buyerEvidence = OrderEvidenceSubmissionResponseDTO.builder()
                .id(UUID.randomUUID())
                .evidenceType(OrderEvidenceType.buyer_receipt)
                .build();
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(reviewRepository.existsByOrderId(order.getId())).thenReturn(false);
        when(payoutService.ensureSellerReleasePayout(order)).thenReturn(Payout.builder()
                .id(UUID.randomUUID())
                .type(PayoutType.seller_release)
                .status(PayoutStatus.pending_transfer)
                .order(order)
                .recipient(seller)
                .build());
        when(orderEvidenceService.createBuyerReceiptEvidence(eq(order), eq(buyer), eq("Xe đúng mô tả"), any(List.class)))
                .thenReturn(buyerEvidence);
        when(orderEvidenceService.getEvidenceByOrderId(order.getId()))
                .thenReturn(java.util.Map.of(OrderEvidenceType.seller_handover, sellerEvidence));

        OrderResponseDTO response = orderService.confirmReceived(
                order.getId(),
                buyer,
                "Xe đúng mô tả",
                List.of(new MockMultipartFile("files", "receipt.jpg", "image/jpeg", new byte[]{1}))
        );

        assertThat(response.getStatus()).isEqualTo(OrderStatus.completed);
        assertThat(response.getFundingStatus()).isEqualTo(OrderFundingStatus.seller_payout_pending);
        assertThat(response.getPaidAmount()).isEqualByComparingTo("18000000");
        assertThat(response.getRemainingAmount()).isEqualByComparingTo("0");
        assertThat(product.getStatus()).isEqualTo(ProductStatus.sold);
        assertThat(response.getSellerHandoverEvidence()).isEqualTo(sellerEvidence);
        assertThat(response.getBuyerReceiptEvidence()).isEqualTo(buyerEvidence);
    }

    @Test
    void sellerCannotConfirmReceivedOnBehalfOfBuyer() {
        User seller = user(AppRole.seller, "seller@test.dev");
        User buyer = user(AppRole.buyer, "buyer@test.dev");
        Product product = Product.builder()
                .id(UUID.randomUUID())
                .seller(seller)
                .title("Bianchi Via Nirone")
                .status(ProductStatus.active)
                .build();
        Order order = Order.builder()
                .id(UUID.randomUUID())
                .buyer(buyer)
                .seller(seller)
                .product(product)
                .status(OrderStatus.awaiting_buyer_confirmation)
                .fundingStatus(OrderFundingStatus.held)
                .paymentMethod(PaymentMethod.transfer)
                .build();

        when(orderRepository.findById(order.getId())).thenReturn(java.util.Optional.of(order));

        assertThatThrownBy(() -> orderService.confirmReceived(order.getId(), seller, null, List.of()))
                .isInstanceOf(AppException.class);
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
