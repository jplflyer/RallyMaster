package org.showpage.rallyserver.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.showpage.rallyserver.RestResponse;
import org.showpage.rallyserver.service.DtoMapper;
import org.showpage.rallyserver.service.RideService;
import org.showpage.rallyserver.ui.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Ride Management", description = "Endpoints for managing personal rides, routes, legs, and waypoints")
public class RideController {
    private final ServiceCaller serviceCaller;
    private final RideService rideService;

    //======================================================================
    // Ride CRUD
    //======================================================================

    @Operation(
        summary = "Create a new ride",
        description = "Create a new ride for the authenticated user. Rides can be standalone or associated with a rally.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Ride created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid ride data"),
            @ApiResponse(responseCode = "401", description = "Not authenticated")
        }
    )
    @PostMapping("/ride")
    ResponseEntity<RestResponse<UiRide>> createRide(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Ride details", required = true)
            @RequestBody CreateRideRequest request
    ) {
        return serviceCaller.call("/api/ride", (member) ->
            DtoMapper.toUiRide(rideService.createRide(member, request)));
    }

    @Operation(
        summary = "Update a ride",
        description = "Update an existing ride. Only the ride owner can update.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Ride updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid ride data"),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "404", description = "Ride not found")
        }
    )
    @PutMapping("/ride/{id}")
    ResponseEntity<RestResponse<UiRide>> updateRide(
            @Parameter(description = "Ride ID", example = "1", required = true)
            @PathVariable Integer id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Updated ride details", required = true)
            @RequestBody UpdateRideRequest request
    ) {
        return serviceCaller.call((member) ->
            DtoMapper.toUiRide(rideService.updateRide(member, id, request)));
    }

    @Operation(
        summary = "Get ride by ID",
        description = "Retrieve detailed information about a specific ride. Only the ride owner can view.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Ride found"),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "404", description = "Ride not found")
        }
    )
    @GetMapping("/ride/{id}")
    ResponseEntity<RestResponse<UiRide>> getRide(
            @Parameter(description = "Ride ID", example = "1", required = true)
            @PathVariable Integer id
    ) {
        return serviceCaller.call((member) ->
            DtoMapper.toUiRide(rideService.getRide(member, id)));
    }

    @Operation(
        summary = "List all rides",
        description = "List all rides for the authenticated user",
        responses = {
            @ApiResponse(responseCode = "200", description = "Rides retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Not authenticated")
        }
    )
    @GetMapping("/rides")
    ResponseEntity<RestResponse<List<UiRide>>> listRides() {
        return serviceCaller.call((member) ->
            rideService.listRides(member).stream()
                    .map(DtoMapper::toUiRide)
                    .toList());
    }

    @Operation(
        summary = "Delete a ride",
        description = "Delete a ride and all associated routes, legs, and waypoints. Only the ride owner can delete.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Ride deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "404", description = "Ride not found")
        }
    )
    @DeleteMapping("/ride/{id}")
    ResponseEntity<RestResponse<Void>> deleteRide(
            @Parameter(description = "Ride ID to delete", example = "1", required = true)
            @PathVariable Integer id
    ) {
        return serviceCaller.call((member) -> {
            rideService.deleteRide(member, id);
            return null;
        });
    }

    //======================================================================
    // Route CRUD
    //======================================================================

    @Operation(
        summary = "Create a route",
        description = "Add a new route to a ride",
        responses = {
            @ApiResponse(responseCode = "200", description = "Route created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid route data"),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "404", description = "Ride not found")
        }
    )
    @PostMapping("/ride/{rideId}/route")
    ResponseEntity<RestResponse<UiRoute>> createRoute(
            @Parameter(description = "Ride ID", example = "1", required = true)
            @PathVariable Integer rideId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Route details", required = true)
            @RequestBody CreateRouteRequest request
    ) {
        return serviceCaller.call((member) ->
            DtoMapper.toUiRoute(rideService.createRoute(member, rideId, request)));
    }

    @Operation(
        summary = "Update a route",
        description = "Update an existing route",
        responses = {
            @ApiResponse(responseCode = "200", description = "Route updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid route data"),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "404", description = "Route not found")
        }
    )
    @PutMapping("/route/{id}")
    ResponseEntity<RestResponse<UiRoute>> updateRoute(
            @Parameter(description = "Route ID", example = "1", required = true)
            @PathVariable Integer id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Updated route details", required = true)
            @RequestBody UpdateRouteRequest request
    ) {
        return serviceCaller.call((member) ->
            DtoMapper.toUiRoute(rideService.updateRoute(member, id, request)));
    }

    @Operation(
        summary = "Get route by ID",
        description = "Retrieve detailed information about a specific route",
        responses = {
            @ApiResponse(responseCode = "200", description = "Route found"),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "404", description = "Route not found")
        }
    )
    @GetMapping("/route/{id}")
    ResponseEntity<RestResponse<UiRoute>> getRoute(
            @Parameter(description = "Route ID", example = "1", required = true)
            @PathVariable Integer id
    ) {
        return serviceCaller.call((member) ->
            DtoMapper.toUiRoute(rideService.getRoute(member, id)));
    }

    @Operation(
        summary = "List all routes for a ride",
        description = "Retrieve all routes associated with a specific ride",
        responses = {
            @ApiResponse(responseCode = "200", description = "Routes retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "404", description = "Ride not found")
        }
    )
    @GetMapping("/ride/{rideId}/routes")
    ResponseEntity<RestResponse<List<UiRoute>>> listRoutes(
            @Parameter(description = "Ride ID", example = "1", required = true)
            @PathVariable Integer rideId
    ) {
        return serviceCaller.call((member) ->
            rideService.listRoutes(member, rideId).stream()
                    .map(DtoMapper::toUiRoute)
                    .toList());
    }

    @Operation(
        summary = "Delete a route",
        description = "Delete a route and all associated legs and waypoints",
        responses = {
            @ApiResponse(responseCode = "200", description = "Route deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "404", description = "Route not found")
        }
    )
    @DeleteMapping("/route/{id}")
    ResponseEntity<RestResponse<Void>> deleteRoute(
            @Parameter(description = "Route ID to delete", example = "1", required = true)
            @PathVariable Integer id
    ) {
        return serviceCaller.call((member) -> {
            rideService.deleteRoute(member, id);
            return null;
        });
    }

    //======================================================================
    // RideLeg CRUD
    //======================================================================

    @Operation(
        summary = "Create a ride leg",
        description = "Add a new leg to a route",
        responses = {
            @ApiResponse(responseCode = "200", description = "Ride leg created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid ride leg data"),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "404", description = "Route not found")
        }
    )
    @PostMapping("/route/{routeId}/leg")
    ResponseEntity<RestResponse<UiRideLeg>> createRideLeg(
            @Parameter(description = "Route ID", example = "1", required = true)
            @PathVariable Integer routeId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Ride leg details", required = true)
            @RequestBody CreateRideLegRequest request
    ) {
        return serviceCaller.call((member) ->
            DtoMapper.toUiRideLeg(rideService.createRideLeg(member, routeId, request)));
    }

    @Operation(
        summary = "Update a ride leg",
        description = "Update an existing ride leg",
        responses = {
            @ApiResponse(responseCode = "200", description = "Ride leg updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid ride leg data"),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "404", description = "Ride leg not found")
        }
    )
    @PutMapping("/leg/{id}")
    ResponseEntity<RestResponse<UiRideLeg>> updateRideLeg(
            @Parameter(description = "Ride leg ID", example = "1", required = true)
            @PathVariable Integer id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Updated ride leg details", required = true)
            @RequestBody UpdateRideLegRequest request
    ) {
        return serviceCaller.call((member) ->
            DtoMapper.toUiRideLeg(rideService.updateRideLeg(member, id, request)));
    }

    @Operation(
        summary = "Get ride leg by ID",
        description = "Retrieve detailed information about a specific ride leg",
        responses = {
            @ApiResponse(responseCode = "200", description = "Ride leg found"),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "404", description = "Ride leg not found")
        }
    )
    @GetMapping("/leg/{id}")
    ResponseEntity<RestResponse<UiRideLeg>> getRideLeg(
            @Parameter(description = "Ride leg ID", example = "1", required = true)
            @PathVariable Integer id
    ) {
        return serviceCaller.call((member) ->
            DtoMapper.toUiRideLeg(rideService.getRideLeg(member, id)));
    }

    @Operation(
        summary = "List all ride legs for a route",
        description = "Retrieve all ride legs associated with a specific route",
        responses = {
            @ApiResponse(responseCode = "200", description = "Ride legs retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "404", description = "Route not found")
        }
    )
    @GetMapping("/route/{routeId}/legs")
    ResponseEntity<RestResponse<List<UiRideLeg>>> listRideLegs(
            @Parameter(description = "Route ID", example = "1", required = true)
            @PathVariable Integer routeId
    ) {
        return serviceCaller.call((member) ->
            rideService.listRideLegs(member, routeId).stream()
                    .map(DtoMapper::toUiRideLeg)
                    .toList());
    }

    @Operation(
        summary = "Delete a ride leg",
        description = "Delete a ride leg and all associated waypoints",
        responses = {
            @ApiResponse(responseCode = "200", description = "Ride leg deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "404", description = "Ride leg not found")
        }
    )
    @DeleteMapping("/leg/{id}")
    ResponseEntity<RestResponse<Void>> deleteRideLeg(
            @Parameter(description = "Ride leg ID to delete", example = "1", required = true)
            @PathVariable Integer id
    ) {
        return serviceCaller.call((member) -> {
            rideService.deleteRideLeg(member, id);
            return null;
        });
    }

    //======================================================================
    // Waypoint CRUD
    //======================================================================

    @Operation(
        summary = "Create a waypoint",
        description = "Add a new waypoint to a ride leg. Waypoint must have either location coordinates or a bonus point reference.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Waypoint created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid waypoint data"),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "404", description = "Ride leg not found")
        }
    )
    @PostMapping("/leg/{legId}/waypoint")
    ResponseEntity<RestResponse<UiWaypoint>> createWaypoint(
            @Parameter(description = "Ride leg ID", example = "1", required = true)
            @PathVariable Integer legId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Waypoint details", required = true)
            @RequestBody CreateWaypointRequest request
    ) {
        return serviceCaller.call((member) ->
            DtoMapper.toUiWaypoint(rideService.createWaypoint(member, legId, request)));
    }

    @Operation(
        summary = "Update a waypoint",
        description = "Update an existing waypoint",
        responses = {
            @ApiResponse(responseCode = "200", description = "Waypoint updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid waypoint data"),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "404", description = "Waypoint not found")
        }
    )
    @PutMapping("/waypoint/{id}")
    ResponseEntity<RestResponse<UiWaypoint>> updateWaypoint(
            @Parameter(description = "Waypoint ID", example = "1", required = true)
            @PathVariable Integer id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Updated waypoint details", required = true)
            @RequestBody UpdateWaypointRequest request
    ) {
        return serviceCaller.call((member) ->
            DtoMapper.toUiWaypoint(rideService.updateWaypoint(member, id, request)));
    }

    @Operation(
        summary = "Get waypoint by ID",
        description = "Retrieve detailed information about a specific waypoint",
        responses = {
            @ApiResponse(responseCode = "200", description = "Waypoint found"),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "404", description = "Waypoint not found")
        }
    )
    @GetMapping("/waypoint/{id}")
    ResponseEntity<RestResponse<UiWaypoint>> getWaypoint(
            @Parameter(description = "Waypoint ID", example = "1", required = true)
            @PathVariable Integer id
    ) {
        return serviceCaller.call((member) ->
            DtoMapper.toUiWaypoint(rideService.getWaypoint(member, id)));
    }

    @Operation(
        summary = "List all waypoints for a ride leg",
        description = "Retrieve all waypoints associated with a specific ride leg",
        responses = {
            @ApiResponse(responseCode = "200", description = "Waypoints retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "404", description = "Ride leg not found")
        }
    )
    @GetMapping("/leg/{legId}/waypoints")
    ResponseEntity<RestResponse<List<UiWaypoint>>> listWaypoints(
            @Parameter(description = "Ride leg ID", example = "1", required = true)
            @PathVariable Integer legId
    ) {
        return serviceCaller.call((member) ->
            rideService.listWaypoints(member, legId).stream()
                    .map(DtoMapper::toUiWaypoint)
                    .toList());
    }

    @Operation(
        summary = "Delete a waypoint",
        description = "Delete a waypoint",
        responses = {
            @ApiResponse(responseCode = "200", description = "Waypoint deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "404", description = "Waypoint not found")
        }
    )
    @DeleteMapping("/waypoint/{id}")
    ResponseEntity<RestResponse<Void>> deleteWaypoint(
            @Parameter(description = "Waypoint ID to delete", example = "1", required = true)
            @PathVariable Integer id
    ) {
        return serviceCaller.call((member) -> {
            rideService.deleteWaypoint(member, id);
            return null;
        });
    }
}
