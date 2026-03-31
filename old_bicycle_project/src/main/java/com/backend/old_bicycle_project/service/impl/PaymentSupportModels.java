package com.backend.old_bicycle_project.service.impl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

record PaymentProvisionResult(
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

record ResolvedWebhookPayload(
        List<String> gatewayOrderCodeCandidates,
        BigDecimal amount,
        String transactionReference,
        LocalDateTime paymentDate,
        String rawPayload
) {
}

record ResolvedBankAccount(
        String bankAccountId,
        String accountNumber,
        String accountName,
        String bankBin,
        String bankCode,
        String bankShortName
) {
}
