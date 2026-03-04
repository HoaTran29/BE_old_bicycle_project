package com.backend.old_bicycle_project.dto.product;

import com.backend.old_bicycle_project.entity.enums.ConditionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class ProductCreateRequest {

    @NotBlank(message = "Tiêu đề không được để trống")
    private String title;

    private String description;

    @NotNull(message = "Giá không được để trống")
    @Positive(message = "Giá phải lớn hơn 0")
    private BigDecimal price;

    private BigDecimal originalPrice;

    @NotNull(message = "Vui lòng chọn loại phanh")
    private UUID brakeTypeId;

    @NotNull(message = "Vui lòng chọn chất liệu khung")
    private UUID frameMaterialId;

    private UUID brandId;
    private UUID categoryId;

    private String frameSize;
    private String wheelSize;
    private String groupset;

    private ConditionType condition;

    @NotBlank(message = "Vui lòng chọn tỉnh/thành")
    private String province;

    private String district;
}
