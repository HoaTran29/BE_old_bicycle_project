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
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
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
    public ResponseEntity<List<Brand>> getAllBrands() {
        return ResponseEntity.ok(brandService.getAll());
    }

    @PostMapping("/api/admin/brands")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Brand> createBrand(
            @RequestParam String name,
            @RequestParam(required = false) String logoUrl
    ) {
        return ResponseEntity.ok(brandService.create(name, logoUrl));
    }

    @PutMapping("/api/admin/brands/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Brand> updateBrand(
            @PathVariable UUID id,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String logoUrl
    ) {
        return ResponseEntity.ok(brandService.update(id, name, logoUrl));
    }

    @DeleteMapping("/api/admin/brands/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> deleteBrand(@PathVariable UUID id) {
        brandService.delete(id);
        return ResponseEntity.ok(Map.of("message", "Đã xóa hãng xe"));
    }

    // ==================== CATEGORIES ====================

    @GetMapping("/api/categories")
    public ResponseEntity<List<Category>> getAllCategories() {
        return ResponseEntity.ok(categoryService.getAll());
    }

    @PostMapping("/api/admin/categories")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Category> createCategory(
            @RequestParam String name,
            @RequestParam String slug,
            @RequestParam(required = false) UUID parentId
    ) {
        return ResponseEntity.ok(categoryService.create(name, slug, parentId));
    }

    // ==================== BRAKE TYPES ====================

    @GetMapping("/api/brake-types")
    public ResponseEntity<List<BrakeType>> getAllBrakeTypes() {
        return ResponseEntity.ok(brakeTypeRepository.findAll());
    }

    // ==================== FRAME MATERIALS ====================

    @GetMapping("/api/frame-materials")
    public ResponseEntity<List<FrameMaterial>> getAllFrameMaterials() {
        return ResponseEntity.ok(frameMaterialRepository.findAll());
    }
}
