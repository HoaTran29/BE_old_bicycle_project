package com.backend.old_bicycle_project.dto.assistant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AssistantMessageDTO {

    @NotBlank(message = "INVALID_KEY")
    private String role;

    @NotBlank(message = "INVALID_KEY")
    @Size(max = 2000, message = "INVALID_KEY")
    private String content;
}
