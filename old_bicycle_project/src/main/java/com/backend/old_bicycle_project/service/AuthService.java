package com.backend.old_bicycle_project.service;

import com.backend.old_bicycle_project.dto.auth.AuthResponse;
import com.backend.old_bicycle_project.dto.auth.ChangePasswordRequest;
import com.backend.old_bicycle_project.dto.auth.ForgotPasswordRequest;
import com.backend.old_bicycle_project.dto.auth.LoginRequest;
import com.backend.old_bicycle_project.dto.auth.ProfileUpdateRequest;
import com.backend.old_bicycle_project.dto.auth.RefreshTokenRequest;
import com.backend.old_bicycle_project.dto.auth.RegisterRequest;
import com.backend.old_bicycle_project.dto.auth.ResetPasswordRequest;
import com.backend.old_bicycle_project.entity.EmailVerification;
import com.backend.old_bicycle_project.entity.PasswordResetToken;
import com.backend.old_bicycle_project.entity.RefreshToken;
import com.backend.old_bicycle_project.entity.User;
import com.backend.old_bicycle_project.entity.enums.AppRole;
import com.backend.old_bicycle_project.entity.enums.UserStatus;
import com.backend.old_bicycle_project.exception.AppException;
import com.backend.old_bicycle_project.exception.ErrorCode;
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

import java.util.UUID;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final Pattern PASSWORD_POLICY = Pattern.compile("^(?=.*[A-Z])(?=.*\\d).{8,}$");

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;
    private final RefreshTokenService refreshTokenService;
    private final EmailService emailService;

    @Value("${jwt.access-token-expiration}")
    private long accessTokenExpiration;

    @Transactional
    public String register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new AppException(ErrorCode.USER_EXISTED);
        }

        validatePasswordPolicy(request.getPassword());

        AppRole role = request.getRole();
        if (role == null || (role != AppRole.buyer && role != AppRole.seller)) {
            role = AppRole.buyer;
        }

        User user = User.builder()
                .email(request.getEmail().trim())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .firstName(trimToNull(request.getFirstName()))
                .lastName(trimToNull(request.getLastName()))
                .phone(trimToNull(request.getPhone()))
                .role(role)
                .isVerified(false)
                .status(UserStatus.active)
                .build();

        userRepository.save(user);

        EmailVerification verificationToken = emailService.createVerificationToken(user);
        emailService.sendVerificationEmail(user, verificationToken.getToken());

        return "Dang ky thanh cong. Vui long kiem tra email de xac thuc tai khoan.";
    }

    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        User user = (User) authentication.getPrincipal();
        String accessToken = jwtTokenProvider.generateAccessToken(user);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

        return buildAuthResponse(user, accessToken, refreshToken.getToken());
    }

    public AuthResponse refreshToken(RefreshTokenRequest request) {
        RefreshToken refreshToken = refreshTokenService.findByToken(request.getRefreshToken())
                .orElseThrow(() -> new AppException(ErrorCode.RECORD_NOT_EXISTS));

        if (refreshToken.isExpired()) {
            refreshTokenService.deleteAllByUser(refreshToken.getUser());
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        User user = refreshToken.getUser();
        String newAccessToken = jwtTokenProvider.generateAccessToken(user);

        return buildAuthResponse(user, newAccessToken, refreshToken.getToken());
    }

    public String requestPasswordReset(ForgotPasswordRequest request) {
        userRepository.findByEmail(request.getEmail().trim())
                .ifPresent(user -> {
                    PasswordResetToken passwordResetToken = emailService.createPasswordResetToken(user);
                    emailService.sendPasswordResetEmail(user, passwordResetToken.getToken());
                });

        return "Neu email ton tai, he thong da gui huong dan dat lai mat khau.";
    }

    @Transactional
    public String resetPassword(ResetPasswordRequest request) {
        validatePasswordPolicy(request.getNewPassword());

        PasswordResetToken passwordResetToken = emailService.findPasswordResetToken(request.getToken())
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_RESET_TOKEN));

        if (passwordResetToken.isExpired()) {
            emailService.deletePasswordResetTokensByUser(passwordResetToken.getUser());
            throw new AppException(ErrorCode.INVALID_RESET_TOKEN);
        }

        User user = passwordResetToken.getUser();
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        refreshTokenService.deleteAllByUser(user);
        emailService.deletePasswordResetTokensByUser(user);

        return "Dat lai mat khau thanh cong. Vui long dang nhap lai.";
    }

    @Transactional
    public void logout(User currentUser) {
        refreshTokenService.deleteAllByUser(currentUser);
    }

    @Transactional
    public String verifyEmail(String token) {
        EmailVerification verification = emailService.findByToken(token)
                .orElseThrow(() -> new AppException(ErrorCode.RECORD_NOT_EXISTS));

        if (verification.isExpired()) {
            emailService.deleteByUser(verification.getUser());
            throw new AppException(ErrorCode.INVALID_KEY);
        }

        User user = verification.getUser();
        if (user.isVerified()) {
            return "Tai khoan da duoc xac thuc truoc do.";
        }

        user.setVerified(true);
        userRepository.save(user);
        emailService.deleteByUser(user);

        return "Xac thuc email thanh cong.";
    }

    public AuthResponse.UserInfo getCurrentUser(User currentUser) {
        return mapUserInfo(loadUser(currentUser.getId()));
    }

    @Transactional
    public AuthResponse.UserInfo updateProfile(User currentUser, ProfileUpdateRequest request) {
        User user = loadUser(currentUser.getId());

        if (request.getFirstName() != null) {
            user.setFirstName(trimToNull(request.getFirstName()));
        }
        if (request.getLastName() != null) {
            user.setLastName(trimToNull(request.getLastName()));
        }
        if (request.getPhone() != null) {
            user.setPhone(trimToNull(request.getPhone()));
        }
        if (request.getAvatarUrl() != null) {
            user.setAvatarUrl(trimToNull(request.getAvatarUrl()));
        }
        if (request.getDefaultAddress() != null) {
            user.setDefaultAddress(trimToNull(request.getDefaultAddress()));
        }

        return mapUserInfo(userRepository.save(user));
    }

    @Transactional
    public String changePassword(User currentUser, ChangePasswordRequest request) {
        User user = loadUser(currentUser.getId());

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new AppException(ErrorCode.CURRENT_PASSWORD_INVALID);
        }

        validatePasswordPolicy(request.getNewPassword());
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        refreshTokenService.deleteAllByUser(user);

        return "Doi mat khau thanh cong. Cac phien dang nhap cu da bi thu hoi.";
    }

    private AuthResponse buildAuthResponse(User user, String accessToken, String refreshToken) {
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(accessTokenExpiration / 1000)
                .user(mapUserInfo(user))
                .build();
    }

    private AuthResponse.UserInfo mapUserInfo(User user) {
        return AuthResponse.UserInfo.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .phone(user.getPhone())
                .avatarUrl(user.getAvatarUrl())
                .defaultAddress(user.getDefaultAddress())
                .role(user.getRole())
                .status(user.getStatus())
                .isVerified(user.isVerified())
                .build();
    }

    private User loadUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
    }

    private void validatePasswordPolicy(String password) {
        if (password == null || !PASSWORD_POLICY.matcher(password).matches()) {
            throw new AppException(ErrorCode.INVALID_PASSWORD);
        }
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
