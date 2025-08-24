package com.laptoprepair.enums;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Defines the possible statuses for a repair request.
 * Each status has a display value and a corresponding CSS class for UI
 * representation.
 */
public enum RequestStatus {
    SCHEDULED("Đã lên lịch", "bg-info"),
    QUOTED("Đã báo giá", "bg-warning"),
    APPROVE_QUOTED("Đã duyệt báo giá", "bg-warning"),
    IN_PROGRESS("Đang thực hiện", "bg-warning"),
    COMPLETED("Hoàn thành", "bg-success"),
    UNDER_WARRANTY("Đang bảo hành", "bg-primary"),
    CANCELLED("Đã hủy", "bg-danger");

    private final String value;
    private final String badgeClass;

    RequestStatus(String value, String badgeClass) {
        this.value = value;
        this.badgeClass = badgeClass;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    public String getBadgeClass() {
        return "badge " + badgeClass;
    }

    // Check if request items can be edited based on status
    public boolean isRequestItemsLocked() {
        return this == COMPLETED || this == UNDER_WARRANTY || this == CANCELLED;
    }

    @Override
    public String toString() {
        return value;
    }
}