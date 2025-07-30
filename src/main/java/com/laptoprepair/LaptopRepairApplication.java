package com.laptoprepair;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main entry point for the Laptop Repair Application.
 * This class configures and runs the Spring Boot application.
 */
@SpringBootApplication
@ComponentScan(basePackages = { "com.laptoprepair" })
@EnableAsync
@EnableScheduling
@EnableJpaAuditing
public class LaptopRepairApplication {

    /**
     * The main method that starts the Spring Boot application.
     * @param args Command line arguments.
     */
    public static void main(String[] args) {
        SpringApplication.run(LaptopRepairApplication.class, args);
    }
}