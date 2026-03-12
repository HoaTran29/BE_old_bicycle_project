package com.backend.old_bicycle_project.dto.request;

import com.backend.old_bicycle_project.entity.enums.ReportReason;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportRequestDTO {

    @NotNull(message = "Target ID is required")
    private UUID targetId;

    @NotBlank(message = "Target type is required (e.g., PRODUCT, USER)")
    private String targetType;

    @NotNull(message = "Reason is required")
    private ReportReason reason;

    private String description;
}
