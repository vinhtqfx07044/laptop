package com.laptoprepair.utils;

import java.time.LocalDateTime;

/**
 * Interface for providing the current time.
 * This abstraction allows for easier testing and mocking of time-dependent logic.
 */
public interface TimeProvider {
    LocalDateTime now();
}