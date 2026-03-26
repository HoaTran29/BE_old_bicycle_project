package com.backend.old_bicycle_project.controller;

import com.backend.old_bicycle_project.dto.request.RefundCreateRequestDTO;
import com.backend.old_bicycle_project.dto.request.RefundReviewRequestDTO;
import com.backend.old_bicycle_project.dto.response.AdminRefundResponseDTO;
import com.backend.old_bicycle_project.dto.response.ApiResponse;
import com.backend.old_bicycle_project.dto.response.RefundResponseDTO;
import com.backend.old_bicycle_project.entity.User;
import com.backend.old_bicycle_project.entity.enums.RefundStatus;
import com.backend.old_bicycle_project.service.RefundService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class RefundController {

    private final RefundService refundService;

    @PostMapping(value = "/api/orders/{orderId}/refunds", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('BUYER')")
    public ApiResponse<RefundResponseDTO> requestRefund(
            @PathVariable UUID orderId,
            @AuthenticationPrincipal User currentUser,
            @ModelAttribute @Valid RefundCreateRequestDTO requestDTO,
            @RequestPart(value = "files", required = false) List<MultipartFile> files) {
        return ApiResponse.<RefundResponseDTO>builder()
                .message("Refund request created successfully")
                .result(refundService.requestRefund(orderId, currentUser, requestDTO, files))
                .build();
    }

    @GetMapping("/api/admin/refunds")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Page<AdminRefundResponseDTO>> getAdminRefunds(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) RefundStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size
    ) {
        return ApiResponse.<Page<AdminRefundResponseDTO>>builder()
                .message("Refund requests fetched successfully")
                .result(refundService.getAdminRefunds(keyword, status, page, size))
                .build();
    }

    @PatchMapping("/api/admin/refunds/{refundId}/review")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<RefundResponseDTO> reviewRefund(
            @PathVariable UUID refundId,
            @AuthenticationPrincipal User currentUser,
            @RequestBody @Valid RefundReviewRequestDTO requestDTO) {
        return ApiResponse.<RefundResponseDTO>builder()
                .message("Refund request reviewed successfully")
                .result(refundService.reviewRefund(refundId, currentUser, requestDTO))
                .build();
    }
}
