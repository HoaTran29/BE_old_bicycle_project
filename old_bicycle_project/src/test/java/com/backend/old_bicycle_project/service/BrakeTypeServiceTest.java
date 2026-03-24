package com.backend.old_bicycle_project.service;

import com.backend.old_bicycle_project.entity.BrakeType;
import com.backend.old_bicycle_project.exception.AppException;
import com.backend.old_bicycle_project.exception.ErrorCode;
import com.backend.old_bicycle_project.repository.BrakeTypeRepository;
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
class BrakeTypeServiceTest {

    @Mock
    private BrakeTypeRepository brakeTypeRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private BrakeTypeService brakeTypeService;

    @Test
    void createRejectsDuplicateBrakeTypeName() {
        when(brakeTypeRepository.existsByNameIgnoreCase("Disc")).thenReturn(true);

        assertThatThrownBy(() -> brakeTypeService.create("Disc", "Hydraulic"))
                .isInstanceOf(AppException.class)
                .extracting(ex -> ((AppException) ex).getErrorCode())
                .isEqualTo(ErrorCode.RECORD_ALREADY_EXISTS);
    }

    @Test
    void deleteRejectsBrakeTypeInUseByProducts() {
        UUID brakeTypeId = UUID.randomUUID();
        when(brakeTypeRepository.findById(brakeTypeId))
                .thenReturn(java.util.Optional.of(BrakeType.builder().id(brakeTypeId).name("Disc").build()));
        when(productRepository.existsByBrakeTypeIdAndDeletedAtIsNull(brakeTypeId)).thenReturn(true);

        assertThatThrownBy(() -> brakeTypeService.delete(brakeTypeId))
                .isInstanceOf(AppException.class)
                .extracting(ex -> ((AppException) ex).getErrorCode())
                .isEqualTo(ErrorCode.REFERENCE_DATA_IN_USE);

        verify(brakeTypeRepository, never()).deleteById(brakeTypeId);
    }
}
