package com.backend.old_bicycle_project.service;

import com.backend.old_bicycle_project.dto.response.AdminUserResponseDTO;
import com.backend.old_bicycle_project.dto.response.AdminUserActivityResponseDTO;
import com.backend.old_bicycle_project.entity.enums.AppRole;
import com.backend.old_bicycle_project.entity.enums.UserStatus;
import org.springframework.data.domain.Page;

import java.util.UUID;

public interface AdminUserService {

    Page<AdminUserResponseDTO> getAllUsers(String keyword, AppRole role, UserStatus status, Boolean verified, int page, int size);

    AdminUserResponseDTO getUserById(UUID userId);

    AdminUserResponseDTO updateUserStatus(UUID userId, UserStatus status, UUID adminId);

    String resetUserPassword(UUID userId, String newPassword);

    AdminUserActivityResponseDTO getUserActivity(UUID userId);
}
