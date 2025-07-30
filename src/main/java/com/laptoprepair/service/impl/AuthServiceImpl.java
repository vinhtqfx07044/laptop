package com.laptoprepair.service.impl;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.laptoprepair.service.AuthService;

/**
 * Implementation of the {@link AuthService} interface.
 * Provides methods for checking user roles and retrieving the current authenticated user.
 */
@Service
public class AuthServiceImpl implements AuthService {

    /**
     * Checks if the current authenticated user has the 'STAFF' role.
     * @return true if the user is staff, false otherwise.
     */
    @Override
    public boolean isStaff() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.isAuthenticated() &&
                auth.getAuthorities().stream()
                        .anyMatch(a -> a.getAuthority().equals("ROLE_STAFF"));
    }

    /**
     * Retrieves the username of the current authenticated user.
     * Returns "Public" if no user is authenticated or if the user is anonymous.
     * @return The username of the current user.
     */
    @Override
    public String currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName()))
                ? auth.getName()
                : "Public";
    }
}