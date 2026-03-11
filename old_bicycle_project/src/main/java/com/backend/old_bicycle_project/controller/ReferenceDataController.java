package com.backend.old_bicycle_project.controller;

import com.backend.old_bicycle_project.entity.Brand;
import com.backend.old_bicycle_project.entity.BrakeType;
import com.backend.old_bicycle_project.entity.Category;
import com.backend.old_bicycle_project.entity.FrameMaterial;
import com.backend.old_bicycle_project.repository.BrakeTypeRepository;
import com.backend.old_bicycle_project.repository.FrameMaterialRepository;
import com.backend.old_bicycle_project.service.BrandService;
import com.backend.old_bicycle_project.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.backend.old_bicycle_project.dto.response.ApiResponse;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class ReferenceDataController {

    private final BrandService brandService;
    private final CategoryService categoryService;
    private final BrakeTypeRepository brakeTypeRepository;
    private final FrameMaterialRepository frameMaterialRepository;

    // ==================== BRANDS ====================

    @GetMapping("/api/brands")
    public ApiResponse<List<Brand>> getAllBrands() {
        return ApiResponse.<List<Brand>>builder()
                .result(brandService.getAll())
                .build();
    }

    @PostMapping("/api/admin/brands")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Brand> createBrand(
            @RequestParam String name,
            @RequestParam(required = false) String logoUrl
    ) {
        return ApiResponse.<Brand>builder()
                .result(brandService.create(name, logoUrl))
                .build();
    }

    @PutMapping("/api/admin/brands/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Brand> updateBrand(
            @PathVariable UUID id,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String logoUrl
    ) {
        return ApiResponse.<Brand>builder()
                .result(brandService.update(id, name, logoUrl))
                .build();
    }

    @DeleteMapping("/api/admin/brands/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<String> deleteBrand(@PathVariable UUID id) {
        brandService.delete(id);
        return ApiResponse.<String>builder()
                .result("Đã xóa hãng xe")
                .build();
    }

    // ==================== CATEGORIES ====================

    @GetMapping("/api/categories")
    public ApiResponse<List<Category>> getAllCategories() {
        return ApiResponse.<List<Category>>builder()
                .result(categoryService.getAll())
                .build();
    }

    @PostMapping("/api/admin/categories")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Category> createCategory(
            @RequestParam String name,
            @RequestParam String slug,
            @RequestParam(required = false) UUID parentId
    ) {
        return ApiResponse.<Category>builder()
                .result(categoryService.create(name, slug, parentId))
                .build();
    }

    // ==================== BRAKE TYPES ====================

    @GetMapping("/api/brake-types")
    public ApiResponse<List<BrakeType>> getAllBrakeTypes() {
        return ApiResponse.<List<BrakeType>>builder()
                .result(brakeTypeRepository.findAll())
                .build();
    }

    // ==================== FRAME MATERIALS ====================

    @GetMapping("/api/frame-materials")
    public ApiResponse<List<FrameMaterial>> getAllFrameMaterials() {
        return ApiResponse.<List<FrameMaterial>>builder()
                .result(frameMaterialRepository.findAll())
                .build();
    }
}
