package com.backend.old_bicycle_project.service;

import com.backend.old_bicycle_project.dto.request.SizeChartRowRequestDTO;
import com.backend.old_bicycle_project.dto.request.SizeChartUpsertRequestDTO;
import com.backend.old_bicycle_project.entity.Category;
import com.backend.old_bicycle_project.entity.SizeChart;
import com.backend.old_bicycle_project.exception.AppException;
import com.backend.old_bicycle_project.exception.ErrorCode;
import com.backend.old_bicycle_project.repository.CategoryRepository;
import com.backend.old_bicycle_project.repository.SizeChartRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SizeChartServiceTest {

    @Mock
    private SizeChartRepository sizeChartRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private SizeChartService sizeChartService;

    @Test
    void createBuildsRowsAndNormalizesFields() {
        Category category = Category.builder()
                .id(UUID.randomUUID())
                .name("Road Bike")
                .slug("road-bike")
                .build();
        SizeChartUpsertRequestDTO request = validRequest(category.getId());

        when(sizeChartRepository.existsByCategoryId(category.getId())).thenReturn(false);
        when(categoryRepository.findById(category.getId())).thenReturn(Optional.of(category));
        when(sizeChartRepository.save(any(SizeChart.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SizeChart sizeChart = sizeChartService.create(request);

        assertThat(sizeChart.getCategory()).isEqualTo(category);
        assertThat(sizeChart.getName()).isEqualTo("Road bike size guide");
        assertThat(sizeChart.getDescription()).isEqualTo("Height guidance for road bikes");
        assertThat(sizeChart.getRows()).hasSize(2);
        assertThat(sizeChart.getRows().getFirst().getFrameSize()).isEqualTo("52");
        assertThat(sizeChart.getRows().getFirst().getDisplayOrder()).isEqualTo(0);
        assertThat(sizeChart.getRows().get(1).getFrameSize()).isEqualTo("54");
        assertThat(sizeChart.getRows().get(1).getDisplayOrder()).isEqualTo(1);
    }

    @Test
    void createRejectsDuplicateCategory() {
        UUID categoryId = UUID.randomUUID();
        SizeChartUpsertRequestDTO request = validRequest(categoryId);
        when(sizeChartRepository.existsByCategoryId(categoryId)).thenReturn(true);

        assertThatThrownBy(() -> sizeChartService.create(request))
                .isInstanceOf(AppException.class)
                .extracting(ex -> ((AppException) ex).getErrorCode())
                .isEqualTo(ErrorCode.SIZE_CHART_ALREADY_EXISTS);

        verify(sizeChartRepository, never()).save(any(SizeChart.class));
    }

    @Test
    void createRejectsRowsWithInvalidHeightRange() {
        UUID categoryId = UUID.randomUUID();
        SizeChartUpsertRequestDTO request = validRequest(categoryId);
        request.getRows().getFirst().setHeightMinCm(180);
        request.getRows().getFirst().setHeightMaxCm(170);

        when(sizeChartRepository.existsByCategoryId(categoryId)).thenReturn(false);
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(Category.builder()
                .id(categoryId)
                .name("Road Bike")
                .slug("road-bike")
                .build()));

        assertThatThrownBy(() -> sizeChartService.create(request))
                .isInstanceOf(AppException.class)
                .extracting(ex -> ((AppException) ex).getErrorCode())
                .isEqualTo(ErrorCode.SIZE_CHART_INVALID);
    }

    @Test
    void createRejectsDuplicateFrameSizesInsideChart() {
        UUID categoryId = UUID.randomUUID();
        SizeChartUpsertRequestDTO request = validRequest(categoryId);
        request.getRows().get(1).setFrameSize("52");

        when(sizeChartRepository.existsByCategoryId(categoryId)).thenReturn(false);
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(Category.builder()
                .id(categoryId)
                .name("Road Bike")
                .slug("road-bike")
                .build()));

        assertThatThrownBy(() -> sizeChartService.create(request))
                .isInstanceOf(AppException.class)
                .extracting(ex -> ((AppException) ex).getErrorCode())
                .isEqualTo(ErrorCode.SIZE_CHART_INVALID);
    }

    private SizeChartUpsertRequestDTO validRequest(UUID categoryId) {
        SizeChartRowRequestDTO firstRow = new SizeChartRowRequestDTO();
        firstRow.setFrameSize("52");
        firstRow.setHeightMinCm(165);
        firstRow.setHeightMaxCm(172);
        firstRow.setNote("Phù hợp dáng người trung bình");

        SizeChartRowRequestDTO secondRow = new SizeChartRowRequestDTO();
        secondRow.setFrameSize("54");
        secondRow.setHeightMinCm(170);
        secondRow.setHeightMaxCm(178);
        secondRow.setNote("Tư thế road tiêu chuẩn");

        SizeChartUpsertRequestDTO request = new SizeChartUpsertRequestDTO();
        request.setCategoryId(categoryId);
        request.setName("Road bike size guide");
        request.setDescription("Height guidance for road bikes");
        request.setRows(List.of(firstRow, secondRow));
        return request;
    }
}
