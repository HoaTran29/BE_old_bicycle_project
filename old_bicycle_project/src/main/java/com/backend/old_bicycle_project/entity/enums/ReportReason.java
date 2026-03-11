package com.backend.old_bicycle_project.entity.enums;

public enum ReportReason {
    FRAUD("fraud"),
    FAKE("fake"),
    WRONG_DESCRIPTION("wrong_description"),
    SPAM("spam"),
    OTHER("other");

    private final String reason;

    ReportReason(String reason) {
        this.reason = reason;
    }

    public String getReason() {
        return reason;
    }
}
