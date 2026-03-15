package com.backend.old_bicycle_project.service.impl;

import com.backend.old_bicycle_project.config.SepayProperties;
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
import com.backend.old_bicycle_project.exception.AppException;
import com.backend.old_bicycle_project.exception.ErrorCode;
import com.backend.old_bicycle_project.repository.OrderRepository;
import com.backend.old_bicycle_project.repository.PaymentRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
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

    @Mock
    private RestTemplate restTemplate;

    private SepayProperties properties;

    private PaymentServiceImpl paymentService;

    @BeforeEach
    void setUp() {
        properties = new SepayProperties();
        properties.setMockMode(true);
        properties.setBankBin("970422");
        properties.setAccountNumber("123456789");
        properties.setAccountName("Old Bicycle Platform");

        paymentService = new PaymentServiceImpl(
                paymentRepository,
                orderRepository,
                properties,
                new ObjectMapper(),
                eventPublisher,
                restTemplate
        );
    }

    @Test
    void createUpfrontPaymentRequestBuildsTransferInstructionsAndQrUrlInMockMode() {
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
        assertThat(response.isMockMode()).isTrue();
    }

    @Test
    void createUpfrontPaymentRequestUsesStaticLiveTransferWhenWebhookIsConfiguredButApiTokenMissing() {
        User buyer = user(AppRole.buyer, "buyer@test.dev");
        Order order = acceptedOrder(buyer);
        properties.setMockMode(false);
        properties.setWebhookApiKey("secret-key");

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

        assertThat(response.isMockMode()).isFalse();
        assertThat(response.getCheckoutUrl()).isNull();
        assertThat(response.getQrCodeUrl()).contains("vietqr.io");
        assertThat(response.getBankAccountNumber()).isEqualTo("123456789");
    }

    @Test
    void createUpfrontPaymentRequestUsesSepayApiWhenLiveTokenIsConfigured() {
        User buyer = user(AppRole.buyer, "buyer@test.dev");
        Order order = acceptedOrder(buyer);
        properties.setMockMode(false);
        properties.setWebhookApiKey("secret-key");
        properties.setApiToken("api-token");
        properties.setBankAccountId("321");
        properties.setAccountNumber("9988776655");

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
        when(restTemplate.exchange(
                eq("https://my.sepay.vn/userapi/bankaccounts/list"),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(String.class)
        )).thenReturn(ResponseEntity.ok("""
                {
                  "status": 200,
                  "bankaccounts": [
                    {
                      "id": "321",
                      "account_number": "9988776655",
                      "account_holder_name": "SEPAY ORDER VA",
                      "bank_code": "BIDV",
                      "bank_short_name": "BIDV",
                      "bank_bin": "970418"
                    }
                  ]
                }
                """));
        when(restTemplate.exchange(
                eq("https://my.sepay.vn/userapi/bidv/321/orders"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(String.class)
        )).thenReturn(ResponseEntity.ok("""
                {
                  "status": 200,
                  "message": "Success",
                  "data": {
                    "qr_code_url": "https://sepay.vn/qr/abc.png",
                    "va_number": "9988776655",
                    "account_holder_name": "SEPAY ORDER VA",
                    "expired_at": "2026-03-15T10:00:00"
                  }
                }
                """));

        PaymentRequestResponseDTO response = paymentService.createUpfrontPaymentRequest(order.getId(), buyer);

        assertThat(response.isMockMode()).isFalse();
        assertThat(response.getQrCodeUrl()).isEqualTo("https://sepay.vn/qr/abc.png");
        assertThat(response.getBankAccountNumber()).isEqualTo("9988776655");
        assertThat(response.getBankAccountName()).isEqualTo("SEPAY ORDER VA");
        assertThat(response.getInstructions()).contains("SePay");
    }

    @Test
    void createUpfrontPaymentRequestFallsBackToStaticTransferForNonBidvAccountEvenWhenApiTokenExists() {
        User buyer = user(AppRole.buyer, "buyer@test.dev");
        Order order = acceptedOrder(buyer);
        properties.setMockMode(false);
        properties.setWebhookApiKey("secret-key");
        properties.setApiToken("api-token");
        properties.setAccountNumber("0363565884");
        properties.setAccountName("NGUYEN HOANG VIET DO");

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
        when(restTemplate.exchange(
                eq("https://my.sepay.vn/userapi/bankaccounts/list"),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(String.class)
        )).thenReturn(ResponseEntity.ok("""
                {
                  "status": 200,
                  "bankaccounts": [
                    {
                      "id": "45992",
                      "account_number": "0363565884",
                      "account_holder_name": "NGUYEN HOANG VIET DO",
                      "bank_code": "MB",
                      "bank_short_name": "MBBank",
                      "bank_bin": "970422"
                    }
                  ]
                }
                """));

        PaymentRequestResponseDTO response = paymentService.createUpfrontPaymentRequest(order.getId(), buyer);

        assertThat(response.isMockMode()).isFalse();
        assertThat(response.getCheckoutUrl()).isNull();
        assertThat(response.getQrCodeUrl()).contains("970422-0363565884");
        assertThat(response.getBankAccountNumber()).isEqualTo("0363565884");
        assertThat(response.getBankAccountName()).isEqualTo("NGUYEN HOANG VIET DO");
        assertThat(response.getInstructions()).contains("chưa hỗ trợ VA order API");
    }

    @Test
    void createUpfrontPaymentRequestRejectsLiveModeWithoutWebhookKey() {
        User buyer = user(AppRole.buyer, "buyer@test.dev");
        Order order = acceptedOrder(buyer);
        properties.setMockMode(false);
        properties.setWebhookApiKey(null);

        when(orderRepository.findByIdAndBuyerId(order.getId(), buyer.getId())).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> paymentService.createUpfrontPaymentRequest(order.getId(), buyer))
                .isInstanceOfSatisfying(AppException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.PAYMENT_NOT_READY));
    }

    @Test
    void handleSepayWebhookMarksPaymentSuccessfulAndOrderHeld() {
        User buyer = user(AppRole.buyer, "buyer@test.dev");
        Order order = acceptedOrder(buyer);
        Payment payment = processingPayment(order, "OB-ORDER-001");
        properties.setMockMode(false);
        properties.setWebhookApiKey("webhook-secret");

        when(paymentRepository.findByGatewayOrderCode("OB-ORDER-001")).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        paymentService.handleSepayWebhook("""
                {
                  "code": "OB-ORDER-001",
                  "transferType": "in",
                  "transferAmount": 2000000,
                  "referenceCode": "TX-001",
                  "transactionDate": "2026-03-12 11:00:00"
                }
                """, "Apikey webhook-secret");

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.success);
        assertThat(payment.getTransactionReference()).isEqualTo("TX-001");
        assertThat(order.getStatus()).isEqualTo(OrderStatus.deposited);
        assertThat(order.getFundingStatus()).isEqualTo(OrderFundingStatus.held);
        assertThat(order.getPaidAmount()).isEqualByComparingTo("2000000");
        assertThat(order.getRemainingAmount()).isEqualByComparingTo("8000000");
        verify(eventPublisher, times(2)).publishEvent(any());
    }

    @Test
    void handleSepayWebhookAcceptsRawAuthorizationKey() {
        User buyer = user(AppRole.buyer, "buyer@test.dev");
        Order order = acceptedOrder(buyer);
        Payment payment = processingPayment(order, "OB-ORDER-002");
        properties.setMockMode(false);
        properties.setWebhookApiKey("secret-key");

        when(paymentRepository.findByGatewayOrderCode("OB-ORDER-002")).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        paymentService.handleSepayWebhook("""
                {
                  "code": "OB-ORDER-002",
                  "transferType": "in",
                  "transferAmount": 2000000,
                  "referenceCode": "TRX-8899",
                  "transactionDate": "2026-03-12 11:00:00"
                }
                """, "secret-key");

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.success);
        assertThat(payment.getTransactionReference()).isEqualTo("TRX-8899");
        assertThat(order.getStatus()).isEqualTo(OrderStatus.deposited);
        assertThat(order.getFundingStatus()).isEqualTo(OrderFundingStatus.held);
    }

    @Test
    void handleSepayWebhookRejectsInvalidAuthorizationHeader() {
        properties.setMockMode(false);
        properties.setWebhookApiKey("secret-key");

        assertThatThrownBy(() -> paymentService.handleSepayWebhook("""
                        {
                          "code": "OB-ORDER-003",
                          "transferType": "in",
                          "transferAmount": 2000000
                        }
                        """, "Apikey wrong-key"))
                .isInstanceOfSatisfying(AppException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.PAYMENT_VALIDATION_FAILED));
    }

    @Test
    void handleSepayWebhookRejectsInsufficientTransferAmount() {
        User buyer = user(AppRole.buyer, "buyer@test.dev");
        Order order = acceptedOrder(buyer);
        Payment payment = processingPayment(order, "OB-ORDER-004");
        properties.setMockMode(false);
        properties.setWebhookApiKey("secret-key");

        when(paymentRepository.findByGatewayOrderCode("OB-ORDER-004")).thenReturn(Optional.of(payment));

        assertThatThrownBy(() -> paymentService.handleSepayWebhook("""
                        {
                          "code": "OB-ORDER-004",
                          "transferType": "in",
                          "transferAmount": 1000000,
                          "referenceCode": "TX-LOW"
                        }
                        """, "Apikey secret-key"))
                .isInstanceOfSatisfying(AppException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.PAYMENT_VALIDATION_FAILED));

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.processing);
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void handleSepayWebhookIgnoresOutgoingTransfer() {
        properties.setMockMode(false);
        properties.setWebhookApiKey("secret-key");

        paymentService.handleSepayWebhook("""
                {
                  "code": "OB-ORDER-005",
                  "transferType": "out",
                  "transferAmount": 2000000,
                  "referenceCode": "TX-OUT"
                }
                """, "Apikey secret-key");

        verify(paymentRepository, never()).findByGatewayOrderCode(any());
        verify(paymentRepository, never()).save(any(Payment.class));
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void handleSepayWebhookIsIdempotentWhenPaymentAlreadySuccessful() {
        User buyer = user(AppRole.buyer, "buyer@test.dev");
        Order order = acceptedOrder(buyer);
        Payment payment = processingPayment(order, "OB-ORDER-006");
        payment.setStatus(PaymentStatus.success);
        payment.setTransactionReference("TX-OLD");
        properties.setMockMode(false);
        properties.setWebhookApiKey("secret-key");

        when(paymentRepository.findByGatewayOrderCode("OB-ORDER-006")).thenReturn(Optional.of(payment));

        paymentService.handleSepayWebhook("""
                {
                  "code": "OB-ORDER-006",
                  "transferType": "in",
                  "transferAmount": 2000000,
                  "referenceCode": "TX-NEW"
                }
                """, "Apikey secret-key");

        assertThat(payment.getTransactionReference()).isEqualTo("TX-OLD");
        verify(paymentRepository, never()).save(any(Payment.class));
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void handleSepayWebhookRejectsLiveModeWithoutWebhookKey() {
        properties.setMockMode(false);
        properties.setWebhookApiKey(null);

        assertThatThrownBy(() -> paymentService.handleSepayWebhook("""
                        {
                          "code": "OB-ORDER-001",
                          "transferType": "in",
                          "transferAmount": 2000000
                        }
                        """, null))
                .isInstanceOfSatisfying(AppException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.PAYMENT_VALIDATION_FAILED));
    }

    private Payment processingPayment(Order order, String gatewayOrderCode) {
        return Payment.builder()
                .id(UUID.randomUUID())
                .order(order)
                .amount(new BigDecimal("2000000"))
                .method(PaymentMethod.transfer)
                .phase(PaymentPhase.upfront)
                .status(PaymentStatus.processing)
                .gatewayOrderCode(gatewayOrderCode)
                .build();
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
