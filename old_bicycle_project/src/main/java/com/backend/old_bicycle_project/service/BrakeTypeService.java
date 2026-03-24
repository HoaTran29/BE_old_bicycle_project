package com.backend.old_bicycle_project.service;

import com.backend.old_bicycle_project.entity.BrakeType;
import com.backend.old_bicycle_project.exception.AppException;
import com.backend.old_bicycle_project.exception.ErrorCode;
import com.backend.old_bicycle_project.repository.BrakeTypeRepository;
import com.backend.old_bicycle_project.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BrakeTypeService {

    private final BrakeTypeRepository brakeTypeRepository;
    private final ProductRepository productRepository;

    public List<BrakeType> getAll() {
        return brakeTypeRepository.findAll(Sort.by(Sort.Direction.ASC, "name"));
    }

    public BrakeType getById(UUID id) {
        return brakeTypeRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND));
    }

    public BrakeType create(String name, String description) {
        String normalizedName = name.trim();
        if (brakeTypeRepository.existsByNameIgnoreCase(normalizedName)) {
            throw new AppException(ErrorCode.RECORD_ALREADY_EXISTS);
        }
        return brakeTypeRepository.save(BrakeType.builder()
                .name(normalizedName)
                .description(description)
                .build());
    }

    public BrakeType update(UUID id, String name, String description) {
        BrakeType brakeType = getById(id);
        String normalizedName = name.trim();
        if (brakeTypeRepository.existsByNameIgnoreCaseAndIdNot(normalizedName, id)) {
            throw new AppException(ErrorCode.RECORD_ALREADY_EXISTS);
        }
        brakeType.setName(normalizedName);
        brakeType.setDescription(description);
        return brakeTypeRepository.save(brakeType);
    }

    public void delete(UUID id) {
        getById(id);
        if (productRepository.existsByBrakeTypeIdAndDeletedAtIsNull(id)) {
            throw new AppException(ErrorCode.REFERENCE_DATA_IN_USE);
        }
        brakeTypeRepository.deleteById(id);
    }
}
