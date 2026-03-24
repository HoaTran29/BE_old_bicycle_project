package com.backend.old_bicycle_project.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminBrandUpsertRequest {

    @NotBlank(message = "Brand name is required")
    private String name;

    private String logoUrl;
}
