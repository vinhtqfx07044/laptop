package com.laptoprepair.enums;

import com.fasterxml.jackson.annotation.JsonValue;

public enum RequestStatus {
    SCHEDULED("Đã lên lịch", "bg-info"),
    QUOTED("Đã báo giá", "bg-warning"),
    APPROVE_QUOTED("Đã duyệt báo giá", "bg-warning"),
    IN_PROGRESS("Đang thực hiện", "bg-warning"),
    COMPLETED("Hoàn thành", "bg-success"),
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

    public String getDisplayName() {
        return value;
    }

    public boolean canTransitionTo(RequestStatus newStatus) {
        if (this == newStatus)
            return true;

        switch (this) {
            case SCHEDULED:
                return newStatus == QUOTED || newStatus == APPROVE_QUOTED || newStatus == IN_PROGRESS
                        || newStatus == COMPLETED || newStatus == CANCELLED;
            case QUOTED:
                return newStatus == APPROVE_QUOTED || newStatus == IN_PROGRESS || newStatus == COMPLETED
                        || newStatus == CANCELLED;
            case APPROVE_QUOTED:
                return newStatus == IN_PROGRESS || newStatus == COMPLETED || newStatus == CANCELLED;
            case IN_PROGRESS:
                return newStatus == COMPLETED || newStatus == CANCELLED;
            case COMPLETED:
                return false;
            case CANCELLED:
                return false;
            default:
                return true;
        }
    }

    // Check if request items can be edited based on status
    public boolean isRequestItemsLocked() {
        return this == APPROVE_QUOTED || this == IN_PROGRESS || this == COMPLETED || this == CANCELLED;
    }

    @Override
    public String toString() {
        return value;
    }
}