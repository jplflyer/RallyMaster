package com.rallymaster;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Smoke test to verify Spring Boot application context loads successfully.
 * 
 * This test ensures that:
 * - Spring Boot application starts without errors
 * - All autowired dependencies resolve correctly
 * 
 * Note: This test uses H2 in-memory database for simplicity.
 * Integration tests with PostgreSQL/PostGIS will use Testcontainers.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test-h2")
class RallyMasterApplicationTest {

    @Test
    void contextLoads() {
        // This test passes if the Spring application context loads successfully
        // with all beans autowired
        assertTrue(true, "Spring Boot application context loaded successfully");
    }
}