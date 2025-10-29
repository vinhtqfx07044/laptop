package com.laptoprepair.service;

import org.springframework.security.core.Authentication;

import java.util.Optional;

/**
 * Service interface for security-related operations.
 * Provides methods to access current authentication information and check user
 * roles.
 */
public interface SecurityService {
    Optional<Authentication> getCurrentAuthentication();

    String getCurrentUsername();

    boolean hasRole(Authentication authentication, String role);
}