package com.backend.old_bicycle_project.service.impl;

import com.backend.old_bicycle_project.config.SepayProperties;
import com.backend.old_bicycle_project.dto.response.PaymentRequestResponseDTO;
import com.backend.old_bicycle_project.dto.response.PaymentResponseDTO;
import com.backend.old_bicycle_project.entity.Order;
import com.backend.old_bicycle_project.entity.Payment;
import com.backend.old_bicycle_project.entity.User;
import com.backend.old_bicycle_project.entity.enums.AppRole;
import com.backend.old_bicycle_project.entity.enums.OrderFundingStatus;
import com.backend.old_bicycle_project.entity.enums.OrderStatus;
import com.backend.old_bicycle_project.entity.enums.PaymentGateway;
import com.backend.old_bicycle_project.entity.enums.PaymentMethod;
import com.backend.old_bicycle_project.entity.enums.PaymentPhase;
import com.backend.old_bicycle_project.entity.enums.PaymentStatus;
import com.backend.old_bicycle_project.exception.AppException;
import com.backend.old_bicycle_project.exception.ErrorCode;
import com.backend.old_bicycle_project.repository.FinancialTransactionRepository;
import com.backend.old_bicycle_project.repository.OrderRepository;
import com.backend.old_bicycle_project.repository.PaymentRepository;
import com.backend.old_bicycle_project.repository.RefundRequestRepository;
import com.backend.old_bicycle_project.service.PaymentService;
import com.backend.old_bicycle_project.service.PayoutService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final SepayProperties sepayProperties;
    private final PaymentGatewaySupport paymentGatewaySupport;
    private final PaymentWebhookSupport paymentWebhookSupport;
    private final PaymentSettlementSupport paymentSettlementSupport;

    public PaymentServiceImpl(
            PaymentRepository paymentRepository,
            OrderRepository orderRepository,
            RefundRequestRepository refundRequestRepository,
            FinancialTransactionRepository financialTransactionRepository,
            SepayProperties sepayProperties,
            ObjectMapper objectMapper,
            ApplicationEventPublisher eventPublisher,
            RestTemplate restTemplate,
            PayoutService payoutService
    ) {
        this.paymentRepository = paymentRepository;
        this.orderRepository = orderRepository;
        this.sepayProperties = sepayProperties;
        this.paymentGatewaySupport = new PaymentGatewaySupport(sepayProperties, objectMapper, restTemplate);
        this.paymentWebhookSupport = new PaymentWebhookSupport(paymentRepository, sepayProperties, objectMapper);
        this.paymentSettlementSupport = new PaymentSettlementSupport(
                paymentRepository,
                orderRepository,
                refundRequestRepository,
                financialTransactionRepository,
                eventPublisher,
                payoutService
        );
    }

    @Override
    @Transactional
    public PaymentRequestResponseDTO createUpfrontPaymentRequest(UUID orderId, User currentUser) {
        Order order = orderRepository.findByIdAndBuyerId(orderId, currentUser.getId())
                .orElseThrow(() -> new AppException(ErrorCode.RECORD_NOT_EXISTS));

        if (order.getPaymentMethod() == null || order.getPaymentMethod() == PaymentMethod.cash) {
            throw new AppException(ErrorCode.PAYMENT_METHOD_NOT_SUPPORTED);
        }
        if (order.getStatus() != OrderStatus.pending) {
            throw new AppException(ErrorCode.INVALID_STATUS);
        }
        if (order.getAcceptedAt() == null || order.getPaymentDeadline() == null) {
            throw new AppException(ErrorCode.PAYMENT_NOT_READY);
        }
        if (order.getPaymentDeadline().isBefore(LocalDateTime.now())) {
            paymentSettlementSupport.expireOrderDueToPaymentTimeout(order, LocalDateTime.now());
            throw new AppException(ErrorCode.PAYMENT_EXPIRED);
        }
        if (order.getFundingStatus() == OrderFundingStatus.held
                || order.getFundingStatus() == OrderFundingStatus.released
                || order.getFundingStatus() == OrderFundingStatus.refund_pending
                || order.getFundingStatus() == OrderFundingStatus.refunded) {
            throw new AppException(ErrorCode.INVALID_STATUS);
        }

        paymentGatewaySupport.validateSepayConfigurationForCurrentMode();

        Payment existingPayment = paymentRepository
                .findFirstByOrderIdAndPhaseOrderByCreatedAtDesc(orderId, PaymentPhase.upfront)
                .orElse(null);

        if (existingPayment != null && existingPayment.getStatus() == PaymentStatus.success) {
            throw new AppException(ErrorCode.RECORD_ALREADY_EXISTS);
        }

        Payment payment = existingPayment;
        if (payment == null
                || payment.getStatus() == PaymentStatus.failed
                || payment.getStatus() == PaymentStatus.refunded
                || payment.getStatus() == PaymentStatus.expired) {
            payment = paymentRepository.save(Payment.builder()
                    .order(order)
                    .amount(resolveBuyerChargeAmount(order))
                    .protectedAmount(order.getRequiredUpfrontAmount())
                    .buyerFeeAmount(resolveBuyerFeeAmount(order))
                    .gateway(PaymentGateway.sepay)
                    .method(order.getPaymentMethod())
                    .phase(PaymentPhase.upfront)
                    .status(PaymentStatus.processing)
                    .gatewayOrderCode(paymentGatewaySupport.generateGatewayOrderCode(order))
                    .build());
        } else {
            payment.setStatus(PaymentStatus.processing);
            payment.setAmount(resolveBuyerChargeAmount(order));
            payment.setProtectedAmount(order.getRequiredUpfrontAmount());
            payment.setBuyerFeeAmount(resolveBuyerFeeAmount(order));
        }

        order.setFundingStatus(OrderFundingStatus.awaiting_payment);
        orderRepository.save(order);

        PaymentProvisionResult provisionResult = paymentGatewaySupport.provisionPayment(order, payment);
        payment.setCheckoutUrl(provisionResult.checkoutUrl());
        payment.setQrCodeUrl(provisionResult.qrCodeUrl());
        if (provisionResult.gatewayResponse() != null) {
            payment.setGatewayResponse(provisionResult.gatewayResponse());
        }
        payment.setExpiresAt(provisionResult.expiresAt());
        payment = paymentRepository.save(payment);

        return PaymentRequestResponseDTO.builder()
                .paymentId(payment.getId())
                .orderId(order.getId())
                .gateway(payment.getGateway())
                .phase(payment.getPhase())
                .status(payment.getStatus())
                .amount(payment.getAmount())
                .protectedAmount(payment.getProtectedAmount() != null ? payment.getProtectedAmount() : payment.getAmount())
                .buyerFeeAmount(payment.getBuyerFeeAmount() != null ? payment.getBuyerFeeAmount() : BigDecimal.ZERO)
                .gatewayOrderCode(payment.getGatewayOrderCode())
                .checkoutUrl(payment.getCheckoutUrl())
                .qrCodeUrl(payment.getQrCodeUrl())
                .transferContent(provisionResult.transferContent())
                .bankBin(provisionResult.bankBin())
                .bankAccountNumber(provisionResult.bankAccountNumber())
                .bankAccountName(provisionResult.bankAccountName())
                .mockMode(sepayProperties.isMockMode())
                .instructions(provisionResult.instructions())
                .expiresAt(provisionResult.expiresAt())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentResponseDTO> getOrderPayments(UUID orderId, User currentUser) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.RECORD_NOT_EXISTS));
        validateOrderAccess(order, currentUser);

        return paymentRepository.findByOrderIdOrderByCreatedAtDesc(orderId)
                .stream()
                .map(this::mapToDTO)
                .toList();
    }

    @Override
    @Transactional
    public void handleSepayWebhook(String rawPayload, String authorizationHeader) {
        paymentWebhookSupport.validateWebhookAuthorization(authorizationHeader);

        ResolvedWebhookPayload webhookPayload = paymentWebhookSupport.resolveWebhookPayload(rawPayload);
        if (webhookPayload == null) {
            return;
        }

        Payment payment = paymentWebhookSupport.findPaymentForWebhook(webhookPayload.gatewayOrderCodeCandidates());
        paymentSettlementSupport.confirmSuccessfulPayment(
                payment,
                webhookPayload.amount(),
                webhookPayload.transactionReference(),
                webhookPayload.paymentDate(),
                webhookPayload.rawPayload()
        );
    }

    @Override
    @Transactional
    public int expireOverdueUpfrontPayments() {
        LocalDateTime now = LocalDateTime.now();
        List<Order> overdueOrders = orderRepository.findByStatusAndFundingStatusAndPaymentDeadlineBefore(
                OrderStatus.pending,
                OrderFundingStatus.awaiting_payment,
                now
        );
        if (overdueOrders.isEmpty()) {
            return 0;
        }

        List<UUID> orderIds = overdueOrders.stream().map(Order::getId).toList();
        Map<UUID, List<Payment>> paymentsByOrderId = paymentRepository
                .findByOrderIdInAndPhaseAndStatusIn(
                        orderIds,
                        PaymentPhase.upfront,
                        PaymentSettlementSupport.OPEN_UPFRONT_PAYMENT_STATUSES
                )
                .stream()
                .collect(java.util.stream.Collectors.groupingBy(payment -> payment.getOrder().getId()));

        overdueOrders.forEach(order -> paymentSettlementSupport.expireOrderDueToPaymentTimeout(
                order,
                now,
                paymentsByOrderId.getOrDefault(order.getId(), List.of())
        ));
        return overdueOrders.size();
    }

    private void validateOrderAccess(Order order, User currentUser) {
        boolean isAllowed = currentUser.getRole() == AppRole.admin
                || order.getBuyer().getId().equals(currentUser.getId())
                || order.getSeller().getId().equals(currentUser.getId());
        if (!isAllowed) {
            throw new AppException(ErrorCode.FORBIDDEN);
        }
    }

    private BigDecimal resolveBuyerChargeAmount(Order order) {
        if (order.getBuyerChargeAmount() != null && order.getBuyerChargeAmount().compareTo(BigDecimal.ZERO) > 0) {
            return order.getBuyerChargeAmount();
        }
        return order.getRequiredUpfrontAmount();
    }

    private BigDecimal resolveBuyerFeeAmount(Order order) {
        return order.getBuyerFeeAmount() != null ? order.getBuyerFeeAmount() : BigDecimal.ZERO;
    }

    private PaymentResponseDTO mapToDTO(Payment payment) {
        return PaymentResponseDTO.builder()
                .id(payment.getId())
                .orderId(payment.getOrder().getId())
                .amount(payment.getAmount())
                .protectedAmount(payment.getProtectedAmount() != null ? payment.getProtectedAmount() : payment.getAmount())
                .buyerFeeAmount(payment.getBuyerFeeAmount() != null ? payment.getBuyerFeeAmount() : BigDecimal.ZERO)
                .gateway(payment.getGateway())
                .method(payment.getMethod())
                .phase(payment.getPhase())
                .status(payment.getStatus())
                .gatewayOrderCode(payment.getGatewayOrderCode())
                .transactionReference(payment.getTransactionReference())
                .checkoutUrl(payment.getCheckoutUrl())
                .qrCodeUrl(payment.getQrCodeUrl())
                .paymentDate(payment.getPaymentDate())
                .expiresAt(payment.getExpiresAt())
                .createdAt(payment.getCreatedAt())
                .build();
    }
}
