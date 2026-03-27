package com.backend.old_bicycle_project.service;

import com.backend.old_bicycle_project.dto.request.ReportProcessDTO;
import com.backend.old_bicycle_project.dto.request.ReportRequestDTO;
import com.backend.old_bicycle_project.dto.response.ReportResponseDTO;
import com.backend.old_bicycle_project.entity.enums.ReportStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface ReportService {

    /**
     * User submits a report
     */
    ReportResponseDTO submitReport(UUID reporterId, ReportRequestDTO requestDTO, List<MultipartFile> files);

    /**
     * Admin gets paginated list of reports
     */
    Page<ReportResponseDTO> getAllReports(ReportStatus status, String targetType, Pageable pageable);

    /**
     * Reporter gets paginated list of their own reports
     */
    Page<ReportResponseDTO> getMyReports(UUID reporterId, Pageable pageable);

    /**
     * Admin processes a report through the moderation flow and only applies sanctions on upheld violations.
     */
    ReportResponseDTO processReport(UUID reportId, ReportProcessDTO processDTO, UUID adminId);
}
