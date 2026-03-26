package com.backend.old_bicycle_project.dto.response;

import com.backend.old_bicycle_project.entity.enums.RefundStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefundResponseDTO {
    private UUID id;
    private UUID orderId;
    private UUID paymentId;
    private UUID requesterId;
    private String requesterName;
    private BigDecimal amount;
    private String reason;
    private String evidenceNote;
    private List<RefundEvidenceFileResponseDTO> evidenceFiles;
    private RefundStatus status;
    private String adminNote;
    private String refundReference;
    private UUID reviewedBy;
    private String reviewedByName;
    private LocalDateTime reviewedAt;
    private LocalDateTime processedAt;
    private LocalDateTime createdAt;
}
