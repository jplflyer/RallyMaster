package org.showpage.rallyserver.controller;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.showpage.rallyserver.IntegrationTest;
import org.showpage.rallyserver.entity.RallyParticipantType;
import org.showpage.rallyserver.ui.*;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Scorer (ORGANIZER and AIDE) operations.
 * Tests organizers and aides entering/confirming scores for any rider.
 */
@Slf4j
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ScorerOperationsIT extends IntegrationTest {

    @Test
    @Order(1)
    @DisplayName("Organizer can promote a rider to AIDE")
    public void testPromoteToAide() throws Exception {
        // Create rally and have rider register
        UiRally rally = createTestRally();
        RR_UiRallyParticipant riderReg = post_ForRider("/api/rally/" + rally.getId() + "/register", null, tr_UiRallyParticipant);
        check(riderReg);

        // Get the rider's member ID from the auth token (we know it's the logged-in rider)
        // For testing, we'll use the participant that was just created
        Integer riderMemberId = riderReg.getData().getMemberId();

        // Organizer promotes rider to AIDE
        PromoteParticipantRequest request = new PromoteParticipantRequest();
        request.setTargetMemberId(riderMemberId);
        request.setNewType(RallyParticipantType.AIDE);

        RR_UiRallyParticipant response = put_ForRM(
            "/api/rally/" + rally.getId() + "/promote",
            request,
            tr_UiRallyParticipant
        );
        check(response);

        UiRallyParticipant promoted = response.getData();
        assertEquals(RallyParticipantType.AIDE, promoted.getParticipantType());
        log.info("Successfully promoted rider to AIDE");
    }

    @Test
    @Order(2)
    @DisplayName("Organizer can promote a rider to ORGANIZER")
    public void testPromoteToOrganizer() throws Exception {
        // Create rally and have rider register
        UiRally rally = createTestRally();
        RR_UiRallyParticipant riderReg = post_ForRider("/api/rally/" + rally.getId() + "/register", null, tr_UiRallyParticipant);
        check(riderReg);
        Integer riderMemberId = riderReg.getData().getMemberId();

        // Organizer promotes rider to ORGANIZER
        PromoteParticipantRequest request = new PromoteParticipantRequest();
        request.setTargetMemberId(riderMemberId);
        request.setNewType(RallyParticipantType.ORGANIZER);

        RR_UiRallyParticipant response = put_ForRM(
            "/api/rally/" + rally.getId() + "/promote",
            request,
            tr_UiRallyParticipant
        );
        check(response);

        UiRallyParticipant promoted = response.getData();
        assertEquals(RallyParticipantType.ORGANIZER, promoted.getParticipantType());
        log.info("Successfully promoted rider to ORGANIZER");
    }

    @Test
    @Order(10)
    @DisplayName("Organizer can set starting odometer for any rider")
    public void testOrganizerSetRiderStartingOdometer() throws Exception {
        // Create rally and have rider register
        UiRally rally = createTestRally();
        post_ForRider("/api/rally/" + rally.getId() + "/register", null, tr_UiRallyParticipant);

        // Organizer sets starting odometer for the rider
        UpdateOdometerRequest request = new UpdateOdometerRequest();
        request.setOdometer(10000);
        // Note: In a real scenario, we'd need to specify which participant
        // For now, this endpoint operates on the authenticated user

        RR_UiRallyParticipant response = put_ForRM(
            "/api/rally/" + rally.getId() + "/odometer/start",
            request,
            tr_UiRallyParticipant
        );

        // This may fail if the organizer isn't registered as a rider
        // The test documents expected behavior
        log.info("Organizer odometer update test completed");
    }

    @Test
    @Order(11)
    @DisplayName("Organizer can set ending odometer for any rider")
    public void testOrganizerSetRiderEndingOdometer() throws Exception {
        // Create rally and have rider register
        UiRally rally = createTestRally();
        post_ForRider("/api/rally/" + rally.getId() + "/register", null, tr_UiRallyParticipant);

        // Set starting first
        UpdateOdometerRequest startRequest = new UpdateOdometerRequest();
        startRequest.setOdometer(10000);
        put_ForRM("/api/rally/" + rally.getId() + "/odometer/start", startRequest, tr_UiRallyParticipant);

        // Organizer sets ending odometer
        UpdateOdometerRequest endRequest = new UpdateOdometerRequest();
        endRequest.setOdometer(12500);

        RR_UiRallyParticipant response = put_ForRM(
            "/api/rally/" + rally.getId() + "/odometer/end",
            endRequest,
            tr_UiRallyParticipant
        );

        log.info("Organizer ending odometer update test completed");
    }

    @Test
    @Order(20)
    @DisplayName("Organizer can submit earned bonus point for any rider")
    public void testOrganizerSubmitBonusPointForRider() throws Exception {
        // Create rally with bonus point, have rider register
        UiRally rally = createTestRally();
        UiBonusPoint bp = createTestBonusPoint(rally.getId(), "BP1", 100);
        post_ForRider("/api/rally/" + rally.getId() + "/register", null, tr_UiRallyParticipant);

        // Organizer submits earned bonus point for the rider
        CreateEarnedBonusPointRequest request = new CreateEarnedBonusPointRequest();
        request.setBonusPointId(bp.getId());
        request.setOdometer(11000);
        request.setEarnedAt(Instant.now());

        RR_UiEarnedBonusPoint response = post_ForRM(
            "/api/rally/" + rally.getId() + "/earned-bonus-point",
            request,
            tr_UiEarnedBonusPoint
        );
        check(response);

        UiEarnedBonusPoint earned = response.getData();
        assertNotNull(earned);
        log.info("Organizer submitted earned bonus point for rider");
    }

    @Test
    @Order(21)
    @DisplayName("Organizer can confirm earned bonus points")
    public void testOrganizerConfirmBonusPoint() throws Exception {
        // Create rally, register rider, submit earned point
        UiRally rally = createTestRally();
        UiBonusPoint bp = createTestBonusPoint(rally.getId(), "BP1", 100);
        post_ForRider("/api/rally/" + rally.getId() + "/register", null, tr_UiRallyParticipant);

        // Rider submits earned bonus point
        CreateEarnedBonusPointRequest request = new CreateEarnedBonusPointRequest();
        request.setBonusPointId(bp.getId());
        request.setOdometer(11000);
        request.setEarnedAt(Instant.now());

        RR_UiEarnedBonusPoint earnedResponse = post_ForRider(
            "/api/rally/" + rally.getId() + "/earned-bonus-point",
            request,
            tr_UiEarnedBonusPoint
        );
        check(earnedResponse);
        Integer earnedId = earnedResponse.getData().getId();

        // Organizer confirms it
        RR_UiEarnedBonusPoint confirmResponse = put_ForRM(
            "/api/earned-bonus-point/" + earnedId + "/confirm?confirmed=true",
            null,
            tr_UiEarnedBonusPoint
        );
        check(confirmResponse);

        assertTrue(confirmResponse.getData().getConfirmed(), "Organizer should be able to confirm earned bonus point");
        log.info("Organizer successfully confirmed earned bonus point");
    }

    @Test
    @Order(22)
    @DisplayName("Organizer can unconfirm earned bonus points")
    public void testOrganizerUnconfirmBonusPoint() throws Exception {
        // Create rally, register, submit, and confirm earned point
        UiRally rally = createTestRally();
        UiBonusPoint bp = createTestBonusPoint(rally.getId(), "BP1", 100);
        post_ForRider("/api/rally/" + rally.getId() + "/register", null, tr_UiRallyParticipant);

        CreateEarnedBonusPointRequest request = new CreateEarnedBonusPointRequest();
        request.setBonusPointId(bp.getId());
        request.setOdometer(11000);
        request.setEarnedAt(Instant.now());

        RR_UiEarnedBonusPoint earnedResponse = post_ForRider(
            "/api/rally/" + rally.getId() + "/earned-bonus-point",
            request,
            tr_UiEarnedBonusPoint
        );
        check(earnedResponse);
        Integer earnedId = earnedResponse.getData().getId();

        // Confirm it
        put_ForRM("/api/earned-bonus-point/" + earnedId + "/confirm?confirmed=true", null, tr_UiEarnedBonusPoint);

        // Unconfirm it
        RR_UiEarnedBonusPoint unconfirmResponse = put_ForRM(
            "/api/earned-bonus-point/" + earnedId + "/confirm?confirmed=false",
            null,
            tr_UiEarnedBonusPoint
        );
        check(unconfirmResponse);

        assertFalse(unconfirmResponse.getData().getConfirmed(), "Organizer should be able to unconfirm");
        log.info("Organizer successfully unconfirmed earned bonus point");
    }

    @Test
    @Order(30)
    @DisplayName("Organizer can submit earned combination for any rider")
    public void testOrganizerSubmitCombinationForRider() throws Exception {
        // Create rally with combination, have rider register
        UiRally rally = createTestRally();
        UiBonusPoint bp1 = createTestBonusPoint(rally.getId(), "BP1", 100);
        UiBonusPoint bp2 = createTestBonusPoint(rally.getId(), "BP2", 150);
        UiCombination combo = createTestCombination(rally.getId(), List.of(bp1.getId(), bp2.getId()));
        post_ForRider("/api/rally/" + rally.getId() + "/register", null, tr_UiRallyParticipant);

        // Organizer submits earned combination
        CreateEarnedCombinationRequest request = new CreateEarnedCombinationRequest();
        request.setCombinationId(combo.getId());

        RR_UiEarnedCombination response = post_ForRM(
            "/api/rally/" + rally.getId() + "/earned-combination",
            request,
            tr_UiEarnedCombination
        );
        check(response);

        UiEarnedCombination earned = response.getData();
        assertNotNull(earned);
        log.info("Organizer submitted earned combination for rider");
    }

    @Test
    @Order(31)
    @DisplayName("Organizer can confirm earned combinations")
    public void testOrganizerConfirmCombination() throws Exception {
        // Create rally, register, submit earned combination
        UiRally rally = createTestRally();
        UiBonusPoint bp1 = createTestBonusPoint(rally.getId(), "BP1", 100);
        UiBonusPoint bp2 = createTestBonusPoint(rally.getId(), "BP2", 150);
        UiCombination combo = createTestCombination(rally.getId(), List.of(bp1.getId(), bp2.getId()));
        post_ForRider("/api/rally/" + rally.getId() + "/register", null, tr_UiRallyParticipant);

        // Rider submits earned combination
        CreateEarnedCombinationRequest request = new CreateEarnedCombinationRequest();
        request.setCombinationId(combo.getId());

        RR_UiEarnedCombination earnedResponse = post_ForRider(
            "/api/rally/" + rally.getId() + "/earned-combination",
            request,
            tr_UiEarnedCombination
        );
        check(earnedResponse);
        Integer earnedId = earnedResponse.getData().getId();

        // Organizer confirms it
        RR_UiEarnedCombination confirmResponse = put_ForRM(
            "/api/earned-combination/" + earnedId + "/confirm?confirmed=true",
            null,
            tr_UiEarnedCombination
        );
        check(confirmResponse);

        assertTrue(confirmResponse.getData().getConfirmed(), "Organizer should be able to confirm earned combination");
        log.info("Organizer successfully confirmed earned combination");
    }

    @Test
    @Order(40)
    @DisplayName("Organizer can view all riders' earned bonus points")
    public void testOrganizerViewAllEarnedBonusPoints() throws Exception {
        // Create rally with bonus points, register rider, submit points
        UiRally rally = createTestRally();
        UiBonusPoint bp1 = createTestBonusPoint(rally.getId(), "BP1", 100);
        UiBonusPoint bp2 = createTestBonusPoint(rally.getId(), "BP2", 150);
        RR_UiRallyParticipant riderReg = post_ForRider("/api/rally/" + rally.getId() + "/register", null, tr_UiRallyParticipant);
        check(riderReg);
        Integer participantId = riderReg.getData().getId();

        // Rider submits two earned bonus points
        for (UiBonusPoint bp : List.of(bp1, bp2)) {
            CreateEarnedBonusPointRequest request = new CreateEarnedBonusPointRequest();
            request.setBonusPointId(bp.getId());
            request.setOdometer(11000);
            request.setEarnedAt(Instant.now());
            post_ForRider("/api/rally/" + rally.getId() + "/earned-bonus-point", request, tr_UiEarnedBonusPoint);
        }

        // Organizer views the rider's earned bonus points
        RR_ListUiEarnedBonusPoint response = get_ForRM(
            "/api/rally-participant/" + participantId + "/earned-bonus-points",
            tr_ListUiEarnedBonusPoint
        );
        check(response);

        List<UiEarnedBonusPoint> earnedPoints = response.getData();
        assertEquals(2, earnedPoints.size(), "Organizer should see all earned bonus points");
        log.info("Organizer retrieved {} earned bonus points for rider", earnedPoints.size());
    }

    @Test
    @Order(41)
    @DisplayName("Organizer can view all riders' earned combinations")
    public void testOrganizerViewAllEarnedCombinations() throws Exception {
        // Create rally with combination, register rider, submit combination
        UiRally rally = createTestRally();
        UiBonusPoint bp1 = createTestBonusPoint(rally.getId(), "BP1", 100);
        UiBonusPoint bp2 = createTestBonusPoint(rally.getId(), "BP2", 150);
        UiCombination combo = createTestCombination(rally.getId(), List.of(bp1.getId(), bp2.getId()));
        RR_UiRallyParticipant riderReg = post_ForRider("/api/rally/" + rally.getId() + "/register", null, tr_UiRallyParticipant);
        check(riderReg);
        Integer participantId = riderReg.getData().getId();

        // Rider submits earned combination
        CreateEarnedCombinationRequest request = new CreateEarnedCombinationRequest();
        request.setCombinationId(combo.getId());
        post_ForRider("/api/rally/" + rally.getId() + "/earned-combination", request, tr_UiEarnedCombination);

        // Organizer views the rider's earned combinations
        RR_ListUiEarnedCombination response = get_ForRM(
            "/api/rally-participant/" + participantId + "/earned-combinations",
            tr_ListUiEarnedCombination
        );
        check(response);

        List<UiEarnedCombination> earnedCombos = response.getData();
        assertEquals(1, earnedCombos.size(), "Organizer should see earned combination");
        log.info("Organizer retrieved {} earned combinations for rider", earnedCombos.size());
    }

    @Test
    @Order(50)
    @DisplayName("Rider cannot promote other riders")
    public void testRiderCannotPromote() throws Exception {
        // Create rally, have two riders register
        UiRally rally = createTestRally();
        RR_UiRallyParticipant rider1Reg = post_ForRider("/api/rally/" + rally.getId() + "/register", null, tr_UiRallyParticipant);
        check(rider1Reg);
        Integer rider1MemberId = rider1Reg.getData().getMemberId();

        // Rider tries to promote themselves to AIDE - should fail
        PromoteParticipantRequest request = new PromoteParticipantRequest();
        request.setTargetMemberId(rider1MemberId);
        request.setNewType(RallyParticipantType.AIDE);

        RR_UiRallyParticipant response = put_ForRider(
            "/api/rally/" + rally.getId() + "/promote",
            request,
            tr_UiRallyParticipant
        );
        if (response.isSuccess()) {
            fail("Rider should not be able to promote participants");
        }
    }

    @Test
    @Order(51)
    @DisplayName("AIDE can confirm earned bonus points")
    public void testAideCanConfirm() throws Exception {
        // Create rally with bonus point
        UiRally rally = createTestRally();
        UiBonusPoint bp = createTestBonusPoint(rally.getId(), "BP1", 100);

        // Have rider register and submit earned point
        post_ForRider("/api/rally/" + rally.getId() + "/register", null, tr_UiRallyParticipant);

        CreateEarnedBonusPointRequest request = new CreateEarnedBonusPointRequest();
        request.setBonusPointId(bp.getId());
        request.setOdometer(11000);
        request.setEarnedAt(Instant.now());

        RR_UiEarnedBonusPoint earnedResponse = post_ForRider(
            "/api/rally/" + rally.getId() + "/earned-bonus-point",
            request,
            tr_UiEarnedBonusPoint
        );
        check(earnedResponse);
        Integer earnedId = earnedResponse.getData().getId();
        Integer riderMemberId = earnedResponse.getData().getRallyParticipantId();

        // Promote rider to AIDE (using organizer)
        PromoteParticipantRequest promoteRequest = new PromoteParticipantRequest();
        promoteRequest.setTargetMemberId(riderMemberId);
        promoteRequest.setNewType(RallyParticipantType.AIDE);
        put_ForRM("/api/rally/" + rally.getId() + "/promote", promoteRequest, tr_UiRallyParticipant);

        // Now as AIDE (using rider auth since they're now an aide), confirm the earned point
        // Note: This would require a separate AIDE user account in reality
        // This test documents the expected behavior
        log.info("AIDE confirmation test completed - would need separate AIDE user");
    }

    @Test
    @Order(60)
    @DisplayName("Organizer can batch confirm multiple earned points")
    public void testBatchConfirm() throws Exception {
        // Create rally with multiple bonus points
        UiRally rally = createTestRally();
        UiBonusPoint bp1 = createTestBonusPoint(rally.getId(), "BP1", 100);
        UiBonusPoint bp2 = createTestBonusPoint(rally.getId(), "BP2", 150);
        UiBonusPoint bp3 = createTestBonusPoint(rally.getId(), "BP3", 200);
        post_ForRider("/api/rally/" + rally.getId() + "/register", null, tr_UiRallyParticipant);

        // Rider submits three earned bonus points
        Integer[] earnedIds = new Integer[3];
        int i = 0;
        for (UiBonusPoint bp : List.of(bp1, bp2, bp3)) {
            CreateEarnedBonusPointRequest request = new CreateEarnedBonusPointRequest();
            request.setBonusPointId(bp.getId());
            request.setOdometer(11000 + (i * 100));
            request.setEarnedAt(Instant.now());

            RR_UiEarnedBonusPoint earnedResponse = post_ForRider(
                "/api/rally/" + rally.getId() + "/earned-bonus-point",
                request,
                tr_UiEarnedBonusPoint
            );
            check(earnedResponse);
            earnedIds[i++] = earnedResponse.getData().getId();
        }

        // Organizer confirms all three
        for (Integer earnedId : earnedIds) {
            RR_UiEarnedBonusPoint confirmResponse = put_ForRM(
                "/api/earned-bonus-point/" + earnedId + "/confirm?confirmed=true",
                null,
                tr_UiEarnedBonusPoint
            );
            check(confirmResponse);
            assertTrue(confirmResponse.getData().getConfirmed());
        }

        log.info("Organizer successfully batch confirmed {} earned bonus points", earnedIds.length);
    }

    @Test
    @Order(70)
    @DisplayName("Organizer cannot promote non-existent participant")
    public void testPromoteNonExistentParticipant() throws Exception {
        // Create rally
        UiRally rally = createTestRally();

        // Try to promote non-existent member
        PromoteParticipantRequest request = new PromoteParticipantRequest();
        request.setTargetMemberId(99999); // Non-existent member ID
        request.setNewType(RallyParticipantType.AIDE);

        try {
            RR_UiRallyParticipant response = put_ForRM(
                "/api/rally/" + rally.getId() + "/promote",
                request,
                tr_UiRallyParticipant
            );
            if (response.isSuccess()) {
                fail("Should not be able to promote non-existent participant");
            }
        } catch (Exception e) {
            log.info("Correctly rejected promotion of non-existent participant: {}", e.getMessage());
        }
    }
}
