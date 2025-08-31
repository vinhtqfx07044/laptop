package com.laptoprepair.validation;

/**
 * Centralized validation constants to ensure consistency across
 * frontend, backend, and database constraints.
 */
public final class ValidationConstants {

    // Request validation constants
    public static final int REQUEST_NAME_MIN_LENGTH = 3;
    public static final int REQUEST_NAME_MAX_LENGTH = 100;
    public static final int REQUEST_DESCRIPTION_MIN_LENGTH = 10;
    public static final int REQUEST_DESCRIPTION_MAX_LENGTH = 1000;
    public static final String REQUEST_PHONE_PATTERN = "0\\d{9}";
    public static final int REQUEST_PHONE_LENGTH = 10;

    // Service Item validation constants
    public static final int SERVICE_ITEM_NAME_MAX_LENGTH = 255;
    public static final int SERVICE_ITEM_WARRANTY_DAYS_MIN = 0;

    // Validation messages
    public static final String REQUEST_NAME_BLANK_MSG = "Tên khách hàng là bắt buộc";
    public static final String REQUEST_NAME_SIZE_MSG = "Tên khách hàng phải có từ " + REQUEST_NAME_MIN_LENGTH + "-"
            + REQUEST_NAME_MAX_LENGTH + " ký tự";
    public static final String REQUEST_PHONE_BLANK_MSG = "Số điện thoại là bắt buộc";
    public static final String REQUEST_PHONE_PATTERN_MSG = "Số điện thoại phải có 10 chữ số và bắt đầu bằng 0";
    public static final String REQUEST_EMAIL_FORMAT_MSG = "Email không đúng định dạng";
    public static final String REQUEST_APPOINTMENT_DATE_NULL_MSG = "Ngày hẹn là bắt buộc";
    public static final String REQUEST_APPOINTMENT_DATE_FUTURE_MSG = "Ngày hẹn phải sau thời điểm hiện tại";
    public static final String REQUEST_DESCRIPTION_BLANK_MSG = "Mô tả tình trạng thiết bị là bắt buộc";
    public static final String REQUEST_DESCRIPTION_SIZE_MSG = "Mô tả phải có từ " + REQUEST_DESCRIPTION_MIN_LENGTH + "-"
            + REQUEST_DESCRIPTION_MAX_LENGTH + " ký tự";

    private ValidationConstants() {
        // Private constructor to prevent instantiation
    }
}