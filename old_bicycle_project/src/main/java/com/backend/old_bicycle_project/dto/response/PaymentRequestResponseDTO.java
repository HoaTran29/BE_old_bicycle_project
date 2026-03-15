package com.backend.old_bicycle_project.dto.response;

import com.backend.old_bicycle_project.entity.enums.PaymentGateway;
import com.backend.old_bicycle_project.entity.enums.PaymentPhase;
import com.backend.old_bicycle_project.entity.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentRequestResponseDTO {
    private UUID paymentId;
    private UUID orderId;
    private PaymentGateway gateway;
    private PaymentPhase phase;
    private PaymentStatus status;
    private BigDecimal amount;
    private String gatewayOrderCode;
    private String checkoutUrl;
    private String qrCodeUrl;
    private String transferContent;
    private String bankBin;
    private String bankAccountNumber;
    private String bankAccountName;
    private boolean mockMode;
    private String instructions;
    private LocalDateTime expiresAt;
}
