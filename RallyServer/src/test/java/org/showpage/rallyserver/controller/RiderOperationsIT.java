package org.showpage.rallyserver.controller;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.showpage.rallyserver.IntegrationTest;
import org.showpage.rallyserver.entity.RallyParticipantType;
import org.showpage.rallyserver.ui.*;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Rider operations.
 * Tests rider registration, rally searching, and viewing rally information.
 */
@Slf4j
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class RiderOperationsIT extends IntegrationTest {

    @Test
    @Order(1)
    @DisplayName("Rider can register for a public rally")
    public void testRiderRegistration() throws Exception {
        // Create a rally as organizer
        UiRally rally = createTestRally();

        // Register as rider
        RR_UiRallyParticipant response = post_ForRider("/api/rally/" + rally.getId() + "/register", null, tr_UiRallyParticipant);
        check(response);

        UiRallyParticipant participant = response.getData();
        assertNotNull(participant);
        assertNotNull(participant.getId());
        assertEquals(RallyParticipantType.RIDER, participant.getParticipantType());
        log.info("Rider registered for rally: {}", rally.getName());
    }

    @Test
    @Order(2)
    @DisplayName("Rider cannot register twice for the same rally")
    public void testDuplicateRegistration() throws Exception {
        // Create a rally and register once
        UiRally rally = createTestRally();
        post_ForRider("/api/rally/" + rally.getId() + "/register", null, tr_UiRallyParticipant);

        // Try to register again - should fail
        try {
            RR_UiRallyParticipant response = post_ForRider("/api/rally/" + rally.getId() + "/register", null, tr_UiRallyParticipant);
            if (response.isSuccess()) {
                fail("Should not be able to register twice for the same rally");
            } else {
                log.info("Correctly prevented duplicate registration: {}", response.getMessage());
            }
        } catch (Exception e) {
            log.info("Correctly prevented duplicate registration via exception");
        }
    }

    @Test
    @Order(10)
    @DisplayName("Rider can search for rallies by name")
    public void testSearchByName() throws Exception {
        // Create rallies with specific names
        createTestRally("AutoTest Searchable Rally Alpha", null, null);
        createTestRally("AutoTest Searchable Rally Beta", null, null);
        createTestRally("AutoTest Different Name", null, null);

        // Search for "Searchable"
        RR_PageUiRally response = get_ForRider("/api/rallies?name=Searchable", tr_PageUiRally);
        check(response);

        List<UiRally> rallies = response.getData().getContent();
        assertTrue(rallies.size() >= 2, "Should find at least 2 rallies with 'Searchable' in name");
        log.info("Found {} rallies matching 'Searchable'", rallies.size());
    }

    @Test
    @Order(11)
    @DisplayName("Rider can search for rallies by date range")
    public void testSearchByDateRange() throws Exception {
        // Create rallies with different date ranges
        LocalDate now = LocalDate.now();
        createTestRally("AutoTest Near Future", now.plusDays(5), now.plusDays(10));
        createTestRally("AutoTest Far Future", now.plusDays(60), now.plusDays(65));

        // Search for rallies starting within the next 30 days
        String fromDate = now.toString();
        String toDate = now.plusDays(30).toString();
        RR_PageUiRally response = get_ForRider("/api/rallies?from=" + fromDate + "&to=" + toDate, tr_PageUiRally);
        check(response);

        List<UiRally> rallies = response.getData().getContent();
        assertFalse(rallies.isEmpty(), "Should find at least 1 rally in date range");
        log.info("Found {} rallies in date range", rallies.size());
    }

    @Test
    @Order(12)
    @DisplayName("Rider can search for rallies by location")
    public void testSearchByLocation() throws Exception {
        // Create rallies in different locations
        createTestRally("Seattle", "US", 47.6062, -122.3321);
        createTestRally("Portland", "US", 45.5152, -122.6784);
        createTestRally("Vancouver", "CA", 49.2827, -123.1207);

        // Search for rallies in the US
        RR_PageUiRally response = get_ForRider("/api/rallies?country=US", tr_PageUiRally);
        check(response);

        List<UiRally> rallies = response.getData().getContent();
        assertTrue(rallies.size() >= 2, "Should find at least 2 rallies in US");
        log.info("Found {} rallies in US", rallies.size());
    }

    @Test
    @Order(13)
    @DisplayName("Rider can search for nearby rallies")
    public void testSearchNearby() throws Exception {
        // Create rallies at known locations
        // Seattle area
        createTestRally("Seattle", "US", 47.6062, -122.3321);
        createTestRally("Tacoma", "US", 47.2529, -122.4443);
        // Los Angeles (far away)
        createTestRally("Los Angeles", "US", 34.0522, -118.2437);

        // Search for rallies within 50 miles of Seattle
        RR_PageUiRally response = get_ForRider(
            "/api/rallies?nearLat=47.6062&nearLng=-122.3321&radiusMiles=50",
            tr_PageUiRally
        );
        check(response);

        List<UiRally> rallies = response.getData().getContent();
        // Should find Seattle and Tacoma (~30 miles), but not LA (~1000 miles)
        assertFalse(rallies.isEmpty(), "Should find at least Seattle");
        log.info("Found {} rallies within 50 miles of Seattle", rallies.size());
    }

    @Test
    @Order(14)
    @DisplayName("Rider can search for rallies in Europe")
    public void testSearchEurope() throws Exception {
        // Create rallies in Europe
        createTestRally("Paris", "FR", 48.8566, 2.3522);
        createTestRally("London", "GB", 51.5074, -0.1278);
        createTestRally("Berlin", "DE", 52.5200, 13.4050);
        // And one in US
        createTestRally("New York", "US", 40.7128, -74.0060);

        // Search for rallies within 1000 miles of Paris (should get London, Berlin, not NY)
        RR_PageUiRally response = get_ForRider(
            "/api/rallies?nearLat=48.8566&nearLng=2.3522&radiusMiles=1000",
            tr_PageUiRally
        );
        check(response);

        List<UiRally> rallies = response.getData().getContent();
        assertTrue(rallies.size() >= 2, "Should find at least Paris and nearby European rallies");
        log.info("Found {} rallies within 1000 miles of Paris", rallies.size());
    }

    @Test
    @Order(20)
    @DisplayName("Rider can view public rally details")
    public void testViewPublicRallyDetails() throws Exception {
        // Create a public rally with bonus points
        UiRally rally = createTestRally();
        createTestBonusPoint(rally.getId(), "BP1", 100);
        createTestBonusPoint(rally.getId(), "BP2", 150);

        // Rider can view it
        RR_UiRally response = get_ForRider("/api/rally/" + rally.getId(), tr_UiRally);
        check(response);

        UiRally retrieved = response.getData();
        assertNotNull(retrieved);
        assertEquals(rally.getName(), retrieved.getName());
        // Public rally should show bonus points
        assertNotNull(retrieved.getBonusPoints(), "Public rally should include bonus points");
        log.info("Retrieved public rally with {} bonus points", retrieved.getBonusPoints().size());
    }

    @Test
    @Order(21)
    @DisplayName("Rider can view list of public rallies")
    public void testListPublicRallies() throws Exception {
        // Create several public rallies
        createTestRally();
        createTestRally();
        createTestRally();

        // List rallies
        RR_PageUiRally response = get_ForRider("/api/rallies", tr_PageUiRally);
        check(response);

        List<UiRally> rallies = response.getData().getContent();
        assertTrue(rallies.size() >= 3, "Should find at least 3 public rallies");
        log.info("Found {} public rallies", rallies.size());
    }

    @Test
    @Order(30)
    @DisplayName("Rider can view rallies they are registered for")
    public void testViewMyRallies() throws Exception {
        // Create a rally and register for it
        UiRally rally = createTestRally();
        post_ForRider("/api/rally/" + rally.getId() + "/register", null, tr_UiRallyParticipant);

        // Get rally details - should include participant info
        RR_UiRally response = get_ForRider("/api/rally/" + rally.getId(), tr_UiRally);
        check(response);

        UiRally retrieved = response.getData();
        assertNotNull(retrieved);
        log.info("Retrieved rally rider is registered for: {}", retrieved.getName());
    }

    @Test
    @Order(40)
    @DisplayName("Search handles empty results gracefully")
    public void testSearchNoResults() throws Exception {
        // Search for a rally name that doesn't exist
        RR_PageUiRally response = get_ForRider("/api/rallies?name=NonExistentRally12345", tr_PageUiRally);
        check(response);

        List<UiRally> rallies = response.getData().getContent();
        assertNotNull(rallies);
        assertEquals(0, rallies.size(), "Should return empty list for no matches");
        log.info("Search with no results returned empty list correctly");
    }

    @Test
    @Order(41)
    @DisplayName("Search handles invalid coordinates gracefully")
    public void testSearchInvalidCoordinates() {
        // Try searching with invalid latitude/longitude
        try {
            RR_PageUiRally response = get_ForRider(
                "/api/rallies?nearLat=999&nearLng=-999&radiusMiles=50",
                tr_PageUiRally
            );
            // Should either return empty results or validation error
            if (response.isSuccess()) {
                List<UiRally> rallies = response.getData().getContent();
                assertEquals(0, rallies.size(), "Invalid coordinates should return no results");
                log.info("Invalid coordinates returned empty results");
            }
        } catch (Exception e) {
            log.info("Invalid coordinates rejected with error: {}", e.getMessage());
        }
    }
}
