package com.laptoprepair.service;

/**
 * Service interface for authentication-related operations.
 */
public interface AuthService {
    boolean isStaff();

    String getCurrentUsername();
}