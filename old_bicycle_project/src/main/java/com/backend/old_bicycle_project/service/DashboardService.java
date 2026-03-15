package com.backend.old_bicycle_project.service;

import com.backend.old_bicycle_project.dto.response.DashboardStatsDTO;

public interface DashboardService {

    /**
     * Get aggregate statistics for the Admin Dashboard
     */
    DashboardStatsDTO getDashboardStats();
}
