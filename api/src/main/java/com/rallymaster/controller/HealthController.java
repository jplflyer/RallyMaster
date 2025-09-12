package com.rallymaster.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

/**
 * Simple health check controller for verifying the API is running.
 * 
 * This controller provides a basic endpoint that can be used to verify
 * the Spring Boot application is successfully initialized and serving requests.
 */
@RestController
@RequestMapping("/health")
public class HealthController {

    @GetMapping
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "timestamp", Instant.now(),
            "service", "Rally Master API",
            "version", "1.0.0"
        ));
    }
}