package com.backend.old_bicycle_project.dto.product;

import com.backend.old_bicycle_project.entity.enums.ConditionType;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class ProductUpdateRequest {
    @Size(max = 255, message = "Tiêu đề không được vượt quá 255 ký tự")
    private String title;

    @Size(max = 5000, message = "Mô tả không được vượt quá 5000 ký tự")
    private String description;

    @Positive(message = "Giá phải lớn hơn 0")
    private BigDecimal price;

    @Positive(message = "Giá gốc phải lớn hơn 0")
    private BigDecimal originalPrice;
    private UUID brakeTypeId;
    private UUID frameMaterialId;
    private UUID brandId;
    private UUID categoryId;

    @Size(max = 50, message = "Kích thước khung không được vượt quá 50 ký tự")
    private String frameSize;

    @Size(max = 50, message = "Kích thước bánh không được vượt quá 50 ký tự")
    private String wheelSize;
    private UUID groupsetId;

    @Size(max = 100, message = "Groupset không được vượt quá 100 ký tự")
    private String groupset;
    private ConditionType condition;

    @Size(max = 100, message = "Tỉnh/thành không được vượt quá 100 ký tự")
    private String province;

    @Size(max = 150, message = "Quận/huyện không được vượt quá 150 ký tự")
    private String district;
}
