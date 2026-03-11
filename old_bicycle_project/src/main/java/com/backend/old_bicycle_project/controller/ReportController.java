package com.backend.old_bicycle_project.controller;

import com.backend.old_bicycle_project.dto.request.ReportProcessDTO;
import com.backend.old_bicycle_project.dto.request.ReportRequestDTO;
import com.backend.old_bicycle_project.dto.response.ApiResponse;
import com.backend.old_bicycle_project.dto.response.ReportResponseDTO;
import com.backend.old_bicycle_project.service.ReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    // User Endpoint
    @PostMapping("/api/reports")
    public ResponseEntity<ApiResponse<ReportResponseDTO>> submitReport(
            @RequestBody @Valid ReportRequestDTO requestDTO) {
        
        ReportResponseDTO responseDTO = reportService.submitReport(requestDTO);
        return ResponseEntity.ok(ApiResponse.<ReportResponseDTO>builder()
                .code(200)
                .message("Report submitted successfully")
                .result(responseDTO)
                .build());
    }

    // Admin Endpoints
    @GetMapping("/api/admin/reports")
    public ResponseEntity<ApiResponse<Page<ReportResponseDTO>>> getAllReports(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<ReportResponseDTO> reports = reportService.getAllReports(pageable);
        
        return ResponseEntity.ok(ApiResponse.<Page<ReportResponseDTO>>builder()
                .code(200)
                .message("Fetched reports successfully")
                .result(reports)
                .build());
    }

    @PutMapping("/api/admin/reports/{reportId}/process")
    public ResponseEntity<ApiResponse<ReportResponseDTO>> processReport(
            @PathVariable UUID reportId,
            @RequestBody @Valid ReportProcessDTO processDTO) {
        
        ReportResponseDTO responseDTO = reportService.processReport(reportId, processDTO);
        return ResponseEntity.ok(ApiResponse.<ReportResponseDTO>builder()
                .code(200)
                .message("Report processed successfully")
                .result(responseDTO)
                .build());
    }
}
