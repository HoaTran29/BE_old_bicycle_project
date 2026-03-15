package com.backend.old_bicycle_project.dto.request;

import com.backend.old_bicycle_project.entity.enums.ReportStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportProcessDTO {
    
    @NotNull(message = "Status is required")
    private ReportStatus status;
    
    private String adminNote;
}
