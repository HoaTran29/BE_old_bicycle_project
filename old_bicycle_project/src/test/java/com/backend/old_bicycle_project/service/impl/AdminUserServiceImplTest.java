package com.backend.old_bicycle_project.service.impl;

import com.backend.old_bicycle_project.dto.response.AdminUserResponseDTO;
import com.backend.old_bicycle_project.entity.User;
import com.backend.old_bicycle_project.entity.enums.AppRole;
import com.backend.old_bicycle_project.entity.enums.UserStatus;
import com.backend.old_bicycle_project.exception.AppException;
import com.backend.old_bicycle_project.exception.ErrorCode;
import com.backend.old_bicycle_project.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AdminUserServiceImpl adminUserService;

    @Test
    void getAllUsersReturnsPagedMappedResults() {
        User user = user(UUID.randomUUID(), "buyer@test.dev", AppRole.buyer, UserStatus.active);
        Page<User> page = new PageImpl<>(List.of(user));

        when(userRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        Page<AdminUserResponseDTO> response = adminUserService.getAllUsers("buyer", AppRole.buyer, UserStatus.active, true, 0, 12);

        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().getFirst().getEmail()).isEqualTo("buyer@test.dev");
        assertThat(response.getContent().getFirst().getRole()).isEqualTo(AppRole.buyer);
    }

    @Test
    void getUserByIdThrowsWhenUserDoesNotExist() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminUserService.getUserById(userId))
                .isInstanceOfSatisfying(AppException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.USER_NOT_EXISTED));
    }

    @Test
    void updateUserStatusRejectsChangingOwnStatus() {
        UUID adminId = UUID.randomUUID();

        assertThatThrownBy(() -> adminUserService.updateUserStatus(adminId, UserStatus.banned, adminId))
                .isInstanceOfSatisfying(AppException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.SELF_STATUS_CHANGE_NOT_ALLOWED));

        verify(userRepository, never()).findById(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    void updateUserStatusPersistsNewStatus() {
        UUID adminId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        User user = user(userId, "seller@test.dev", AppRole.seller, UserStatus.active);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        AdminUserResponseDTO response = adminUserService.updateUserStatus(userId, UserStatus.banned, adminId);

        assertThat(response.getStatus()).isEqualTo(UserStatus.banned);
        assertThat(user.getStatus()).isEqualTo(UserStatus.banned);
        verify(userRepository).save(eq(user));
    }

    private User user(UUID id, String email, AppRole role, UserStatus status) {
        return User.builder()
                .id(id)
                .email(email)
                .role(role)
                .status(status)
                .isVerified(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }
}
