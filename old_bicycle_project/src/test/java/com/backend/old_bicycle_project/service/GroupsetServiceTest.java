package com.backend.old_bicycle_project.service;

import com.backend.old_bicycle_project.entity.Groupset;
import com.backend.old_bicycle_project.exception.AppException;
import com.backend.old_bicycle_project.exception.ErrorCode;
import com.backend.old_bicycle_project.repository.GroupsetRepository;
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
class GroupsetServiceTest {

    @Mock
    private GroupsetRepository groupsetRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private GroupsetService groupsetService;

    @Test
    void createRejectsDuplicateGroupsetName() {
        when(groupsetRepository.existsByNameIgnoreCase("Shimano 105")).thenReturn(true);

        assertThatThrownBy(() -> groupsetService.create("Shimano 105", "11-speed road groupset"))
                .isInstanceOf(AppException.class)
                .extracting(ex -> ((AppException) ex).getErrorCode())
                .isEqualTo(ErrorCode.RECORD_ALREADY_EXISTS);
    }

    @Test
    void deleteRejectsGroupsetStillReferencedByProducts() {
        UUID groupsetId = UUID.randomUUID();
        when(groupsetRepository.findById(groupsetId))
                .thenReturn(Optional.of(Groupset.builder().id(groupsetId).name("Shimano 105").build()));
        when(productRepository.existsByGroupsetReferenceIdAndDeletedAtIsNull(groupsetId)).thenReturn(true);

        assertThatThrownBy(() -> groupsetService.delete(groupsetId))
                .isInstanceOf(AppException.class)
                .extracting(ex -> ((AppException) ex).getErrorCode())
                .isEqualTo(ErrorCode.REFERENCE_DATA_IN_USE);

        verify(groupsetRepository, never()).deleteById(groupsetId);
    }
}
