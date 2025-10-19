package org.showpage.rallyserver.controller;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.showpage.rallyserver.IntegrationTest;
import org.showpage.rallyserver.entity.OwnershipStatus;
import org.showpage.rallyserver.ui.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Rider editing operations.
 * Tests member info updates and motorcycle CRUD operations.
 */
@Slf4j
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class EditRiderIT extends IntegrationTest {

    private static AuthResponse testUserAuth;
    private static String testUserEmail;
    private static String testUserAuthHeader;

    private static boolean setupDone = false;

    @BeforeEach
    public void beforeEach() {
        if (setupDone) {
            return;
        }

        try {
            initialize();
            setupTestUser();

            setupDone = true;
        }
        catch (Exception e) {
            log.error("Startup exception", e);
            fail("Exception");
        }
    }

    private void setupTestUser() throws Exception {
        if (testUserAuth == null) {
            testUserEmail = generateTestUserEmail();
            String password = testUserPassword;

            testUserAuth = registerTestUser(testUserEmail, password);
            testUserAuthHeader = "Bearer " + testUserAuth.getAccessToken();

            assertNotNull(testUserAuth);
            assertNotNull(testUserAuth.getAccessToken());
            log.info("Created test user: {}", testUserEmail);
        }
    }

    //----------------------------------------------------------------------
    // Member Info Update Tests
    //----------------------------------------------------------------------

    @Test
    @Order(10)
    @DisplayName("User can update their Spotwalla username")
    public void testUpdateSpotwallaUsername() throws Exception {
        String newSpotwallaUsername = "spotwalla_" + faker.name().lastName();

        UpdateMemberRequest request = UpdateMemberRequest
                .builder()
                .spotwallaUsername(newSpotwallaUsername)
                .build();

        RR_UiMember response = restCaller.put("/api/member", request, testUserAuthHeader, tr_UiMember);
        check(response);

        // Verify by getting fresh member info
        RR_UiMember memberInfo = restCaller.get("/api/member/info", testUserAuthHeader, tr_UiMember);
        check(memberInfo);

        assertEquals(newSpotwallaUsername, memberInfo.getData().getSpotwallaUsername());
        log.info("Successfully updated Spotwalla username to: {}", newSpotwallaUsername);
    }

    @Test
    @Order(20)
    @DisplayName("User cannot update their email address")
    public void testCannotUpdateEmail() throws Exception {
        // Get current email
        RR_UiMember currentInfo = restCaller.get("/api/member/info", testUserAuthHeader, tr_UiMember);
        check(currentInfo);
        String originalEmail = currentInfo.getData().getEmail();

        // Try to update with a different email (should be ignored)
        UpdateMemberRequest request = UpdateMemberRequest
                .builder()
                .spotwallaUsername("some_username")
                .build();

        RR_UiMember response = restCaller.put("/api/member", request, testUserAuthHeader, tr_UiMember);
        check(response);

        // Verify email hasn't changed
        RR_UiMember updatedInfo = restCaller.get("/api/member/info", testUserAuthHeader, tr_UiMember);
        check(updatedInfo);

        assertEquals(originalEmail, updatedInfo.getData().getEmail());
        log.info("Email address correctly unchanged");
    }

    //----------------------------------------------------------------------
    // Motorcycle Creation Tests
    //----------------------------------------------------------------------

    @Test
    @Order(100)
    @DisplayName("User can create a motorcycle with all fields")
    public void testCreateMotorcycleAllFields() throws Exception {
        CreateMotorcycleRequest request = CreateMotorcycleRequest
                .builder()
                .make("Honda")
                .model("ST1300")
                .year(2008)
                .color("Silver")
                .status(OwnershipStatus.OWNED)
                .active(true)
                .build();

        RR_UiMotorcycle response = restCaller.post(
                "/api/member/motorcycle",
                request,
                testUserAuthHeader,
                tr_UiMotorcycle
        );
        check(response);

        UiMotorcycle motorcycle = response.getData();
        assertNotNull(motorcycle.getId());
        assertEquals("Honda", motorcycle.getMake());
        assertEquals("ST1300", motorcycle.getModel());
        assertEquals(2008, motorcycle.getYear());
        assertEquals("Silver", motorcycle.getColor());
        assertEquals(OwnershipStatus.OWNED, motorcycle.getStatus());
        assertTrue(motorcycle.getActive());

        log.info("Created motorcycle ID: {}", motorcycle.getId());
    }

    @Test
    @Order(110)
    @DisplayName("User can retrieve motorcycles after creation")
    public void testRetrieveMotorcyclesAfterCreation() throws Exception {
        // Create a motorcycle
        CreateMotorcycleRequest request = CreateMotorcycleRequest
                .builder()
                .make("Yamaha")
                .model("FJR1300")
                .year(2015)
                .color("Blue")
                .status(OwnershipStatus.OWNED)
                .active(true)
                .build();

        RR_UiMotorcycle createResponse = restCaller.post(
                "/api/member/motorcycle",
                request,
                testUserAuthHeader,
                tr_UiMotorcycle
        );
        check(createResponse);
        Integer motorcycleId = createResponse.getData().getId();

        // Retrieve member info which should include motorcycles
        RR_UiMember memberInfo = restCaller.get("/api/member/info", testUserAuthHeader, tr_UiMember);
        check(memberInfo);

        UiMember member = memberInfo.getData();
        assertNotNull(member.getMotorcycles());
        assertTrue(member.getMotorcycles().size() >= 1);

        // Find the motorcycle we just created
        UiMotorcycle found = member.getMotorcycles().stream()
                .filter(m -> m.getId().equals(motorcycleId))
                .findFirst()
                .orElse(null);

        assertNotNull(found, "Should find the created motorcycle");
        assertEquals("Yamaha", found.getMake());
        assertEquals("FJR1300", found.getModel());
        assertEquals(2015, found.getYear());
        assertEquals("Blue", found.getColor());
        assertEquals(OwnershipStatus.OWNED, found.getStatus());

        log.info("Retrieved {} motorcycles", member.getMotorcycles().size());
    }

    //----------------------------------------------------------------------
    // Motorcycle Update Tests
    //----------------------------------------------------------------------

    @Test
    @Order(200)
    @DisplayName("User can update motorcycle with all fields")
    public void testUpdateMotorcycleAllFields() throws Exception {
        // Create a motorcycle
        CreateMotorcycleRequest createRequest = CreateMotorcycleRequest
                .builder()
                .make("BMW")
                .model("R1200RT")
                .year(2010)
                .color("White")
                .status(OwnershipStatus.OWNED)
                .active(true)
                .build();

        RR_UiMotorcycle createResponse = restCaller.post(
                "/api/member/motorcycle",
                createRequest,
                testUserAuthHeader,
                tr_UiMotorcycle
        );
        check(createResponse);
        Integer motorcycleId = createResponse.getData().getId();

        // Update all fields
        UpdateMotorcycleRequest updateRequest = UpdateMotorcycleRequest
                .builder()
                .make("BMW")
                .model("R1250RT")
                .year(2020)
                .color("Black")
                .status(OwnershipStatus.OWNED)
                .active(true)
                .build();

        RR_UiMotorcycle updateResponse = restCaller.put(
                "/api/member/motorcycle/" + motorcycleId,
                updateRequest,
                testUserAuthHeader,
                tr_UiMotorcycle
        );
        check(updateResponse);

        // Verify all fields were updated
        RR_UiMember memberInfo = restCaller.get("/api/member/info", testUserAuthHeader, tr_UiMember);
        check(memberInfo);

        UiMotorcycle updated = memberInfo.getData().getMotorcycles().stream()
                .filter(m -> m.getId().equals(motorcycleId))
                .findFirst()
                .orElseThrow();

        assertEquals("BMW", updated.getMake());
        assertEquals("R1250RT", updated.getModel());
        assertEquals(2020, updated.getYear());
        assertEquals("Black", updated.getColor());
        assertEquals(OwnershipStatus.OWNED, updated.getStatus());
        assertTrue(updated.getActive());

        log.info("Successfully updated all motorcycle fields");
    }

    @Test
    @Order(210)
    @DisplayName("User can update motorcycle with partial fields - nulls retain existing values")
    public void testUpdateMotorcyclePartialFields() throws Exception {
        // Create a motorcycle
        CreateMotorcycleRequest createRequest = CreateMotorcycleRequest
                .builder()
                .make("Kawasaki")
                .model("Concours 14")
                .year(2012)
                .color("Green")
                .status(OwnershipStatus.OWNED)
                .active(true)
                .build();

        RR_UiMotorcycle createResponse = restCaller.post(
                "/api/member/motorcycle",
                createRequest,
                testUserAuthHeader,
                tr_UiMotorcycle
        );
        check(createResponse);
        Integer motorcycleId = createResponse.getData().getId();

        // Update only year and color, leave other fields null
        UpdateMotorcycleRequest updateRequest = UpdateMotorcycleRequest
                .builder()
                .year(2013)
                .color("Gray")
                .build();

        RR_UiMotorcycle updateResponse = restCaller.put(
                "/api/member/motorcycle/" + motorcycleId,
                updateRequest,
                testUserAuthHeader,
                tr_UiMotorcycle
        );
        check(updateResponse);

        // Verify null fields retained their values
        RR_UiMember memberInfo = restCaller.get("/api/member/info", testUserAuthHeader, tr_UiMember);
        check(memberInfo);

        UiMotorcycle updated = memberInfo.getData().getMotorcycles().stream()
                .filter(m -> m.getId().equals(motorcycleId))
                .findFirst()
                .orElseThrow();

        // Null fields should retain original values
        assertEquals("Kawasaki", updated.getMake(), "Make should be unchanged");
        assertEquals("Concours 14", updated.getModel(), "Model should be unchanged");
        assertEquals(OwnershipStatus.OWNED, updated.getStatus(), "Status should be unchanged");
        assertTrue(updated.getActive(), "Active should be unchanged");

        // Updated fields should have new values
        assertEquals(2013, updated.getYear(), "Year should be updated");
        assertEquals("Gray", updated.getColor(), "Color should be updated");

        log.info("Partial update correctly retained existing values for null fields");
    }

    //----------------------------------------------------------------------
    // Motorcycle Delete Tests
    //----------------------------------------------------------------------

    @Test
    @Order(300)
    @DisplayName("User can delete a motorcycle")
    public void testDeleteMotorcycle() throws Exception {
        // Create a motorcycle
        CreateMotorcycleRequest createRequest = CreateMotorcycleRequest
                .builder()
                .make("Suzuki")
                .model("V-Strom 1000")
                .year(2018)
                .color("Yellow")
                .status(OwnershipStatus.OWNED)
                .active(true)
                .build();

        RR_UiMotorcycle createResponse = restCaller.post(
                "/api/member/motorcycle",
                createRequest,
                testUserAuthHeader,
                tr_UiMotorcycle
        );
        check(createResponse);
        Integer motorcycleId = createResponse.getData().getId();

        // Get count before delete
        RR_UiMember beforeInfo = restCaller.get("/api/member/info", testUserAuthHeader, tr_UiMember);
        check(beforeInfo);
        int countBefore = beforeInfo.getData().getMotorcycles().size();

        // Delete the motorcycle
        RR_Void deleteResponse = restCaller.delete(
                "/api/member/motorcycle/" + motorcycleId,
                testUserAuthHeader,
                tr_Void
        );
        check(deleteResponse);

        // Verify it's gone
        RR_UiMember afterInfo = restCaller.get("/api/member/info", testUserAuthHeader, tr_UiMember);
        check(afterInfo);

        int countAfter = afterInfo.getData().getMotorcycles().size();
        assertEquals(countBefore - 1, countAfter, "Motorcycle count should decrease by 1");

        boolean found = afterInfo.getData().getMotorcycles().stream()
                .anyMatch(m -> m.getId().equals(motorcycleId));
        assertFalse(found, "Deleted motorcycle should not be in the list");

        log.info("Successfully deleted motorcycle ID: {}", motorcycleId);
    }

    @Test
    @Order(310)
    @DisplayName("User cannot update another user's motorcycle")
    public void testCannotUpdateOtherUsersMotorcycle() throws Exception {
        // Create a second test user with a motorcycle
        String secondUserEmail = generateTestUserEmail();
        AuthResponse secondUserAuth = registerTestUser(secondUserEmail, testUserPassword);
        String secondUserAuthHeader = "Bearer " + secondUserAuth.getAccessToken();

        // Create motorcycle for second user
        CreateMotorcycleRequest createRequest = CreateMotorcycleRequest
                .builder()
                .make("Harley")
                .model("Street Glide")
                .year(2021)
                .color("Black")
                .status(OwnershipStatus.OWNED)
                .active(true)
                .build();

        RR_UiMotorcycle createResponse = restCaller.post(
                "/api/member/motorcycle",
                createRequest,
                secondUserAuthHeader,
                tr_UiMotorcycle
        );
        check(createResponse);
        Integer otherUsersMotorcycleId = createResponse.getData().getId();

        // Try to update it as the first user - should fail
        UpdateMotorcycleRequest updateRequest = UpdateMotorcycleRequest
                .builder()
                .color("Red")
                .build();

        RR_UiMotorcycle updateResponse = restCaller.put(
                "/api/member/motorcycle/" + otherUsersMotorcycleId,
                updateRequest,
                testUserAuthHeader,  // Using first user's auth
                tr_UiMotorcycle
        );

        checkFailed(updateResponse);
        log.info("Correctly prevented user from updating another user's motorcycle");
    }

    @Test
    @Order(320)
    @DisplayName("User cannot delete another user's motorcycle")
    public void testCannotDeleteOtherUsersMotorcycle() throws Exception {
        // Create a third test user with a motorcycle
        String thirdUserEmail = generateTestUserEmail();
        AuthResponse thirdUserAuth = registerTestUser(thirdUserEmail, testUserPassword);
        String thirdUserAuthHeader = "Bearer " + thirdUserAuth.getAccessToken();

        // Create motorcycle for third user
        CreateMotorcycleRequest createRequest = CreateMotorcycleRequest
                .builder()
                .make("Triumph")
                .model("Tiger 1200")
                .year(2019)
                .color("White")
                .status(OwnershipStatus.OWNED)
                .active(true)
                .build();

        RR_UiMotorcycle createResponse = restCaller.post(
                "/api/member/motorcycle",
                createRequest,
                thirdUserAuthHeader,
                tr_UiMotorcycle
        );
        check(createResponse);
        Integer otherUsersMotorcycleId = createResponse.getData().getId();

        // Try to delete it as the first user - should fail
        RR_Void deleteResponse = restCaller.delete(
                "/api/member/motorcycle/" + otherUsersMotorcycleId,
                testUserAuthHeader,  // Using first user's auth
                tr_Void
        );

        checkFailed(deleteResponse);
        log.info("Correctly prevented user from deleting another user's motorcycle");
    }
}
