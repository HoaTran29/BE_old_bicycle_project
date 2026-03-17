package com.backend.old_bicycle_project.service.impl;

import com.backend.old_bicycle_project.dto.response.AdminUserResponseDTO;
import com.backend.old_bicycle_project.entity.User;
import com.backend.old_bicycle_project.entity.enums.AppRole;
import com.backend.old_bicycle_project.entity.enums.UserStatus;
import com.backend.old_bicycle_project.exception.AppException;
import com.backend.old_bicycle_project.exception.ErrorCode;
import com.backend.old_bicycle_project.repository.UserRepository;
import com.backend.old_bicycle_project.service.AdminUserService;
import com.backend.old_bicycle_project.specification.UserSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminUserServiceImpl implements AdminUserService {

    private final UserRepository userRepository;

    @Override
    public Page<AdminUserResponseDTO> getAllUsers(
            String keyword,
            AppRole role,
            UserStatus status,
            Boolean verified,
            int page,
            int size
    ) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return userRepository.findAll(
                UserSpecification.fromAdminFilter(keyword, role, status, verified),
                pageable
        ).map(this::toResponse);
    }

    @Override
    public AdminUserResponseDTO getUserById(UUID userId) {
        return toResponse(getRequiredUser(userId));
    }

    @Override
    @Transactional
    public AdminUserResponseDTO updateUserStatus(UUID userId, UserStatus status, UUID adminId) {
        if (adminId != null && adminId.equals(userId)) {
            throw new AppException(ErrorCode.SELF_STATUS_CHANGE_NOT_ALLOWED);
        }

        User user = getRequiredUser(userId);
        user.setStatus(status);
        return toResponse(userRepository.save(user));
    }

    private User getRequiredUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
    }

    private AdminUserResponseDTO toResponse(User user) {
        return AdminUserResponseDTO.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .avatarUrl(user.getAvatarUrl())
                .defaultAddress(user.getDefaultAddress())
                .role(user.getRole())
                .status(user.getStatus())
                .isVerified(user.isVerified())
                .averageRating(user.getAverageRating())
                .totalReviews(user.getTotalReviews())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
