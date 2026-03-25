package com.backend.old_bicycle_project.dto.response;

import com.backend.old_bicycle_project.entity.enums.OrderFundingStatus;
import com.backend.old_bicycle_project.entity.enums.OrderStatus;
import com.backend.old_bicycle_project.entity.enums.OrderCancelReason;
import com.backend.old_bicycle_project.entity.enums.PaymentMethod;
import com.backend.old_bicycle_project.entity.enums.PaymentOption;
import com.backend.old_bicycle_project.entity.enums.PlatformFeeStatus;
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
    private BigDecimal feeBaseAmount;
    private BigDecimal platformFeeRate;
    private BigDecimal platformFeeTotal;
    private BigDecimal buyerFeeAmount;
    private BigDecimal sellerFeeAmount;
    private BigDecimal buyerChargeAmount;
    private BigDecimal sellerGrossPayoutAmount;
    private BigDecimal sellerNetPayoutAmount;
    private PlatformFeeStatus platformFeeStatus;
    private LocalDateTime platformFeeRecognizedAt;
    private LocalDateTime platformFeeReversedAt;
    private PaymentOption paymentOption;
    private OrderStatus status;
    private OrderFundingStatus fundingStatus;
    private PaymentMethod paymentMethod;
    private boolean buyerReviewSubmitted;
    private OrderEvidenceSubmissionResponseDTO sellerHandoverEvidence;
    private OrderEvidenceSubmissionResponseDTO buyerReceiptEvidence;
    private LocalDateTime acceptedAt;
    private LocalDateTime paymentDeadline;
    private OrderCancelReason cancelReason;
    private LocalDateTime cancelledAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
