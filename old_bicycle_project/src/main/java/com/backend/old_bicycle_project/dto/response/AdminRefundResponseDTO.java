package com.backend.old_bicycle_project.dto.response;

import com.backend.old_bicycle_project.entity.enums.OrderFundingStatus;
import com.backend.old_bicycle_project.entity.enums.OrderStatus;
import com.backend.old_bicycle_project.entity.enums.PaymentMethod;
import com.backend.old_bicycle_project.entity.enums.RefundStatus;
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
public class AdminRefundResponseDTO {
    private UUID id;
    private UUID orderId;
    private UUID paymentId;
    private UUID requesterId;
    private String requesterName;
    private UUID buyerId;
    private String buyerName;
    private UUID sellerId;
    private String sellerName;
    private UUID productId;
    private String productTitle;
    private boolean hasInspection;
    private BigDecimal amount;
    private String reason;
    private String evidenceNote;
    private RefundStatus status;
    private String adminNote;
    private String refundReference;
    private UUID reviewedBy;
    private String reviewedByName;
    private LocalDateTime reviewedAt;
    private LocalDateTime processedAt;
    private LocalDateTime createdAt;
    private OrderStatus orderStatus;
    private OrderFundingStatus fundingStatus;
    private PaymentMethod paymentMethod;
}
