package com.backend.old_bicycle_project.controller;

import com.backend.old_bicycle_project.dto.product.ProductCreateRequest;
import com.backend.old_bicycle_project.dto.product.ProductFilterRequest;
import com.backend.old_bicycle_project.dto.product.ProductResponse;
import com.backend.old_bicycle_project.dto.product.ProductUpdateRequest;
import com.backend.old_bicycle_project.dto.response.ApiResponse;
import com.backend.old_bicycle_project.entity.User;
import com.backend.old_bicycle_project.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public ApiResponse<Page<ProductResponse>> searchProducts(
            @ModelAttribute ProductFilterRequest filter,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size
    ) {
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

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('SELLER', 'ADMIN')")
    public ApiResponse<ProductResponse> createProduct(
            @Valid @ModelAttribute ProductCreateRequest request,
            @RequestPart(value = "images", required = false) List<MultipartFile> images,
            @AuthenticationPrincipal User currentUser
    ) {
        ProductResponse response = productService.create(request, images, currentUser);
        return ApiResponse.<ProductResponse>builder()
                .result(response)
                .build();
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('SELLER', 'ADMIN')")
    public ApiResponse<ProductResponse> updateProduct(
            @PathVariable UUID id,
            @ModelAttribute ProductUpdateRequest request,
            @RequestPart(value = "images", required = false) List<MultipartFile> newImages,
            @AuthenticationPrincipal User currentUser
    ) {
        return ApiResponse.<ProductResponse>builder()
                .result(productService.update(id, request, newImages, currentUser))
                .build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SELLER', 'ADMIN')")
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
    @PreAuthorize("hasAnyRole('SELLER', 'ADMIN')")
    public ApiResponse<Page<ProductResponse>> getMyProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @AuthenticationPrincipal User currentUser
    ) {
        return ApiResponse.<Page<ProductResponse>>builder()
                .result(productService.getMyProducts(currentUser, page, size))
                .build();
    }
}
