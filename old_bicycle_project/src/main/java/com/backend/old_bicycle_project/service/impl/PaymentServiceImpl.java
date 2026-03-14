package com.backend.old_bicycle_project.service.impl;

import com.backend.old_bicycle_project.config.NotificationEvent;
import com.backend.old_bicycle_project.config.SepayProperties;
import com.backend.old_bicycle_project.dto.request.SepayWebhookRequestDTO;
import com.backend.old_bicycle_project.dto.response.PaymentRequestResponseDTO;
import com.backend.old_bicycle_project.dto.response.PaymentResponseDTO;
import com.backend.old_bicycle_project.entity.Order;
import com.backend.old_bicycle_project.entity.Payment;
import com.backend.old_bicycle_project.entity.User;
import com.backend.old_bicycle_project.entity.enums.AppRole;
import com.backend.old_bicycle_project.entity.enums.NotificationType;
import com.backend.old_bicycle_project.entity.enums.OrderFundingStatus;
import com.backend.old_bicycle_project.entity.enums.OrderStatus;
import com.backend.old_bicycle_project.entity.enums.PaymentGateway;
import com.backend.old_bicycle_project.entity.enums.PaymentMethod;
import com.backend.old_bicycle_project.entity.enums.PaymentPhase;
import com.backend.old_bicycle_project.entity.enums.PaymentStatus;
import com.backend.old_bicycle_project.exception.AppException;
import com.backend.old_bicycle_project.exception.ErrorCode;
import com.backend.old_bicycle_project.repository.OrderRepository;
import com.backend.old_bicycle_project.repository.PaymentRepository;
import com.backend.old_bicycle_project.service.PaymentService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final SepayProperties sepayProperties;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;

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
            throw new AppException(ErrorCode.PAYMENT_NOT_READY);
        }
        if (order.getFundingStatus() == OrderFundingStatus.held
                || order.getFundingStatus() == OrderFundingStatus.released
                || order.getFundingStatus() == OrderFundingStatus.refund_pending
                || order.getFundingStatus() == OrderFundingStatus.refunded) {
            throw new AppException(ErrorCode.INVALID_STATUS);
        }

        validateSepayConfigurationForCurrentMode();

        Payment existingPayment = paymentRepository.findFirstByOrderIdAndPhaseOrderByCreatedAtDesc(orderId, PaymentPhase.upfront)
                .orElse(null);

        if (existingPayment != null && existingPayment.getStatus() == PaymentStatus.success) {
            throw new AppException(ErrorCode.RECORD_ALREADY_EXISTS);
        }

        Payment payment = existingPayment;
        if (payment == null || payment.getStatus() == PaymentStatus.failed || payment.getStatus() == PaymentStatus.refunded) {
            payment = paymentRepository.save(Payment.builder()
                    .order(order)
                    .amount(order.getRequiredUpfrontAmount())
                    .gateway(PaymentGateway.sepay)
                    .method(order.getPaymentMethod())
                    .phase(PaymentPhase.upfront)
                    .status(PaymentStatus.processing)
                    .gatewayOrderCode(generateGatewayOrderCode(order))
                    .build());
        } else {
            payment.setStatus(PaymentStatus.processing);
        }

        order.setFundingStatus(OrderFundingStatus.awaiting_payment);
        orderRepository.save(order);

        payment.setCheckoutUrl(null);
        payment.setQrCodeUrl(buildQrCodeUrl(payment));
        payment = paymentRepository.save(payment);

        return PaymentRequestResponseDTO.builder()
                .paymentId(payment.getId())
                .orderId(order.getId())
                .gateway(payment.getGateway())
                .phase(payment.getPhase())
                .status(payment.getStatus())
                .amount(payment.getAmount())
                .gatewayOrderCode(payment.getGatewayOrderCode())
                .checkoutUrl(payment.getCheckoutUrl())
                .qrCodeUrl(payment.getQrCodeUrl())
                .transferContent(payment.getGatewayOrderCode())
                .bankBin(sepayProperties.getBankBin())
                .bankAccountNumber(sepayProperties.getAccountNumber())
                .bankAccountName(sepayProperties.getAccountName())
                .mockMode(sepayProperties.isMockMode())
                .instructions(buildInstructions(order, payment))
                .expiresAt(order.getPaymentDeadline())
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
    public void handleSepayWebhook(SepayWebhookRequestDTO requestDTO, String authorizationHeader) {
        validateWebhookAuthorization(authorizationHeader);

        if (requestDTO.getTransferType() != null && !"in".equalsIgnoreCase(requestDTO.getTransferType())) {
            return;
        }
        if (requestDTO.getCode() == null || requestDTO.getTransferAmount() == null) {
            throw new AppException(ErrorCode.PAYMENT_VALIDATION_FAILED);
        }

        Payment payment = paymentRepository.findByGatewayOrderCode(requestDTO.getCode())
                .orElseThrow(() -> new AppException(ErrorCode.PAYMENT_VALIDATION_FAILED));

        if (payment.getStatus() == PaymentStatus.success) {
            return;
        }
        if (requestDTO.getTransferAmount().compareTo(payment.getAmount()) < 0) {
            throw new AppException(ErrorCode.PAYMENT_VALIDATION_FAILED);
        }

        payment.setStatus(PaymentStatus.success);
        payment.setTransactionReference(resolveTransactionReference(requestDTO));
        payment.setPaymentDate(requestDTO.getTransactionDate() != null ? requestDTO.getTransactionDate() : LocalDateTime.now());
        payment.setGatewayResponse(serializeWebhookPayload(requestDTO));
        paymentRepository.save(payment);

        Order order = payment.getOrder();
        BigDecimal currentPaid = order.getPaidAmount() != null ? order.getPaidAmount() : BigDecimal.ZERO;
        BigDecimal newPaid = currentPaid.add(payment.getAmount());
        order.setPaidAmount(newPaid);
        order.setRemainingAmount(maxZero(order.getTotalAmount().subtract(newPaid)));
        if (newPaid.compareTo(order.getRequiredUpfrontAmount()) >= 0) {
            order.setStatus(OrderStatus.deposited);
            order.setFundingStatus(OrderFundingStatus.held);
        }
        orderRepository.save(order);

        publishOrderNotification(
                order.getBuyer().getId(),
                "Thanh toan dat coc thanh cong",
                "He thong da ghi nhan khoan thanh toan cho order cua ban.",
                "{\"orderId\":\"" + order.getId() + "\",\"paymentId\":\"" + payment.getId() + "\"}"
        );
        publishOrderNotification(
                order.getSeller().getId(),
                "Order da duoc thanh toan tien dat coc",
                "Nguoi mua da thanh toan thanh cong khoan ung truoc cho order.",
                "{\"orderId\":\"" + order.getId() + "\",\"paymentId\":\"" + payment.getId() + "\"}"
        );
    }

    private void validateOrderAccess(Order order, User currentUser) {
        boolean isAllowed = currentUser.getRole() == AppRole.admin
                || order.getBuyer().getId().equals(currentUser.getId())
                || order.getSeller().getId().equals(currentUser.getId());
        if (!isAllowed) {
            throw new AppException(ErrorCode.FORBIDDEN);
        }
    }

    private void validateWebhookAuthorization(String authorizationHeader) {
        if (sepayProperties.getWebhookApiKey() == null || sepayProperties.getWebhookApiKey().isBlank()) {
            if (!sepayProperties.isMockMode()) {
                throw new AppException(ErrorCode.PAYMENT_VALIDATION_FAILED);
            }
            return;
        }

        String normalizedHeader = authorizationHeader == null ? "" : authorizationHeader.trim();
        boolean matches = normalizedHeader.equals(sepayProperties.getWebhookApiKey())
                || normalizedHeader.equalsIgnoreCase("Apikey " + sepayProperties.getWebhookApiKey());
        if (!matches) {
            throw new AppException(ErrorCode.PAYMENT_VALIDATION_FAILED);
        }
    }

    private void validateSepayConfigurationForCurrentMode() {
        if (sepayProperties.isMockMode()) {
            return;
        }

        boolean missingTransferConfig = isBlank(sepayProperties.getBankBin())
                || isBlank(sepayProperties.getAccountNumber());
        boolean missingWebhookKey = isBlank(sepayProperties.getWebhookApiKey());

        if (missingTransferConfig || missingWebhookKey) {
            throw new AppException(ErrorCode.PAYMENT_NOT_READY);
        }
    }

    private String generateGatewayOrderCode(Order order) {
        String shortOrderId = order.getId().toString().replace("-", "").substring(0, 12).toUpperCase();
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HHmmss"));
        return "OB-" + shortOrderId + "-" + timestamp;
    }

    private String buildInstructions(Order order, Payment payment) {
        if (sepayProperties.getAccountNumber() == null || sepayProperties.getAccountNumber().isBlank()) {
            return "Chua cau hinh tai khoan SePay/nhan tien. Ban co the dung mock mode va goi webhook de mo phong giao dich.";
        }
        return "Chuyen khoan dung so tien "
                + payment.getAmount().toPlainString()
                + " VND voi noi dung "
                + payment.getGatewayOrderCode()
                + ". He thong se doi trang thai order sau khi webhook ghi nhan giao dich hop le.";
    }

    private String buildQrCodeUrl(Payment payment) {
        if (sepayProperties.getBankBin() == null || sepayProperties.getBankBin().isBlank()
                || sepayProperties.getAccountNumber() == null || sepayProperties.getAccountNumber().isBlank()) {
            return null;
        }

        String accountName = sepayProperties.getAccountName() != null ? sepayProperties.getAccountName() : "";
        return "https://img.vietqr.io/image/"
                + sepayProperties.getBankBin()
                + "-"
                + sepayProperties.getAccountNumber()
                + "-compact2.png?amount="
                + payment.getAmount().toPlainString()
                + "&addInfo="
                + URLEncoder.encode(payment.getGatewayOrderCode(), StandardCharsets.UTF_8)
                + "&accountName="
                + URLEncoder.encode(accountName, StandardCharsets.UTF_8);
    }

    private String resolveTransactionReference(SepayWebhookRequestDTO requestDTO) {
        if (requestDTO.getReferenceCode() != null && !requestDTO.getReferenceCode().isBlank()) {
            return requestDTO.getReferenceCode();
        }
        return requestDTO.getId() != null ? String.valueOf(requestDTO.getId()) : UUID.randomUUID().toString();
    }

    private String serializeWebhookPayload(SepayWebhookRequestDTO requestDTO) {
        try {
            return objectMapper.writeValueAsString(requestDTO);
        } catch (JsonProcessingException e) {
            return "{\"serialization\":\"failed\"}";
        }
    }

    private BigDecimal maxZero(BigDecimal value) {
        return value.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : value;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private void publishOrderNotification(UUID userId, String title, String content, String metadata) {
        eventPublisher.publishEvent(new NotificationEvent(
                this,
                userId,
                title,
                content,
                NotificationType.order,
                metadata
        ));
    }

    private PaymentResponseDTO mapToDTO(Payment payment) {
        return PaymentResponseDTO.builder()
                .id(payment.getId())
                .orderId(payment.getOrder().getId())
                .amount(payment.getAmount())
                .gateway(payment.getGateway())
                .method(payment.getMethod())
                .phase(payment.getPhase())
                .status(payment.getStatus())
                .gatewayOrderCode(payment.getGatewayOrderCode())
                .transactionReference(payment.getTransactionReference())
                .checkoutUrl(payment.getCheckoutUrl())
                .qrCodeUrl(payment.getQrCodeUrl())
                .paymentDate(payment.getPaymentDate())
                .createdAt(payment.getCreatedAt())
                .build();
    }
}
