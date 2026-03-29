package com.backend.old_bicycle_project.controller;

import com.backend.old_bicycle_project.dto.product.ProductCreateRequest;
import com.backend.old_bicycle_project.dto.product.ProductFilterRequest;
import com.backend.old_bicycle_project.dto.product.ProductResponse;
import com.backend.old_bicycle_project.dto.product.ProductUpdateRequest;
import com.backend.old_bicycle_project.dto.response.ApiResponse;
import com.backend.old_bicycle_project.entity.User;
import com.backend.old_bicycle_project.entity.enums.ConditionType;
import com.backend.old_bicycle_project.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping
    @Operation(
            summary = "Lấy danh sách xe công khai",
            description = "Để trống toàn bộ bộ lọc nếu muốn lấy tất cả tin đang hiển thị cho người mua. "
                    + "Chỉ nhập các query param thực sự cần lọc, ví dụ keyword hoặc categoryId."
    )
    public ApiResponse<Page<ProductResponse>> searchProducts(
            @Parameter(description = "Từ khóa tìm trong tiêu đề xe")
            @RequestParam(required = false) String keyword,
            @Parameter(description = "ID thương hiệu")
            @RequestParam(required = false) UUID brandId,
            @Parameter(description = "ID danh mục")
            @RequestParam(required = false) UUID categoryId,
            @Parameter(description = "ID loại phanh")
            @RequestParam(required = false) UUID brakeTypeId,
            @Parameter(description = "ID chất liệu khung")
            @RequestParam(required = false) UUID frameMaterialId,
            @Parameter(description = "Tình trạng xe")
            @RequestParam(required = false) ConditionType condition,
            @Parameter(description = "Kích thước khung")
            @RequestParam(required = false) String frameSize,
            @Parameter(description = "Kích thước bánh")
            @RequestParam(required = false) String wheelSize,
            @Parameter(description = "Nhóm truyền động")
            @RequestParam(required = false) String groupset,
            @Parameter(description = "ID groupset")
            @RequestParam(required = false) UUID groupsetId,
            @Parameter(description = "Giá tối thiểu")
            @RequestParam(required = false) BigDecimal minPrice,
            @Parameter(description = "Giá tối đa")
            @RequestParam(required = false) BigDecimal maxPrice,
            @Parameter(description = "Tỉnh hoặc thành phố")
            @RequestParam(required = false) String province,
            @Parameter(description = "Chỉ lấy xe có inspection hợp lệ")
            @RequestParam(required = false) Boolean hasInspection,
            @Parameter(description = "Cách sắp xếp")
            @RequestParam(required = false) String sortBy,
            @Parameter(description = "Trang bắt đầu từ 0", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Số lượng phần tử mỗi trang", example = "12")
            @RequestParam(defaultValue = "12") int size
    ) {
        ProductFilterRequest filter = new ProductFilterRequest();
        filter.setKeyword(keyword);
        filter.setBrandId(brandId);
        filter.setCategoryId(categoryId);
        filter.setBrakeTypeId(brakeTypeId);
        filter.setFrameMaterialId(frameMaterialId);
        filter.setCondition(condition);
        filter.setFrameSize(frameSize);
        filter.setWheelSize(wheelSize);
        filter.setGroupset(groupset);
        filter.setGroupsetId(groupsetId);
        filter.setMinPrice(minPrice);
        filter.setMaxPrice(maxPrice);
        filter.setProvince(province);
        filter.setHasInspection(hasInspection);
        filter.setSortBy(sortBy);

        return ApiResponse.<Page<ProductResponse>>builder()
                .result(productService.searchProducts(filter, page, size))
                .build();
    }

    @GetMapping("/{id}")
    public ApiResponse<ProductResponse> getProduct(@PathVariable UUID id) {
        return ApiResponse.<ProductResponse>builder()
                .result(productService.getById(id))
                .build();
    }

    @GetMapping("/my/{id}")
    @PreAuthorize("hasRole('SELLER')")
    public ApiResponse<ProductResponse> getMyProductById(
            @PathVariable UUID id,
            @AuthenticationPrincipal User currentUser
    ) {
        return ApiResponse.<ProductResponse>builder()
                .result(productService.getMineById(id, currentUser))
                .build();
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('SELLER')")
    public ApiResponse<ProductResponse> createProduct(
            @Valid @ModelAttribute ProductCreateRequest request,
            @RequestPart(value = "images", required = false) List<MultipartFile> images,
            @AuthenticationPrincipal User currentUser
    ) {
        return ApiResponse.<ProductResponse>builder()
                .result(productService.create(request, images, currentUser))
                .build();
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('SELLER')")
    public ApiResponse<ProductResponse> updateProduct(
            @PathVariable UUID id,
            @Valid @ModelAttribute ProductUpdateRequest request,
            @RequestPart(value = "images", required = false) List<MultipartFile> newImages,
            @AuthenticationPrincipal User currentUser
    ) {
        return ApiResponse.<ProductResponse>builder()
                .result(productService.update(id, request, newImages, currentUser))
                .build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SELLER')")
    public ApiResponse<String> deleteProduct(
            @PathVariable UUID id,
            @AuthenticationPrincipal User currentUser
    ) {
        productService.delete(id, currentUser);
        return ApiResponse.<String>builder()
                .result("Đã xóa tin đăng thành công")
                .build();
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('SELLER')")
    public ApiResponse<Page<ProductResponse>> getMyProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @AuthenticationPrincipal User currentUser
    ) {
        return ApiResponse.<Page<ProductResponse>>builder()
                .result(productService.getMyProducts(currentUser, page, size))
                .build();
    }

    @PatchMapping("/{id}/hide")
    @PreAuthorize("hasRole('SELLER')")
    public ApiResponse<ProductResponse> hideProduct(
            @PathVariable UUID id,
            @AuthenticationPrincipal User currentUser
    ) {
        return ApiResponse.<ProductResponse>builder()
                .result(productService.hide(id, currentUser))
                .build();
    }

    @PatchMapping("/{id}/show")
    @PreAuthorize("hasRole('SELLER')")
    public ApiResponse<ProductResponse> showProduct(
            @PathVariable UUID id,
            @AuthenticationPrincipal User currentUser
    ) {
        return ApiResponse.<ProductResponse>builder()
                .result(productService.show(id, currentUser))
                .build();
    }
}
