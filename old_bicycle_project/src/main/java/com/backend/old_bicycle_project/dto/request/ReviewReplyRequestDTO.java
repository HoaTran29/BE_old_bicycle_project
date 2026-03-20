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
public class ReviewReplyRequestDTO {

    @NotBlank(message = "Reply is required")
    private String reply;
}
