package com.laptoprepair.common;

import java.time.LocalDateTime;

public interface TimeProvider {
    LocalDateTime now();
}