package com.backend.old_bicycle_project.dto.response;

import com.backend.old_bicycle_project.entity.enums.OrderFundingStatus;
import com.backend.old_bicycle_project.entity.enums.OrderStatus;
import com.backend.old_bicycle_project.entity.enums.PaymentMethod;
import com.backend.old_bicycle_project.entity.enums.PaymentOption;
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
public class OrderResponseDTO {
    private UUID id;
    private UUID productId;
    private String productTitle;
    private UUID buyerId;
    private String buyerName;
    private UUID sellerId;
    private String sellerName;
    private BigDecimal totalAmount;
    private BigDecimal depositAmount;
    private BigDecimal requiredUpfrontAmount;
    private BigDecimal paidAmount;
    private BigDecimal remainingAmount;
    private BigDecimal serviceFee;
    private PaymentOption paymentOption;
    private OrderStatus status;
    private OrderFundingStatus fundingStatus;
    private PaymentMethod paymentMethod;
    private boolean buyerReviewSubmitted;
    private LocalDateTime acceptedAt;
    private LocalDateTime paymentDeadline;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
