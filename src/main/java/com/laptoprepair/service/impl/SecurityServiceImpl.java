package com.laptoprepair.service.impl;

import com.laptoprepair.service.SecurityService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Implementation of SecurityService providing security-related operations.
 */
@Service
public class SecurityServiceImpl implements SecurityService {

    private static final String ANONYMOUS_USER = "anonymous";

    /**
     * Get the current authenticated user's Authentication object.
     *
     * @return Optional containing Authentication if user is authenticated, empty
     *         otherwise
     */
    @Override
    public Optional<Authentication> getCurrentAuthentication() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
                && !ANONYMOUS_USER.equals(authentication.getName())
                && !"anonymousUser".equals(authentication.getName())) {
            return Optional.of(authentication);
        }
        return Optional.empty();
    }

    /**
     * Get the current authenticated user's username.
     * Can be used for audit, logging, or any other purpose.
     *
     * @return Username if authenticated, "anonymous" otherwise
     */
    @Override
    public String getCurrentUsername() {
        return getCurrentAuthentication()
                .map(Authentication::getName)
                .orElse(ANONYMOUS_USER);
    }

    /**
     * Check if a given Authentication has a specific role.
     * Null-safe - returns false if authentication is null.
     *
     * @param authentication User authentication (can be null)
     * @param role           Role to check (e.g., "ROLE_STAFF")
     * @return true if authentication has the role, false otherwise
     */
    @Override
    public boolean hasRole(Authentication authentication, String role) {
        if (authentication == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority -> authority.equals(role));
    }
}