package com.backend.old_bicycle_project.service;

import com.backend.old_bicycle_project.entity.Category;
import com.backend.old_bicycle_project.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public List<Category> getRootCategories() {
        return categoryRepository.findByParentIsNull();
    }

    public List<Category> getAll() {
        return categoryRepository.findAll();
    }

    public Category getById(UUID id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy danh mục với ID: " + id));
    }

    public Category create(String name, String slug, UUID parentId) {
        if (categoryRepository.existsBySlug(slug)) {
            throw new RuntimeException("Slug '" + slug + "' đã tồn tại");
        }
        Category parent = parentId != null ? getById(parentId) : null;
        return categoryRepository.save(Category.builder()
                .name(name)
                .slug(slug)
                .parent(parent)
                .build());
    }

    public void delete(UUID id) {
        categoryRepository.deleteById(id);
    }
}
