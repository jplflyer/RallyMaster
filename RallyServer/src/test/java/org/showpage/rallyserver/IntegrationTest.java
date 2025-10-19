package org.showpage.rallyserver;

import com.fasterxml.jackson.core.type.TypeReference;
import lombok.extern.slf4j.Slf4j;
import net.datafaker.Faker;
import org.junit.jupiter.api.BeforeEach;
import org.showpage.rallyserver.controller.RallyControllerIT;
import org.showpage.rallyserver.ui.*;
import org.showpage.rallyserver.util.RESTCaller;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.Base64;
import java.util.List;

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
    public static class RR_UiRally extends RestResponse<UiRally> {}
    public static class RR_UiRallyParticipant extends RestResponse<UiRallyParticipant> {}
    public static class RR_UiBonusPoint extends RestResponse<UiBonusPoint> {}
    public static class RR_UiCombination extends RestResponse<UiCombination> {}
    public static class RR_UiCombinationPoint extends RestResponse<UiCombinationPoint> {}
    public static class RR_UiEarnedBonusPoint extends RestResponse<UiEarnedBonusPoint> {}
    public static class RR_UiEarnedCombination extends RestResponse<UiEarnedCombination> {}
    public static class RR_ListUiBonusPoint extends RestResponse<List<UiBonusPoint>> {}
    public static class RR_ListUiCombination extends RestResponse<List<UiCombination>> {}
    public static class RR_ListUiEarnedBonusPoint extends RestResponse<List<UiEarnedBonusPoint>> {}
    public static class RR_ListUiEarnedCombination extends RestResponse<List<UiEarnedCombination>> {}
    public static class RR_Void extends RestResponse<Void> {}

    // Type references for the above.
    public static final TypeReference<RR_AuthResponse> tr_AuthResponse = new TypeReference<>() {};
    public static final TypeReference<RR_PageUiRally> tr_PageUiRally = new TypeReference<>() {};
    public static final TypeReference<RR_UiRally> tr_UiRally = new TypeReference<>() {};
    public static final TypeReference<RR_UiRallyParticipant> tr_UiRallyParticipant = new TypeReference<>() {};
    public static final TypeReference<RR_UiBonusPoint> tr_UiBonusPoint = new TypeReference<>() {};
    public static final TypeReference<RR_UiCombination> tr_UiCombination = new TypeReference<>() {};
    public static final TypeReference<RR_UiCombinationPoint> tr_UiCombinationPoint = new TypeReference<>() {};
    public static final TypeReference<RR_UiEarnedBonusPoint> tr_UiEarnedBonusPoint = new TypeReference<>() {};
    public static final TypeReference<RR_UiEarnedCombination> tr_UiEarnedCombination = new TypeReference<>() {};
    public static final TypeReference<RR_ListUiBonusPoint> tr_ListUiBonusPoint = new TypeReference<>() {};
    public static final TypeReference<RR_ListUiCombination> tr_ListUiCombination = new TypeReference<>() {};
    public static final TypeReference<RR_ListUiEarnedBonusPoint> tr_ListUiEarnedBonusPoint = new TypeReference<>() {};
    public static final TypeReference<RR_ListUiEarnedCombination> tr_ListUiEarnedCombination = new TypeReference<>() {};
    public static final TypeReference<RR_Void> tr_Void = new TypeReference<>() {};

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

    protected static String testUserPassword;

    protected static RESTCaller restCaller;
    protected static Faker faker;

    // Additional test users created by tests
    protected static String aide1Email;
    protected static String aide1AuthHeader;
    protected static String rider2Email;
    protected static String rider2AuthHeader;
    protected static String organizer2Email;
    protected static String organizer2AuthHeader;

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

        // Create REST caller and Faker
        restCaller = new RESTCaller(serverUrl);
        faker = new Faker();

        // Login as both users
        organizerAuthHeader = loginAs(organizerEmail, organizerPassword);
        riderAuthHeader = loginAs(riderEmail, riderPassword);

        // Clean up old test data
        cleanupTestData();

        initialized = true;
    }

    /**
     * Clean up old test data from previous test runs.
     * Deletes all rallies starting with "AutoTest".
     */
    private void cleanupTestData() {
        try {
            log.info("Cleaning up old test data...");

            // Search for all rallies with name starting with "AutoTest"
            RR_PageUiRally response = get_ForRM("/api/rallies?name=AutoTest&all=true", tr_PageUiRally);

            if (response != null && response.isSuccess() && response.getData() != null) {
                List<UiRally> rallies = response.getData().getContent();
                log.info("Found {} test rallies to clean up", rallies.size());

                for (UiRally rally : rallies) {
                    try {
                        log.info("Deleting test rally: {} (ID: {})", rally.getName(), rally.getId());
                        delete_ForRM("/api/rally/" + rally.getId(), tr_Void);
                    } catch (Exception e) {
                        log.warn("Failed to delete rally {}: {}", rally.getId(), e.getMessage());
                    }
                }
            }

            log.info("Test data cleanup complete");
        } catch (Exception e) {
            log.warn("Error during test data cleanup: {}", e.getMessage());
        }
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

    public static void setTestUserPassword(String value) {
        testUserPassword = value;
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

    //----------------------------------------------------------------------
    // Helper Methods for Test Data Creation
    //----------------------------------------------------------------------

    /**
     * Create a test rally with automatic naming and default settings.
     */
    protected UiRally createTestRally() throws Exception {
        return createTestRally(null, null, null);
    }

    /**
     * Create a test rally at a specific location.
     */
    protected UiRally createTestRally(String city, String country, Double latitude, Double longitude) throws Exception {
        CreateRallyRequest request = new CreateRallyRequest();
        request.setName("AutoTest " + faker.company().buzzword() + " Rally");
        request.setDescription("Automated test rally created by integration tests");
        request.setLocationCity(city != null ? city : faker.address().city());
        request.setLocationState(faker.address().state());
        request.setLocationCountry(country != null ? country : "US");
        request.setStartDate(LocalDate.now().plusDays(7));
        request.setEndDate(LocalDate.now().plusDays(14));
        request.setIsPublic(true);
        request.setPointsPublic(true);
        request.setRidersPublic(true);
        request.setOrganizersPublic(true);

        RR_UiRally response = post_ForRM("/api/rally", request, tr_UiRally);
        check(response);
        return response.getData();
    }

    /**
     * Create a test rally with full control over parameters.
     */
    protected UiRally createTestRally(String name, LocalDate startDate, LocalDate endDate) throws Exception {
        CreateRallyRequest request = new CreateRallyRequest();
        request.setName(name != null ? name : ("AutoTest " + faker.company().buzzword() + " Rally"));
        request.setDescription("Automated test rally created by integration tests");
        request.setLocationCity(faker.address().city());
        request.setLocationState(faker.address().state());
        request.setLocationCountry("US");
        request.setStartDate(startDate != null ? startDate : LocalDate.now().plusDays(7));
        request.setEndDate(endDate != null ? endDate : LocalDate.now().plusDays(14));
        request.setIsPublic(true);
        request.setPointsPublic(true);
        request.setRidersPublic(true);
        request.setOrganizersPublic(true);

        RR_UiRally response = post_ForRM("/api/rally", request, tr_UiRally);
        check(response);
        return response.getData();
    }

    /**
     * Create a bonus point for a rally.
     */
    protected UiBonusPoint createTestBonusPoint(Integer rallyId, String code, Integer points) throws Exception {
        CreateBonusPointRequest request = CreateBonusPointRequest
                .builder()
                .code(code != null ? code : ("BP" + faker.number().numberBetween(1, 999)))
                .name(faker.address().cityName() + " Bonus")
                .description("Test bonus point at " + faker.address().fullAddress())
                .latitude(faker.number().randomDouble(6, -90, 90))
                .longitude(faker.number().randomDouble(6, -180, 180))
                .address(faker.address().fullAddress())
                .points(points != null ? points : faker.number().numberBetween(10, 100))
                .required(false)
                .repeatable(false)
                .build();

        RR_UiBonusPoint response = post_ForRM("/api/rally/" + rallyId + "/bonuspoint", request, tr_UiBonusPoint);
        check(response);
        return response.getData();
    }

    /**
     * Create a combination for a rally.
     */
    protected UiCombination createTestCombination(Integer rallyId, List<Integer> bonusPointIds) throws Exception {
        CreateCombinationRequest request = CreateCombinationRequest
                .builder()
                .code("COMBO" + faker.number().numberBetween(1, 99))
                .name(faker.company().buzzword() + " Combination")
                .description("Test combination bonus")
                .points(faker.number().numberBetween(50, 200))
                .requiresAll(true)
                .numRequired(bonusPointIds.size())
                .build();

        // Add combination points
        List<CreateCombinationPointRequest> combinationPoints = bonusPointIds.stream()
                .map(bpId -> {
                    CreateCombinationPointRequest cp = new CreateCombinationPointRequest();
                    cp.setBonusPointId(bpId);
                    cp.setRequired(true);
                    return cp;
                })
                .toList();
        request.setCombinationPoints(combinationPoints);

        RR_UiCombination response = post_ForRM("/api/rally/" + rallyId + "/combination", request, tr_UiCombination);
        check(response);
        return response.getData();
    }

    /**
     * Generate a test user email using DataFaker.
     */
    protected String generateTestUserEmail() {
        String firstName = faker.name().firstName().toLowerCase().replaceAll("[^a-z]", "");
        String lastName = faker.name().lastName().toLowerCase().replaceAll("[^a-z]", "");
        String company = faker.company().name().replaceAll("[^a-zA-Z]", "").substring(0, Math.min(5, faker.company().name().length())).toLowerCase();
        return firstName + "." + lastName + "@" + company + ".nowhere.com";
    }
}
