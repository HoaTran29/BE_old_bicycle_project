package com.backend.old_bicycle_project.service;

import com.backend.old_bicycle_project.entity.Brand;
import com.backend.old_bicycle_project.exception.AppException;
import com.backend.old_bicycle_project.exception.ErrorCode;
import com.backend.old_bicycle_project.repository.BrandRepository;
import com.backend.old_bicycle_project.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BrandService {

    private final BrandRepository brandRepository;
    private final ProductRepository productRepository;

    public List<Brand> getAll() {
        return brandRepository.findAll(Sort.by(Sort.Direction.ASC, "name"));
    }

    public Brand getById(UUID id) {
        return brandRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND));
    }

    public Brand create(String name, String logoUrl) {
        String normalizedName = name.trim();
        if (brandRepository.existsByNameIgnoreCase(normalizedName)) {
            throw new AppException(ErrorCode.RECORD_ALREADY_EXISTS);
        }
        return brandRepository.save(Brand.builder()
                .name(normalizedName)
                .logoUrl(logoUrl)
                .build());
    }

    public Brand update(UUID id, String name, String logoUrl) {
        Brand brand = getById(id);
        String normalizedName = name.trim();
        if (brandRepository.existsByNameIgnoreCaseAndIdNot(normalizedName, id)) {
            throw new AppException(ErrorCode.RECORD_ALREADY_EXISTS);
        }
        brand.setName(normalizedName);
        brand.setLogoUrl(logoUrl);
        return brandRepository.save(brand);
    }

    public void delete(UUID id) {
        getById(id);
        if (productRepository.existsByBrandIdAndDeletedAtIsNull(id)) {
            throw new AppException(ErrorCode.REFERENCE_DATA_IN_USE);
        }
        brandRepository.deleteById(id);
    }
}
