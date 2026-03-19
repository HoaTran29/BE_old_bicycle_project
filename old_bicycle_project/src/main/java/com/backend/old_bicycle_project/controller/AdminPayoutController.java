package com.backend.old_bicycle_project.controller;

import com.backend.old_bicycle_project.dto.request.PayoutCompleteRequestDTO;
import com.backend.old_bicycle_project.dto.response.AdminPayoutResponseDTO;
import com.backend.old_bicycle_project.dto.response.ApiResponse;
import com.backend.old_bicycle_project.entity.User;
import com.backend.old_bicycle_project.entity.enums.PayoutStatus;
import com.backend.old_bicycle_project.entity.enums.PayoutType;
import com.backend.old_bicycle_project.service.PayoutService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/payouts")
@RequiredArgsConstructor
public class AdminPayoutController {

    private final PayoutService payoutService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Page<AdminPayoutResponseDTO>> getAdminPayouts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) PayoutType type,
            @RequestParam(required = false) PayoutStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size
    ) {
        return ApiResponse.<Page<AdminPayoutResponseDTO>>builder()
                .message("Payouts fetched successfully")
                .result(payoutService.getAdminPayouts(keyword, type, status, page, size))
                .build();
    }

    @PatchMapping("/{payoutId}/complete")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<AdminPayoutResponseDTO> completePayout(
            @PathVariable UUID payoutId,
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody PayoutCompleteRequestDTO request
    ) {
        return ApiResponse.<AdminPayoutResponseDTO>builder()
                .message("Payout completed successfully")
                .result(payoutService.completePayout(payoutId, currentUser, request))
                .build();
    }
}
