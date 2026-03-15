package com.backend.old_bicycle_project.controller;

import com.backend.old_bicycle_project.dto.auth.AuthResponse;
import com.backend.old_bicycle_project.dto.auth.ChangePasswordRequest;
import com.backend.old_bicycle_project.dto.auth.ForgotPasswordRequest;
import com.backend.old_bicycle_project.dto.auth.LoginRequest;
import com.backend.old_bicycle_project.dto.response.ApiResponse;
import com.backend.old_bicycle_project.entity.User;
import com.backend.old_bicycle_project.entity.enums.AppRole;
import com.backend.old_bicycle_project.entity.enums.UserStatus;
import com.backend.old_bicycle_project.service.AuthService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    @Test
    void loginReturnsWrappedAuthResponse() {
        LoginRequest request = new LoginRequest();
        request.setEmail("buyer@test.dev");
        request.setPassword("Password1");

        AuthResponse authResponse = AuthResponse.builder()
                .accessToken("access-token")
                .refreshToken("refresh-token")
                .tokenType("Bearer")
                .expiresIn(900L)
                .user(AuthResponse.UserInfo.builder()
                        .id(UUID.randomUUID())
                        .email("buyer@test.dev")
                        .firstName("Ngọc")
                        .lastName("Buyer")
                        .role(AppRole.buyer)
                        .status(UserStatus.active)
                        .isVerified(true)
                        .build())
                .build();

        when(authService.login(request)).thenReturn(authResponse);

        ApiResponse<AuthResponse> response = authController.login(request);

        assertThat(response.getResult()).isSameAs(authResponse);
        assertThat(response.getResult().getAccessToken()).isEqualTo("access-token");
    }

    @Test
    void forgotPasswordDelegatesToServiceAndReturnsMessage() {
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail("buyer@test.dev");
        when(authService.requestPasswordReset(request)).thenReturn("Đã gửi email");

        ApiResponse<String> response = authController.forgotPassword(request);

        assertThat(response.getResult()).isEqualTo("Đã gửi email");
        verify(authService).requestPasswordReset(request);
    }

    @Test
    void changePasswordUsesAuthenticatedUserAndReturnsServiceMessage() {
        User currentUser = User.builder()
                .id(UUID.randomUUID())
                .email("buyer@test.dev")
                .role(AppRole.buyer)
                .build();
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("OldPassword1");
        request.setNewPassword("NewPassword1");

        when(authService.changePassword(currentUser, request)).thenReturn("Đổi mật khẩu thành công");

        ApiResponse<String> response = authController.changePassword(currentUser, request);

        assertThat(response.getResult()).isEqualTo("Đổi mật khẩu thành công");
        verify(authService).changePassword(currentUser, request);
    }
}
