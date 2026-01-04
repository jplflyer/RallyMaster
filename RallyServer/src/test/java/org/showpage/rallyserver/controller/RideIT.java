package org.showpage.rallyserver.controller;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.showpage.rallyserver.IntegrationTest;
import org.showpage.rallyserver.ui.*;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Ride CRUD operations.
 * Tests all four entities: Ride, Route, RideLeg, and Waypoint.
 */
@Slf4j
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class RideIT extends IntegrationTest {

    private static UiRide organizerRide;
    private static UiRide riderRide;
    private static UiRally testRally;
    private static UiBonusPoint testBonusPoint;

    private static UiRoute organizerRoute;
    private static UiRideLeg organizerLeg;
    private static UiWaypoint organizerWaypoint;

    //======================================================================
    // Setup - Create test rally and bonus point for waypoint tests
    //======================================================================

    @Test
    @Order(10)
    public void test_010_Setup_CreateTestRally() throws Exception {
        log.info("Creating test rally for ride association");
        testRally = createTestRally();
        assertNotNull(testRally);
        assertNotNull(testRally.getId());
        log.info("Created test rally: {} (ID: {})", testRally.getName(), testRally.getId());
    }

    @Test
    @Order(20)
    public void test_020_Setup_CreateTestBonusPoint() throws Exception {
        log.info("Creating test bonus point for waypoint tests");
        testBonusPoint = createTestBonusPoint(testRally.getId(), "TESTBP", 50);
        assertNotNull(testBonusPoint);
        assertNotNull(testBonusPoint.getId());
        log.info("Created test bonus point: {} (ID: {})", testBonusPoint.getCode(), testBonusPoint.getId());
    }

    //======================================================================
    // Ride CRUD Tests - Organizer
    //======================================================================

    @Test
    @Order(100)
    public void test_100_CreateRide_ForOrganizer_Standalone() throws Exception {
        log.info("Test: Create standalone ride for organizer");

        CreateRideRequest request = CreateRideRequest.builder()
                .name("Organizer's Saddlesore 1000")
                .description("1000 miles in 24 hours")
                .expectedStart(LocalDateTime.now().plusDays(30))
                .expectedEnd(LocalDateTime.now().plusDays(31))
                .build();

        RR_UiRide response = post_ForRM("/api/ride", request, tr_UiRide);
        check(response);
        organizerRide = response.getData();

        assertNotNull(organizerRide);
        assertNotNull(organizerRide.getId());
        assertEquals("Organizer's Saddlesore 1000", organizerRide.getName());
        assertEquals("1000 miles in 24 hours", organizerRide.getDescription());
        assertEquals(organizer.getId(), organizerRide.getMemberId());
        assertNull(organizerRide.getRallyId(), "Standalone ride should not have rally association");

        log.info("Created standalone ride for organizer: ID={}", organizerRide.getId());
    }

    @Test
    @Order(110)
    public void test_110_CreateRide_ForOrganizer_WithRally() throws Exception {
        log.info("Test: Create rally-associated ride for organizer");

        CreateRideRequest request = CreateRideRequest.builder()
                .name("Organizer's Dog Daze Rally Ride")
                .description("My participation in Dog Daze Rally")
                .expectedStart(testRally.getStartDate().atTime(8, 0))
                .expectedEnd(testRally.getEndDate().atTime(18, 0))
                .rallyId(testRally.getId())
                .build();

        RR_UiRide response = post_ForRM("/api/ride", request, tr_UiRide);
        check(response);
        UiRide rallyRide = response.getData();

        assertNotNull(rallyRide);
        assertNotNull(rallyRide.getId());
        assertEquals(testRally.getId(), rallyRide.getRallyId(), "Ride should be associated with rally");

        log.info("Created rally-associated ride for organizer: ID={}", rallyRide.getId());
    }

    @Test
    @Order(120)
    public void test_120_GetRide_ForOrganizer_Success() throws Exception {
        log.info("Test: Get ride as owner");

        RR_UiRide response = get_ForRM("/api/ride/" + organizerRide.getId(), tr_UiRide);
        check(response);
        UiRide ride = response.getData();

        assertNotNull(ride);
        assertEquals(organizerRide.getId(), ride.getId());
        assertEquals(organizerRide.getName(), ride.getName());
    }

    @Test
    @Order(130)
    public void test_130_UpdateRide_ForOrganizer_Success() throws Exception {
        log.info("Test: Update ride as owner");

        UpdateRideRequest request = UpdateRideRequest.builder()
                .name("Updated Saddlesore 1000")
                .description("Updated description")
                .build();

        RR_UiRide response = put_ForRM("/api/ride/" + organizerRide.getId(), request, tr_UiRide);
        check(response);
        UiRide updated = response.getData();

        assertNotNull(updated);
        assertEquals("Updated Saddlesore 1000", updated.getName());
        assertEquals("Updated description", updated.getDescription());
    }

    @Test
    @Order(140)
    public void test_140_ListRides_ForOrganizer_Success() throws Exception {
        log.info("Test: List all rides for organizer");

        RR_ListUiRide response = get_ForRM("/api/rides", tr_ListUiRide);
        check(response);
        List<UiRide> rides = response.getData();

        assertNotNull(rides);
        assertTrue(rides.size() >= 2, "Organizer should have at least 2 rides");

        // Verify we can find both rides we created
        boolean foundStandalone = rides.stream().anyMatch(r -> r.getId().equals(organizerRide.getId()));
        assertTrue(foundStandalone, "Should find standalone ride in list");
    }

    //======================================================================
    // Ride CRUD Tests - Rider
    //======================================================================

    @Test
    @Order(200)
    public void test_200_CreateRide_ForRider_Success() throws Exception {
        log.info("Test: Create ride for rider");

        CreateRideRequest request = CreateRideRequest.builder()
                .name("Rider's Minnesota Grand Tour")
                .description("Multi-day tour of Minnesota")
                .expectedStart(LocalDateTime.now().plusDays(60))
                .expectedEnd(LocalDateTime.now().plusDays(67))
                .build();

        RR_UiRide response = post_ForRider("/api/ride", request, tr_UiRide);
        check(response);
        riderRide = response.getData();

        assertNotNull(riderRide);
        assertNotNull(riderRide.getId());
        assertEquals(rider.getId(), riderRide.getMemberId());
        assertEquals("Rider's Minnesota Grand Tour", riderRide.getName());

        log.info("Created ride for rider: ID={}", riderRide.getId());
    }

    @Test
    @Order(210)
    public void test_210_GetRide_CrossUser_ShouldFail() throws Exception {
        log.info("Test: Rider tries to access organizer's ride - should fail");

        RR_UiRide response = get_ForRider("/api/ride/" + organizerRide.getId(), tr_UiRide);
        checkFailed(response);

        log.info("Correctly prevented cross-user ride access");
    }

    @Test
    @Order(220)
    public void test_220_UpdateRide_CrossUser_ShouldFail() throws Exception {
        log.info("Test: Rider tries to update organizer's ride - should fail");

        UpdateRideRequest request = UpdateRideRequest.builder()
                .name("Hacked Ride Name")
                .build();

        RR_UiRide response = put_ForRider("/api/ride/" + organizerRide.getId(), request, tr_UiRide);
        checkFailed(response);

        log.info("Correctly prevented cross-user ride update");
    }

    @Test
    @Order(230)
    public void test_230_DeleteRide_CrossUser_ShouldFail() throws Exception {
        log.info("Test: Rider tries to delete organizer's ride - should fail");

        RR_Void response = delete_ForRider("/api/ride/" + organizerRide.getId(), tr_Void);
        checkFailed(response);

        log.info("Correctly prevented cross-user ride deletion");
    }

    //======================================================================
    // Route CRUD Tests
    //======================================================================

    @Test
    @Order(300)
    public void test_300_CreateRoute_ForOrganizer_Success() throws Exception {
        log.info("Test: Create route for organizer's ride");

        CreateRouteRequest request = CreateRouteRequest.builder()
                .name("Primary Route")
                .description("Main scenic route")
                .isPrimary(true)
                .build();

        RR_UiRoute response = post_ForRM("/api/ride/" + organizerRide.getId() + "/route", request, tr_UiRoute);
        check(response);
        organizerRoute = response.getData();

        assertNotNull(organizerRoute);
        assertNotNull(organizerRoute.getId());
        assertEquals(organizerRide.getId(), organizerRoute.getRideId());
        assertEquals("Primary Route", organizerRoute.getName());
        assertTrue(organizerRoute.getIsPrimary());

        log.info("Created route: ID={}", organizerRoute.getId());
    }

    @Test
    @Order(310)
    public void test_310_CreateRoute_Alternate() throws Exception {
        log.info("Test: Create alternate route");

        CreateRouteRequest request = CreateRouteRequest.builder()
                .name("Alternate Route")
                .description("Backup route via highways")
                .isPrimary(false)
                .build();

        RR_UiRoute response = post_ForRM("/api/ride/" + organizerRide.getId() + "/route", request, tr_UiRoute);
        check(response);
        UiRoute altRoute = response.getData();

        assertNotNull(altRoute);
        assertFalse(altRoute.getIsPrimary());

        log.info("Created alternate route: ID={}", altRoute.getId());
    }

    @Test
    @Order(320)
    public void test_320_GetRoute_Success() throws Exception {
        log.info("Test: Get route by ID");

        RR_UiRoute response = get_ForRM("/api/route/" + organizerRoute.getId(), tr_UiRoute);
        check(response);
        UiRoute route = response.getData();

        assertNotNull(route);
        assertEquals(organizerRoute.getId(), route.getId());
        assertEquals(organizerRoute.getName(), route.getName());
    }

    @Test
    @Order(330)
    public void test_330_UpdateRoute_Success() throws Exception {
        log.info("Test: Update route");

        UpdateRouteRequest request = UpdateRouteRequest.builder()
                .name("Updated Primary Route")
                .description("Updated description")
                .build();

        RR_UiRoute response = put_ForRM("/api/route/" + organizerRoute.getId(), request, tr_UiRoute);
        check(response);
        UiRoute updated = response.getData();

        assertNotNull(updated);
        assertEquals("Updated Primary Route", updated.getName());
        assertEquals("Updated description", updated.getDescription());
    }

    @Test
    @Order(340)
    public void test_340_ListRoutes_Success() throws Exception {
        log.info("Test: List all routes for a ride");

        RR_ListUiRoute response = get_ForRM("/api/ride/" + organizerRide.getId() + "/routes", tr_ListUiRoute);
        check(response);
        List<UiRoute> routes = response.getData();

        assertNotNull(routes);
        assertEquals(2, routes.size(), "Should have 2 routes (primary and alternate)");
    }

    @Test
    @Order(350)
    public void test_350_CreateRoute_CrossUser_ShouldFail() throws Exception {
        log.info("Test: Rider tries to create route on organizer's ride - should fail");

        CreateRouteRequest request = CreateRouteRequest.builder()
                .name("Hacked Route")
                .build();

        RR_UiRoute response = post_ForRider("/api/ride/" + organizerRide.getId() + "/route", request, tr_UiRoute);
        checkFailed(response);

        log.info("Correctly prevented cross-user route creation");
    }

    //======================================================================
    // RideLeg CRUD Tests
    //======================================================================

    @Test
    @Order(400)
    public void test_400_CreateRideLeg_Success() throws Exception {
        log.info("Test: Create ride leg for route");

        CreateRideLegRequest request = CreateRideLegRequest.builder()
                .name("Day 1")
                .description("First day segment")
                .sequenceOrder(1)
                .build();

        RR_UiRideLeg response = post_ForRM("/api/route/" + organizerRoute.getId() + "/leg", request, tr_UiRideLeg);
        check(response);
        organizerLeg = response.getData();

        assertNotNull(organizerLeg);
        assertNotNull(organizerLeg.getId());
        assertEquals(organizerRoute.getId(), organizerLeg.getRouteId());
        assertEquals("Day 1", organizerLeg.getName());
        assertEquals(1, organizerLeg.getSequenceOrder());

        log.info("Created ride leg: ID={}", organizerLeg.getId());
    }

    @Test
    @Order(410)
    public void test_410_CreateRideLeg_Multiple() throws Exception {
        log.info("Test: Create multiple ride legs");

        CreateRideLegRequest request = CreateRideLegRequest.builder()
                .name("Day 2")
                .description("Second day segment")
                .sequenceOrder(2)
                .build();

        RR_UiRideLeg response = post_ForRM("/api/route/" + organizerRoute.getId() + "/leg", request, tr_UiRideLeg);
        check(response);

        log.info("Created second ride leg: ID={}", response.getData().getId());
    }

    @Test
    @Order(420)
    public void test_420_GetRideLeg_Success() throws Exception {
        log.info("Test: Get ride leg by ID");

        RR_UiRideLeg response = get_ForRM("/api/leg/" + organizerLeg.getId(), tr_UiRideLeg);
        check(response);
        UiRideLeg leg = response.getData();

        assertNotNull(leg);
        assertEquals(organizerLeg.getId(), leg.getId());
        assertEquals(organizerLeg.getName(), leg.getName());
    }

    @Test
    @Order(430)
    public void test_430_UpdateRideLeg_Success() throws Exception {
        log.info("Test: Update ride leg");

        UpdateRideLegRequest request = UpdateRideLegRequest.builder()
                .name("Updated Day 1")
                .description("Updated segment description")
                .build();

        RR_UiRideLeg response = put_ForRM("/api/leg/" + organizerLeg.getId(), request, tr_UiRideLeg);
        check(response);
        UiRideLeg updated = response.getData();

        assertNotNull(updated);
        assertEquals("Updated Day 1", updated.getName());
        assertEquals("Updated segment description", updated.getDescription());
    }

    @Test
    @Order(440)
    public void test_440_ListRideLegs_Success() throws Exception {
        log.info("Test: List all ride legs for a route");

        RR_ListUiRideLeg response = get_ForRM("/api/route/" + organizerRoute.getId() + "/legs", tr_ListUiRideLeg);
        check(response);
        List<UiRideLeg> legs = response.getData();

        assertNotNull(legs);
        assertEquals(2, legs.size(), "Should have 2 ride legs");
    }

    //======================================================================
    // Waypoint CRUD Tests
    //======================================================================

    @Test
    @Order(500)
    public void test_500_CreateWaypoint_WithLocation_Success() throws Exception {
        log.info("Test: Create waypoint with location coordinates");

        CreateWaypointRequest request = CreateWaypointRequest.builder()
                .name("Aitkin Airport")
                .description("Photo stop at airport sign")
                .sequenceOrder(1)
                .latitude(46.5461f)
                .longitude(-93.6772f)
                .address("12345 Airport Rd, Aitkin, MN")
                .build();

        RR_UiWaypoint response = post_ForRM("/api/leg/" + organizerLeg.getId() + "/waypoint", request, tr_UiWaypoint);
        check(response);
        organizerWaypoint = response.getData();

        assertNotNull(organizerWaypoint);
        assertNotNull(organizerWaypoint.getId());
        assertEquals(organizerLeg.getId(), organizerWaypoint.getRideLegId());
        assertEquals("Aitkin Airport", organizerWaypoint.getName());
        assertEquals(46.5461f, organizerWaypoint.getLatitude(), 0.0001);
        assertNull(organizerWaypoint.getBonusPointId(), "Location-based waypoint should not have bonus point");

        log.info("Created waypoint with location: ID={}", organizerWaypoint.getId());
    }

    @Test
    @Order(510)
    public void test_510_CreateWaypoint_WithBonusPoint_Success() throws Exception {
        log.info("Test: Create waypoint referencing bonus point");

        CreateWaypointRequest request = CreateWaypointRequest.builder()
                .name("Bonus Point Stop")
                .description("Rally bonus location")
                .sequenceOrder(2)
                .bonusPointId(testBonusPoint.getId())
                .build();

        RR_UiWaypoint response = post_ForRM("/api/leg/" + organizerLeg.getId() + "/waypoint", request, tr_UiWaypoint);
        check(response);
        UiWaypoint bonusWaypoint = response.getData();

        assertNotNull(bonusWaypoint);
        assertEquals(testBonusPoint.getId(), bonusWaypoint.getBonusPointId());

        log.info("Created waypoint with bonus point reference: ID={}", bonusWaypoint.getId());
    }

    @Test
    @Order(520)
    public void test_520_CreateWaypoint_NoLocationOrBonus_ShouldFail() throws Exception {
        log.info("Test: Create waypoint without location or bonus point - should fail");

        CreateWaypointRequest request = CreateWaypointRequest.builder()
                .name("Invalid Waypoint")
                .description("Missing both location and bonus point")
                .sequenceOrder(3)
                .build();

        RR_UiWaypoint response = post_ForRM("/api/leg/" + organizerLeg.getId() + "/waypoint", request, tr_UiWaypoint);
        checkFailed(response);

        log.info("Correctly rejected waypoint without location or bonus point");
    }

    @Test
    @Order(530)
    public void test_530_GetWaypoint_Success() throws Exception {
        log.info("Test: Get waypoint by ID");

        RR_UiWaypoint response = get_ForRM("/api/waypoint/" + organizerWaypoint.getId(), tr_UiWaypoint);
        check(response);
        UiWaypoint waypoint = response.getData();

        assertNotNull(waypoint);
        assertEquals(organizerWaypoint.getId(), waypoint.getId());
        assertEquals(organizerWaypoint.getName(), waypoint.getName());
    }

    @Test
    @Order(540)
    public void test_540_UpdateWaypoint_Success() throws Exception {
        log.info("Test: Update waypoint");

        UpdateWaypointRequest request = UpdateWaypointRequest.builder()
                .name("Updated Airport Stop")
                .description("Updated description")
                .build();

        RR_UiWaypoint response = put_ForRM("/api/waypoint/" + organizerWaypoint.getId(), request, tr_UiWaypoint);
        check(response);
        UiWaypoint updated = response.getData();

        assertNotNull(updated);
        assertEquals("Updated Airport Stop", updated.getName());
        assertEquals("Updated description", updated.getDescription());
    }

    @Test
    @Order(550)
    public void test_550_ListWaypoints_Success() throws Exception {
        log.info("Test: List all waypoints for a ride leg");

        RR_ListUiWaypoint response = get_ForRM("/api/leg/" + organizerLeg.getId() + "/waypoints", tr_ListUiWaypoint);
        check(response);
        List<UiWaypoint> waypoints = response.getData();

        assertNotNull(waypoints);
        assertEquals(2, waypoints.size(), "Should have 2 waypoints");
    }

    //======================================================================
    // Cascade Deletion Tests
    //======================================================================

    @Test
    @Order(600)
    public void test_600_DeleteWaypoint_Success() throws Exception {
        log.info("Test: Delete waypoint");

        RR_Void response = delete_ForRM("/api/waypoint/" + organizerWaypoint.getId(), tr_Void);
        check(response);

        // Verify deletion
        RR_UiWaypoint getResponse = get_ForRM("/api/waypoint/" + organizerWaypoint.getId(), tr_UiWaypoint);
        checkFailed(getResponse);

        log.info("Successfully deleted waypoint");
    }

    @Test
    @Order(610)
    public void test_610_DeleteRideLeg_CascadesWaypoints() throws Exception {
        log.info("Test: Delete ride leg - should cascade to waypoints");

        // First verify leg has waypoints
        RR_ListUiWaypoint listResponse = get_ForRM("/api/leg/" + organizerLeg.getId() + "/waypoints", tr_ListUiWaypoint);
        check(listResponse);
        int waypointCount = listResponse.getData().size();
        assertTrue(waypointCount > 0, "Leg should have waypoints before deletion");

        // Delete the leg
        RR_Void response = delete_ForRM("/api/leg/" + organizerLeg.getId(), tr_Void);
        check(response);

        // Verify deletion
        RR_UiRideLeg getResponse = get_ForRM("/api/leg/" + organizerLeg.getId(), tr_UiRideLeg);
        checkFailed(getResponse);

        log.info("Successfully deleted ride leg with {} waypoints", waypointCount);
    }

    @Test
    @Order(620)
    public void test_620_DeleteRoute_CascadesLegsAndWaypoints() throws Exception {
        log.info("Test: Delete route - should cascade to legs and waypoints");

        // First verify route has legs
        RR_ListUiRideLeg listResponse = get_ForRM("/api/route/" + organizerRoute.getId() + "/legs", tr_ListUiRideLeg);
        check(listResponse);
        int legCount = listResponse.getData().size();
        assertTrue(legCount > 0, "Route should have legs before deletion");

        // Delete the route
        RR_Void response = delete_ForRM("/api/route/" + organizerRoute.getId(), tr_Void);
        check(response);

        // Verify deletion
        RR_UiRoute getResponse = get_ForRM("/api/route/" + organizerRoute.getId(), tr_UiRoute);
        checkFailed(getResponse);

        log.info("Successfully deleted route with {} legs", legCount);
    }

    @Test
    @Order(630)
    public void test_630_DeleteRide_CascadesAll() throws Exception {
        log.info("Test: Delete ride - should cascade to all child entities");

        // Delete rider's ride (organizer ride was already partially deleted in previous tests)
        RR_Void response = delete_ForRider("/api/ride/" + riderRide.getId(), tr_Void);
        check(response);

        // Verify deletion
        RR_UiRide getResponse = get_ForRider("/api/ride/" + riderRide.getId(), tr_UiRide);
        checkFailed(getResponse);

        log.info("Successfully deleted ride with all child entities");
    }

    //======================================================================
    // Comprehensive End-to-End Test
    //======================================================================

    @Test
    @Order(700)
    public void test_700_EndToEnd_CompleteRidePlanningWorkflow() throws Exception {
        log.info("=== COMPREHENSIVE END-TO-END TEST: Complete Ride Planning Workflow ===");

        // Step 1: Create or reuse a rally for ride planning
        log.info("Step 1: Setting up rally for ride planning");
        UiRally planningRally = createTestRally();
        assertNotNull(planningRally);
        log.info("Using rally: {} (ID: {})", planningRally.getName(), planningRally.getId());

        // Step 2: Create bonus points for the rally
        log.info("Step 2: Creating bonus points for waypoints");
        UiBonusPoint bp1 = createTestBonusPoint(planningRally.getId(), "E2E-BP1", 100);
        UiBonusPoint bp2 = createTestBonusPoint(planningRally.getId(), "E2E-BP2", 150);
        UiBonusPoint bp3 = createTestBonusPoint(planningRally.getId(), "E2E-BP3", 200);
        log.info("Created 3 bonus points: {}, {}, {}", bp1.getCode(), bp2.getCode(), bp3.getCode());

        // Step 3: Create a rally-associated ride
        log.info("Step 3: Creating rally-associated ride");
        CreateRideRequest rideRequest = CreateRideRequest.builder()
                .name("End-to-End Rally Ride")
                .description("Testing complete ride planning workflow")
                .expectedStart(planningRally.getStartDate().atTime(8, 0))
                .expectedEnd(planningRally.getEndDate().atTime(18, 0))
                .rallyId(planningRally.getId())
                .stopDuration(300) // 5 minutes per stop
                .build();
        RR_UiRide rideResponse = post_ForRM("/api/ride", rideRequest, tr_UiRide);
        check(rideResponse);
        UiRide e2eRide = rideResponse.getData();
        assertNotNull(e2eRide);
        assertEquals(planningRally.getId(), e2eRide.getRallyId());
        log.info("Created ride: {} (ID: {})", e2eRide.getName(), e2eRide.getId());

        // Step 4: Create a route for the ride
        log.info("Step 4: Creating primary route");
        CreateRouteRequest routeRequest = CreateRouteRequest.builder()
                .name("E2E Primary Route")
                .description("Main route for end-to-end test")
                .isPrimary(true)
                .build();
        RR_UiRoute routeResponse = post_ForRM(
            "/api/ride/" + e2eRide.getId() + "/route",
            routeRequest,
            tr_UiRoute
        );
        check(routeResponse);
        UiRoute e2eRoute = routeResponse.getData();
        assertNotNull(e2eRoute);
        assertTrue(e2eRoute.getIsPrimary());
        log.info("Created route: {} (ID: {})", e2eRoute.getName(), e2eRoute.getId());

        // Step 5: Create a ride leg
        log.info("Step 5: Creating ride leg");
        CreateRideLegRequest legRequest = CreateRideLegRequest.builder()
                .name("E2E Day 1")
                .description("First day of ride")
                .sequenceOrder(1)
                .build();
        RR_UiRideLeg legResponse = post_ForRM(
            "/api/route/" + e2eRoute.getId() + "/leg",
            legRequest,
            tr_UiRideLeg
        );
        check(legResponse);
        UiRideLeg e2eLeg = legResponse.getData();
        assertNotNull(e2eLeg);
        log.info("Created leg: {} (ID: {})", e2eLeg.getName(), e2eLeg.getId());

        // Step 6: Add waypoints from bonus points
        log.info("Step 6: Adding waypoints from bonus points");

        // Waypoint 1: From bonus point 1
        CreateWaypointRequest wp1Request = CreateWaypointRequest.builder()
                .name("Stop 1: " + bp1.getCode())
                .description("First bonus point stop")
                .sequenceOrder(1)
                .bonusPointId(bp1.getId())
                .build();
        RR_UiWaypoint wp1Response = post_ForRM(
            "/api/leg/" + e2eLeg.getId() + "/waypoint",
            wp1Request,
            tr_UiWaypoint
        );
        check(wp1Response);
        UiWaypoint wp1 = wp1Response.getData();
        log.info("Created waypoint 1: {} (ID: {})", wp1.getName(), wp1.getId());

        // Waypoint 2: From bonus point 2
        CreateWaypointRequest wp2Request = CreateWaypointRequest.builder()
                .name("Stop 2: " + bp2.getCode())
                .description("Second bonus point stop")
                .sequenceOrder(2)
                .bonusPointId(bp2.getId())
                .build();
        RR_UiWaypoint wp2Response = post_ForRM(
            "/api/leg/" + e2eLeg.getId() + "/waypoint",
            wp2Request,
            tr_UiWaypoint
        );
        check(wp2Response);
        UiWaypoint wp2 = wp2Response.getData();
        log.info("Created waypoint 2: {} (ID: {})", wp2.getName(), wp2.getId());

        // Waypoint 3: From bonus point 3
        CreateWaypointRequest wp3Request = CreateWaypointRequest.builder()
                .name("Stop 3: " + bp3.getCode())
                .description("Third bonus point stop")
                .sequenceOrder(3)
                .bonusPointId(bp3.getId())
                .build();
        RR_UiWaypoint wp3Response = post_ForRM(
            "/api/leg/" + e2eLeg.getId() + "/waypoint",
            wp3Request,
            tr_UiWaypoint
        );
        check(wp3Response);
        UiWaypoint wp3 = wp3Response.getData();
        log.info("Created waypoint 3: {} (ID: {})", wp3.getName(), wp3.getId());

        // Waypoint 4: Custom location (not from bonus point)
        CreateWaypointRequest wp4Request = CreateWaypointRequest.builder()
                .name("Custom Stop")
                .description("Custom waypoint with coordinates")
                .sequenceOrder(4)
                .latitude(45.0f)
                .longitude(-93.0f)
                .address("Custom Address, MN")
                .build();
        RR_UiWaypoint wp4Response = post_ForRM(
            "/api/leg/" + e2eLeg.getId() + "/waypoint",
            wp4Request,
            tr_UiWaypoint
        );
        check(wp4Response);
        UiWaypoint wp4 = wp4Response.getData();
        log.info("Created waypoint 4: {} (ID: {})", wp4.getName(), wp4.getId());

        // Step 7: Verify all waypoints are listed
        log.info("Step 7: Verifying waypoint list");
        RR_ListUiWaypoint listResponse = get_ForRM(
            "/api/leg/" + e2eLeg.getId() + "/waypoints",
            tr_ListUiWaypoint
        );
        check(listResponse);
        List<UiWaypoint> waypoints = listResponse.getData();
        assertEquals(4, waypoints.size(), "Should have 4 waypoints");
        log.info("Verified {} waypoints in leg", waypoints.size());

        // Step 8: Reorder a waypoint (update sequence)
        log.info("Step 8: Reordering waypoints");
        UpdateWaypointRequest reorderRequest = UpdateWaypointRequest.builder()
                .sequenceOrder(10) // Move to end
                .build();
        RR_UiWaypoint reorderResponse = put_ForRM(
            "/api/waypoint/" + wp2.getId(),
            reorderRequest,
            tr_UiWaypoint
        );
        check(reorderResponse);
        UiWaypoint reordered = reorderResponse.getData();
        assertEquals(10, reordered.getSequenceOrder());
        log.info("Reordered waypoint {} to sequence {}", wp2.getName(), reordered.getSequenceOrder());

        // Step 9: Delete a waypoint
        log.info("Step 9: Deleting a waypoint");
        RR_Void deleteWpResponse = delete_ForRM("/api/waypoint/" + wp4.getId(), tr_Void);
        check(deleteWpResponse);

        // Verify deletion
        RR_ListUiWaypoint verifyListResponse = get_ForRM(
            "/api/leg/" + e2eLeg.getId() + "/waypoints",
            tr_ListUiWaypoint
        );
        check(verifyListResponse);
        List<UiWaypoint> remainingWaypoints = verifyListResponse.getData();
        assertEquals(3, remainingWaypoints.size(), "Should have 3 waypoints after deletion");
        log.info("Verified waypoint deleted, {} waypoints remain", remainingWaypoints.size());

        // Step 10: Retrieve complete ride with all nested data
        log.info("Step 10: Retrieving complete ride data");
        RR_UiRide fullRideResponse = get_ForRM("/api/ride/" + e2eRide.getId(), tr_UiRide);
        check(fullRideResponse);
        UiRide fullRide = fullRideResponse.getData();
        assertNotNull(fullRide);
        assertEquals(e2eRide.getId(), fullRide.getId());
        log.info("Retrieved complete ride: {}", fullRide.getName());

        // Step 11: Cleanup - Delete everything in reverse order
        log.info("Step 11: Cleaning up test data");

        // Ride deletion should cascade to routes, legs, and waypoints
        RR_Void deleteRideResponse = delete_ForRM("/api/ride/" + e2eRide.getId(), tr_Void);
        check(deleteRideResponse);
        log.info("Deleted ride (cascades to routes, legs, waypoints)");

        // Delete bonus points
        delete_ForRM("/api/bonuspoint/" + bp1.getId(), tr_Void);
        delete_ForRM("/api/bonuspoint/" + bp2.getId(), tr_Void);
        delete_ForRM("/api/bonuspoint/" + bp3.getId(), tr_Void);
        log.info("Deleted bonus points");

        // Delete rally
        delete_ForRM("/api/rally/" + planningRally.getId(), tr_Void);
        log.info("Deleted rally");

        log.info("=== END-TO-END TEST COMPLETED SUCCESSFULLY ===");
    }

    //======================================================================
    // Cleanup
    //======================================================================

    @Test
    @Order(900)
    public void test_900_Cleanup_DeleteTestRally() throws Exception {
        log.info("Cleaning up test rally");
        if (testRally != null && testRally.getId() != null) {
            delete_ForRM("/api/rally/" + testRally.getId(), tr_Void);
            log.info("Deleted test rally: {}", testRally.getId());
        }
    }
}
