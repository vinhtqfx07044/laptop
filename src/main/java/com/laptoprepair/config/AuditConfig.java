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
     * AuditorAware implementation that returns current username or "anonymousUser" for unauthenticated users.
     */
    @Bean
    public AuditorAware<String> auditorAware() {
        return () -> {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            return Optional.of(auth != null ? auth.getName() : "anonymous User");
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