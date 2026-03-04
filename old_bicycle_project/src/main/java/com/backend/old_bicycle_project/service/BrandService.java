package com.backend.old_bicycle_project.service;

import com.backend.old_bicycle_project.entity.Brand;
import com.backend.old_bicycle_project.repository.BrandRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BrandService {

    private final BrandRepository brandRepository;

    public List<Brand> getAll() {
        return brandRepository.findAll();
    }

    public Brand getById(UUID id) {
        return brandRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy hãng xe với ID: " + id));
    }

    public Brand create(String name, String logoUrl) {
        if (brandRepository.existsByNameIgnoreCase(name)) {
            throw new RuntimeException("Hãng xe '" + name + "' đã tồn tại");
        }
        return brandRepository.save(Brand.builder()
                .name(name)
                .logoUrl(logoUrl)
                .build());
    }

    public Brand update(UUID id, String name, String logoUrl) {
        Brand brand = getById(id);
        if (name != null && !name.isBlank()) brand.setName(name);
        if (logoUrl != null) brand.setLogoUrl(logoUrl);
        return brandRepository.save(brand);
    }

    public void delete(UUID id) {
        brandRepository.deleteById(id);
    }
}
