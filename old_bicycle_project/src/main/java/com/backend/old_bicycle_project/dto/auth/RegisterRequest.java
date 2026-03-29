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
    @Size(max = 255, message = "Email khong duoc vuot qua 255 ky tu")
    private String email;

    @NotBlank(message = "Mat khau khong duoc de trong")
    @Size(min = 8, message = "INVALID_PASSWORD")
    private String password;

    @Size(max = 100, message = "Ten khong duoc vuot qua 100 ky tu")
    private String firstName;

    @Size(max = 100, message = "Ho khong duoc vuot qua 100 ky tu")
    private String lastName;

    @Size(max = 20, message = "So dien thoai khong duoc vuot qua 20 ky tu")
    private String phone;

    private AppRole role = AppRole.buyer;
}
