package com.backend.old_bicycle_project.controller;

import com.backend.old_bicycle_project.dto.response.ApiResponse;
import com.backend.old_bicycle_project.dto.response.DashboardStatsDTO;
import com.backend.old_bicycle_project.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<DashboardStatsDTO>> getDashboardStats() {
        
        DashboardStatsDTO stats = dashboardService.getDashboardStats();
        
        return ResponseEntity.ok(ApiResponse.<DashboardStatsDTO>builder()
                .code(200)
                .message("Fetched dashboard stats successfully")
                .result(stats)
                .build());
    }
}
