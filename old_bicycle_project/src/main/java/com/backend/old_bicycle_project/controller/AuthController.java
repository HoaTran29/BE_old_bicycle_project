package com.backend.old_bicycle_project.controller;

import com.backend.old_bicycle_project.dto.auth.AuthResponse;
import com.backend.old_bicycle_project.dto.auth.ChangePasswordRequest;
import com.backend.old_bicycle_project.dto.auth.ForgotPasswordRequest;
import com.backend.old_bicycle_project.dto.auth.LoginRequest;
import com.backend.old_bicycle_project.dto.auth.ProfileUpdateRequest;
import com.backend.old_bicycle_project.dto.auth.RefreshTokenRequest;
import com.backend.old_bicycle_project.dto.auth.RegisterRequest;
import com.backend.old_bicycle_project.dto.auth.ResetPasswordRequest;
import com.backend.old_bicycle_project.dto.response.ApiResponse;
import com.backend.old_bicycle_project.entity.User;
import com.backend.old_bicycle_project.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ApiResponse<String> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.<String>builder()
                .result(authService.register(request))
                .build();
    }

    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.<AuthResponse>builder()
                .result(authService.login(request))
                .build();
    }

    @PostMapping("/refresh")
    public ApiResponse<AuthResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        return ApiResponse.<AuthResponse>builder()
                .result(authService.refreshToken(request))
                .build();
    }

    @PostMapping("/forgot-password")
    public ApiResponse<String> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        return ApiResponse.<String>builder()
                .result(authService.requestPasswordReset(request))
                .build();
    }

    @PostMapping("/reset-password")
    public ApiResponse<String> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        return ApiResponse.<String>builder()
                .result(authService.resetPassword(request))
                .build();
    }

    @PostMapping("/logout")
    public ApiResponse<String> logout(@AuthenticationPrincipal User currentUser) {
        authService.logout(currentUser);
        return ApiResponse.<String>builder()
                .result("Dang xuat thanh cong")
                .build();
    }

    @GetMapping("/verify-email")
    public ApiResponse<String> verifyEmail(@RequestParam String token) {
        return ApiResponse.<String>builder()
                .result(authService.verifyEmail(token))
                .build();
    }

    @GetMapping("/me")
    public ApiResponse<AuthResponse.UserInfo> getCurrentUser(@AuthenticationPrincipal User currentUser) {
        return ApiResponse.<AuthResponse.UserInfo>builder()
                .result(authService.getCurrentUser(currentUser))
                .build();
    }

    @PatchMapping("/profile")
    public ApiResponse<AuthResponse.UserInfo> updateProfile(
            @AuthenticationPrincipal User currentUser,
            @RequestBody ProfileUpdateRequest request
    ) {
        return ApiResponse.<AuthResponse.UserInfo>builder()
                .result(authService.updateProfile(currentUser, request))
                .build();
    }

    @PatchMapping("/change-password")
    public ApiResponse<String> changePassword(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        return ApiResponse.<String>builder()
                .result(authService.changePassword(currentUser, request))
                .build();
    }
}
