package com.backend.old_bicycle_project.controller;

import com.backend.old_bicycle_project.dto.product.ProductResponse;
import com.backend.old_bicycle_project.dto.response.ApiResponse;
import com.backend.old_bicycle_project.dto.response.InspectionResponseDTO;
import com.backend.old_bicycle_project.entity.User;
import com.backend.old_bicycle_project.entity.enums.ProductStatus;
import com.backend.old_bicycle_project.service.InspectionService;
import com.backend.old_bicycle_project.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/products")
@RequiredArgsConstructor
public class AdminProductController {

    private final ProductService productService;
    private final InspectionService inspectionService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Page<ProductResponse>> getAdminProducts(
            @RequestParam(required = false) ProductStatus status,
            @RequestParam(required = false) UUID sellerId,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size
    ) {
        return ApiResponse.<Page<ProductResponse>>builder()
                .result(productService.getAllForAdmin(status, sellerId, keyword, page, size))
                .build();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<ProductResponse> getAdminProductById(@PathVariable UUID id) {
        return ApiResponse.<ProductResponse>builder()
                .result(productService.getAdminById(id))
                .build();
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<ProductResponse> updateAdminProductStatus(
            @PathVariable UUID id,
            @RequestParam ProductStatus status
    ) {
        return ApiResponse.<ProductResponse>builder()
                .result(productService.changeStatus(id, status))
                .build();
    }

    @PatchMapping("/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<ProductResponse> approveAdminProduct(@PathVariable UUID id) {
        return ApiResponse.<ProductResponse>builder()
                .result(productService.changeStatus(id, ProductStatus.active))
                .build();
    }

    @PatchMapping("/{id}/send-to-inspection")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<InspectionResponseDTO> sendAdminProductToInspection(
            @PathVariable UUID id,
            @AuthenticationPrincipal User currentUser
    ) {
        return ApiResponse.<InspectionResponseDTO>builder()
                .result(inspectionService.requestInspection(id, currentUser.getId()))
                .build();
    }

    @PatchMapping("/{id}/hide")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<ProductResponse> hideAdminProduct(@PathVariable UUID id) {
        return ApiResponse.<ProductResponse>builder()
                .result(productService.changeStatus(id, ProductStatus.hidden))
                .build();
    }
}
