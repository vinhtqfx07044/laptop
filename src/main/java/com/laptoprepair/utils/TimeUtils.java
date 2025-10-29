package com.laptoprepair.utils;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Centralized utility class for time-related operations.
 * Provides consistent timezone handling across the application.
 */
public final class TimeUtils {

    private static final ZoneId VIETNAM_TIMEZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    // Private constructor to prevent instantiation
    private TimeUtils() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Gets the current date and time in Vietnam timezone.
     *
     * @return Current LocalDateTime in Vietnam timezone
     */
    public static LocalDateTime nowInVietnam() {
        return LocalDateTime.now(VIETNAM_TIMEZONE);
    }

    /**
     * Formats current time for display with day of week (yyyy-MM-dd HH:mm:ss EEEE).
     * Used by TimeTools for AI responses.
     *
     * @return Formatted current time with day of week
     */
    public static String getCurrentTimeFormatted() {
        return nowInVietnam().format(
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss EEEE"));
    }
}