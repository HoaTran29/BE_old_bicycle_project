package com.backend.old_bicycle_project.controller;

import com.backend.old_bicycle_project.dto.request.PayoutProfileUpsertRequestDTO;
import com.backend.old_bicycle_project.dto.response.ApiResponse;
import com.backend.old_bicycle_project.dto.response.PayoutProfileResponseDTO;
import com.backend.old_bicycle_project.entity.User;
import com.backend.old_bicycle_project.service.PayoutService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payout-profiles")
@RequiredArgsConstructor
public class PayoutProfileController {

    private final PayoutService payoutService;

    @GetMapping("/me")
    public ApiResponse<PayoutProfileResponseDTO> getMyPayoutProfile(@AuthenticationPrincipal User currentUser) {
        return ApiResponse.<PayoutProfileResponseDTO>builder()
                .message("Fetched payout profile successfully")
                .result(payoutService.getMyProfile(currentUser))
                .build();
    }

    @PutMapping("/me")
    public ApiResponse<PayoutProfileResponseDTO> upsertMyPayoutProfile(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody PayoutProfileUpsertRequestDTO request
    ) {
        return ApiResponse.<PayoutProfileResponseDTO>builder()
                .message("Payout profile updated successfully")
                .result(payoutService.upsertMyProfile(currentUser, request))
                .build();
    }
}
