package org.showpage.rallyserver.controller;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.showpage.rallyserver.IntegrationTest;
import org.showpage.rallyserver.ui.AuthResponse;
import org.showpage.rallyserver.ui.ChangePasswordRequest;
import org.showpage.rallyserver.ui.TokenRequest;
import org.showpage.rallyserver.ui.UiMember;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Login and authentication operations.
 * Tests password changes and refresh token functionality.
 */
@Slf4j
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class LoginControllerIT extends IntegrationTest {

    @Test
    @Order(10)
    @DisplayName("User can change their password with valid old password")
    public void testChangePasswordSuccess() throws Exception {
        // Register a test user
        String email = generateTestUserEmail();
        String oldPassword = "oldPassword123";
        String newPassword = "newPassword456";

        AuthResponse auth = registerTestUser(email, oldPassword);
        String authHeader = "Bearer " + auth.getAccessToken();

        // Change password
        ChangePasswordRequest request = ChangePasswordRequest
                .builder()
                .oldPassword(oldPassword)
                .newPassword(newPassword)
                .build();

        RR_Void response = restCaller.put("/api/member/password", request, authHeader, tr_Void);
        check(response);

        log.info("Successfully changed password for user: {}", email);

        // Verify can login with new password
        AuthResponse newAuth = loginAndGetAuthResponse(email, newPassword);
        assertNotNull(newAuth.getAccessToken());
        assertNotNull(newAuth.getRefreshToken());
        log.info("Successfully logged in with new password");
    }

    @Test
    @Order(20)
    @DisplayName("User cannot change password with invalid old password")
    public void testChangePasswordWithInvalidOldPassword() throws Exception {
        // Register a test user
        String email = generateTestUserEmail();
        String password = "correctPassword123";

        AuthResponse auth = registerTestUser(email, password);
        String authHeader = "Bearer " + auth.getAccessToken();

        // Try to change password with wrong old password
        ChangePasswordRequest request = ChangePasswordRequest
                .builder()
                .oldPassword("wrongPassword")
                .newPassword("newPassword456")
                .build();

        RR_Void response = restCaller.put("/api/member/password", request, authHeader, tr_Void);
        checkFailed(response);

        log.info("Correctly rejected password change with invalid old password");
    }

    @Test
    @Order(30)
    @DisplayName("Refresh token works once and returns new tokens")
    public void testRefreshTokenWorksOnce() throws Exception {
        // Register a test user
        String email = generateTestUserEmail();
        String password = "password123";

        AuthResponse auth = registerTestUser(email, password);
        String originalAccessToken = auth.getAccessToken();
        String originalRefreshToken = auth.getRefreshToken();

        assertNotNull(originalAccessToken, "Original access token should not be null");
        assertNotNull(originalRefreshToken, "Original refresh token should not be null");

        // Use refresh token to get new tokens
        TokenRequest tokenRequest = new TokenRequest(originalRefreshToken);
        RR_AuthResponse refreshResponse = restCaller.post(
                "/api/auth/token",
                tokenRequest,
                null,  // No auth header needed for token refresh
                tr_AuthResponse
        );
        check(refreshResponse);

        AuthResponse newAuth = refreshResponse.getData();
        String newAccessToken = newAuth.getAccessToken();
        String newRefreshToken = newAuth.getRefreshToken();

        assertNotNull(newAccessToken, "New access token should not be null");
        assertNotNull(newRefreshToken, "New refresh token should not be null");
        assertNotEquals(originalAccessToken, newAccessToken, "New access token should be different");
        assertNotEquals(originalRefreshToken, newRefreshToken, "New refresh token should be different");

        log.info("Successfully refreshed tokens");

        // Try to use the original refresh token again - should fail
        RR_AuthResponse secondRefreshResponse = restCaller.post(
                "/api/auth/token",
                tokenRequest,
                null,
                tr_AuthResponse
        );
        checkFailed(secondRefreshResponse);

        log.info("Correctly rejected reuse of old refresh token");
    }

    @Test
    @Order(40)
    @DisplayName("New access token works after refresh")
    public void testNewAccessTokenWorks() throws Exception {
        // Register a test user
        String email = generateTestUserEmail();
        String password = "password123";

        AuthResponse auth = registerTestUser(email, password);

        // Refresh tokens
        TokenRequest tokenRequest = new TokenRequest(auth.getRefreshToken());
        RR_AuthResponse refreshResponse = restCaller.post(
                "/api/auth/token",
                tokenRequest,
                null,
                tr_AuthResponse
        );
        check(refreshResponse);

        String newAccessToken = refreshResponse.getData().getAccessToken();
        String newAuthHeader = "Bearer " + newAccessToken;

        // Use new access token to get member info
        RR_UiMember memberResponse = restCaller.get("/api/member/info", newAuthHeader, tr_UiMember);
        check(memberResponse);

        UiMember member = memberResponse.getData();
        assertNotNull(member);
        assertEquals(email, member.getEmail());

        log.info("New access token works correctly");
    }

    @Test
    @Order(50)
    @DisplayName("New refresh token works")
    public void testNewRefreshTokenWorks() throws Exception {
        // Register a test user
        String email = generateTestUserEmail();
        String password = "password123";

        AuthResponse auth = registerTestUser(email, password);

        // First refresh
        TokenRequest tokenRequest1 = new TokenRequest(auth.getRefreshToken());
        RR_AuthResponse refreshResponse1 = restCaller.post(
                "/api/auth/token",
                tokenRequest1,
                null,
                tr_AuthResponse
        );
        check(refreshResponse1);

        String firstNewRefreshToken = refreshResponse1.getData().getRefreshToken();
        assertNotNull(firstNewRefreshToken);

        // Use the new refresh token to get another set of tokens
        TokenRequest tokenRequest2 = new TokenRequest(firstNewRefreshToken);
        RR_AuthResponse refreshResponse2 = restCaller.post(
                "/api/auth/token",
                tokenRequest2,
                null,
                tr_AuthResponse
        );
        check(refreshResponse2);

        AuthResponse secondNewAuth = refreshResponse2.getData();
        assertNotNull(secondNewAuth.getAccessToken());
        assertNotNull(secondNewAuth.getRefreshToken());
        assertNotEquals(firstNewRefreshToken, secondNewAuth.getRefreshToken(),
                "Second refresh should provide a different refresh token");

        log.info("New refresh token works correctly");
    }
}
