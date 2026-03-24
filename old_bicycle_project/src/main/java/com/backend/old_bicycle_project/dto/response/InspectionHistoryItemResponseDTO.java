package com.backend.old_bicycle_project.dto.response;

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
public class InspectionHistoryItemResponseDTO {
    private UUID inspectionId;
    private UUID productId;
    private String productTitle;
    private BigDecimal productPrice;
    private String province;
    private String productImageUrl;
    private UUID sellerId;
    private String sellerName;
    private String sellerPhone;
    private UUID inspectorId;
    private String inspectorName;
    private BigDecimal overallScore;
    private Boolean passed;
    private String reportFileUrl;
    private LocalDateTime requestedAt;
    private LocalDateTime evaluatedAt;
    private LocalDateTime validUntil;
}
