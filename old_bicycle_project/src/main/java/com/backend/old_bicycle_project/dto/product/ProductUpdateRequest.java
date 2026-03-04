package com.backend.old_bicycle_project.dto.product;

import com.backend.old_bicycle_project.entity.enums.ConditionType;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class ProductUpdateRequest {
    private String title;
    private String description;
    private BigDecimal price;
    private BigDecimal originalPrice;
    private UUID brakeTypeId;
    private UUID frameMaterialId;
    private UUID brandId;
    private UUID categoryId;
    private String frameSize;
    private String wheelSize;
    private String groupset;
    private ConditionType condition;
    private String province;
    private String district;
}
