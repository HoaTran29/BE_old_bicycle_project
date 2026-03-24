package com.backend.old_bicycle_project.repository;

import com.backend.old_bicycle_project.entity.SizeChart;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SizeChartRepository extends JpaRepository<SizeChart, UUID> {

    @EntityGraph(attributePaths = {"category", "rows"})
    List<SizeChart> findAllByOrderByCreatedAtDesc();

    @Override
    @EntityGraph(attributePaths = {"category", "rows"})
    Optional<SizeChart> findById(UUID id);

    @EntityGraph(attributePaths = {"category", "rows"})
    Optional<SizeChart> findByCategoryId(UUID categoryId);

    boolean existsByCategoryId(UUID categoryId);

    boolean existsByCategoryIdAndIdNot(UUID categoryId, UUID id);
}
