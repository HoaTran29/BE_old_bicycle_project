package com.backend.old_bicycle_project.service;

import com.backend.old_bicycle_project.entity.Groupset;
import com.backend.old_bicycle_project.exception.AppException;
import com.backend.old_bicycle_project.exception.ErrorCode;
import com.backend.old_bicycle_project.repository.GroupsetRepository;
import com.backend.old_bicycle_project.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GroupsetService {

    private final GroupsetRepository groupsetRepository;
    private final ProductRepository productRepository;

    public List<Groupset> getAll() {
        return groupsetRepository.findAllSortedByName();
    }

    public Groupset getById(UUID id) {
        return groupsetRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND));
    }

    public Groupset create(String name, String description) {
        String normalizedName = name.trim();
        if (groupsetRepository.existsByNameIgnoreCase(normalizedName)) {
            throw new AppException(ErrorCode.RECORD_ALREADY_EXISTS);
        }
        return groupsetRepository.save(Groupset.builder()
                .name(normalizedName)
                .description(normalizeDescription(description))
                .build());
    }

    public Groupset update(UUID id, String name, String description) {
        Groupset groupset = getById(id);
        String normalizedName = name.trim();
        if (groupsetRepository.existsByNameIgnoreCaseAndIdNot(normalizedName, id)) {
            throw new AppException(ErrorCode.RECORD_ALREADY_EXISTS);
        }
        groupset.setName(normalizedName);
        groupset.setDescription(normalizeDescription(description));
        return groupsetRepository.save(groupset);
    }

    public void delete(UUID id) {
        getById(id);
        if (productRepository.existsByGroupsetReferenceIdAndDeletedAtIsNull(id)) {
            throw new AppException(ErrorCode.REFERENCE_DATA_IN_USE);
        }
        groupsetRepository.deleteById(id);
    }

    private String normalizeDescription(String description) {
        if (description == null) {
            return null;
        }
        String normalizedDescription = description.trim();
        return normalizedDescription.isEmpty() ? null : normalizedDescription;
    }
}
