package com.backend.old_bicycle_project.controller;

import com.backend.old_bicycle_project.dto.auth.*;
import com.backend.old_bicycle_project.entity.User;
import com.backend.old_bicycle_project.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import com.backend.old_bicycle_project.dto.response.ApiResponse;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * POST /api/auth/register
     * Đăng ký tài khoản mới (buyer hoặc seller)
     */
    @PostMapping("/register")
    public ApiResponse<String> register(@Valid @RequestBody RegisterRequest request) {
        String message = authService.register(request);
        return ApiResponse.<String>builder()
                .result(message)
                .build();
    }

    /**
     * POST /api/auth/login
     * Đăng nhập → trả về access token & refresh token
     */
    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.<AuthResponse>builder()
                .result(authService.login(request))
                .build();
    }

    /**
     * POST /api/auth/refresh
     * Lấy access token mới từ refresh token
     */
    @PostMapping("/refresh")
    public ApiResponse<AuthResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        return ApiResponse.<AuthResponse>builder()
                .result(authService.refreshToken(request))
                .build();
    }

    /**
     * POST /api/auth/logout
     * Đăng xuất - xóa refresh token trong DB
     * Yêu cầu: Authorization: Bearer <access_token>
     */
    @PostMapping("/logout")
    public ApiResponse<String> logout(@AuthenticationPrincipal User currentUser) {
        authService.logout(currentUser);
        return ApiResponse.<String>builder()
                .result("Đăng xuất thành công")
                .build();
    }

    /**
     * GET /api/auth/verify-email?token=xxx
     * Xác thực email từ link gửi trong mail
     */
    @GetMapping("/verify-email")
    public ApiResponse<String> verifyEmail(@RequestParam String token) {
        String message = authService.verifyEmail(token);
        return ApiResponse.<String>builder()
                .result(message)
                .build();
    }

    /**
     * GET /api/auth/me
     * Lấy thông tin user hiện tại
     * Yêu cầu: Authorization: Bearer <access_token>
     */
    @GetMapping("/me")
    public ApiResponse<AuthResponse.UserInfo> getCurrentUser(@AuthenticationPrincipal User currentUser) {
        return ApiResponse.<AuthResponse.UserInfo>builder()
                .result(authService.getCurrentUser(currentUser))
                .build();
    }
}
