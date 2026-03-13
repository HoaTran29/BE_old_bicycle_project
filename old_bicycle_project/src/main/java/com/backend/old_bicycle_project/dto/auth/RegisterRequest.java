package com.backend.old_bicycle_project.dto.auth;

import com.backend.old_bicycle_project.entity.enums.AppRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank(message = "Email khong duoc de trong")
    @Email(message = "Email khong hop le")
    private String email;

    @NotBlank(message = "Mat khau khong duoc de trong")
    @Size(min = 8, message = "INVALID_PASSWORD")
    private String password;

    private String firstName;
    private String lastName;
    private String phone;

    private AppRole role = AppRole.buyer;
}
