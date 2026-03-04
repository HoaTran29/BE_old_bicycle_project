package com.backend.old_bicycle_project.controller;

import com.backend.old_bicycle_project.dto.product.*;
import com.backend.old_bicycle_project.entity.User;
import com.backend.old_bicycle_project.entity.enums.ProductStatus;
import com.backend.old_bicycle_project.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    /**
     * GET /api/products — Tìm kiếm & lọc xe (public)
     */
    @GetMapping
    public ResponseEntity<Page<ProductResponse>> searchProducts(
            @ModelAttribute ProductFilterRequest filter,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size
    ) {
        return ResponseEntity.ok(productService.searchProducts(filter, page, size));
    }

    /**
     * GET /api/products/{id} — Xem chi tiết xe (public)
     */
    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getProduct(@PathVariable UUID id) {
        return ResponseEntity.ok(productService.getById(id));
    }

    /**
     * POST /api/products — Seller đăng tin xe mới (multipart: data + images)
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('SELLER', 'ADMIN')")
    public ResponseEntity<ProductResponse> createProduct(
            @Valid @ModelAttribute ProductCreateRequest request,
            @RequestPart(value = "images", required = false) List<MultipartFile> images,
            @AuthenticationPrincipal User currentUser
    ) {
        ProductResponse response = productService.create(request, images, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * PUT /api/products/{id} — Seller cập nhật tin
     */
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('SELLER', 'ADMIN')")
    public ResponseEntity<ProductResponse> updateProduct(
            @PathVariable UUID id,
            @ModelAttribute ProductUpdateRequest request,
            @RequestPart(value = "images", required = false) List<MultipartFile> newImages,
            @AuthenticationPrincipal User currentUser
    ) {
        return ResponseEntity.ok(productService.update(id, request, newImages, currentUser));
    }

    /**
     * DELETE /api/products/{id} — Seller xóa tin của mình
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SELLER', 'ADMIN')")
    public ResponseEntity<Map<String, String>> deleteProduct(
            @PathVariable UUID id,
            @AuthenticationPrincipal User currentUser
    ) {
        productService.delete(id, currentUser);
        return ResponseEntity.ok(Map.of("message", "Đã xóa tin đăng thành công"));
    }

    /**
     * GET /api/products/my — Seller xem tin của mình
     */
    @GetMapping("/my")
    @PreAuthorize("hasAnyRole('SELLER', 'ADMIN')")
    public ResponseEntity<Page<ProductResponse>> getMyProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @AuthenticationPrincipal User currentUser
    ) {
        // Dùng filter với sellerId
        ProductFilterRequest filter = new ProductFilterRequest();
        // Lấy tất cả trạng thái của seller → cần custom method
        return ResponseEntity.ok(productService.getAllForAdmin(null, page, size));
    }
}
