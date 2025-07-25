package com.laptoprepair.util;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public class TimeZoneUtil {

    private TimeZoneUtil() {
    }

    public static final ZoneId VIETNAM_TIMEZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    public static final ZoneId UTC_TIMEZONE = ZoneId.of("UTC");

    public static LocalDateTime nowInVietnam() {
        return ZonedDateTime.now(UTC_TIMEZONE)
                .withZoneSameInstant(VIETNAM_TIMEZONE)
                .toLocalDateTime();
    }

}