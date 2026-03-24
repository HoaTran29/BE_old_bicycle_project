package com.backend.old_bicycle_project.service;

import com.backend.old_bicycle_project.exception.AppException;
import com.backend.old_bicycle_project.exception.ErrorCode;
import com.backend.old_bicycle_project.entity.Brand;
import com.backend.old_bicycle_project.repository.BrandRepository;
import com.backend.old_bicycle_project.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BrandServiceTest {

    @Mock
    private BrandRepository brandRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private BrandService brandService;

    @Test
    void createRejectsDuplicateBrandName() {
        when(brandRepository.existsByNameIgnoreCase("Trek")).thenReturn(true);

        assertThatThrownBy(() -> brandService.create("Trek", null))
                .isInstanceOf(AppException.class)
                .extracting(ex -> ((AppException) ex).getErrorCode())
                .isEqualTo(ErrorCode.RECORD_ALREADY_EXISTS);
    }

    @Test
    void deleteRejectsBrandInUseByProducts() {
        UUID brandId = UUID.randomUUID();
        when(brandRepository.findById(brandId)).thenReturn(java.util.Optional.of(Brand.builder().id(brandId).name("Trek").build()));
        when(productRepository.existsByBrandIdAndDeletedAtIsNull(brandId)).thenReturn(true);

        assertThatThrownBy(() -> brandService.delete(brandId))
                .isInstanceOf(AppException.class)
                .extracting(ex -> ((AppException) ex).getErrorCode())
                .isEqualTo(ErrorCode.REFERENCE_DATA_IN_USE);

        verify(brandRepository, never()).deleteById(brandId);
    }
}
