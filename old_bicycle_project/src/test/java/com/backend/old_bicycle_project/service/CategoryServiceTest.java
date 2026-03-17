package com.backend.old_bicycle_project.service;

import com.backend.old_bicycle_project.entity.Category;
import com.backend.old_bicycle_project.exception.AppException;
import com.backend.old_bicycle_project.exception.ErrorCode;
import com.backend.old_bicycle_project.repository.CategoryRepository;
import com.backend.old_bicycle_project.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private CategoryService categoryService;

    @Test
    void updateRejectsCircularCategoryHierarchy() {
        UUID categoryId = UUID.randomUUID();
        UUID childId = UUID.randomUUID();
        Category category = Category.builder().id(categoryId).name("Road").slug("road").build();
        Category child = Category.builder().id(childId).name("Race").slug("race").parent(category).build();
        category.setParent(child);

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(categoryRepository.findById(childId)).thenReturn(Optional.of(child));
        when(categoryRepository.existsBySlugAndIdNot("road", categoryId)).thenReturn(false);

        assertThatThrownBy(() -> categoryService.update(categoryId, "Road", "road", childId))
                .isInstanceOf(AppException.class)
                .extracting(ex -> ((AppException) ex).getErrorCode())
                .isEqualTo(ErrorCode.CATEGORY_HIERARCHY_INVALID);
    }

    @Test
    void deleteRejectsCategoryWithChildren() {
        UUID categoryId = UUID.randomUUID();
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(Category.builder().id(categoryId).name("Road").slug("road").build()));
        when(categoryRepository.existsByParentId(categoryId)).thenReturn(true);

        assertThatThrownBy(() -> categoryService.delete(categoryId))
                .isInstanceOf(AppException.class)
                .extracting(ex -> ((AppException) ex).getErrorCode())
                .isEqualTo(ErrorCode.CATEGORY_HIERARCHY_INVALID);

        verify(categoryRepository, never()).deleteById(categoryId);
    }

    @Test
    void deleteRejectsCategoryInUseByProducts() {
        UUID categoryId = UUID.randomUUID();
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(Category.builder().id(categoryId).name("Road").slug("road").build()));
        when(categoryRepository.existsByParentId(categoryId)).thenReturn(false);
        when(productRepository.existsByCategoryIdAndDeletedAtIsNull(categoryId)).thenReturn(true);

        assertThatThrownBy(() -> categoryService.delete(categoryId))
                .isInstanceOf(AppException.class)
                .extracting(ex -> ((AppException) ex).getErrorCode())
                .isEqualTo(ErrorCode.REFERENCE_DATA_IN_USE);

        verify(categoryRepository, never()).deleteById(categoryId);
    }
}
