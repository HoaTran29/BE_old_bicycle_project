package com.backend.old_bicycle_project.service;

import com.backend.old_bicycle_project.dto.auth.ChangePasswordRequest;
import com.backend.old_bicycle_project.dto.auth.ForgotPasswordRequest;
import com.backend.old_bicycle_project.dto.auth.ProfileUpdateRequest;
import com.backend.old_bicycle_project.dto.auth.RegisterRequest;
import com.backend.old_bicycle_project.dto.auth.ResetPasswordRequest;
import com.backend.old_bicycle_project.entity.EmailVerification;
import com.backend.old_bicycle_project.entity.PasswordResetToken;
import com.backend.old_bicycle_project.entity.User;
import com.backend.old_bicycle_project.entity.enums.AppRole;
import com.backend.old_bicycle_project.entity.enums.UserStatus;
import com.backend.old_bicycle_project.repository.UserRepository;
import com.backend.old_bicycle_project.security.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
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
