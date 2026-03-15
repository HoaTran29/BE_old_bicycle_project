package com.backend.old_bicycle_project.dto.auth;

import lombok.Data;

@Data
public class ProfileUpdateRequest {
    private String firstName;
    private String lastName;
    private String phone;
    private String avatarUrl;
    private String defaultAddress;
}
