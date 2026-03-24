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
public class InspectionRequestItemResponseDTO {
    private UUID inspectionId;
    private UUID productId;
    private String productTitle;
    private BigDecimal productPrice;
    private String province;
    private String productImageUrl;
    private UUID sellerId;
    private String sellerName;
    private String sellerPhone;
    private LocalDateTime requestedAt;
}
