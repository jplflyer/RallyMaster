package com.rallymaster.integration;

import com.rallymaster.config.TestContainerConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * Base class for integration tests.
 * 
 * Provides:
 * - Full Spring Boot application context
 * - PostgreSQL container via Testcontainers
 * - Random port for web server
 * - Transaction rollback for test isolation
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Transactional
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public abstract class BaseIntegrationTest extends TestContainerConfiguration {

    @LocalServerPort
    protected int port;

    protected String baseUrl;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port + "/api/v1";
    }
    
    /**
     * Get the base URL for API calls in tests.
     * @return Base URL including port and context path
     */
    protected String getBaseUrl() {
        return baseUrl;
    }
    
    /**
     * Get the server port for this test instance.
     * @return Random port assigned by Spring Boot
     */
    protected int getPort() {
        return port;
    }
}