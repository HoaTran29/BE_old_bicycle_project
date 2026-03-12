package com.backend.old_bicycle_project.entity.enums;

public enum ReportStatus {
    pending("pending"),
    reviewed("reviewed"),
    resolved("resolved");

    private final String status;

    ReportStatus(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }
}
