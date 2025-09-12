package com.rallymaster;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Main Spring Boot application class for Rally Master API.
 * 
 * This application provides REST API endpoints for motorcycle rally management,
 * including rally creation, rider registration, route planning, and scoring.
 */
@SpringBootApplication
@EnableJpaAuditing
public class RallyMasterApplication {

    public static void main(String[] args) {
        SpringApplication.run(RallyMasterApplication.class, args);
    }
}