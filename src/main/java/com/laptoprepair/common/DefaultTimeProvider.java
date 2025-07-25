package com.laptoprepair.common;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

@Component
public class DefaultTimeProvider implements TimeProvider {

    private static final ZoneId VIETNAM_TIMEZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final ZoneId UTC_TIMEZONE = ZoneId.of("UTC");

    @Override
    public LocalDateTime now() {
        return ZonedDateTime.now(UTC_TIMEZONE)
                .withZoneSameInstant(VIETNAM_TIMEZONE)
                .toLocalDateTime();
    }

    // Static method for backward compatibility with JPA entities
    public static LocalDateTime nowInVietnam() {
        return ZonedDateTime.now(UTC_TIMEZONE)
                .withZoneSameInstant(VIETNAM_TIMEZONE)
                .toLocalDateTime();
    }
}