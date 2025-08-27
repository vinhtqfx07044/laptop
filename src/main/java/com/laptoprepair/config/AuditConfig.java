package com.laptoprepair.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;

@Configuration
public class AuditConfig {

    /**
     * Custom DateTimeProvider that provides current time in Vietnam timezone.
     */
    @Bean(name = "dateTimeProvider")
    public DateTimeProvider dateTimeProvider() {
        return () -> Optional.of(vietnamTime());
    }

    /**
     * AuditorAware implementation that returns current user or "Public" for guests.
     */
    @Bean
    public AuditorAware<String> auditorAware() {
        return () -> {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String username = (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName()))
                    ? auth.getName()
                    : "Public";
            return Optional.of(username);
        };
    }

    /**
     * Get current Vietnam time - same logic as VietnamTimeProvider
     */
    private LocalDateTime vietnamTime() {
        return ZonedDateTime.now(ZoneId.of("UTC"))
                .withZoneSameInstant(ZoneId.of("Asia/Ho_Chi_Minh"))
                .toLocalDateTime();
    }
}