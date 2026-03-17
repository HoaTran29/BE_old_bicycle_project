package com.backend.old_bicycle_project.controller;

import com.backend.old_bicycle_project.dto.request.AdminUserStatusUpdateRequest;
import com.backend.old_bicycle_project.dto.response.AdminUserResponseDTO;
import com.backend.old_bicycle_project.dto.response.ApiResponse;
import com.backend.old_bicycle_project.entity.User;
import com.backend.old_bicycle_project.entity.enums.AppRole;
import com.backend.old_bicycle_project.entity.enums.UserStatus;
import com.backend.old_bicycle_project.service.AdminUserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminUserControllerTest {

    @Mock
    private AdminUserService adminUserService;

    @InjectMocks
    private AdminUserController adminUserController;

    @Test
    void getAdminUsersDelegatesAndWrapsPage() {
        AdminUserResponseDTO user = AdminUserResponseDTO.builder()
                .id(UUID.randomUUID())
                .email("buyer@test.dev")
                .role(AppRole.buyer)
                .status(UserStatus.active)
                .build();
        Page<AdminUserResponseDTO> page = new PageImpl<>(List.of(user));

        when(adminUserService.getAllUsers("buyer", AppRole.buyer, UserStatus.active, true, 0, 12))
                .thenReturn(page);

        ApiResponse<Page<AdminUserResponseDTO>> response = adminUserController.getAdminUsers(
                "buyer", AppRole.buyer, UserStatus.active, true, 0, 12
        );

        assertThat(response.getResult()).isSameAs(page);
        assertThat(response.getResult().getContent()).hasSize(1);
        verify(adminUserService).getAllUsers("buyer", AppRole.buyer, UserStatus.active, true, 0, 12);
    }

    @Test
    void updateAdminUserStatusUsesAuthenticatedAdminId() {
        UUID userId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();
        User currentUser = User.builder().id(adminId).build();
        AdminUserStatusUpdateRequest request = new AdminUserStatusUpdateRequest();
        request.setStatus(UserStatus.banned);
        AdminUserResponseDTO updatedUser = AdminUserResponseDTO.builder()
                .id(userId)
                .status(UserStatus.banned)
                .build();

        when(adminUserService.updateUserStatus(userId, UserStatus.banned, adminId)).thenReturn(updatedUser);

        ApiResponse<AdminUserResponseDTO> response = adminUserController.updateAdminUserStatus(userId, request, currentUser);

        assertThat(response.getResult()).isSameAs(updatedUser);
        verify(adminUserService).updateUserStatus(userId, UserStatus.banned, adminId);
    }
}
