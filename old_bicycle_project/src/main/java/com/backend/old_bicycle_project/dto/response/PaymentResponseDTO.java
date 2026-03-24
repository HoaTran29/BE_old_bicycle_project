package com.backend.old_bicycle_project.dto.response;

import com.backend.old_bicycle_project.entity.enums.PaymentGateway;
import com.backend.old_bicycle_project.entity.enums.PaymentMethod;
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
public class PaymentResponseDTO {
    private UUID id;
    private UUID orderId;
    private BigDecimal amount;
    private PaymentGateway gateway;
    private PaymentMethod method;
    private PaymentPhase phase;
    private PaymentStatus status;
    private String gatewayOrderCode;
    private String transactionReference;
    private String checkoutUrl;
    private String qrCodeUrl;
    private LocalDateTime paymentDate;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
}
