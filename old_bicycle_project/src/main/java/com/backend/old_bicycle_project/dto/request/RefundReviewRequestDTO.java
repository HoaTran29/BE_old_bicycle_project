package com.backend.old_bicycle_project.dto.request;

import com.backend.old_bicycle_project.entity.enums.RefundStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefundReviewRequestDTO {

    @NotNull(message = "Target refund status is required")
    private RefundStatus status;

    private String adminNote;

    private String refundReference;
}
