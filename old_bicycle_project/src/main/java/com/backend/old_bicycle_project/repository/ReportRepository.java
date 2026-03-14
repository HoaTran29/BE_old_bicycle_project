package com.backend.old_bicycle_project.repository;

import com.backend.old_bicycle_project.entity.Report;
import com.backend.old_bicycle_project.entity.enums.ReportStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.UUID;

@Repository
public interface ReportRepository extends JpaRepository<Report, UUID>, JpaSpecificationExecutor<Report> {
    
    Page<Report> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<Report> findByReporterIdOrderByCreatedAtDesc(UUID reporterId, Pageable pageable);

    boolean existsByReporterIdAndTargetIdAndStatusIn(UUID reporterId, UUID targetId, Collection<ReportStatus> statuses);
}
