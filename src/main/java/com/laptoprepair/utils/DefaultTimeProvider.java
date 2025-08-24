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

    @Override
    public LocalDateTime now() {
        return ZonedDateTime.now(UTC_TIMEZONE)
                .withZoneSameInstant(VIETNAM_TIMEZONE)
                .toLocalDateTime();
    }


}