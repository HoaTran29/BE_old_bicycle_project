package com.backend.old_bicycle_project.controller;

import com.backend.old_bicycle_project.dto.request.ReportProcessDTO;
import com.backend.old_bicycle_project.dto.request.ReportRequestDTO;
import com.backend.old_bicycle_project.dto.response.ApiResponse;
import com.backend.old_bicycle_project.dto.response.ReportResponseDTO;
import com.backend.old_bicycle_project.entity.User;
import com.backend.old_bicycle_project.entity.enums.ReportStatus;
import com.backend.old_bicycle_project.service.ReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    // User Endpoint
    @PostMapping(value = "/api/reports", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ReportResponseDTO>> submitReport(
            @AuthenticationPrincipal User currentUser,
            @ModelAttribute @Valid ReportRequestDTO requestDTO,
            @RequestPart(value = "files", required = false) List<MultipartFile> files) {
        ReportResponseDTO responseDTO = reportService.submitReport(currentUser.getId(), requestDTO, files);
        return ResponseEntity.ok(ApiResponse.<ReportResponseDTO>builder()
                .code(200)
                .message("Report submitted successfully")
                .result(responseDTO)
                .build());
    }

    @GetMapping("/api/reports/me")
    public ResponseEntity<ApiResponse<Page<ReportResponseDTO>>> getMyReports(
            @AuthenticationPrincipal User currentUser,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<ReportResponseDTO> reports = reportService.getMyReports(currentUser.getId(), pageable);

        return ResponseEntity.ok(ApiResponse.<Page<ReportResponseDTO>>builder()
                .code(200)
                .message("Fetched my reports successfully")
                .result(reports)
                .build());
    }

    // Admin Endpoints
    @GetMapping("/api/admin/reports")
    public ResponseEntity<ApiResponse<Page<ReportResponseDTO>>> getAllReports(
            @RequestParam(required = false) ReportStatus status,
            @RequestParam(required = false) String targetType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<ReportResponseDTO> reports = reportService.getAllReports(status, targetType, pageable);
        
        return ResponseEntity.ok(ApiResponse.<Page<ReportResponseDTO>>builder()
                .code(200)
                .message("Fetched reports successfully")
                .result(reports)
                .build());
    }

    @PutMapping("/api/admin/reports/{reportId}/process")
    public ResponseEntity<ApiResponse<ReportResponseDTO>> processReport(
            @PathVariable UUID reportId,
            @AuthenticationPrincipal User currentUser,
            @RequestBody @Valid ReportProcessDTO processDTO) {
        
        ReportResponseDTO responseDTO = reportService.processReport(reportId, processDTO, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.<ReportResponseDTO>builder()
                .code(200)
                .message("Report processed successfully")
                .result(responseDTO)
                .build());
    }
}
