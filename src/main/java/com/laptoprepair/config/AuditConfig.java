package com.laptoprepair.config;

import java.util.Optional;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.domain.AuditorAware;

import com.laptoprepair.service.SecurityService;
import com.laptoprepair.utils.TimeUtils;

@Configuration
public class AuditConfig {

    private final SecurityService securityService;

    public AuditConfig(SecurityService securityService) {
        this.securityService = securityService;
    }

    /**
     * Custom DateTimeProvider that provides current time in Vietnam timezone.
     * Uses the centralized TimeUtils for consistency.
     */
    @Bean(name = "dateTimeProvider")
    public DateTimeProvider dateTimeProvider() {
        return () -> Optional.of(TimeUtils.nowInVietnam());
    }

    /**
     * AuditorAware implementation that returns current username.
     */
    @Bean
    public AuditorAware<String> auditorAware() {
        return () -> Optional.of(securityService.getCurrentUsername());
    }
}