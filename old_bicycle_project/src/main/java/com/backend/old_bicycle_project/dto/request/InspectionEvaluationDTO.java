package com.backend.old_bicycle_project.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InspectionEvaluationDTO {

    @NotNull(message = "Frame score is required")
    @Min(value = 1, message = "Score must be between 1 and 5")
    @Max(value = 5, message = "Score must be between 1 and 5")
    private Integer frameScore;

    @NotNull(message = "Fork score is required")
    @Min(value = 1, message = "Score must be between 1 and 5")
    @Max(value = 5, message = "Score must be between 1 and 5")
    private Integer forkScore;

    @NotNull(message = "Brakes score is required")
    @Min(value = 1, message = "Score must be between 1 and 5")
    @Max(value = 5, message = "Score must be between 1 and 5")
    private Integer brakesScore;

    @NotNull(message = "Drivetrain score is required")
    @Min(value = 1, message = "Score must be between 1 and 5")
    @Max(value = 5, message = "Score must be between 1 and 5")
    private Integer drivetrainScore;

    @NotNull(message = "Wheels score is required")
    @Min(value = 1, message = "Score must be between 1 and 5")
    @Max(value = 5, message = "Score must be between 1 and 5")
    private Integer wheelsScore;

    @NotNull(message = "Wear percentage is required")
    @Min(value = 0, message = "Wear percentage must be between 0 and 100")
    @Max(value = 100, message = "Wear percentage must be between 0 and 100")
    private Integer wearPercentage;

    private String expertNotes;

    @NotNull(message = "Passed status is required")
    private Boolean passed;
}
