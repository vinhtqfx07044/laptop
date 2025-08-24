package com.laptoprepair;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@ComponentScan(basePackages = { "com.laptoprepair" })
@EnableAsync
@EnableScheduling
@EnableJpaAuditing
public class LaptopRepairApplication {

    public static void main(String[] args) {
        SpringApplication.run(LaptopRepairApplication.class, args);
    }
}