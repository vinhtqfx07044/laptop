package com.laptoprepair.config;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * Provides current time in Vietnam timezone (UTC+7).
 * Used by services that need to manually set timestamps.
 */
@Component
public class VietnamTimeProvider {
    private static final ZoneId VIETNAM_TIMEZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final ZoneId UTC_TIMEZONE = ZoneId.of("UTC");

    public LocalDateTime now() {
        return ZonedDateTime.now(UTC_TIMEZONE)
                .withZoneSameInstant(VIETNAM_TIMEZONE)
                .toLocalDateTime();
    }
}