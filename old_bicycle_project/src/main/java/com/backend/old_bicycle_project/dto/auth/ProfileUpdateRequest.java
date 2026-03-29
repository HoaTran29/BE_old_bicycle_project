package com.backend.old_bicycle_project.dto.auth;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ProfileUpdateRequest {
    @Size(max = 100, message = "Ten khong duoc vuot qua 100 ky tu")
    private String firstName;

    @Size(max = 100, message = "Ho khong duoc vuot qua 100 ky tu")
    private String lastName;

    @Size(max = 20, message = "So dien thoai khong duoc vuot qua 20 ky tu")
    private String phone;

    @Size(max = 500, message = "Avatar URL khong duoc vuot qua 500 ky tu")
    private String avatarUrl;

    @Size(max = 500, message = "Dia chi mac dinh khong duoc vuot qua 500 ky tu")
    private String defaultAddress;
}
