package com.laptoprepair.utils;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * Default implementation of {@link TimeProvider} that provides the current time
 * in Vietnam's timezone (Asia/Ho_Chi_Minh).
 */
@Component
public class DefaultTimeProvider implements TimeProvider {

    private static final ZoneId VIETNAM_TIMEZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final ZoneId UTC_TIMEZONE = ZoneId.of("UTC");

    /**
     * Returns the current local date and time in Vietnam's timezone.
     * @return A LocalDateTime object representing the current time in Vietnam.
     */
    @Override
    public LocalDateTime now() {
        return ZonedDateTime.now(UTC_TIMEZONE)
                .withZoneSameInstant(VIETNAM_TIMEZONE)
                .toLocalDateTime();
    }

    /**
     * Static method to get the current local date and time in Vietnam's timezone.
     * This is primarily for backward compatibility with JPA entities that might not have access to the Spring context.
     * @return A LocalDateTime object representing the current time in Vietnam.
     */
    public static LocalDateTime nowInVietnam() {
        return ZonedDateTime.now(UTC_TIMEZONE)
                .withZoneSameInstant(VIETNAM_TIMEZONE)
                .toLocalDateTime();
    }
}