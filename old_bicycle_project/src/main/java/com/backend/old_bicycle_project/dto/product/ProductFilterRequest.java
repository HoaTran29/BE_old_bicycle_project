package com.backend.old_bicycle_project.dto.product;

import com.backend.old_bicycle_project.entity.enums.ConditionType;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class ProductFilterRequest {
    private String keyword;          // tìm theo title
    private UUID brandId;
    private UUID categoryId;
    private UUID brakeTypeId;
    private UUID frameMaterialId;
    private ConditionType condition;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private String province;
    private Boolean hasInspection;   // lọc xe có Verified Badge
    private String sortBy;           // "price_asc", "price_desc", "newest"
}
