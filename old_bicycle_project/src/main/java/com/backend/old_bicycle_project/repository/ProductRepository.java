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

import java.util.UUID;

@Repository
public interface ProductRepository extends JpaRepository<Product, UUID>, JpaSpecificationExecutor<Product> {

    Page<Product> findByStatus(ProductStatus status, Pageable pageable);

    Page<Product> findBySellerId(UUID sellerId, Pageable pageable);

    @Query("SELECT COUNT(p) FROM Product p WHERE p.seller.id = :sellerId")
    long countBySellerId(@Param("sellerId") UUID sellerId);

    long countByStatus(ProductStatus status);
}
