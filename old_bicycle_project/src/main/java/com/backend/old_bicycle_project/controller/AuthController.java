package com.backend.old_bicycle_project.controller;

import com.backend.old_bicycle_project.dto.auth.*;
import com.backend.old_bicycle_project.entity.User;
import com.backend.old_bicycle_project.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

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
    public ResponseEntity<Map<String, String>> register(@Valid @RequestBody RegisterRequest request) {
        String message = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("message", message));
    }

    /**
     * POST /api/auth/login
     * Đăng nhập → trả về access token & refresh token
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    /**
     * POST /api/auth/refresh
     * Lấy access token mới từ refresh token
     */
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refreshToken(request));
    }

    /**
     * POST /api/auth/logout
     * Đăng xuất - xóa refresh token trong DB
     * Yêu cầu: Authorization: Bearer <access_token>
     */
    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(@AuthenticationPrincipal User currentUser) {
        authService.logout(currentUser);
        return ResponseEntity.ok(Map.of("message", "Đăng xuất thành công"));
    }

    /**
     * GET /api/auth/verify-email?token=xxx
     * Xác thực email từ link gửi trong mail
     */
    @GetMapping("/verify-email")
    public ResponseEntity<Map<String, String>> verifyEmail(@RequestParam String token) {
        String message = authService.verifyEmail(token);
        return ResponseEntity.ok(Map.of("message", message));
    }

    /**
     * GET /api/auth/me
     * Lấy thông tin user hiện tại
     * Yêu cầu: Authorization: Bearer <access_token>
     */
    @GetMapping("/me")
    public ResponseEntity<AuthResponse.UserInfo> getCurrentUser(@AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(authService.getCurrentUser(currentUser));
    }
}
