package com.backend.old_bicycle_project.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SizeChartRowRequestDTO {

    @NotBlank(message = "Frame size is required")
    private String frameSize;

    @NotNull(message = "Minimum height is required")
    @Positive(message = "Minimum height must be greater than 0")
    private Integer heightMinCm;

    @NotNull(message = "Maximum height is required")
    @Positive(message = "Maximum height must be greater than 0")
    private Integer heightMaxCm;

    private String note;
}
