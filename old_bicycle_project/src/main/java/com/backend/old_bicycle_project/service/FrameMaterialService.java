package com.backend.old_bicycle_project.service;

import com.backend.old_bicycle_project.entity.FrameMaterial;
import com.backend.old_bicycle_project.exception.AppException;
import com.backend.old_bicycle_project.exception.ErrorCode;
import com.backend.old_bicycle_project.repository.FrameMaterialRepository;
import com.backend.old_bicycle_project.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FrameMaterialService {

    private final FrameMaterialRepository frameMaterialRepository;
    private final ProductRepository productRepository;

    public List<FrameMaterial> getAll() {
        return frameMaterialRepository.findAll(Sort.by(Sort.Direction.ASC, "name"));
    }

    public FrameMaterial getById(UUID id) {
        return frameMaterialRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND));
    }

    public FrameMaterial create(String name, String description) {
        String normalizedName = name.trim();
        if (frameMaterialRepository.existsByNameIgnoreCase(normalizedName)) {
            throw new AppException(ErrorCode.RECORD_ALREADY_EXISTS);
        }
        return frameMaterialRepository.save(FrameMaterial.builder()
                .name(normalizedName)
                .description(description)
                .build());
    }

    public FrameMaterial update(UUID id, String name, String description) {
        FrameMaterial frameMaterial = getById(id);
        String normalizedName = name.trim();
        if (frameMaterialRepository.existsByNameIgnoreCaseAndIdNot(normalizedName, id)) {
            throw new AppException(ErrorCode.RECORD_ALREADY_EXISTS);
        }
        frameMaterial.setName(normalizedName);
        frameMaterial.setDescription(description);
        return frameMaterialRepository.save(frameMaterial);
    }

    public void delete(UUID id) {
        getById(id);
        if (productRepository.existsByFrameMaterialIdAndDeletedAtIsNull(id)) {
            throw new AppException(ErrorCode.REFERENCE_DATA_IN_USE);
        }
        frameMaterialRepository.deleteById(id);
    }
}
