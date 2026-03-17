package com.backend.old_bicycle_project.repository;

import com.backend.old_bicycle_project.entity.Product;
import com.backend.old_bicycle_project.entity.enums.ProductStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductRepository extends JpaRepository<Product, UUID>, JpaSpecificationExecutor<Product> {

    Page<Product> findByStatusAndDeletedAtIsNull(ProductStatus status, Pageable pageable);

    Page<Product> findBySellerIdAndDeletedAtIsNull(UUID sellerId, Pageable pageable);

    Page<Product> findAllByDeletedAtIsNull(Pageable pageable);

    Optional<Product> findByIdAndDeletedAtIsNull(UUID id);

    boolean existsByBrandIdAndDeletedAtIsNull(UUID brandId);

    boolean existsByCategoryIdAndDeletedAtIsNull(UUID categoryId);

    boolean existsByBrakeTypeIdAndDeletedAtIsNull(UUID brakeTypeId);

    boolean existsByFrameMaterialIdAndDeletedAtIsNull(UUID frameMaterialId);

    @Query("SELECT COUNT(p) FROM Product p WHERE p.seller.id = :sellerId")
    long countBySellerId(@Param("sellerId") UUID sellerId);

    long countByStatus(ProductStatus status);
}
