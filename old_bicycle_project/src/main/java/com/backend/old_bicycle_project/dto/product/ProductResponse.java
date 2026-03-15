package com.backend.old_bicycle_project.dto.product;

import com.backend.old_bicycle_project.entity.enums.ConditionType;
import com.backend.old_bicycle_project.entity.enums.ProductStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class ProductResponse {

    private UUID id;
    private String title;
    private String description;
    private BigDecimal price;
    private BigDecimal originalPrice;
    private ConditionType condition;
    private ProductStatus status;
    private String province;
    private String district;
    private String frameSize;
    private String wheelSize;
    private String groupset;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private SellerInfo seller;
    private String brandName;
    private String categoryName;
    private String brakeTypeName;
    private String frameMaterialName;
    private List<ImageInfo> images;
    private boolean isVerified;
    private InspectionInfo inspection;

    @Data
    @Builder
    public static class SellerInfo {
        private UUID id;
        private String firstName;
        private String lastName;
        private String avatarUrl;
        private String phone;
    }

    @Data
    @Builder
    public static class ImageInfo {
        private UUID id;
        private String url;
        private boolean isPrimary;
        private int displayOrder;
    }

    @Data
    @Builder
    public static class InspectionInfo {
        private UUID id;
        private BigDecimal overallScore;
        private Boolean passed;
        private String reportFileUrl;
        private LocalDateTime validUntil;
        private LocalDateTime createdAt;
    }
}
