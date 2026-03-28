package com.backend.old_bicycle_project.repository;

import com.backend.old_bicycle_project.entity.Product;
import com.backend.old_bicycle_project.entity.enums.ProductStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductRepository extends JpaRepository<Product, UUID>, JpaSpecificationExecutor<Product> {

    @EntityGraph(attributePaths = {"seller", "brand", "category", "brakeType", "frameMaterial", "groupsetReference"})
    Page<Product> findAll(Specification<Product> spec, Pageable pageable);

    Page<Product> findByStatusAndDeletedAtIsNull(ProductStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"seller", "brand", "category", "brakeType", "frameMaterial", "groupsetReference"})
    Page<Product> findBySellerIdAndDeletedAtIsNull(UUID sellerId, Pageable pageable);

    Page<Product> findAllByDeletedAtIsNull(Pageable pageable);

    @EntityGraph(attributePaths = {"seller", "brand", "category", "brakeType", "frameMaterial", "groupsetReference"})
    Optional<Product> findByIdAndDeletedAtIsNull(UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Product p where p.id = :id")
    Optional<Product> findByIdForUpdate(@Param("id") UUID id);

    boolean existsByBrandIdAndDeletedAtIsNull(UUID brandId);

    boolean existsByCategoryIdAndDeletedAtIsNull(UUID categoryId);

    boolean existsByBrakeTypeIdAndDeletedAtIsNull(UUID brakeTypeId);

    boolean existsByFrameMaterialIdAndDeletedAtIsNull(UUID frameMaterialId);

    boolean existsByGroupsetReferenceIdAndDeletedAtIsNull(UUID groupsetId);

    @Query("SELECT COUNT(p) FROM Product p WHERE p.seller.id = :sellerId")
    long countBySellerId(@Param("sellerId") UUID sellerId);

    long countByStatus(ProductStatus status);
}
