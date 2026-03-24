package com.backend.old_bicycle_project.dto.response;

import com.backend.old_bicycle_project.entity.enums.AppRole;
import com.backend.old_bicycle_project.entity.enums.OrderEvidenceType;
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
public class OrderEvidenceSubmissionResponseDTO {
    private UUID id;
    private OrderEvidenceType evidenceType;
    private UUID submittedByUserId;
    private String submittedByName;
    private AppRole submittedByRole;
    private String note;
    private LocalDateTime createdAt;
    private List<OrderEvidenceFileResponseDTO> files;
}
