package com.laptoprepair.config;

import com.laptoprepair.interceptor.RateLimitInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.format.datetime.standard.DateTimeFormatterRegistrar;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.format.DateTimeFormatter;

/**
 * Web configuration for the Laptop Repair Application.
 * Configures resource handlers, formatters, and interceptors.
 */
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final RateLimitInterceptor rateLimitInterceptor;

    /**
     * Configures resource handlers to serve static resources.
     * Specifically, maps "/images/**" to the "uploads/" directory.
     * @param registry The ResourceHandlerRegistry to configure.
     */
    @Override
    public void addResourceHandlers(@org.springframework.lang.NonNull ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/images/**")
                .addResourceLocations("file:uploads/");
    }

    /**
     * Adds custom formatters to the registry.
     * Registers a DateTimeFormatter for "yyyy-MM-dd'T'HH:mm" pattern.
     * @param registry The FormatterRegistry to configure.
     */
    @Override
    public void addFormatters(@org.springframework.lang.NonNull FormatterRegistry registry) {
        DateTimeFormatterRegistrar registrar = new DateTimeFormatterRegistrar();
        // Format for datetime-local input (ISO format)
        registrar.setDateTimeFormatter(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"));
        registrar.registerFormatters(registry);
    }

    /**
     * Adds interceptors to the registry.
     * Registers the RateLimitInterceptor for specific paths.
     * @param registry The InterceptorRegistry to configure.
     */
    @Override
    public void addInterceptors(@org.springframework.lang.NonNull InterceptorRegistry registry) {
        registry.addInterceptor(rateLimitInterceptor)
                .addPathPatterns("/submit", "/lookup", "/recover", "/login");
    }

}