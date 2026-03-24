package com.backend.old_bicycle_project.controller;

import com.backend.old_bicycle_project.dto.request.AdminUserPasswordResetRequest;
import com.backend.old_bicycle_project.dto.request.AdminUserStatusUpdateRequest;
import com.backend.old_bicycle_project.dto.response.AdminUserActivityResponseDTO;
import com.backend.old_bicycle_project.dto.response.AdminUserResponseDTO;
import com.backend.old_bicycle_project.dto.response.ApiResponse;
import com.backend.old_bicycle_project.entity.User;
import com.backend.old_bicycle_project.entity.enums.AppRole;
import com.backend.old_bicycle_project.entity.enums.UserStatus;
import com.backend.old_bicycle_project.service.AdminUserService;
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
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService adminUserService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Page<AdminUserResponseDTO>> getAdminUsers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) AppRole role,
            @RequestParam(required = false) UserStatus status,
            @RequestParam(required = false) Boolean verified,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size
    ) {
        return ApiResponse.<Page<AdminUserResponseDTO>>builder()
                .result(adminUserService.getAllUsers(keyword, role, status, verified, page, size))
                .build();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<AdminUserResponseDTO> getAdminUserById(@PathVariable UUID id) {
        return ApiResponse.<AdminUserResponseDTO>builder()
                .result(adminUserService.getUserById(id))
                .build();
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<AdminUserResponseDTO> updateAdminUserStatus(
            @PathVariable UUID id,
            @Valid @RequestBody AdminUserStatusUpdateRequest request,
            @AuthenticationPrincipal User currentUser
    ) {
        return ApiResponse.<AdminUserResponseDTO>builder()
                .result(adminUserService.updateUserStatus(id, request.getStatus(), currentUser.getId()))
                .build();
    }

    @PatchMapping("/{id}/password")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<String> resetAdminUserPassword(
            @PathVariable UUID id,
            @Valid @RequestBody AdminUserPasswordResetRequest request
    ) {
        return ApiResponse.<String>builder()
                .result(adminUserService.resetUserPassword(id, request.getNewPassword()))
                .build();
    }

    @GetMapping("/{id}/activity")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<AdminUserActivityResponseDTO> getAdminUserActivity(@PathVariable UUID id) {
        return ApiResponse.<AdminUserActivityResponseDTO>builder()
                .result(adminUserService.getUserActivity(id))
                .build();
    }
}
