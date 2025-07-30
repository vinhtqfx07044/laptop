package com.laptoprepair.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

/**
 * Configuration class for application-wide settings.
 * Loads properties from application.properties.
 */
@Configuration
@PropertySource("classpath:application.properties")
public class AppConfig {
}