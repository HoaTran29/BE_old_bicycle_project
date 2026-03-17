package com.backend.old_bicycle_project.service;

import com.backend.old_bicycle_project.entity.FrameMaterial;
import com.backend.old_bicycle_project.exception.AppException;
import com.backend.old_bicycle_project.exception.ErrorCode;
import com.backend.old_bicycle_project.repository.FrameMaterialRepository;
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
class FrameMaterialServiceTest {

    @Mock
    private FrameMaterialRepository frameMaterialRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private FrameMaterialService frameMaterialService;

    @Test
    void createRejectsDuplicateFrameMaterialName() {
        when(frameMaterialRepository.existsByNameIgnoreCase("Carbon")).thenReturn(true);

        assertThatThrownBy(() -> frameMaterialService.create("Carbon", "Carbon frame"))
                .isInstanceOf(AppException.class)
                .extracting(ex -> ((AppException) ex).getErrorCode())
                .isEqualTo(ErrorCode.RECORD_ALREADY_EXISTS);
    }

    @Test
    void deleteRejectsFrameMaterialInUseByProducts() {
        UUID frameMaterialId = UUID.randomUUID();
        when(frameMaterialRepository.findById(frameMaterialId))
                .thenReturn(java.util.Optional.of(FrameMaterial.builder().id(frameMaterialId).name("Carbon").build()));
        when(productRepository.existsByFrameMaterialIdAndDeletedAtIsNull(frameMaterialId)).thenReturn(true);

        assertThatThrownBy(() -> frameMaterialService.delete(frameMaterialId))
                .isInstanceOf(AppException.class)
                .extracting(ex -> ((AppException) ex).getErrorCode())
                .isEqualTo(ErrorCode.REFERENCE_DATA_IN_USE);

        verify(frameMaterialRepository, never()).deleteById(frameMaterialId);
    }
}
