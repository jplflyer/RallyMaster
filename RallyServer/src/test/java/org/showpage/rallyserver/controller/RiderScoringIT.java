package org.showpage.rallyserver.controller;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.showpage.rallyserver.IntegrationTest;
import org.showpage.rallyserver.ui.*;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Rider scoring operations.
 * Tests riders submitting their own bonus points, combinations, and odometer readings.
 */
@Slf4j
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class RiderScoringIT extends IntegrationTest {

    //======================================================================
    // Odometer settings. Riders can't set their own.
    //======================================================================

    /**
     * This is several tests that should all fail.
     */
    @Test
    @Order(2)
    @DisplayName("Setting odometer request must be properly created")
    public void properSetOdometer() throws Exception {
        // Create rally and register
        UiRally rally = createTestRally();
        RR_UiRallyParticipant regRR = post_ForRider("/api/rally/" + rally.getId() + "/register", null, tr_UiRallyParticipant);
        check(regRR);

        // No request body.
        RR_UiRallyParticipant response = put_ForRM(
                "/api/rally/" + rally.getId() + "/odometer/start",
                null,
                tr_UiRallyParticipant
        );
        checkFailed(response);

        // No rider ID.
        UpdateOdometerRequest request = UpdateOdometerRequest
                .builder()
                .odometer(12345)
                .build();

        response = put_ForRM(
                "/api/rally/" + rally.getId() + "/odometer/start",
                request,
                tr_UiRallyParticipant
        );
        checkFailed(response);

        // No odometer
        request = UpdateOdometerRequest
                .builder()
                .riderId(rally.getId())
                .build();

        response = put_ForRM(
                "/api/rally/" + rally.getId() + "/odometer/start",
                request,
                tr_UiRallyParticipant
        );
        checkFailed(response);
    }

    @Test
    @Order(4)
    @DisplayName("Rider cannot set their starting odometer")
    public void testSetStartingOdometer() throws Exception {
        // Create rally and register
        UiRally rally = createTestRally();
        RR_UiRallyParticipant regRR = post_ForRider("/api/rally/" + rally.getId() + "/register", null, tr_UiRallyParticipant);
        check(regRR);

        // Set starting odometer
        UpdateOdometerRequest request = UpdateOdometerRequest
                .builder()
                .riderId(rider.getId())
                .odometer(12345)
                .build();

        RR_UiRallyParticipant response = put_ForRider(
            "/api/rally/" + rally.getId() + "/odometer/start",
            request,
            tr_UiRallyParticipant
        );
        checkFailed(response);
    }

    @Test
    @Order(6)
    @DisplayName("Rider cannot set their ending odometer")
    public void testSetEndingOdometer() throws Exception {
        // Create rally and register
        UiRally rally = createTestRally();
        post_ForRider("/api/rally/" + rally.getId() + "/register", null, tr_UiRallyParticipant);

        // Set ending odometer
        UpdateOdometerRequest endRequest = UpdateOdometerRequest
                .builder()
                .riderId(rider.getId())
                .odometer(12845)
                .build();

        RR_UiRallyParticipant response = put_ForRider(
            "/api/rally/" + rally.getId() + "/odometer/end",
            endRequest,
            tr_UiRallyParticipant
        );
        checkFailed(response);
    }

    //======================================================================
    // Earned bonus points.
    //======================================================================

    @Test
    @Order(10)
    @DisplayName("Rider can submit an earned bonus point")
    public void testSubmitEarnedBonusPoint() throws Exception {
        // Create rally with bonus point and register
        UiRally rally = createTestRally();
        UiBonusPoint bp = createTestBonusPoint(rally.getId(), "BP1", 100);
        post_ForRider("/api/rally/" + rally.getId() + "/register", null, tr_UiRallyParticipant);

        // Submit earned bonus point
        CreateEarnedBonusPointRequest request = CreateEarnedBonusPointRequest
                .builder()
                .riderId(rider.getId())
                .bonusPointId(bp.getId())
                .odometer(13000)
                .earnedAt(Instant.now())
                .build();

        RR_UiEarnedBonusPoint response = post_ForRider(
            "/api/rally/" + rally.getId() + "/earned-bonus-point",
            request,
            tr_UiEarnedBonusPoint
        );
        check(response);

        UiEarnedBonusPoint earned = response.getData();
        assertNotNull(earned);
        assertNotNull(earned.getId());
        assertEquals(bp.getId(), earned.getBonusPointId());
        assertFalse(earned.getConfirmed(), "Should not be confirmed yet");
    }

    @Test
    @Order(11)
    @DisplayName("Rider can submit multiple earned bonus points")
    public void testSubmitMultipleBonusPoints() throws Exception {
        // Create rally with multiple bonus points and register
        UiRally rally = createTestRally();
        UiBonusPoint bp1 = createTestBonusPoint(rally.getId(), "BP1", 100);
        UiBonusPoint bp2 = createTestBonusPoint(rally.getId(), "BP2", 150);
        UiBonusPoint bp3 = createTestBonusPoint(rally.getId(), "BP3", 200);
        RR_UiRallyParticipant regResponse = post_ForRider("/api/rally/" + rally.getId() + "/register", null, tr_UiRallyParticipant);
        check(regResponse);

        // Submit three earned bonus points
        for (UiBonusPoint bp : List.of(bp1, bp2, bp3)) {
            CreateEarnedBonusPointRequest request = CreateEarnedBonusPointRequest
                    .builder()
                    .riderId(rider.getId())
                    .bonusPointId(bp.getId())
                    .odometer(13000 + bp.getPoints())
                    .earnedAt(Instant.now())
                    .build();

            RR_UiEarnedBonusPoint response = post_ForRider(
                "/api/rally/" + rally.getId() + "/earned-bonus-point",
                request,
                tr_UiEarnedBonusPoint
            );
            check(response);
        }

        log.info("Successfully submitted 3 earned bonus points");
    }

    @Test
    @Order(12)
    @DisplayName("Rider can view their own earned bonus points")
    public void testViewOwnEarnedBonusPoints() throws Exception {
        // Create rally, register, and submit earned points
        UiRally rally = createTestRally();
        UiBonusPoint bp1 = createTestBonusPoint(rally.getId(), "BP1", 100);
        UiBonusPoint bp2 = createTestBonusPoint(rally.getId(), "BP2", 150);
        RR_UiRallyParticipant regResponse = post_ForRider("/api/rally/" + rally.getId() + "/register", null, tr_UiRallyParticipant);
        check(regResponse);
        Integer participantId = regResponse.getData().getId();

        // Submit two earned bonus points
        for (UiBonusPoint bp : List.of(bp1, bp2)) {
            CreateEarnedBonusPointRequest request = new CreateEarnedBonusPointRequest();
            request.setBonusPointId(bp.getId());
            request.setOdometer(13000);
            request.setEarnedAt(Instant.now());
            post_ForRider("/api/rally/" + rally.getId() + "/earned-bonus-point", request, tr_UiEarnedBonusPoint);
        }

        // Retrieve earned bonus points
        RR_ListUiEarnedBonusPoint response = get_ForRider(
            "/api/rally-participant/" + participantId + "/earned-bonus-points",
            tr_ListUiEarnedBonusPoint
        );
        check(response);

        List<UiEarnedBonusPoint> earnedPoints = response.getData();
        assertEquals(2, earnedPoints.size(), "Should have 2 earned bonus points");
        log.info("Retrieved {} earned bonus points", earnedPoints.size());
    }

    //======================================================================
    // Earned Combinations.
    //======================================================================
    @Test
    @Order(20)
    @DisplayName("Rider can submit an earned combination")
    public void testSubmitEarnedCombination() throws Exception {
        // Create rally with combination and register
        UiRally rally = createTestRally();
        UiBonusPoint bp1 = createTestBonusPoint(rally.getId(), "BP1", 100);
        UiBonusPoint bp2 = createTestBonusPoint(rally.getId(), "BP2", 150);
        UiCombination combo = createTestCombination(rally.getId(), List.of(bp1.getId(), bp2.getId()));
        post_ForRider("/api/rally/" + rally.getId() + "/register", null, tr_UiRallyParticipant);

        // Submit earned combination
        CreateEarnedCombinationRequest request = new CreateEarnedCombinationRequest();
        request.setCombinationId(combo.getId());

        RR_UiEarnedCombination response = post_ForRider(
            "/api/rally/" + rally.getId() + "/earned-combination",
            request,
            tr_UiEarnedCombination
        );
        check(response);

        UiEarnedCombination earned = response.getData();
        assertNotNull(earned);
        assertNotNull(earned.getId());
        assertEquals(combo.getId(), earned.getCombinationId());
        assertFalse(earned.getConfirmed(), "Should not be confirmed yet");
        log.info("Submitted earned combination, ID: {}", earned.getId());
    }

    @Test
    @Order(21)
    @DisplayName("Rider can view their own earned combinations")
    public void testViewOwnEarnedCombinations() throws Exception {
        // Create rally, register, and submit earned combination
        UiRally rally = createTestRally();
        UiBonusPoint bp1 = createTestBonusPoint(rally.getId(), "BP1", 100);
        UiBonusPoint bp2 = createTestBonusPoint(rally.getId(), "BP2", 150);
        UiCombination combo = createTestCombination(rally.getId(), List.of(bp1.getId(), bp2.getId()));
        RR_UiRallyParticipant regResponse = post_ForRider("/api/rally/" + rally.getId() + "/register", null, tr_UiRallyParticipant);
        check(regResponse);
        Integer participantId = regResponse.getData().getId();

        // Submit earned combination
        CreateEarnedCombinationRequest request = new CreateEarnedCombinationRequest();
        request.setCombinationId(combo.getId());
        post_ForRider("/api/rally/" + rally.getId() + "/earned-combination", request, tr_UiEarnedCombination);

        // Retrieve earned combinations
        RR_ListUiEarnedCombination response = get_ForRider(
            "/api/rally-participant/" + participantId + "/earned-combinations",
            tr_ListUiEarnedCombination
        );
        check(response);

        List<UiEarnedCombination> earnedCombos = response.getData();
        assertEquals(1, earnedCombos.size(), "Should have 1 earned combination");
        log.info("Retrieved {} earned combinations", earnedCombos.size());
    }

    //======================================================================
    // Other things riders can't do.
    //======================================================================
    @Test
    @Order(30)
    @DisplayName("Rider cannot confirm their own earned bonus points")
    public void testRiderCannotConfirmOwnBonusPoints() throws Exception {
        // Create rally, register, and submit earned point
        UiRally rally = createTestRally();
        UiBonusPoint bp = createTestBonusPoint(rally.getId(), "BP1", 100);
        post_ForRider("/api/rally/" + rally.getId() + "/register", null, tr_UiRallyParticipant);

        CreateEarnedBonusPointRequest request = new CreateEarnedBonusPointRequest();
        request.setBonusPointId(bp.getId());
        request.setOdometer(13000);
        request.setEarnedAt(Instant.now());

        RR_UiEarnedBonusPoint earnedResponse = post_ForRider(
            "/api/rally/" + rally.getId() + "/earned-bonus-point",
            request,
            tr_UiEarnedBonusPoint
        );
        check(earnedResponse);
        Integer earnedId = earnedResponse.getData().getId();

        // Try to confirm it as rider - should fail
        try {
            RR_UiEarnedBonusPoint confirmResponse = put_ForRider(
                "/api/earned-bonus-point/" + earnedId + "/confirm?confirmed=true",
                null,
                tr_UiEarnedBonusPoint
            );
            if (confirmResponse.isSuccess() && confirmResponse.getData().getConfirmed()) {
                fail("Rider should not be able to confirm their own bonus points");
            }
        } catch (Exception e) {
            log.info("Correctly prevented rider from confirming own bonus point");
        }
    }

    @Test
    @Order(31)
    @DisplayName("Rider cannot confirm their own earned combinations")
    public void testRiderCannotConfirmOwnCombinations() throws Exception {
        // Create rally, register, and submit earned combination
        UiRally rally = createTestRally();
        UiBonusPoint bp1 = createTestBonusPoint(rally.getId(), "BP1", 100);
        UiBonusPoint bp2 = createTestBonusPoint(rally.getId(), "BP2", 150);
        UiCombination combo = createTestCombination(rally.getId(), List.of(bp1.getId(), bp2.getId()));
        post_ForRider("/api/rally/" + rally.getId() + "/register", null, tr_UiRallyParticipant);

        CreateEarnedCombinationRequest request = new CreateEarnedCombinationRequest();
        request.setCombinationId(combo.getId());

        RR_UiEarnedCombination earnedResponse = post_ForRider(
            "/api/rally/" + rally.getId() + "/earned-combination",
            request,
            tr_UiEarnedCombination
        );
        check(earnedResponse);
        Integer earnedId = earnedResponse.getData().getId();

        // Try to confirm it as rider - should fail
        try {
            RR_UiEarnedCombination confirmResponse = put_ForRider(
                "/api/earned-combination/" + earnedId + "/confirm?confirmed=true",
                null,
                tr_UiEarnedCombination
            );
            if (confirmResponse.isSuccess() && confirmResponse.getData().getConfirmed()) {
                fail("Rider should not be able to confirm their own combinations");
            }
        } catch (Exception e) {
            log.info("Correctly prevented rider from confirming own combination");
        }
    }

    @Test
    @Order(40)
    @DisplayName("Rider cannot submit earned points for non-existent bonus point")
    public void testSubmitInvalidBonusPoint() throws Exception {
        // Create rally and register
        UiRally rally = createTestRally();
        post_ForRider("/api/rally/" + rally.getId() + "/register", null, tr_UiRallyParticipant);

        // Try to submit earned point for non-existent bonus point
        CreateEarnedBonusPointRequest request = new CreateEarnedBonusPointRequest();
        request.setBonusPointId(99999); // Non-existent ID
        request.setOdometer(13000);
        request.setEarnedAt(Instant.now());

        try {
            RR_UiEarnedBonusPoint response = post_ForRider(
                "/api/rally/" + rally.getId() + "/earned-bonus-point",
                request,
                tr_UiEarnedBonusPoint
            );
            if (response.isSuccess()) {
                fail("Should not be able to submit earned point for non-existent bonus point");
            }
        } catch (Exception e) {
            log.info("Correctly rejected invalid bonus point: {}", e.getMessage());
        }
    }

    @Test
    @Order(41)
    @DisplayName("Rider cannot submit earned points without being registered")
    public void testSubmitWithoutRegistration() throws Exception {
        // Create rally with bonus point but DON'T register
        UiRally rally = createTestRally();
        UiBonusPoint bp = createTestBonusPoint(rally.getId(), "BP1", 100);

        // Try to submit earned point without being registered
        CreateEarnedBonusPointRequest request = new CreateEarnedBonusPointRequest();
        request.setBonusPointId(bp.getId());
        request.setOdometer(13000);
        request.setEarnedAt(Instant.now());

        try {
            RR_UiEarnedBonusPoint response = post_ForRider(
                "/api/rally/" + rally.getId() + "/earned-bonus-point",
                request,
                tr_UiEarnedBonusPoint
            );
            if (response.isSuccess()) {
                fail("Should not be able to submit earned points without being registered");
            }
        } catch (Exception e) {
            log.info("Correctly rejected submission without registration: {}", e.getMessage());
        }
    }
}
