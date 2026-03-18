package com.backend.old_bicycle_project.controller;

import com.backend.old_bicycle_project.dto.request.InspectionEvaluationDTO;
import com.backend.old_bicycle_project.dto.response.ApiResponse;
import com.backend.old_bicycle_project.dto.response.InspectionDashboardResponseDTO;
import com.backend.old_bicycle_project.dto.response.InspectionHistoryItemResponseDTO;
import com.backend.old_bicycle_project.dto.response.InspectionRequestItemResponseDTO;
import com.backend.old_bicycle_project.dto.response.InspectionResponseDTO;
import com.backend.old_bicycle_project.entity.User;
import com.backend.old_bicycle_project.service.InspectionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/inspections")
@RequiredArgsConstructor
public class InspectionController {

    private final InspectionService inspectionService;

    @PostMapping("/request/{productId}")
    @PreAuthorize("hasAnyRole('SELLER', 'ADMIN')")
    public ResponseEntity<ApiResponse<InspectionResponseDTO>> requestInspection(
            @PathVariable UUID productId,
            @AuthenticationPrincipal User currentUser) {
        InspectionResponseDTO responseDTO = inspectionService.requestInspection(productId, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.<InspectionResponseDTO>builder()
                .code(200)
                .message("Inspection requested successfully")
                .result(responseDTO)
                .build());
    }

    @PostMapping("/evaluate/{productId}")
    @PreAuthorize("hasAnyRole('INSPECTOR', 'ADMIN')")
    public ResponseEntity<ApiResponse<InspectionResponseDTO>> evaluateInspection(
            @PathVariable UUID productId,
            @AuthenticationPrincipal User currentUser,
            @RequestBody @Valid InspectionEvaluationDTO evaluationDTO) {
        InspectionResponseDTO responseDTO =
                inspectionService.evaluateInspection(productId, currentUser.getId(), evaluationDTO);
        return ResponseEntity.ok(ApiResponse.<InspectionResponseDTO>builder()
                .code(200)
                .message("Inspection evaluated successfully")
                .result(responseDTO)
                .build());
    }

    @GetMapping("/product/{productId}")
    public ResponseEntity<ApiResponse<InspectionResponseDTO>> getInspectionByProductId(
            @PathVariable UUID productId) {
        
        InspectionResponseDTO responseDTO = inspectionService.getInspectionByProductId(productId);
        return ResponseEntity.ok(ApiResponse.<InspectionResponseDTO>builder()
                .code(200)
                .message("Inspection fetched successfully")
                .result(responseDTO)
                .build());
    }

    @GetMapping("/requests")
    @PreAuthorize("hasAnyRole('INSPECTOR', 'ADMIN')")
    public ResponseEntity<ApiResponse<Page<InspectionRequestItemResponseDTO>>> getInspectionRequests(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<InspectionRequestItemResponseDTO> result = inspectionService.getInspectionRequests(keyword, page, size);
        return ResponseEntity.ok(ApiResponse.<Page<InspectionRequestItemResponseDTO>>builder()
                .code(200)
                .message("Inspection requests fetched successfully")
                .result(result)
                .build());
    }

    @GetMapping("/history")
    @PreAuthorize("hasAnyRole('INSPECTOR', 'ADMIN')")
    public ResponseEntity<ApiResponse<Page<InspectionHistoryItemResponseDTO>>> getInspectionHistory(
            @AuthenticationPrincipal User currentUser,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<InspectionHistoryItemResponseDTO> result =
                inspectionService.getInspectionHistory(currentUser, keyword, page, size);
        return ResponseEntity.ok(ApiResponse.<Page<InspectionHistoryItemResponseDTO>>builder()
                .code(200)
                .message("Inspection history fetched successfully")
                .result(result)
                .build());
    }

    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyRole('INSPECTOR', 'ADMIN')")
    public ResponseEntity<ApiResponse<InspectionDashboardResponseDTO>> getInspectionDashboard(
            @AuthenticationPrincipal User currentUser) {
        InspectionDashboardResponseDTO result = inspectionService.getInspectionDashboard(currentUser);
        return ResponseEntity.ok(ApiResponse.<InspectionDashboardResponseDTO>builder()
                .code(200)
                .message("Inspection dashboard fetched successfully")
                .result(result)
                .build());
    }
}
