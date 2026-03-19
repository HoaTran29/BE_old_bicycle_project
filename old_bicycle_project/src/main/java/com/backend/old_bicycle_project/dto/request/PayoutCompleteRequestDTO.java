package com.backend.old_bicycle_project.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PayoutCompleteRequestDTO {

    @NotBlank(message = "Bank reference is required")
    private String bankReference;

    private String adminNote;
}
