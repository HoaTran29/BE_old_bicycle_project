package com.backend.old_bicycle_project.repository;

import com.backend.old_bicycle_project.entity.Inspection;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InspectionRepository extends JpaRepository<Inspection, UUID>, JpaSpecificationExecutor<Inspection> {

    Optional<Inspection> findByProductId(UUID productId);

    boolean existsByProductId(UUID productId);

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
