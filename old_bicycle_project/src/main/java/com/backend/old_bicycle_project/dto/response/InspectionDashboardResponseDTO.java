package com.backend.old_bicycle_project.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InspectionDashboardResponseDTO {
    private long pendingRequests;
    private long completedThisWeek;
    private BigDecimal passRate;
    private BigDecimal averageScore;
    private List<InspectionHistoryItemResponseDTO> recentInspections;
}
