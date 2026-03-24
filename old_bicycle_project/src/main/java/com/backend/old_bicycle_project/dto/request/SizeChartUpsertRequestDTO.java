package com.backend.old_bicycle_project.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class SizeChartUpsertRequestDTO {

    @NotNull(message = "Category is required")
    private UUID categoryId;

    @NotBlank(message = "Chart name is required")
    private String name;

    private String description;

    @NotEmpty(message = "At least one size chart row is required")
    @Valid
    private List<SizeChartRowRequestDTO> rows;
}
