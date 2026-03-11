package com.backend.old_bicycle_project.repository;

import com.backend.old_bicycle_project.entity.Report;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ReportRepository extends JpaRepository<Report, UUID> {
    
    // For admin to view all reports, optionally filtered by status (if needed later)
    Page<Report> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
