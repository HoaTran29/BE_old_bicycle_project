package com.backend.old_bicycle_project.entity.enums;

public enum ReportReason {
    fraud("fraud"),
    fake("fake"),
    wrong_description("wrong_description"),
    spam("spam"),
    other("other");

    private final String reason;

    ReportReason(String reason) {
        this.reason = reason;
    }

    public String getReason() {
        return reason;
    }
}
