package com.laptoprepair.exception;

/**
 * Custom exception for validation failures.
 * This exception is typically thrown when business logic validation fails.
 */
public class ValidationException extends RuntimeException {
    public ValidationException(String message) {
        super(message);
    }
}