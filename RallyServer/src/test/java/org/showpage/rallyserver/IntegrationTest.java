package org.showpage.rallyserver;

import com.fasterxml.jackson.core.type.TypeReference;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.showpage.rallyserver.controller.RallyControllerIT;
import org.showpage.rallyserver.ui.AuthResponse;
import org.showpage.rallyserver.ui.PageResponse;
import org.showpage.rallyserver.ui.UiRally;
import org.showpage.rallyserver.util.RESTCaller;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * Base class for all integration tests.
 * Provides common REST calling infrastructure, authentication, and property loading.
 */
@Slf4j
public abstract class IntegrationTest {

    // Helper classes that just make the code a little more compact.
    public static class RR_AuthResponse extends RestResponse<AuthResponse> {}
    public static class RR_PageUiRally extends RestResponse<PageResponse<UiRally>> {}

    // Type references for the above.
    public static final TypeReference<RR_AuthResponse> tr_AuthResponse = new TypeReference<>() {};
    public static final TypeReference<RR_PageUiRally> tr_PageUiRally = new TypeReference<>() {};

    //----------------------------------------------------------------------
    // Variables
    //----------------------------------------------------------------------
    protected static boolean initialized = false;
    protected static String serverUrl;
    protected static String organizerEmail;
    protected static String organizerPassword;
    protected static String riderEmail;
    protected static String riderPassword;
    protected static String organizerAuthHeader;
    protected static String riderAuthHeader;

    protected static RESTCaller restCaller;

    /**
     * Step B: Setup method called before each test.
     */
    @BeforeEach
    public void setup() throws Exception {
        initialize();
    }

    /**
     * Step B: Initialize method - loads properties and logs in users.
     * Only runs once per test suite.
     */
    protected void initialize() throws Exception {
        if (initialized) {
            return;
        }

        // Load integration properties
        loadIntegrationProperties();

        // Create REST caller
        restCaller = new RESTCaller(serverUrl);

        // Login as both users
        organizerAuthHeader = loginAs(organizerEmail, organizerPassword);
        riderAuthHeader = loginAs(riderEmail, riderPassword);

        initialized = true;
    }

    /**
     * Load properties from .integration.properties file using reflection.
     */
    private void loadIntegrationProperties() throws Exception {
        Path propertiesPath = Paths.get(".integration.properties");

        try (BufferedReader reader = new BufferedReader(new FileReader(propertiesPath.toFile()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();

                // Skip blank lines and comments
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                // Parse property
                int equalsIndex = line.indexOf('=');
                if (equalsIndex > 0) {
                    String key = line.substring(0, equalsIndex).trim();
                    String value = line.substring(equalsIndex + 1).trim();

                    // Convert snake_case to camelCase for setter method
                    String setterName = "set" + snakeToCamel(key);

                    // Use reflection to set the field
                    try {
                        Method setter = this.getClass().getMethod(setterName, String.class);
                        setter.invoke(this, value);
                    } catch (NoSuchMethodException e) {
                        // Try static field setter
                        try {
                            Method setter = IntegrationTest.class.getDeclaredMethod(setterName, String.class);
                            setter.setAccessible(true);
                            setter.invoke(null, value);
                        } catch (NoSuchMethodException ex) {
                            System.err.println("Warning: No setter found for property: " + key);
                        }
                    }
                }
            }
        } catch (IOException e) {
            fail("Could not load .integration.properties file: " + e.getMessage());
        }
    }

    /**
     * Convert snake_case to camelCase (e.g., "server_url" -> "ServerUrl")
     */
    private String snakeToCamel(String snake) {
        StringBuilder result = new StringBuilder();
        boolean capitalizeNext = true;

        for (char c : snake.toCharArray()) {
            if (c == '_') {
                capitalizeNext = true;
            } else {
                if (capitalizeNext) {
                    result.append(Character.toUpperCase(c));
                    capitalizeNext = false;
                } else {
                    result.append(c);
                }
            }
        }

        return result.toString();
    }

    // Setters for reflection-based property loading
    public static void setServerUrl(String value) {
        serverUrl = value;
    }

    public static void setOrganizerEmail(String value) {
        organizerEmail = value;
    }

    public static void setOrganizerPassword(String value) {
        organizerPassword = value;
    }

    public static void setRiderEmail(String value) {
        riderEmail = value;
    }

    public static void setRiderPassword(String value) {
        riderPassword = value;
    }

    /**
     * Step D: Login as a user and return the Bearer authorization header.
     */
    protected String loginAs(String username, String password) throws IOException, InterruptedException {
        log.info("Attempting to authenticate user: {} pw: {}", username, password);

        // Create Basic auth header
        String credentials = username + ":" + password;
        String basicAuth = "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes());

        // Call login endpoint
        RR_AuthResponse response = restCaller.post("/api/auth/login", null, basicAuth, tr_AuthResponse);

        if (response == null || !response.isSuccess() || response.getData() == null) {
            fail("Login failed for user: " + username);
        }

        // Return Bearer token
        return "Bearer " + response.getData().getAccessToken();
    }

    /**
     * Step C: Check that a RestResponse is valid (not null and success=true).
     */
    protected void check(RestResponse<?> response) {
        if (response == null) {
            fail("null return");
        }
        if (!response.isSuccess()) {
            fail(response.getMessage());
        }
    }

    // Step E: REST methods for Rally Master (Organizer)
    protected <T> T get_ForRM(String path, TypeReference<T> typeRef) throws IOException, InterruptedException {
        return restCaller.get(path, organizerAuthHeader, typeRef);
    }

    protected <T> T post_ForRM(String path, Object body, TypeReference<T> typeRef) throws IOException, InterruptedException {
        return restCaller.post(path, body, organizerAuthHeader, typeRef);
    }

    protected <T> T put_ForRM(String path, Object body, TypeReference<T> typeRef) throws IOException, InterruptedException {
        return restCaller.put(path, body, organizerAuthHeader, typeRef);
    }

    protected <T> T delete_ForRM(String path, TypeReference<T> typeRef) throws IOException, InterruptedException {
        return restCaller.delete(path, organizerAuthHeader, typeRef);
    }

    // Step E: REST methods for Rider
    protected <T> T get_ForRider(String path, TypeReference<T> typeRef) throws IOException, InterruptedException {
        return restCaller.get(path, riderAuthHeader, typeRef);
    }

    protected <T> T post_ForRider(String path, Object body, TypeReference<T> typeRef) throws IOException, InterruptedException {
        return restCaller.post(path, body, riderAuthHeader, typeRef);
    }

    protected <T> T put_ForRider(String path, Object body, TypeReference<T> typeRef) throws IOException, InterruptedException {
        return restCaller.put(path, body, riderAuthHeader, typeRef);
    }

    protected <T> T delete_ForRider(String path, TypeReference<T> typeRef) throws IOException, InterruptedException {
        return restCaller.delete(path, riderAuthHeader, typeRef);
    }
}
