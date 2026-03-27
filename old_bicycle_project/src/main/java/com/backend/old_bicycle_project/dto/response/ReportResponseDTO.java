package com.backend.old_bicycle_project.dto.response;

import com.backend.old_bicycle_project.entity.enums.ReportReason;
import com.backend.old_bicycle_project.entity.enums.ReportStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportResponseDTO {
    private UUID id;
    private UUID reporterId;
    private String reporterName;
    private UUID targetId;
    private String targetType;
    private ReportReason reason;
    private String description;
    private List<ReportEvidenceFileResponseDTO> evidenceFiles;
    private ReportStatus status;
    private String adminNote;
    private UUID processedById;
    private String processedByName;
    private LocalDateTime createdAt;
    private LocalDateTime processedAt;
}
