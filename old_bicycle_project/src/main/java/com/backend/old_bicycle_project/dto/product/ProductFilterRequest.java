package com.backend.old_bicycle_project.dto.product;

import com.backend.old_bicycle_project.entity.enums.ConditionType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Schema(description = "Bộ lọc query cho API lấy danh sách xe. Mọi field đều là tùy chọn.")
public class ProductFilterRequest {
    @Schema(description = "Từ khóa tìm trong tiêu đề xe")
    private String keyword;

    @Schema(description = "ID thương hiệu")
    private UUID brandId;

    @Schema(description = "ID danh mục")
    private UUID categoryId;

    @Schema(description = "ID loại phanh")
    private UUID brakeTypeId;

    @Schema(description = "ID chất liệu khung")
    private UUID frameMaterialId;

    @Schema(description = "Tình trạng xe")
    private ConditionType condition;

    @Schema(description = "Kích thước khung")
    private String frameSize;

    @Schema(description = "Kích thước bánh")
    private String wheelSize;

    @Schema(description = "Nhóm truyền động")
    private String groupset;

    @Schema(description = "ID groupset")
    private UUID groupsetId;

    @Schema(description = "Giá tối thiểu")
    private BigDecimal minPrice;

    @Schema(description = "Giá tối đa")
    private BigDecimal maxPrice;

    @Schema(description = "Tỉnh hoặc thành phố")
    private String province;

    @Schema(description = "Chỉ lấy xe có inspection hợp lệ")
    private Boolean hasInspection;

    @Schema(description = "Cách sắp xếp")
    private String sortBy;
}
