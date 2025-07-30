package com.laptoprepair.config;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.lang.NonNull;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.HashMap;
import java.util.Map;

/**
 * Initializes the Spring application context by loading environment variables from a .env file.
 * This class implements ApplicationContextInitializer to ensure .env variables are available early in the application lifecycle.
 */
public class DotEnvConfig implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    /**
     * Initializes the configurable application context.
     * Loads environment variables from a .env file located in the current directory
     * and adds them as a property source to the Spring environment.
     * @param applicationContext The configurable application context.
     */
    @Override
    public void initialize(@NonNull ConfigurableApplicationContext applicationContext) {
        Dotenv dotenv = Dotenv.configure()
                .directory("./")
                .ignoreIfMalformed()
                .ignoreIfMissing()
                .load();

        ConfigurableEnvironment environment = applicationContext.getEnvironment();
        Map<String, Object> dotenvMap = new HashMap<>();

        dotenv.entries().forEach(entry -> dotenvMap.put(entry.getKey(), entry.getValue()));

        environment.getPropertySources().addFirst(new MapPropertySource("dotenv", dotenvMap));
    }
}