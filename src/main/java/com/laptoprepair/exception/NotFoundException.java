package com.laptoprepair.exception;

/**
 * Custom exception to indicate that a requested resource was not found.
 */
public class NotFoundException extends RuntimeException {
    public NotFoundException(String message) {
        super(message);
    }
}