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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private static final String SEPAY_ORDER_NOTIFICATION = "ORDER_PAID";
    private static final DateTimeFormatter ORDER_CODE_TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("HHmmss");

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final SepayProperties sepayProperties;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final RestTemplate restTemplate;

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

        Payment existingPayment = paymentRepository
                .findFirstByOrderIdAndPhaseOrderByCreatedAtDesc(orderId, PaymentPhase.upfront)
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

        PaymentProvisionResult provisionResult = provisionPayment(order, payment);
        payment.setCheckoutUrl(provisionResult.checkoutUrl());
        payment.setQrCodeUrl(provisionResult.qrCodeUrl());
        if (provisionResult.gatewayResponse() != null) {
            payment.setGatewayResponse(provisionResult.gatewayResponse());
        }
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
    public void handleSepayWebhook(String rawPayload, String authorizationHeader, String secretKeyHeader) {
        validateWebhookAuthorization(authorizationHeader, secretKeyHeader);

        ResolvedWebhookPayload webhookPayload = resolveWebhookPayload(rawPayload);
        if (webhookPayload == null) {
            return;
        }

        Payment payment = paymentRepository.findByGatewayOrderCode(webhookPayload.gatewayOrderCode())
                .orElseThrow(() -> new AppException(ErrorCode.PAYMENT_VALIDATION_FAILED));

        confirmSuccessfulPayment(
                payment,
                webhookPayload.amount(),
                webhookPayload.transactionReference(),
                webhookPayload.paymentDate(),
                webhookPayload.rawPayload()
        );
    }

    private PaymentProvisionResult provisionPayment(Order order, Payment payment) {
        if (!sepayProperties.isMockMode() && hasText(sepayProperties.getApiToken())) {
            ResolvedBankAccount resolvedBankAccount = resolveBankAccount();
            if (resolvedBankAccount != null && isBidvAccount(resolvedBankAccount)) {
                return createLiveSepayOrder(order, payment, resolvedBankAccount);
            }
            if (resolvedBankAccount != null) {
                return buildStaticTransferProvision(
                        order,
                        payment,
                        resolvedBankAccount.bankBin(),
                        resolvedBankAccount.accountNumber(),
                        resolvedBankAccount.accountName(),
                        "Tài khoản SePay hiện tại chưa hỗ trợ VA order API cho ngân hàng này. " +
                                "Hệ thống dùng QR/chuyển khoản trực tiếp và vẫn xác nhận bằng webhook."
                );
            }
        }
        return buildStaticTransferProvision(order, payment);
    }

    private PaymentProvisionResult createLiveSepayOrder(
            Order order,
            Payment payment,
            ResolvedBankAccount resolvedBankAccount
    ) {
        if (resolvedBankAccount == null || !hasText(resolvedBankAccount.bankAccountId())) {
            throw new AppException(ErrorCode.PAYMENT_NOT_READY);
        }

        String url = normalizeApiBaseUrl() + "/bidv/" + resolvedBankAccount.bankAccountId() + "/orders";
        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("amount", payment.getAmount());
        requestBody.put("order_code", payment.getGatewayOrderCode());
        requestBody.put("duration", computeDurationSeconds(order));
        requestBody.put("with_qrcode", true);

        JsonNode root = exchangeJson(url, HttpMethod.POST, requestBody, true);
        JsonNode data = root.path("data");
        if (data.isMissingNode() || data.isNull()) {
            throw new AppException(ErrorCode.PAYMENT_GATEWAY_ERROR);
        }

        String qrCodeUrl = textOrNull(data, "qr_code_url");
        String accountNumber = firstNonBlank(
                textOrNull(data, "va_number"),
                textOrNull(data, "account_number"),
                resolvedBankAccount.accountNumber(),
                sepayProperties.getAccountNumber()
        );
        String accountName = firstNonBlank(
                textOrNull(data, "account_holder_name"),
                textOrNull(data, "va_holder_name"),
                resolvedBankAccount.accountName(),
                sepayProperties.getAccountName()
        );
        LocalDateTime expiresAt = parseDateTime(firstNonBlank(
                textOrNull(data, "expired_at"),
                textOrNull(data, "expiredAt")
        ));

        String gatewayResponse = serializeJson(root);
        String instructions = buildLiveInstructions(payment, accountNumber, accountName);

        return new PaymentProvisionResult(
                null,
                qrCodeUrl,
                firstNonBlank(resolvedBankAccount.bankBin(), sepayProperties.getBankBin()),
                accountNumber,
                accountName,
                payment.getGatewayOrderCode(),
                instructions,
                expiresAt != null ? expiresAt : order.getPaymentDeadline(),
                gatewayResponse
        );
    }

    private PaymentProvisionResult buildStaticTransferProvision(Order order, Payment payment) {
        return buildStaticTransferProvision(
                order,
                payment,
                sepayProperties.getBankBin(),
                sepayProperties.getAccountNumber(),
                sepayProperties.getAccountName(),
                buildStaticInstructions(payment)
        );
    }

    private PaymentProvisionResult buildStaticTransferProvision(
            Order order,
            Payment payment,
            String bankBin,
            String accountNumber,
            String accountName,
            String instructions
    ) {
        return new PaymentProvisionResult(
                null,
                buildQrCodeUrl(payment, bankBin, accountNumber, accountName),
                bankBin,
                accountNumber,
                accountName,
                payment.getGatewayOrderCode(),
                instructions,
                order.getPaymentDeadline(),
                null
        );
    }

    private void confirmSuccessfulPayment(
            Payment payment,
            BigDecimal actualAmount,
            String transactionReference,
            LocalDateTime paymentDate,
            String gatewayPayload
    ) {
        if (payment.getStatus() == PaymentStatus.success) {
            return;
        }
        if (actualAmount == null || actualAmount.compareTo(payment.getAmount()) < 0) {
            throw new AppException(ErrorCode.PAYMENT_VALIDATION_FAILED);
        }

        payment.setStatus(PaymentStatus.success);
        payment.setTransactionReference(firstNonBlank(transactionReference, UUID.randomUUID().toString()));
        payment.setPaymentDate(paymentDate != null ? paymentDate : LocalDateTime.now());
        payment.setGatewayResponse(gatewayPayload);
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
                "Thanh toán đặt cọc thành công",
                "Hệ thống đã ghi nhận khoản thanh toán cho order của bạn.",
                "{\"orderId\":\"" + order.getId() + "\",\"paymentId\":\"" + payment.getId() + "\"}"
        );
        publishOrderNotification(
                order.getSeller().getId(),
                "Order đã được thanh toán tiền đặt cọc",
                "Người mua đã thanh toán thành công khoản ứng trước cho order.",
                "{\"orderId\":\"" + order.getId() + "\",\"paymentId\":\"" + payment.getId() + "\"}"
        );
    }

    private ResolvedWebhookPayload resolveWebhookPayload(String rawPayload) {
        if (!hasText(rawPayload)) {
            throw new AppException(ErrorCode.PAYMENT_VALIDATION_FAILED);
        }

        JsonNode root = parseJson(rawPayload);
        if (root.hasNonNull("notification_type") && root.has("order")) {
            return resolveGatewayIpn(root, rawPayload);
        }

        SepayWebhookRequestDTO legacyPayload = parseLegacyWebhook(rawPayload);
        return resolveLegacyWebhook(legacyPayload, rawPayload);
    }

    private ResolvedWebhookPayload resolveGatewayIpn(JsonNode root, String rawPayload) {
        String notificationType = textOrNull(root, "notification_type");
        if (!SEPAY_ORDER_NOTIFICATION.equalsIgnoreCase(notificationType)) {
            return null;
        }

        JsonNode orderNode = root.path("order");
        JsonNode transactionNode = root.path("transaction");
        String gatewayOrderCode = firstNonBlank(
                textOrNull(orderNode, "order_invoice_number"),
                textOrNull(orderNode, "order_code")
        );
        BigDecimal transactionAmount = parseBigDecimal(firstNonBlank(
                textOrNull(transactionNode, "transaction_amount"),
                textOrNull(orderNode, "amount")
        ));
        String transactionReference = firstNonBlank(
                textOrNull(transactionNode, "transaction_id"),
                textOrNull(transactionNode, "gateway_transaction_id"),
                textOrNull(orderNode, "order_id")
        );
        LocalDateTime paymentDate = parseDateTime(firstNonBlank(
                textOrNull(transactionNode, "transaction_date"),
                textOrNull(root, "created_at")
        ));

        if (!hasText(gatewayOrderCode) || transactionAmount == null) {
            throw new AppException(ErrorCode.PAYMENT_VALIDATION_FAILED);
        }

        return new ResolvedWebhookPayload(
                gatewayOrderCode,
                transactionAmount,
                transactionReference,
                paymentDate,
                rawPayload
        );
    }

    private ResolvedWebhookPayload resolveLegacyWebhook(SepayWebhookRequestDTO requestDTO, String rawPayload) {
        if (requestDTO.getTransferType() != null && !"in".equalsIgnoreCase(requestDTO.getTransferType())) {
            return null;
        }
        if (requestDTO.getCode() == null || requestDTO.getTransferAmount() == null) {
            throw new AppException(ErrorCode.PAYMENT_VALIDATION_FAILED);
        }

        return new ResolvedWebhookPayload(
                requestDTO.getCode(),
                requestDTO.getTransferAmount(),
                resolveTransactionReference(requestDTO),
                requestDTO.getTransactionDate() != null ? requestDTO.getTransactionDate() : LocalDateTime.now(),
                rawPayload
        );
    }

    private SepayWebhookRequestDTO parseLegacyWebhook(String rawPayload) {
        JsonNode root = parseJson(rawPayload);
        return SepayWebhookRequestDTO.builder()
                .id(parseLong(textOrNull(root, "id")))
                .gateway(textOrNull(root, "gateway"))
                .transactionDate(parseDateTime(firstNonBlank(
                        textOrNull(root, "transactionDate"),
                        textOrNull(root, "transaction_date")
                )))
                .accountNumber(firstNonBlank(
                        textOrNull(root, "accountNumber"),
                        textOrNull(root, "account_number")
                ))
                .transferType(firstNonBlank(
                        textOrNull(root, "transferType"),
                        textOrNull(root, "transfer_type")
                ))
                .transferAmount(parseBigDecimal(firstNonBlank(
                        textOrNull(root, "transferAmount"),
                        textOrNull(root, "transfer_amount")
                )))
                .accumulated(textOrNull(root, "accumulated"))
                .code(textOrNull(root, "code"))
                .content(textOrNull(root, "content"))
                .referenceCode(firstNonBlank(
                        textOrNull(root, "referenceCode"),
                        textOrNull(root, "reference_code")
                ))
                .description(textOrNull(root, "description"))
                .build();
    }

    private JsonNode parseJson(String rawPayload) {
        try {
            return objectMapper.readTree(rawPayload);
        } catch (JsonProcessingException e) {
            throw new AppException(ErrorCode.PAYMENT_VALIDATION_FAILED);
        }
    }

    private void validateOrderAccess(Order order, User currentUser) {
        boolean isAllowed = currentUser.getRole() == AppRole.admin
                || order.getBuyer().getId().equals(currentUser.getId())
                || order.getSeller().getId().equals(currentUser.getId());
        if (!isAllowed) {
            throw new AppException(ErrorCode.FORBIDDEN);
        }
    }

    private void validateWebhookAuthorization(String authorizationHeader, String secretKeyHeader) {
        if (!hasText(sepayProperties.getWebhookApiKey())) {
            if (!sepayProperties.isMockMode()) {
                throw new AppException(ErrorCode.PAYMENT_VALIDATION_FAILED);
            }
            return;
        }

        String expectedKey = sepayProperties.getWebhookApiKey().trim();
        String normalizedAuth = authorizationHeader == null ? "" : authorizationHeader.trim();
        String normalizedSecret = secretKeyHeader == null ? "" : secretKeyHeader.trim();

        boolean matches = expectedKey.equals(normalizedSecret)
                || expectedKey.equals(normalizedAuth)
                || ("Apikey " + expectedKey).equalsIgnoreCase(normalizedAuth);
        if (!matches) {
            throw new AppException(ErrorCode.PAYMENT_VALIDATION_FAILED);
        }
    }

    private void validateSepayConfigurationForCurrentMode() {
        if (sepayProperties.isMockMode()) {
            return;
        }

        boolean missingWebhookKey = !hasText(sepayProperties.getWebhookApiKey());
        boolean hasLiveApiPath = hasText(sepayProperties.getApiToken())
                && (hasText(sepayProperties.getBankAccountId()) || hasText(sepayProperties.getAccountNumber()));
        boolean hasStaticTransferPath = hasText(sepayProperties.getBankBin())
                && hasText(sepayProperties.getAccountNumber());

        if (missingWebhookKey || (!hasLiveApiPath && !hasStaticTransferPath)) {
            throw new AppException(ErrorCode.PAYMENT_NOT_READY);
        }
    }

    private ResolvedBankAccount resolveBankAccount() {
        JsonNode root = exchangeJson(normalizeApiBaseUrl() + "/bankaccounts/list", HttpMethod.GET, null, true);
        JsonNode bankAccounts = root.path("bankaccounts");
        if (!bankAccounts.isArray()) {
            return null;
        }

        for (JsonNode bankAccount : bankAccounts) {
            String bankAccountId = firstNonBlank(
                    textOrNull(bankAccount, "id"),
                    textOrNull(bankAccount, "bank_account_id")
            );
            String accountNumber = firstNonBlank(
                    textOrNull(bankAccount, "account_number"),
                    textOrNull(bankAccount, "accountNumber")
            );
            boolean matchesConfiguredId = hasText(sepayProperties.getBankAccountId())
                    && sepayProperties.getBankAccountId().equals(bankAccountId);
            boolean matchesConfiguredNumber = hasText(sepayProperties.getAccountNumber())
                    && sepayProperties.getAccountNumber().equals(accountNumber);
            if (matchesConfiguredId || matchesConfiguredNumber) {
                return new ResolvedBankAccount(
                        bankAccountId,
                        accountNumber,
                        firstNonBlank(
                                textOrNull(bankAccount, "account_holder_name"),
                                textOrNull(bankAccount, "label"),
                                sepayProperties.getAccountName()
                        ),
                        firstNonBlank(
                                textOrNull(bankAccount, "bank_bin"),
                                sepayProperties.getBankBin()
                        ),
                        firstNonBlank(
                                textOrNull(bankAccount, "bank_code"),
                                textOrNull(bankAccount, "bank_short_name")
                        ),
                        textOrNull(bankAccount, "bank_short_name")
                );
            }
        }

        if (hasText(sepayProperties.getBankAccountId()) || hasText(sepayProperties.getAccountNumber())) {
            return new ResolvedBankAccount(
                    sepayProperties.getBankAccountId(),
                    sepayProperties.getAccountNumber(),
                    sepayProperties.getAccountName(),
                    sepayProperties.getBankBin(),
                    null,
                    null
            );
        }
        return null;
    }

    private JsonNode exchangeJson(String url, HttpMethod method, Object body, boolean bearerAuth) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            if (bearerAuth) {
                headers.setBearerAuth(sepayProperties.getApiToken());
            }

            HttpEntity<?> entity = body == null
                    ? new HttpEntity<>(headers)
                    : new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.exchange(url, method, entity, String.class);

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new AppException(ErrorCode.PAYMENT_GATEWAY_ERROR);
            }
            return parseJson(response.getBody());
        } catch (RestClientException ex) {
            throw new AppException(ErrorCode.PAYMENT_GATEWAY_ERROR);
        }
    }

    private String normalizeApiBaseUrl() {
        String baseUrl = sepayProperties.getApiBaseUrl();
        if (baseUrl == null || baseUrl.isBlank()) {
            return "https://my.sepay.vn/userapi";
        }
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    private String generateGatewayOrderCode(Order order) {
        String shortOrderId = order.getId().toString().replace("-", "").substring(0, 12).toUpperCase();
        String timestamp = LocalDateTime.now().format(ORDER_CODE_TIMESTAMP_FORMAT);
        return "OB-" + shortOrderId + "-" + timestamp;
    }

    private String buildStaticInstructions(Payment payment) {
        if (!hasText(sepayProperties.getAccountNumber())) {
            return "Chưa cấu hình tài khoản SePay/nhận tiền. Bạn có thể dùng mock mode và gọi webhook để mô phỏng giao dịch.";
        }
        return "Chuyển khoản đúng số tiền "
                + payment.getAmount().toPlainString()
                + " VND với nội dung "
                + payment.getGatewayOrderCode()
                + ". Hệ thống sẽ đổi trạng thái order sau khi webhook ghi nhận giao dịch hợp lệ.";
    }

    private String buildLiveInstructions(Payment payment, String accountNumber, String accountName) {
        return "Thanh toán qua SePay cho khoản "
                + payment.getAmount().toPlainString()
                + " VND. Bạn có thể quét QR hoặc chuyển tới tài khoản "
                + firstNonBlank(accountNumber, "VA do SePay cấp")
                + (hasText(accountName) ? " - " + accountName : "")
                + ".";
    }

    private boolean isBidvAccount(ResolvedBankAccount resolvedBankAccount) {
        String bankCode = firstNonBlank(resolvedBankAccount.bankCode(), resolvedBankAccount.bankShortName());
        return hasText(bankCode) && bankCode.toUpperCase().contains("BIDV");
    }

    private String buildQrCodeUrl(Payment payment, String bankBin, String accountNumber, String accountName) {
        if (!hasText(bankBin) || !hasText(accountNumber)) {
            return null;
        }

        String safeAccountName = accountName != null ? accountName : "";
        return "https://img.vietqr.io/image/"
                + bankBin
                + "-"
                + accountNumber
                + "-compact2.png?amount="
                + payment.getAmount().toPlainString()
                + "&addInfo="
                + URLEncoder.encode(payment.getGatewayOrderCode(), StandardCharsets.UTF_8)
                + "&accountName="
                + URLEncoder.encode(safeAccountName, StandardCharsets.UTF_8);
    }

    private String resolveTransactionReference(SepayWebhookRequestDTO requestDTO) {
        if (requestDTO.getReferenceCode() != null && !requestDTO.getReferenceCode().isBlank()) {
            return requestDTO.getReferenceCode();
        }
        return requestDTO.getId() != null ? String.valueOf(requestDTO.getId()) : UUID.randomUUID().toString();
    }

    private BigDecimal maxZero(BigDecimal value) {
        return value.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : value;
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

    private int computeDurationSeconds(Order order) {
        if (order.getPaymentDeadline() == null) {
            return 900;
        }
        long seconds = Duration.between(LocalDateTime.now(), order.getPaymentDeadline()).getSeconds();
        if (seconds < 60) {
            return 60;
        }
        return Math.toIntExact(Math.min(seconds, 86400));
    }

    private LocalDateTime parseDateTime(String value) {
        if (!hasText(value)) {
            return null;
        }

        try {
            return LocalDateTime.parse(value);
        } catch (DateTimeParseException ignored) {
        }
        try {
            return OffsetDateTime.parse(value).toLocalDateTime();
        } catch (DateTimeParseException ignored) {
        }
        try {
            return LocalDateTime.parse(value, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        } catch (DateTimeParseException ignored) {
        }
        return null;
    }

    private BigDecimal parseBigDecimal(String value) {
        if (!hasText(value)) {
            return null;
        }
        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private Long parseLong(String value) {
        if (!hasText(value)) {
            return null;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String textOrNull(JsonNode node, String fieldName) {
        if (node == null) {
            return null;
        }
        JsonNode child = node.path(fieldName);
        if (child.isMissingNode() || child.isNull()) {
            return null;
        }
        String value = child.asText();
        return value == null || value.isBlank() ? null : value;
    }

    private String serializeJson(JsonNode root) {
        try {
            return objectMapper.writeValueAsString(root);
        } catch (JsonProcessingException ex) {
            return "{\"serialization\":\"failed\"}";
        }
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (hasText(value)) {
                return value;
            }
        }
        return null;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private record PaymentProvisionResult(
            String checkoutUrl,
            String qrCodeUrl,
            String bankBin,
            String bankAccountNumber,
            String bankAccountName,
            String transferContent,
            String instructions,
            LocalDateTime expiresAt,
            String gatewayResponse
    ) {
    }

    private record ResolvedWebhookPayload(
            String gatewayOrderCode,
            BigDecimal amount,
            String transactionReference,
            LocalDateTime paymentDate,
            String rawPayload
    ) {
    }

    private record ResolvedBankAccount(
            String bankAccountId,
            String accountNumber,
            String accountName,
            String bankBin,
            String bankCode,
            String bankShortName
    ) {
    }
}
