package com.backend.old_bicycle_project.service;

import com.backend.old_bicycle_project.dto.auth.*;
import com.backend.old_bicycle_project.entity.EmailVerification;
import com.backend.old_bicycle_project.entity.RefreshToken;
import com.backend.old_bicycle_project.entity.User;
import com.backend.old_bicycle_project.entity.enums.AppRole;
import com.backend.old_bicycle_project.entity.enums.UserStatus;
import com.backend.old_bicycle_project.repository.UserRepository;
import com.backend.old_bicycle_project.security.JwtTokenProvider;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;
    private final RefreshTokenService refreshTokenService;
    private final EmailService emailService;

    @Value("${jwt.access-token-expiration}")
    private long accessTokenExpiration;

    // ==================== REGISTER ====================

    @Transactional
    public String register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email đã được sử dụng: " + request.getEmail());
        }

        // Chỉ cho phép đăng ký với role buyer hoặc seller
        AppRole role = request.getRole();
        if (role == null || (role != AppRole.buyer && role != AppRole.seller)) {
            role = AppRole.buyer;
        }

        User user = User.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .phone(request.getPhone())
                .role(role)
                .isVerified(false)
                .status(UserStatus.active)
                .build();

        userRepository.save(user);

        // Tạo token xác thực và gửi email
        EmailVerification verificationToken = emailService.createVerificationToken(user);
        emailService.sendVerificationEmail(user, verificationToken.getToken());

        return "Đăng ký thành công! Vui lòng kiểm tra email để xác thực tài khoản.";
    }

    // ==================== LOGIN ====================

    public AuthResponse login(LoginRequest request) {
        // Spring Security sẽ tự kiểm tra password, status (isEnabled, isAccountNonLocked)
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        User user = (User) authentication.getPrincipal();

        String accessToken = jwtTokenProvider.generateAccessToken(user);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

        return buildAuthResponse(user, accessToken, refreshToken.getToken());
    }

    // ==================== REFRESH TOKEN ====================

    public AuthResponse refreshToken(RefreshTokenRequest request) {
        RefreshToken refreshToken = refreshTokenService.findByToken(request.getRefreshToken())
                .orElseThrow(() -> new RuntimeException("Refresh token không hợp lệ hoặc không tồn tại"));

        if (refreshToken.isExpired()) {
            refreshTokenService.deleteAllByUser(refreshToken.getUser());
            throw new RuntimeException("Refresh token đã hết hạn. Vui lòng đăng nhập lại.");
        }

        User user = refreshToken.getUser();
        String newAccessToken = jwtTokenProvider.generateAccessToken(user);

        return buildAuthResponse(user, newAccessToken, refreshToken.getToken());
    }

    // ==================== LOGOUT ====================

    @Transactional
    public void logout(User currentUser) {
        refreshTokenService.deleteAllByUser(currentUser);
    }

    // ==================== VERIFY EMAIL ====================

    @Transactional
    public String verifyEmail(String token) {
        EmailVerification verification = emailService.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Token xác thực không hợp lệ"));

        if (verification.isExpired()) {
            emailService.deleteByUser(verification.getUser());
            throw new RuntimeException("Token xác thực đã hết hạn. Vui lòng đăng ký lại để nhận email mới.");
        }

        User user = verification.getUser();
        if (user.isVerified()) {
            return "Tài khoản đã được xác thực trước đó.";
        }

        user.setVerified(true);
        userRepository.save(user);
        emailService.deleteByUser(user);

        return "Xác thực email thành công! Bạn có thể đăng nhập ngay bây giờ.";
    }

    // ==================== GET CURRENT USER ====================

    public AuthResponse.UserInfo getCurrentUser(User user) {
        return AuthResponse.UserInfo.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole())
                .status(user.getStatus())
                .isVerified(user.isVerified())
                .build();
    }

    // ==================== PRIVATE HELPERS ====================

    private AuthResponse buildAuthResponse(User user, String accessToken, String refreshToken) {
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(accessTokenExpiration / 1000)
                .user(AuthResponse.UserInfo.builder()
                        .id(user.getId())
                        .email(user.getEmail())
                        .firstName(user.getFirstName())
                        .lastName(user.getLastName())
                        .role(user.getRole())
                        .status(user.getStatus())
                        .isVerified(user.isVerified())
                        .build())
                .build();
    }
}
