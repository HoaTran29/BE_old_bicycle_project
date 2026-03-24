package com.backend.old_bicycle_project.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InspectionResponseDTO {
    private UUID id;
    private UUID productId;
    private UUID inspectorId;
    private BigDecimal overallScore;
    private Integer frameScore;
    private Integer forkScore;
    private Integer brakesScore;
    private Integer drivetrainScore;
    private Integer wheelsScore;
    private Integer wearPercentage;
    private String expertNotes;
    private Boolean passed;
    private String reportFileUrl;
    private LocalDateTime validUntil;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
