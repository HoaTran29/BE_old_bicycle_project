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
    private BigDecimal totalRevenue; // Legacy alias of totalGmv for FE compatibility
    private BigDecimal totalGmv;
    private BigDecimal pendingPlatformFee;
    private BigDecimal recognizedPlatformRevenue;
    private BigDecimal reversedPlatformFee;

    // Inspection Stats
    private long totalInspections;
    private long passedInspections;
    private long failedInspections;

    // Monthly Data (for charts)
    private Map<String, BigDecimal> monthlyRevenue; // Legacy alias of monthlyGmv for FE compatibility
    private Map<String, BigDecimal> monthlyGmv; // "YYYY-MM" -> amount
    private Map<String, BigDecimal> monthlyRecognizedPlatformRevenue; // "YYYY-MM" -> amount
    private Map<String, Long> monthlyOrders; // "YYYY-MM" -> count
}
