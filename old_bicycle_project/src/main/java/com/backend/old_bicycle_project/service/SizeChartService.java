package com.backend.old_bicycle_project.service;

import com.backend.old_bicycle_project.dto.request.SizeChartRowRequestDTO;
import com.backend.old_bicycle_project.dto.request.SizeChartUpsertRequestDTO;
import com.backend.old_bicycle_project.entity.Category;
import com.backend.old_bicycle_project.entity.SizeChart;
import com.backend.old_bicycle_project.entity.SizeChartRow;
import com.backend.old_bicycle_project.exception.AppException;
import com.backend.old_bicycle_project.exception.ErrorCode;
import com.backend.old_bicycle_project.repository.CategoryRepository;
import com.backend.old_bicycle_project.repository.SizeChartRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SizeChartService {

    private final SizeChartRepository sizeChartRepository;
    private final CategoryRepository categoryRepository;

    public List<SizeChart> getAll() {
        return sizeChartRepository.findAllByOrderByCreatedAtDesc();
    }

    public Optional<SizeChart> getByCategory(UUID categoryId) {
        return sizeChartRepository.findByCategoryId(categoryId);
    }

    public SizeChart getById(UUID id) {
        return sizeChartRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND));
    }

    @Transactional
    public SizeChart create(SizeChartUpsertRequestDTO request) {
        if (sizeChartRepository.existsByCategoryId(request.getCategoryId())) {
            throw new AppException(ErrorCode.SIZE_CHART_ALREADY_EXISTS);
        }

        SizeChart sizeChart = SizeChart.builder().build();
        applyUpsertRequest(sizeChart, request);
        return sizeChartRepository.save(sizeChart);
    }

    @Transactional
    public SizeChart update(UUID id, SizeChartUpsertRequestDTO request) {
        SizeChart sizeChart = getById(id);
        if (sizeChartRepository.existsByCategoryIdAndIdNot(request.getCategoryId(), id)) {
            throw new AppException(ErrorCode.SIZE_CHART_ALREADY_EXISTS);
        }

        applyUpsertRequest(sizeChart, request);
        return sizeChartRepository.save(sizeChart);
    }

    public void delete(UUID id) {
        getById(id);
        sizeChartRepository.deleteById(id);
    }

    private void applyUpsertRequest(SizeChart sizeChart, SizeChartUpsertRequestDTO request) {
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND));

        sizeChart.setCategory(category);
        sizeChart.setName(normalizeRequiredText(request.getName()));
        sizeChart.setDescription(normalizeOptionalText(request.getDescription()));

        List<SizeChartRow> normalizedRows = normalizeRows(sizeChart, request.getRows());
        sizeChart.getRows().clear();
        sizeChart.getRows().addAll(normalizedRows);
    }

    private List<SizeChartRow> normalizeRows(SizeChart sizeChart, List<SizeChartRowRequestDTO> rows) {
        if (rows == null || rows.isEmpty()) {
            throw new AppException(ErrorCode.SIZE_CHART_INVALID);
        }

        List<SizeChartRow> normalizedRows = new ArrayList<>();
        Set<String> seenFrameSizes = new HashSet<>();

        for (int index = 0; index < rows.size(); index++) {
            SizeChartRowRequestDTO rowRequest = rows.get(index);
            String normalizedFrameSize = normalizeRequiredText(rowRequest.getFrameSize());
            if (!seenFrameSizes.add(normalizedFrameSize.toLowerCase(Locale.ROOT))) {
                throw new AppException(ErrorCode.SIZE_CHART_INVALID);
            }

            Integer heightMinCm = rowRequest.getHeightMinCm();
            Integer heightMaxCm = rowRequest.getHeightMaxCm();
            if (heightMinCm == null || heightMaxCm == null || heightMinCm > heightMaxCm) {
                throw new AppException(ErrorCode.SIZE_CHART_INVALID);
            }

            normalizedRows.add(SizeChartRow.builder()
                    .sizeChart(sizeChart)
                    .frameSize(normalizedFrameSize)
                    .heightMinCm(heightMinCm)
                    .heightMaxCm(heightMaxCm)
                    .note(normalizeOptionalText(rowRequest.getNote()))
                    .displayOrder(index)
                    .build());
        }

        return normalizedRows;
    }

    private String normalizeRequiredText(String value) {
        if (value == null) {
            throw new AppException(ErrorCode.SIZE_CHART_INVALID);
        }
        String normalizedValue = value.trim();
        if (normalizedValue.isEmpty()) {
            throw new AppException(ErrorCode.SIZE_CHART_INVALID);
        }
        return normalizedValue;
    }

    private String normalizeOptionalText(String value) {
        if (value == null) {
            return null;
        }
        String normalizedValue = value.trim();
        return normalizedValue.isEmpty() ? null : normalizedValue;
    }
}
