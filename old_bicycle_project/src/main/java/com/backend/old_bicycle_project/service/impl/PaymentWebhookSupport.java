package com.backend.old_bicycle_project.service.impl;

import com.backend.old_bicycle_project.config.SepayProperties;
import com.backend.old_bicycle_project.dto.request.SepayWebhookRequestDTO;
import com.backend.old_bicycle_project.entity.Payment;
import com.backend.old_bicycle_project.exception.AppException;
import com.backend.old_bicycle_project.exception.ErrorCode;
import com.backend.old_bicycle_project.repository.PaymentRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.backend.old_bicycle_project.service.impl.PaymentSupportUtils.firstNonBlank;
import static com.backend.old_bicycle_project.service.impl.PaymentSupportUtils.hasText;
import static com.backend.old_bicycle_project.service.impl.PaymentSupportUtils.parseBigDecimal;
import static com.backend.old_bicycle_project.service.impl.PaymentSupportUtils.parseDateTime;
import static com.backend.old_bicycle_project.service.impl.PaymentSupportUtils.parseLong;
import static com.backend.old_bicycle_project.service.impl.PaymentSupportUtils.textOrNull;

final class PaymentWebhookSupport {

    private static final Pattern GATEWAY_ORDER_CODE_PATTERN =
            Pattern.compile("(?i)OB-[A-Z0-9-]{5,40}");
    private static final Pattern COMPACT_GATEWAY_ORDER_CODE_PATTERN =
            Pattern.compile("(?i)OB[A-Z0-9]{18}");

    private final PaymentRepository paymentRepository;
    private final SepayProperties sepayProperties;
    private final ObjectMapper objectMapper;

    PaymentWebhookSupport(
            PaymentRepository paymentRepository,
            SepayProperties sepayProperties,
            ObjectMapper objectMapper
    ) {
        this.paymentRepository = paymentRepository;
        this.sepayProperties = sepayProperties;
        this.objectMapper = objectMapper;
    }

    void validateWebhookAuthorization(String authorizationHeader) {
        if (!hasText(sepayProperties.getWebhookApiKey())) {
            if (!sepayProperties.isMockMode()) {
                throw new AppException(ErrorCode.PAYMENT_VALIDATION_FAILED);
            }
            return;
        }

        String expectedKey = sepayProperties.getWebhookApiKey().trim();
        String normalizedAuth = authorizationHeader == null ? "" : authorizationHeader.trim();

        boolean matches = expectedKey.equals(normalizedAuth)
                || ("Apikey " + expectedKey).equalsIgnoreCase(normalizedAuth);
        if (!matches) {
            throw new AppException(ErrorCode.PAYMENT_VALIDATION_FAILED);
        }
    }

    ResolvedWebhookPayload resolveWebhookPayload(String rawPayload) {
        if (!hasText(rawPayload)) {
            throw new AppException(ErrorCode.PAYMENT_VALIDATION_FAILED);
        }

        SepayWebhookRequestDTO legacyPayload = parseLegacyWebhook(rawPayload);
        return resolveLegacyWebhook(legacyPayload, rawPayload);
    }

    Payment findPaymentForWebhook(List<String> gatewayOrderCodeCandidates) {
        for (String gatewayOrderCodeCandidate : gatewayOrderCodeCandidates) {
            if (!hasText(gatewayOrderCodeCandidate)) {
                continue;
            }
            Payment payment = paymentRepository.findByGatewayOrderCode(gatewayOrderCodeCandidate)
                    .orElseGet(() -> paymentRepository.findByGatewayOrderCode(gatewayOrderCodeCandidate.toUpperCase())
                            .orElse(null));
            if (payment != null) {
                return payment;
            }
        }
        throw new AppException(ErrorCode.PAYMENT_VALIDATION_FAILED);
    }

    private ResolvedWebhookPayload resolveLegacyWebhook(SepayWebhookRequestDTO requestDTO, String rawPayload) {
        if (requestDTO.getTransferType() != null && !"in".equalsIgnoreCase(requestDTO.getTransferType())) {
            return null;
        }
        List<String> gatewayOrderCodeCandidates = resolveGatewayOrderCodeCandidates(requestDTO);
        if (gatewayOrderCodeCandidates.isEmpty() || requestDTO.getTransferAmount() == null) {
            throw new AppException(ErrorCode.PAYMENT_VALIDATION_FAILED);
        }

        return new ResolvedWebhookPayload(
                gatewayOrderCodeCandidates,
                requestDTO.getTransferAmount(),
                resolveTransactionReference(requestDTO),
                requestDTO.getTransactionDate() != null ? requestDTO.getTransactionDate() : LocalDateTime.now(),
                rawPayload
        );
    }

    private SepayWebhookRequestDTO parseLegacyWebhook(String rawPayload) {
        JsonNode root = PaymentSupportUtils.parseJson(rawPayload, objectMapper);
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

    private List<String> resolveGatewayOrderCodeCandidates(SepayWebhookRequestDTO requestDTO) {
        List<String> candidates = new ArrayList<>();
        addGatewayOrderCodeCandidate(candidates, requestDTO.getCode());
        addGatewayOrderCodeCandidate(candidates, requestDTO.getContent());
        addGatewayOrderCodeCandidate(candidates, requestDTO.getDescription());
        return candidates;
    }

    private void addGatewayOrderCodeCandidate(List<String> candidates, String rawValue) {
        if (!hasText(rawValue)) {
            return;
        }

        String trimmedValue = rawValue.trim();
        boolean looksLikeStandaloneCode = !trimmedValue.isBlank()
                && !trimmedValue.contains(" ")
                && !trimmedValue.contains("\t")
                && !trimmedValue.contains("\n");
        if (looksLikeStandaloneCode && !candidates.contains(trimmedValue)) {
            candidates.add(trimmedValue);
        }

        String normalizedCompactCode = normalizeCompactGatewayOrderCode(trimmedValue);
        if (hasText(normalizedCompactCode) && !candidates.contains(normalizedCompactCode)) {
            candidates.add(normalizedCompactCode);
        }

        String extractedCode = extractGatewayOrderCodeFromText(rawValue);
        if (hasText(extractedCode) && !candidates.contains(extractedCode)) {
            candidates.add(extractedCode);
        }
    }

    private String extractGatewayOrderCodeFromText(String rawText) {
        if (!hasText(rawText)) {
            return null;
        }

        Matcher matcher = GATEWAY_ORDER_CODE_PATTERN.matcher(rawText);
        if (!matcher.find()) {
            Matcher compactMatcher = COMPACT_GATEWAY_ORDER_CODE_PATTERN.matcher(rawText);
            if (!compactMatcher.find()) {
                return null;
            }
            return normalizeCompactGatewayOrderCode(compactMatcher.group());
        }

        return matcher.group().toUpperCase();
    }

    private String normalizeCompactGatewayOrderCode(String rawValue) {
        if (!hasText(rawValue)) {
            return null;
        }

        String alphanumericOnly = rawValue.replaceAll("[^A-Za-z0-9]", "").toUpperCase();
        if (!alphanumericOnly.startsWith("OB") || alphanumericOnly.length() != 20) {
            return null;
        }

        String compactBody = alphanumericOnly.substring(2);
        return "OB-" + compactBody.substring(0, 12) + "-" + compactBody.substring(12);
    }

    private String resolveTransactionReference(SepayWebhookRequestDTO requestDTO) {
        if (requestDTO.getReferenceCode() != null && !requestDTO.getReferenceCode().isBlank()) {
            return requestDTO.getReferenceCode();
        }
        return requestDTO.getId() != null ? String.valueOf(requestDTO.getId()) : UUID.randomUUID().toString();
    }
}
