package com.backend.old_bicycle_project.service;

import com.backend.old_bicycle_project.dto.auth.ChangePasswordRequest;
import com.backend.old_bicycle_project.dto.auth.ForgotPasswordRequest;
import com.backend.old_bicycle_project.dto.auth.ProfileUpdateRequest;
import com.backend.old_bicycle_project.dto.auth.RegisterRequest;
import com.backend.old_bicycle_project.dto.auth.ResendVerificationRequest;
import com.backend.old_bicycle_project.dto.auth.ResetPasswordRequest;
import com.backend.old_bicycle_project.entity.EmailVerification;
import com.backend.old_bicycle_project.entity.PasswordResetToken;
import com.backend.old_bicycle_project.entity.User;
import com.backend.old_bicycle_project.entity.RefreshToken;
import com.backend.old_bicycle_project.entity.enums.AppRole;
import com.backend.old_bicycle_project.entity.enums.UserStatus;
import com.backend.old_bicycle_project.exception.AppException;
import com.backend.old_bicycle_project.exception.ErrorCode;
import com.backend.old_bicycle_project.repository.UserRepository;
import com.backend.old_bicycle_project.security.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private EmailService emailService;

    @Spy
    private PasswordPolicyValidator passwordPolicyValidator;

    @InjectMocks
    private AuthService authService;

    @Test
    void registerWithStrongPasswordSavesUserAndSendsVerificationEmail() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("seller@test.dev");
        request.setPassword("StrongPass1");
        request.setFirstName("Lan");
        request.setLastName("Seller");
        request.setPhone("0909000999");
        request.setRole(AppRole.seller);

        when(userRepository.existsByEmail("seller@test.dev")).thenReturn(false);
        when(passwordEncoder.encode("StrongPass1")).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(UUID.randomUUID());
            return user;
        });
        when(emailService.createVerificationToken(any(User.class))).thenReturn(EmailVerification.builder()
                .token("verify-token")
                .build());

        String message = authService.register(request);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        verify(emailService).sendVerificationEmail(any(User.class), org.mockito.ArgumentMatchers.eq("verify-token"));

        User savedUser = userCaptor.getValue();
        assertThat(savedUser.getPasswordHash()).isEqualTo("encoded-password");
        assertThat(savedUser.getRole()).isEqualTo(AppRole.seller);
        assertThat(savedUser.getPhone()).isEqualTo("0909000999");
        assertThat(message).contains("Dang ky thanh cong");
    }

    @Test
    void registerWithMissingRoleDefaultsToBuyer() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("newbuyer@test.dev");
        request.setPassword("StrongPass1");
        request.setFirstName("Minh");
        request.setLastName("Le");
        request.setRole(null);

        when(userRepository.existsByEmail("newbuyer@test.dev")).thenReturn(false);
        when(passwordEncoder.encode("StrongPass1")).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(emailService.createVerificationToken(any(User.class))).thenReturn(EmailVerification.builder()
                .token("verify-token")
                .build());

        authService.register(request);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getRole()).isEqualTo(AppRole.buyer);
    }

    @Test
    void registerRejectsWeakPassword() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("weak@test.dev");
        request.setPassword("weak");
        request.setFirstName("Weak");
        request.setLastName("User");
        request.setRole(AppRole.buyer);

        when(userRepository.existsByEmail("weak@test.dev")).thenReturn(false);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOfSatisfying(AppException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVALID_PASSWORD));
    }

    @Test
    void requestPasswordResetReturnsGenericMessageAndSendsEmailWhenUserExists() {
        User user = user("buyer@test.dev");
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .token("reset-token")
                .build();

        when(userRepository.findByEmail("buyer@test.dev")).thenReturn(Optional.of(user));
        when(emailService.createPasswordResetToken(user)).thenReturn(resetToken);

        String message = authService.requestPasswordReset(new ForgotPasswordRequest() {{
            setEmail("buyer@test.dev");
        }});

        verify(emailService).sendPasswordResetEmail(user, "reset-token");
        assertThat(message).contains("Neu email ton tai");
    }

    @Test
    void resendVerificationEmailCreatesAndSendsTokenForUnverifiedUser() {
        User user = user("buyer@test.dev");
        user.setVerified(false);

        when(userRepository.findByEmail("buyer@test.dev")).thenReturn(Optional.of(user));
        when(emailService.createVerificationToken(user)).thenReturn(EmailVerification.builder()
                .token("resend-token")
                .build());

        ResendVerificationRequest request = new ResendVerificationRequest();
        request.setEmail("buyer@test.dev");

        String message = authService.resendVerificationEmail(request);

        verify(emailService).sendVerificationEmail(user, "resend-token");
        assertThat(message).contains("gửi lại email xác thực");
    }

    @Test
    void resendVerificationEmailSkipsVerifiedUserAndStillReturnsGenericMessage() {
        User user = user("buyer@test.dev");
        when(userRepository.findByEmail("buyer@test.dev")).thenReturn(Optional.of(user));

        ResendVerificationRequest request = new ResendVerificationRequest();
        request.setEmail("buyer@test.dev");

        String message = authService.resendVerificationEmail(request);

        verify(emailService, never()).createVerificationToken(any(User.class));
        verify(emailService, never()).sendVerificationEmail(any(User.class), any());
        assertThat(message).contains("gửi lại email xác thực");
    }

    @Test
    void refreshTokenReturnsNewAccessTokenWhenTokenIsValid() {
        ReflectionTestUtils.setField(authService, "accessTokenExpiration", 900000L);
        User user = user("buyer@test.dev");
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token("refresh-token")
                .expiresAt(LocalDateTime.now().plusDays(1))
                .build();

        when(refreshTokenService.findByToken("refresh-token")).thenReturn(Optional.of(refreshToken));
        when(jwtTokenProvider.generateAccessToken(user)).thenReturn("new-access-token");

        var response = authService.refreshToken(new com.backend.old_bicycle_project.dto.auth.RefreshTokenRequest() {{
            setRefreshToken("refresh-token");
        }});

        assertThat(response.getAccessToken()).isEqualTo("new-access-token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
    }

    @Test
    void refreshTokenRejectsExpiredTokenAndRevokesUserSessions() {
        User user = user("buyer@test.dev");
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token("refresh-token")
                .expiresAt(LocalDateTime.now().minusMinutes(1))
                .build();

        when(refreshTokenService.findByToken("refresh-token")).thenReturn(Optional.of(refreshToken));

        assertThatThrownBy(() -> authService.refreshToken(new com.backend.old_bicycle_project.dto.auth.RefreshTokenRequest() {{
                    setRefreshToken("refresh-token");
                }}))
                .isInstanceOfSatisfying(AppException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.UNAUTHENTICATED));

        verify(refreshTokenService).deleteAllByUser(user);
    }

    @Test
    void loginRejectsUnverifiedUserAndRevokesExistingSessions() {
        User user = user("buyer@test.dev");
        user.setVerified(false);

        Authentication authentication = mock(Authentication.class);
        when(authenticationManager.authenticate(any())).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(user);

        assertThatThrownBy(() -> authService.login(new com.backend.old_bicycle_project.dto.auth.LoginRequest() {{
                    setEmail("buyer@test.dev");
                    setPassword("Password1");
                }}))
                .isInstanceOfSatisfying(AppException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.EMAIL_NOT_VERIFIED));

        verify(refreshTokenService).deleteAllByUser(user);
    }

    @Test
    void loginRejectsInvalidCredentialsWithClearError() {
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authService.login(new com.backend.old_bicycle_project.dto.auth.LoginRequest() {{
                    setEmail("buyer@test.dev");
                    setPassword("WrongPassword1");
                }}))
                .isInstanceOfSatisfying(AppException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVALID_CREDENTIALS));
    }

    @Test
    void loginRejectsInactiveUserWithClearError() {
        when(authenticationManager.authenticate(any()))
                .thenThrow(new DisabledException("Account disabled"));

        assertThatThrownBy(() -> authService.login(new com.backend.old_bicycle_project.dto.auth.LoginRequest() {{
                    setEmail("seller.city@oldbicycle.dev");
                    setPassword("Password1");
                }}))
                .isInstanceOfSatisfying(AppException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.ACCOUNT_INACTIVE));
    }

    @Test
    void loginRejectsBannedUserWithClearError() {
        when(authenticationManager.authenticate(any()))
                .thenThrow(new LockedException("Account banned"));

        assertThatThrownBy(() -> authService.login(new com.backend.old_bicycle_project.dto.auth.LoginRequest() {{
                    setEmail("buyer.banned@oldbicycle.dev");
                    setPassword("Password1");
                }}))
                .isInstanceOfSatisfying(AppException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.ACCOUNT_BANNED));
    }

    @Test
    void refreshTokenRejectsUnverifiedUserAndRevokesSessions() {
        User user = user("buyer@test.dev");
        user.setVerified(false);
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token("refresh-token")
                .expiresAt(LocalDateTime.now().plusDays(1))
                .build();

        when(refreshTokenService.findByToken("refresh-token")).thenReturn(Optional.of(refreshToken));

        assertThatThrownBy(() -> authService.refreshToken(new com.backend.old_bicycle_project.dto.auth.RefreshTokenRequest() {{
                    setRefreshToken("refresh-token");
                }}))
                .isInstanceOfSatisfying(AppException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.EMAIL_NOT_VERIFIED));

        verify(refreshTokenService).deleteAllByUser(user);
    }

    @Test
    void resetPasswordUpdatesHashAndRevokesOldSessions() {
        User user = user("buyer@test.dev");
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .user(user)
                .token("reset-token")
                .expiresAt(LocalDateTime.now().plusMinutes(30))
                .build();

        when(emailService.findPasswordResetToken("reset-token")).thenReturn(Optional.of(resetToken));
        when(passwordEncoder.encode("NewStrong1")).thenReturn("new-encoded-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        String message = authService.resetPassword(new ResetPasswordRequest() {{
            setToken("reset-token");
            setNewPassword("NewStrong1");
        }});

        assertThat(user.getPasswordHash()).isEqualTo("new-encoded-password");
        verify(refreshTokenService).deleteAllByUser(user);
        verify(emailService).deletePasswordResetTokensByUser(user);
        assertThat(message).contains("Dat lai mat khau thanh cong");
    }

    @Test
    void updateProfileUpdatesEditableFields() {
        User currentUser = user("buyer@test.dev");
        when(userRepository.findById(currentUser.getId())).thenReturn(Optional.of(currentUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProfileUpdateRequest request = new ProfileUpdateRequest();
        request.setFirstName("Mai");
        request.setLastName("Nguyen");
        request.setPhone("0988111222");
        request.setAvatarUrl("https://cdn.example/avatar.png");
        request.setDefaultAddress("123 Nguyen Trai");

        var response = authService.updateProfile(currentUser, request);

        assertThat(response.getFirstName()).isEqualTo("Mai");
        assertThat(response.getLastName()).isEqualTo("Nguyen");
        assertThat(response.getPhone()).isEqualTo("0988111222");
        assertThat(response.getAvatarUrl()).isEqualTo("https://cdn.example/avatar.png");
        assertThat(response.getDefaultAddress()).isEqualTo("123 Nguyen Trai");
    }

    @Test
    void changePasswordValidatesCurrentPasswordAndRevokesRefreshTokens() {
        User currentUser = user("buyer@test.dev");
        currentUser.setPasswordHash("old-hash");

        when(userRepository.findById(currentUser.getId())).thenReturn(Optional.of(currentUser));
        when(passwordEncoder.matches("OldPass1", "old-hash")).thenReturn(true);
        when(passwordEncoder.encode("FreshPass2")).thenReturn("fresh-hash");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        String message = authService.changePassword(currentUser, new ChangePasswordRequest() {{
            setCurrentPassword("OldPass1");
            setNewPassword("FreshPass2");
        }});

        assertThat(currentUser.getPasswordHash()).isEqualTo("fresh-hash");
        verify(refreshTokenService).deleteAllByUser(currentUser);
        assertThat(message).contains("Doi mat khau thanh cong");
    }

    private User user(String email) {
        return User.builder()
                .id(UUID.randomUUID())
                .email(email)
                .firstName("Test")
                .lastName("User")
                .role(AppRole.buyer)
                .status(UserStatus.active)
                .isVerified(true)
                .build();
    }
}
