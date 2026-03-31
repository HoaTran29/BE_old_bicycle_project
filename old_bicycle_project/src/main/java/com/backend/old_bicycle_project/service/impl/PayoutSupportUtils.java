package com.backend.old_bicycle_project.service.impl;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

final class PayoutSupportUtils {

    private PayoutSupportUtils() {
    }

    static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    static String buildTransferContent(com.backend.old_bicycle_project.entity.enums.PayoutType type, UUID referenceId) {
        String compactReference = referenceId.toString().replace("-", "").substring(0, 12).toUpperCase();
        return (type == com.backend.old_bicycle_project.entity.enums.PayoutType.refund ? "RF-" : "SL-") + compactReference;
    }

    static String buildQrCodeUrl(
            String bankBin,
            String accountNumber,
            String accountName,
            BigDecimal amount,
            String transferContent
    ) {
        if (!hasText(bankBin) || !hasText(accountNumber) || amount == null) {
            return null;
        }

        String safeAccountName = accountName != null ? accountName : "";
        return "https://img.vietqr.io/image/"
                + bankBin
                + "-"
                + accountNumber
                + "-compact2.png?amount="
                + amount.toPlainString()
                + "&addInfo="
                + URLEncoder.encode(transferContent, StandardCharsets.UTF_8)
                + "&accountName="
                + URLEncoder.encode(safeAccountName, StandardCharsets.UTF_8);
    }
}
