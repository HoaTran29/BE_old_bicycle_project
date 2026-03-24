package com.backend.old_bicycle_project.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminReferenceValueUpsertRequest {

    @NotBlank(message = "Name is required")
    private String name;

    private String description;
}
