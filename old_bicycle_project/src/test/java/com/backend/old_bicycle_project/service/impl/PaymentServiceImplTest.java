package com.backend.old_bicycle_project.service.impl;

import com.backend.old_bicycle_project.config.SepayProperties;
import com.backend.old_bicycle_project.dto.request.SepayWebhookRequestDTO;
import com.backend.old_bicycle_project.dto.response.PaymentRequestResponseDTO;
import com.backend.old_bicycle_project.entity.Order;
import com.backend.old_bicycle_project.entity.Payment;
import com.backend.old_bicycle_project.entity.User;
import com.backend.old_bicycle_project.entity.enums.AppRole;
import com.backend.old_bicycle_project.entity.enums.OrderFundingStatus;
import com.backend.old_bicycle_project.entity.enums.OrderStatus;
import com.backend.old_bicycle_project.entity.enums.PaymentMethod;
import com.backend.old_bicycle_project.entity.enums.PaymentPhase;
import com.backend.old_bicycle_project.entity.enums.PaymentStatus;
import com.backend.old_bicycle_project.repository.OrderRepository;
import com.backend.old_bicycle_project.repository.PaymentRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private PaymentServiceImpl paymentService;

    @BeforeEach
    void setUp() {
        SepayProperties properties = new SepayProperties();
        properties.setMockMode(true);
        properties.setBankBin("970422");
        properties.setAccountNumber("123456789");
        properties.setAccountName("Old Bicycle Platform");

        paymentService = new PaymentServiceImpl(
                paymentRepository,
                orderRepository,
                properties,
                new ObjectMapper(),
                eventPublisher
        );
    }

    @Test
    void createUpfrontPaymentRequestBuildsTransferInstructionsAndQrUrl() {
        User buyer = user(AppRole.buyer, "buyer@test.dev");
        Order order = acceptedOrder(buyer);

        when(orderRepository.findByIdAndBuyerId(order.getId(), buyer.getId())).thenReturn(Optional.of(order));
        when(paymentRepository.findFirstByOrderIdAndPhaseOrderByCreatedAtDesc(order.getId(), PaymentPhase.upfront))
                .thenReturn(Optional.empty());
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment payment = invocation.getArgument(0);
            if (payment.getId() == null) {
                payment.setId(UUID.randomUUID());
            }
            return payment;
        });

        PaymentRequestResponseDTO response = paymentService.createUpfrontPaymentRequest(order.getId(), buyer);

        assertThat(response.getAmount()).isEqualByComparingTo("2000000");
        assertThat(response.getGatewayOrderCode()).startsWith("OB-");
        assertThat(response.getTransferContent()).isEqualTo(response.getGatewayOrderCode());
        assertThat(response.getInstructions()).contains("2000000");
        assertThat(response.getQrCodeUrl()).contains("vietqr.io");
        assertThat(response.getQrCodeUrl()).contains("addInfo=");
        assertThat(response.isMockMode()).isTrue();
    }

    @Test
    void handleSepayWebhookMarksPaymentSuccessfulAndOrderHeld() {
        User buyer = user(AppRole.buyer, "buyer@test.dev");
        Order order = acceptedOrder(buyer);
        Payment payment = Payment.builder()
                .id(UUID.randomUUID())
                .order(order)
                .amount(new BigDecimal("2000000"))
                .method(PaymentMethod.transfer)
                .phase(PaymentPhase.upfront)
                .status(PaymentStatus.processing)
                .gatewayOrderCode("OB-ORDER-001")
                .build();

        when(paymentRepository.findByGatewayOrderCode("OB-ORDER-001")).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        paymentService.handleSepayWebhook(SepayWebhookRequestDTO.builder()
                .code("OB-ORDER-001")
                .transferType("in")
                .transferAmount(new BigDecimal("2000000"))
                .referenceCode("TX-001")
                .transactionDate(LocalDateTime.of(2026, 3, 12, 11, 0))
                .build(), null);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.success);
        assertThat(payment.getTransactionReference()).isEqualTo("TX-001");
        assertThat(order.getStatus()).isEqualTo(OrderStatus.deposited);
        assertThat(order.getFundingStatus()).isEqualTo(OrderFundingStatus.held);
        assertThat(order.getPaidAmount()).isEqualByComparingTo("2000000");
        assertThat(order.getRemainingAmount()).isEqualByComparingTo("8000000");

        verify(eventPublisher, times(2)).publishEvent(any());
    }

    private Order acceptedOrder(User buyer) {
        User seller = user(AppRole.seller, "seller@test.dev");
        return Order.builder()
                .id(UUID.randomUUID())
                .buyer(buyer)
                .seller(seller)
                .totalAmount(new BigDecimal("10000000"))
                .depositAmount(new BigDecimal("2000000"))
                .requiredUpfrontAmount(new BigDecimal("2000000"))
                .paidAmount(BigDecimal.ZERO)
                .remainingAmount(new BigDecimal("10000000"))
                .paymentMethod(PaymentMethod.transfer)
                .status(OrderStatus.pending)
                .fundingStatus(OrderFundingStatus.awaiting_payment)
                .acceptedAt(LocalDateTime.now().minusHours(1))
                .paymentDeadline(LocalDateTime.now().plusHours(10))
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
