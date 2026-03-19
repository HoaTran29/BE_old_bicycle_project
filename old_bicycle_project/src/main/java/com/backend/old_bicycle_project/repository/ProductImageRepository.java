package com.backend.old_bicycle_project.repository;

import com.backend.old_bicycle_project.entity.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface ProductImageRepository extends JpaRepository<ProductImage, UUID> {
    List<ProductImage> findByProductIdOrderByDisplayOrderAsc(UUID productId);
    List<ProductImage> findByProductIdInOrderByProductIdAscDisplayOrderAsc(Collection<UUID> productIds);
    void deleteAllByProductId(UUID productId);
}
