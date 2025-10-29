package com.laptoprepair.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Custom exception for system-level errors.
 * Used for rate limiting, storage failures, and other system issues.
 */
@ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
public class SystemException extends RuntimeException {

    private final String errorCode;

    public SystemException(String message) {
        super(message);
        this.errorCode = null;
    }

    public SystemException(String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}