package com.backend.old_bicycle_project.dto.request;

import com.backend.old_bicycle_project.entity.enums.UserStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminUserStatusUpdateRequest {

    @NotNull(message = "User status is required")
    private UserStatus status;
}
