package com.backend.old_bicycle_project.dto.product;

import com.backend.old_bicycle_project.entity.enums.ConditionType;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class ProductFilterRequest {
    private String keyword;
    private UUID brandId;
    private UUID categoryId;
    private UUID brakeTypeId;
    private UUID frameMaterialId;
    private ConditionType condition;
    private String frameSize;
    private String wheelSize;
    private String groupset;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private String province;
    private Boolean hasInspection;
    private String sortBy;
}
