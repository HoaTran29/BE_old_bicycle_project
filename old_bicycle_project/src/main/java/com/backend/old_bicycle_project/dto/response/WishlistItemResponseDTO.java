package com.backend.old_bicycle_project.dto.response;

import com.backend.old_bicycle_project.entity.enums.ProductStatus;
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
public class WishlistItemResponseDTO {
    private UUID productId;
    private String title;
    private BigDecimal price;
    private ProductStatus status;
    private UUID sellerId;
    private String sellerName;
    private String primaryImageUrl;
    private LocalDateTime addedAt;
}
