package com.backend.old_bicycle_project.dto.response;

import com.backend.old_bicycle_project.entity.enums.OrderFundingStatus;
import com.backend.old_bicycle_project.entity.enums.OrderStatus;
import com.backend.old_bicycle_project.entity.enums.PayoutProvider;
import com.backend.old_bicycle_project.entity.enums.PayoutStatus;
import com.backend.old_bicycle_project.entity.enums.PayoutType;
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
public class AdminPayoutResponseDTO {
    private UUID id;
    private PayoutType type;
    private PayoutStatus status;
    private PayoutProvider provider;
    private BigDecimal amount;
    private BigDecimal grossAmount;
    private BigDecimal feeDeductionAmount;
    private BigDecimal netAmount;
    private UUID recipientId;
    private String recipientName;
    private String bankCode;
    private String bankBin;
    private String accountNumber;
    private String accountName;
    private String transferContent;
    private String qrCodeUrl;
    private String bankReference;
    private String adminNote;
    private UUID orderId;
    private OrderStatus orderStatus;
    private OrderFundingStatus fundingStatus;
    private UUID refundRequestId;
    private UUID productId;
    private String productTitle;
    private UUID buyerId;
    private String buyerName;
    private UUID sellerId;
    private String sellerName;
    private UUID completedById;
    private String completedByName;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;
}
