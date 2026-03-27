package com.backend.old_bicycle_project.entity.enums;

public enum ReportStatus {
    pending("pending"),
    investigating("investigating"),
    resolved_upheld("resolved_upheld"),
    resolved_dismissed("resolved_dismissed");

    private final String status;

    ReportStatus(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }
}
