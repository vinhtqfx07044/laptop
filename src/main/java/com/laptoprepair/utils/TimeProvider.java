package com.laptoprepair.utils;

import java.time.LocalDateTime;

public interface TimeProvider {
    LocalDateTime now();
}