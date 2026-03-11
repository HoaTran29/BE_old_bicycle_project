package com.backend.old_bicycle_project.controller;

import com.backend.old_bicycle_project.dto.request.InspectionEvaluationDTO;
import com.backend.old_bicycle_project.dto.response.ApiResponse;
import com.backend.old_bicycle_project.dto.response.InspectionResponseDTO;
import com.backend.old_bicycle_project.service.InspectionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/inspections")
@RequiredArgsConstructor
public class InspectionController {

    private final InspectionService inspectionService;

    // TODO: Require Seller role and get sellerId from authenticated user (SecurityContextHolder)
    @PostMapping("/request/{productId}")
    public ResponseEntity<ApiResponse<InspectionResponseDTO>> requestInspection(
            @PathVariable UUID productId,
            @RequestParam UUID sellerId) {
        
        // Temporarily passing sellerId as RequestParam until Spring Security is fully wired
        InspectionResponseDTO responseDTO = inspectionService.requestInspection(productId, sellerId);
        return ResponseEntity.ok(ApiResponse.<InspectionResponseDTO>builder()
                .code(200)
                .message("Inspection requested successfully")
                .result(responseDTO)
                .build());
    }

    // TODO: Require Inspector or Admin role
    @PostMapping("/evaluate/{productId}")
    public ResponseEntity<ApiResponse<InspectionResponseDTO>> evaluateInspection(
            @PathVariable UUID productId,
            @RequestParam UUID inspectorId,
            @RequestBody @Valid InspectionEvaluationDTO evaluationDTO) {
        
        // Temporarily passing inspectorId as RequestParam until Spring Security is fully wired
        InspectionResponseDTO responseDTO = inspectionService.evaluateInspection(productId, inspectorId, evaluationDTO);
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
}
