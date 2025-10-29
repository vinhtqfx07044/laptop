package com.laptoprepair.config;

import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.concurrent.Executor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.format.datetime.standard.DateTimeFormatterRegistrar;
import org.springframework.lang.NonNull;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.laptoprepair.interceptor.RateLimitInterceptor;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableAsync
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final @NonNull RateLimitInterceptor rateLimitInterceptor;

    private static final String IMAGE_DIRECTORY = "images";

    @Override
    public void addResourceHandlers(@NonNull ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/images/**")
                .addResourceLocations("file:" + IMAGE_DIRECTORY + "/");
    }

    @Override
    public void addFormatters(@NonNull FormatterRegistry registry) {
        DateTimeFormatterRegistrar registrar = new DateTimeFormatterRegistrar();
        DateTimeFormatter formatter = Objects.requireNonNull(
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"));
        registrar.setDateTimeFormatter(formatter);
        registrar.registerFormatters(registry);
    }

    @Override
    public void addInterceptors(@NonNull InterceptorRegistry registry) {
        registry.addInterceptor(Objects.requireNonNull(rateLimitInterceptor))
                .addPathPatterns("/submit", "/lookup", "/recover", "/login");
    }

    @Bean(name = { "emailTaskExecutor", "documentEtlExecutor" })
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setThreadNamePrefix("Background-");
        executor.initialize();
        return executor;
    }
}
