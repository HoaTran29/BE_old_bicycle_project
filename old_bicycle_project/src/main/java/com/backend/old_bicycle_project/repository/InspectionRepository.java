package com.backend.old_bicycle_project.repository;

import com.backend.old_bicycle_project.entity.Inspection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InspectionRepository extends JpaRepository<Inspection, UUID>, JpaSpecificationExecutor<Inspection> {

    Optional<Inspection> findByProductId(UUID productId);

    List<Inspection> findByProductIdIn(Collection<UUID> productIds);

    @EntityGraph(attributePaths = {"product", "product.seller", "inspector"})
    Page<Inspection> findAll(Specification<Inspection> spec, Pageable pageable);

    boolean existsByProductId(UUID productId);

    @Query("select distinct i.product.id from Inspection i where i.product.id in :productIds")
    List<UUID> findDistinctProductIdsWithInspection(@Param("productIds") Collection<UUID> productIds);

    long countByInspectorId(UUID inspectorId);

    long countByInspectorIsNotNull();

    long countByInspectorIdAndPassedTrue(UUID inspectorId);

    long countByInspectorIsNotNullAndPassedTrue();

    long countByInspectorIdAndUpdatedAtAfter(UUID inspectorId, LocalDateTime updatedAt);

    long countByInspectorIsNotNullAndUpdatedAtAfter(LocalDateTime updatedAt);

    @Query("select avg(i.overallScore) from Inspection i where i.inspector.id = :inspectorId")
    BigDecimal findAverageOverallScoreByInspectorId(@Param("inspectorId") UUID inspectorId);

    @Query("select avg(i.overallScore) from Inspection i where i.inspector is not null")
    BigDecimal findAverageOverallScoreForAll();
}
