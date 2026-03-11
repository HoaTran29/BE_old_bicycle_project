package com.backend.old_bicycle_project.entity.enums;

public enum ReportStatus {
    PENDING("pending"),
    REVIEWED("reviewed"),
    RESOLVED("resolved");

    private final String status;

    ReportStatus(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }
}
