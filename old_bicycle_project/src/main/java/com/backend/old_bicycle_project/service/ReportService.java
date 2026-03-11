package com.backend.old_bicycle_project.service;

import com.backend.old_bicycle_project.dto.request.ReportProcessDTO;
import com.backend.old_bicycle_project.dto.request.ReportRequestDTO;
import com.backend.old_bicycle_project.dto.response.ReportResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ReportService {

    /**
     * User submits a report
     */
    ReportResponseDTO submitReport(ReportRequestDTO requestDTO);

    /**
     * Admin gets paginated list of reports
     */
    Page<ReportResponseDTO> getAllReports(Pageable pageable);

    /**
     * Admin processes a report (e.g., changes status to RESOLVED and potentially bans user/hides product)
     */
    ReportResponseDTO processReport(UUID reportId, ReportProcessDTO processDTO);
}
