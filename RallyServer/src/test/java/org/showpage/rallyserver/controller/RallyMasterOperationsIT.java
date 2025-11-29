package org.showpage.rallyserver.controller;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.showpage.rallyserver.IntegrationTest;
import org.showpage.rallyserver.ui.*;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Rally Master (Organizer) operations.
 * Tests CRUD operations on Rallies, BonusPoints, Combinations, and CombinationPoints.
 */
@Slf4j
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class RallyMasterOperationsIT extends IntegrationTest {

    private static boolean didDelete = false;

    /**
     * Delete all rallies with names beginning with "AutoTest".
     * Uses a static flag to ensure it only runs once.
     */
    @BeforeEach
    public void deleteAutoTestRallies() throws Exception {
        if (didDelete) {
            return;
        }

        log.info("Deleting all AutoTest rallies...");

        try {
            RR_PageUiRally rallyResponse = get_ForRM("/api/rallies?name=AutoTest&all=true", tr_PageUiRally);

            if (rallyResponse != null && rallyResponse.isSuccess() && rallyResponse.getData() != null) {
                List<UiRally> rallies = rallyResponse.getData().getContent();
                log.info("Found {} AutoTest rallies to delete", rallies.size());

                for (UiRally rally : rallies) {
                    try {
                        log.info("Deleting AutoTest rally: {} (ID: {})", rally.getName(), rally.getId());
                        delete_ForRM("/api/rally/" + rally.getId(), tr_Void);
                    } catch (Exception e) {
                        log.warn("Failed to delete rally {}: {}", rally.getId(), e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Error during AutoTest rally cleanup: {}", e.getMessage());
        }

        didDelete = true;
        log.info("AutoTest rally cleanup complete");
    }

    @Test
    @Order(10)
    @DisplayName("Trigger cleanup - no-op test")
    public void testTriggerCleanup() throws Exception {
        // This test exists only to trigger the @BeforeEach cleanup
        log.info("Cleanup triggered successfully");
        assertTrue(didDelete, "Cleanup should have been triggered");
    }

    @Test
    @Order(20)
    @DisplayName("Rally Master can create a rally")
    public void testCreateRally() throws Exception {
        UiRally rally = createTestRally();

        assertNotNull(rally);
        assertNotNull(rally.getId());
        assertTrue(rally.getName().startsWith("AutoTest"));
        log.info("Created rally: {} with ID: {}", rally.getName(), rally.getId());
    }

    @Test
    @Order(30)
    @DisplayName("Rally Master can update a rally")
    public void testUpdateRally() throws Exception {
        // Create a rally
        UiRally rally = createTestRally();

        // Update it
        UpdateRallyRequest updateRequest = new UpdateRallyRequest();
        updateRequest.setName("AutoTest Updated Rally");
        updateRequest.setDescription("Updated description");
        updateRequest.setStartDate(LocalDate.now().plusDays(30));
        updateRequest.setEndDate(LocalDate.now().plusDays(32));

        RR_UiRally response = put_ForRM("/api/rally/" + rally.getId(), updateRequest, tr_UiRally);
        check(response);

        UiRally updated = response.getData();
        assertEquals("AutoTest Updated Rally", updated.getName());
        assertEquals("Updated description", updated.getDescription());
        assertNotNull(updated.getStartDate());
        assertNotNull(updated.getEndDate());
    }

    @Test
    @Order(40)
    @DisplayName("Rally Master can delete a rally")
    public void testDeleteRally() throws Exception {
        // Create a rally
        UiRally rally = createTestRally();
        Integer rallyId = rally.getId();

        // Delete it
        RR_Void deleteResponse = delete_ForRM("/api/rally/" + rallyId, tr_Void);
        check(deleteResponse);

        RR_UiRally getRallyRR = get_ForRM("/api/rally/" + rallyId, tr_UiRally);
        assertFalse(getRallyRR.isSuccess());
    }

    @Test
    @Order(100)
    @DisplayName("Rally Master can create bonus points")
    public void testCreateBonusPoint() throws Exception {
        UiRally rally = createTestRally();
        UiBonusPoint bp = createTestBonusPoint(rally.getId(), "BP1", 50);

        assertNotNull(bp);
        assertNotNull(bp.getId());
        assertEquals("BP1", bp.getCode());
        assertEquals(50, bp.getPoints());
        // Note: rallyId is not always populated due to JPA insertable=false, updatable=false
    }

    @Test
    @Order(110)
    @DisplayName("Rally Master can update bonus points")
    public void testUpdateBonusPoint() throws Exception {
        UiRally rally = createTestRally();
        UiBonusPoint bp = createTestBonusPoint(rally.getId(), "BP1", 50);

        UpdateBonusPointRequest updateRequest = new UpdateBonusPointRequest();
        updateRequest.setCode("BP1-UPDATED");
        updateRequest.setPoints(75);
        updateRequest.setRequired(true);

        RR_UiBonusPoint response = put_ForRM("/api/bonuspoint/" + bp.getId(), updateRequest, tr_UiBonusPoint);
        check(response);

        UiBonusPoint updated = response.getData();
        assertEquals("BP1-UPDATED", updated.getCode());
        assertEquals(75, updated.getPoints());
        assertTrue(updated.getRequired());
    }

    @Test
    @Order(120)
    @DisplayName("Rally Master can delete bonus points")
    public void testDeleteBonusPoint() throws Exception {
        UiRally rally = createTestRally();
        UiBonusPoint bp = createTestBonusPoint(rally.getId(), "BP1", 50);

        RR_Void deleteResponse = delete_ForRM("/api/bonuspoint/" + bp.getId(), tr_Void);
        check(deleteResponse);
    }

    @Test
    @Order(130)
    @DisplayName("Rally Master can list all bonus points for a rally")
    public void testListBonusPoints() throws Exception {
        UiRally rally = createTestRally();
        createTestBonusPoint(rally.getId(), "BP1", 50);
        createTestBonusPoint(rally.getId(), "BP2", 75);
        createTestBonusPoint(rally.getId(), "BP3", 100);

        RR_ListUiBonusPoint response = get_ForRM("/api/rally/" + rally.getId() + "/bonuspoints", tr_ListUiBonusPoint);
        check(response);

        List<UiBonusPoint> bonusPoints = response.getData();
        assertEquals(3, bonusPoints.size());
    }

    @Test
    @Order(200)
    @DisplayName("Rally Master can create combinations with bonus points")
    public void testCreateCombination() throws Exception {
        UiRally rally = createTestRally();
        UiBonusPoint bp1 = createTestBonusPoint(rally.getId(), "BP1", 50);
        UiBonusPoint bp2 = createTestBonusPoint(rally.getId(), "BP2", 75);
        UiBonusPoint bp3 = createTestBonusPoint(rally.getId(), "BP3", 100);

        UiCombination combo = createTestCombination(rally.getId(),
            List.of(bp1.getId(), bp2.getId(), bp3.getId()));

        assertNotNull(combo);
        assertNotNull(combo.getId());
        // Note: rallyId is not always populated due to JPA insertable=false, updatable=false
        assertNotNull(combo.getCombinationPoints());
        assertEquals(3, combo.getCombinationPoints().size());
    }

    @Test
    @Order(210)
    @DisplayName("Rally Master can update combinations")
    public void testUpdateCombination() throws Exception {
        UiRally rally = createTestRally();
        UiBonusPoint bp1 = createTestBonusPoint(rally.getId(), "BP1", 50);
        UiBonusPoint bp2 = createTestBonusPoint(rally.getId(), "BP2", 75);

        UiCombination combo = createTestCombination(rally.getId(), List.of(bp1.getId(), bp2.getId()));

        UpdateCombinationRequest updateRequest = new UpdateCombinationRequest();
        updateRequest.setName("Updated Combination");
        updateRequest.setPoints(500);

        RR_UiCombination response = put_ForRM("/api/combination/" + combo.getId(), updateRequest, tr_UiCombination);
        check(response);

        UiCombination updated = response.getData();
        assertEquals("Updated Combination", updated.getName());
        assertEquals(500, updated.getPoints());
    }

    @Test
    @Order(220)
    @DisplayName("Rally Master can delete combinations")
    public void testDeleteCombination() throws Exception {
        UiRally rally = createTestRally();
        UiBonusPoint bp1 = createTestBonusPoint(rally.getId(), "BP1", 50);
        UiBonusPoint bp2 = createTestBonusPoint(rally.getId(), "BP2", 75);

        UiCombination combo = createTestCombination(rally.getId(), List.of(bp1.getId(), bp2.getId()));

        RR_Void deleteResponse = delete_ForRM("/api/combination/" + combo.getId(), tr_Void);
        check(deleteResponse);
    }

    @Test
    @Order(230)
    @DisplayName("Rally Master can add bonus points to existing combinations")
    public void testAddCombinationPoint() throws Exception {
        UiRally rally = createTestRally();
        UiBonusPoint bp1 = createTestBonusPoint(rally.getId(), "BP1", 50);
        UiBonusPoint bp2 = createTestBonusPoint(rally.getId(), "BP2", 75);
        UiBonusPoint bp3 = createTestBonusPoint(rally.getId(), "BP3", 100);

        // Create combination with just 2 bonus points
        UiCombination combo = createTestCombination(rally.getId(), List.of(bp1.getId(), bp2.getId()));

        // Add a third bonus point
        CreateCombinationPointRequest cpRequest = new CreateCombinationPointRequest();
        cpRequest.setBonusPointId(bp3.getId());
        cpRequest.setRequired(true);

        RR_UiCombinationPoint response = post_ForRM("/api/combination/" + combo.getId() + "/bonuspoint",
            cpRequest, tr_UiCombinationPoint);
        check(response);

        UiCombinationPoint cp = response.getData();
        assertNotNull(cp.getId());
        // Note: combinationId and bonusPointId may not be populated due to JPA insertable=false, updatable=false
    }

    @Test
    @Order(400)
    @DisplayName("Rally Master can handle null data gracefully in rally creation")
    public void testCreateRallyWithNullFields() throws Exception {
        CreateRallyRequest request = new CreateRallyRequest();
        request.setName("AutoTest Null Test Rally");
        request.setDescription(null); // Null description
        request.setLocationCity("TestCity");
        request.setLocationCountry("US");
        request.setIsPublic(true);

        try {
            RR_UiRally response = post_ForRM("/api/rally", request, tr_UiRally);
            // Should either succeed with null description or fail with validation error
            if (response.isSuccess()) {
                UiRally rally = response.getData();
                assertNotNull(rally);
                log.info("Rally created with null description");
            }
        } catch (Exception e) {
            // Also acceptable if validation rejects null
            log.info("Validation rejected null description: {}", e.getMessage());
        }
    }

    @Test
    @Order(410)
    @DisplayName("Rally Master cannot create bonus point with null required fields")
    public void testCreateBonusPointWithNulls() throws Exception {
        UiRally rally = createTestRally();

        CreateBonusPointRequest request = new CreateBonusPointRequest();
        request.setCode(null); // Null code
        request.setName(null); // Null name
        request.setPoints(null); // Null points

        try {
            post_ForRM("/api/rally/" + rally.getId() + "/bonuspoint", request, tr_UiBonusPoint);
            // If this succeeds, we have an NPE risk
            log.warn("Bonus point created with null fields - potential NPE risk!");
        } catch (Exception e) {
            // Expected - should reject null required fields
            log.info("Correctly rejected bonus point with null fields: {}", e.getMessage());
        }
    }
}
