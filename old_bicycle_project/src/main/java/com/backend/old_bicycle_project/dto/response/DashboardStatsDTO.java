package com.backend.old_bicycle_project.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardStatsDTO {

    // General Stats
    private long totalUsers;
    private long totalProducts;
    private long totalOrders;
    private BigDecimal totalRevenue; // Sum of total_amount where status is COMPLETED

    // Inspection Stats
    private long totalInspections;
    private long passedInspections;
    private long failedInspections;

    // Monthly Data (for charts)
    private Map<String, BigDecimal> monthlyRevenue; // "YYYY-MM" -> amount
    private Map<String, Long> monthlyOrders;        // "YYYY-MM" -> count
}
