package com.backend.old_bicycle_project.service;

import com.backend.old_bicycle_project.entity.Category;
import com.backend.old_bicycle_project.exception.AppException;
import com.backend.old_bicycle_project.exception.ErrorCode;
import com.backend.old_bicycle_project.repository.CategoryRepository;
import com.backend.old_bicycle_project.repository.ProductRepository;
import com.backend.old_bicycle_project.repository.SizeChartRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final SizeChartRepository sizeChartRepository;

    public List<Category> getRootCategories() {
        return categoryRepository.findByParentIsNull();
    }

    public List<Category> getAll() {
        return categoryRepository.findAll(Sort.by(Sort.Direction.ASC, "name"));
    }

    public Category getById(UUID id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND));
    }

    public Category create(String name, String slug, UUID parentId) {
        String normalizedSlug = slug.trim();
        if (categoryRepository.existsBySlug(normalizedSlug)) {
            throw new AppException(ErrorCode.RECORD_ALREADY_EXISTS);
        }

        Category parent = parentId != null ? getById(parentId) : null;
        return categoryRepository.save(Category.builder()
                .name(name.trim())
                .slug(normalizedSlug)
                .parent(parent)
                .build());
    }

    public Category update(UUID id, String name, String slug, UUID parentId) {
        Category category = getById(id);
        String normalizedSlug = slug.trim();
        if (categoryRepository.existsBySlugAndIdNot(normalizedSlug, id)) {
            throw new AppException(ErrorCode.RECORD_ALREADY_EXISTS);
        }

        category.setName(name.trim());
        category.setSlug(normalizedSlug);
        category.setParent(resolveParentCategory(id, parentId));
        return categoryRepository.save(category);
    }

    public void delete(UUID id) {
        getById(id);
        if (categoryRepository.existsByParentId(id)) {
            throw new AppException(ErrorCode.CATEGORY_HIERARCHY_INVALID);
        }
        if (productRepository.existsByCategoryIdAndDeletedAtIsNull(id)) {
            throw new AppException(ErrorCode.REFERENCE_DATA_IN_USE);
        }
        if (sizeChartRepository.existsByCategoryId(id)) {
            throw new AppException(ErrorCode.REFERENCE_DATA_IN_USE);
        }
        categoryRepository.deleteById(id);
    }

    private Category resolveParentCategory(UUID categoryId, UUID parentId) {
        if (parentId == null) {
            return null;
        }
        if (categoryId.equals(parentId)) {
            throw new AppException(ErrorCode.CATEGORY_HIERARCHY_INVALID);
        }

        Category parent = getById(parentId);
        Category current = parent;
        while (current != null) {
            if (categoryId.equals(current.getId())) {
                throw new AppException(ErrorCode.CATEGORY_HIERARCHY_INVALID);
            }
            current = current.getParent();
        }
        return parent;
    }
}
