package org.showpage.rallyserver.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.showpage.rallyserver.entity.*;
import org.showpage.rallyserver.exception.NotFoundException;
import org.showpage.rallyserver.exception.ValidationException;
import org.showpage.rallyserver.repository.*;
import org.showpage.rallyserver.ui.*;
import org.showpage.rallyserver.util.DataValidator;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * CRUD operations for Rides and associated entities (Routes, RideLegs, Waypoints).
 * Rides are strictly owned by the member who creates them.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class RideService {
    private final RideRepository rideRepository;
    private final RouteRepository routeRepository;
    private final RideLegRepository rideLegRepository;
    private final WaypointRepository waypointRepository;
    private final BonusPointRepository bonusPointRepository;

    //======================================================================
    // Ride CRUD
    //======================================================================

    /**
     * Create a new ride for the authenticated member.
     */
    public Ride createRide(Member member, CreateRideRequest request) throws ValidationException {
        request.checkValid();

        Ride ride = Ride
                .builder()
                .memberId(member.getId())
                .rallyId(request.getRallyId())
                .name(request.getName().trim())
                .description(request.getDescription() != null ? request.getDescription().trim() : null)
                .expectedStart(request.getExpectedStart())
                .expectedEnd(request.getExpectedEnd())
                .stopDuration(request.getStopDuration())
                .spotwallaLink(request.getSpotwallaLink())
                .build();

        return rideRepository.save(ride);
    }

    /**
     * Update an existing ride. Only the owner can update.
     */
    public Ride updateRide(
        Member member,
        Integer rideId,
        UpdateRideRequest request
    ) throws NotFoundException, ValidationException {
        Ride ride = rideRepository.findById_WithThrow(rideId);
        checkOwnership(member, ride);

        if (DataValidator.nonEmpty(request.getName())) {
            ride.setName(request.getName().trim());
        }
        if (DataValidator.nonEmpty(request.getDescription())) {
            ride.setDescription(request.getDescription().trim());
        }
        if (request.getExpectedStart() != null) {
            ride.setExpectedStart(request.getExpectedStart());
        }
        if (request.getExpectedEnd() != null) {
            ride.setExpectedEnd(request.getExpectedEnd());
        }
        if (request.getRallyId() != null) {
            ride.setRallyId(request.getRallyId());
        }

        if (request.getActualStart() != null) {
            ride.setActualStart(request.getActualStart());
        }
        if (request.getActualEnd() != null) {
            ride.setActualEnd(request.getActualEnd());
        }

        if (request.getSpotwallaLink() != null) {
            ride.setSpotwallaLink(request.getSpotwallaLink());
        }
        if (request.getStopDuration() != null) {
            ride.setStopDuration(request.getStopDuration());
        }
        if (request.getOdometerStart() != null) {
            ride.setOdometerStart(request.getOdometerStart());
        }
        if (request.getOdometerEnd() != null) {
            ride.setOdometerEnd(request.getOdometerEnd());
        }

        return rideRepository.save(ride);
    }

    /**
     * Get a ride by ID. Only the owner can view.
     */
    public Ride getRide(Member member, Integer rideId) throws NotFoundException {
        Ride ride = rideRepository.findById_WithThrow(rideId);
        checkOwnership(member, ride);
        return ride;
    }

    /**
     * List all rides for the authenticated member.
     */
    public List<Ride> listRides(Member member) {
        return rideRepository.findByMemberId(member.getId());
    }

    /**
     * Delete a ride. Only the owner can delete.
     */
    public void deleteRide(Member member, Integer rideId) throws NotFoundException, ValidationException {
        Ride ride = rideRepository.findById_WithThrow(rideId);
        checkOwnership(member, ride);

        // Cascade deletion manually to avoid issues
        if (ride.getRoutes() != null) {
            for (Route route : ride.getRoutes()) {
                if (route.getRideLegs() != null) {
                    for (RideLeg leg : route.getRideLegs()) {
                        if (leg.getWaypoints() != null) {
                            waypointRepository.deleteAll(leg.getWaypoints());
                        }
                    }
                    rideLegRepository.deleteAll(route.getRideLegs());
                }
            }
            routeRepository.deleteAll(ride.getRoutes());
        }

        rideRepository.delete(ride);
    }

    //======================================================================
    // Route CRUD
    //======================================================================

    /**
     * Create a route for a ride.
     */
    public Route createRoute(
        Member member,
        Integer rideId,
        CreateRouteRequest request
    ) throws NotFoundException, ValidationException {
        Ride ride = rideRepository.findById_WithThrow(rideId);
        checkOwnership(member, ride);
        request.checkValid();

        Route route = Route
                .builder()
                .rideId(rideId)
                .name(request.getName().trim())
                .description(request.getDescription() != null ? request.getDescription().trim() : null)
                .isPrimary(request.getIsPrimary() != null ? request.getIsPrimary() : false)
                .build();

        return routeRepository.save(route);
    }

    /**
     * Update a route.
     */
    public Route updateRoute(
        Member member,
        Integer routeId,
        UpdateRouteRequest request
    ) throws NotFoundException, ValidationException {
        Route route = routeRepository.findById_WithThrow(routeId);
        Ride ride = rideRepository.findById_WithThrow(route.getRideId());
        checkOwnership(member, ride);

        if (DataValidator.nonEmpty(request.getName())) {
            route.setName(request.getName().trim());
        }
        if (DataValidator.nonEmpty(request.getDescription())) {
            route.setDescription(request.getDescription().trim());
        }
        if (request.getIsPrimary() != null && request.getIsPrimary()) {
            List<Route> allRoutes = routeRepository.findByRideId(route.getRideId());
            for (Route otherRoute : allRoutes) {
                if (otherRoute.getIsPrimary() != null && otherRoute.getIsPrimary() && !otherRoute.getId().equals(routeId)) {
                    otherRoute.setIsPrimary(false);
                    routeRepository.save(otherRoute);
                }
            }
            route.setIsPrimary(true);
        } else if (request.getIsPrimary() != null) {
            route.setIsPrimary(request.getIsPrimary());
        }

        return routeRepository.save(route);
    }

    /**
     * Get a route by ID.
     */
    public Route getRoute(Member member, Integer routeId) throws NotFoundException {
        Route route = routeRepository.findById_WithThrow(routeId);
        Ride ride = rideRepository.findById_WithThrow(route.getRideId());
        checkOwnership(member, ride);
        return route;
    }

    /**
     * List all routes for a ride.
     */
    public List<Route> listRoutes(Member member, Integer rideId) throws NotFoundException {
        Ride ride = rideRepository.findById_WithThrow(rideId);
        checkOwnership(member, ride);
        return routeRepository.findByRideId(rideId);
    }

    /**
     * Delete a route.
     */
    public void deleteRoute(Member member, Integer routeId) throws NotFoundException, ValidationException {
        Route route = routeRepository.findById_WithThrow(routeId);
        Ride ride = rideRepository.findById_WithThrow(route.getRideId());
        checkOwnership(member, ride);

        // Cascade deletion
        if (route.getRideLegs() != null) {
            for (RideLeg leg : route.getRideLegs()) {
                if (leg.getWaypoints() != null) {
                    waypointRepository.deleteAll(leg.getWaypoints());
                }
            }
            rideLegRepository.deleteAll(route.getRideLegs());
        }

        routeRepository.delete(route);
    }

    //======================================================================
    // RideLeg CRUD
    //======================================================================

    /**
     * Create a ride leg for a route.
     */
    public RideLeg createRideLeg(
        Member member,
        Integer routeId,
        CreateRideLegRequest request
    ) throws NotFoundException, ValidationException {
        Route route = routeRepository.findById_WithThrow(routeId);
        Ride ride = rideRepository.findById_WithThrow(route.getRideId());
        checkOwnership(member, ride);
        request.checkValid();

        RideLeg rideLeg = RideLeg
                .builder()
                .routeId(routeId)
                .name(request.getName().trim())
                .description(request.getDescription() != null ? request.getDescription().trim() : null)
                .sequenceOrder(request.getSequenceOrder())
                .build();

        return rideLegRepository.save(rideLeg);
    }

    /**
     * Update a ride leg.
     */
    public RideLeg updateRideLeg(
        Member member,
        Integer rideLegId,
        UpdateRideLegRequest request
    ) throws NotFoundException, ValidationException {
        RideLeg rideLeg = rideLegRepository.findById_WithThrow(rideLegId);
        Route route = routeRepository.findById_WithThrow(rideLeg.getRouteId());
        Ride ride = rideRepository.findById_WithThrow(route.getRideId());
        checkOwnership(member, ride);

        if (DataValidator.nonEmpty(request.getName())) {
            rideLeg.setName(request.getName().trim());
        }
        if (DataValidator.nonEmpty(request.getDescription())) {
            rideLeg.setDescription(request.getDescription().trim());
        }
        if (request.getSequenceOrder() != null) {
            rideLeg.setSequenceOrder(request.getSequenceOrder());
        }

        return rideLegRepository.save(rideLeg);
    }

    /**
     * Get a ride leg by ID.
     */
    public RideLeg getRideLeg(Member member, Integer rideLegId) throws NotFoundException {
        RideLeg rideLeg = rideLegRepository.findById_WithThrow(rideLegId);
        Route route = routeRepository.findById_WithThrow(rideLeg.getRouteId());
        Ride ride = rideRepository.findById_WithThrow(route.getRideId());
        checkOwnership(member, ride);
        return rideLeg;
    }

    /**
     * List all ride legs for a route.
     */
    public List<RideLeg> listRideLegs(Member member, Integer routeId) throws NotFoundException {
        Route route = routeRepository.findById_WithThrow(routeId);
        Ride ride = rideRepository.findById_WithThrow(route.getRideId());
        checkOwnership(member, ride);
        return rideLegRepository.findByRouteId(routeId);
    }

    /**
     * Delete a ride leg.
     */
    public void deleteRideLeg(Member member, Integer rideLegId) throws NotFoundException, ValidationException {
        RideLeg rideLeg = rideLegRepository.findById_WithThrow(rideLegId);
        Route route = routeRepository.findById_WithThrow(rideLeg.getRouteId());
        Ride ride = rideRepository.findById_WithThrow(route.getRideId());
        checkOwnership(member, ride);

        // Cascade deletion
        if (rideLeg.getWaypoints() != null) {
            waypointRepository.deleteAll(rideLeg.getWaypoints());
        }

        rideLegRepository.delete(rideLeg);
    }

    //======================================================================
    // Waypoint CRUD
    //======================================================================

    /**
     * Create a waypoint for a ride leg.
     */
    public Waypoint createWaypoint(
        Member member,
        Integer rideLegId,
        CreateWaypointRequest request
    ) throws NotFoundException, ValidationException {
        RideLeg rideLeg = rideLegRepository.findById_WithThrow(rideLegId);
        Route route = routeRepository.findById_WithThrow(rideLeg.getRouteId());
        Ride ride = rideRepository.findById_WithThrow(route.getRideId());
        checkOwnership(member, ride);
        request.checkValid();

        // If bonusPointId is provided, validate it exists
        if (request.getBonusPointId() != null) {
            bonusPointRepository.findById(request.getBonusPointId())
                    .orElseThrow(() -> new NotFoundException("Bonus point not found"));
        }

        Waypoint waypoint = Waypoint
                .builder()
                .rideLegId(rideLegId)
                .bonusPointId(request.getBonusPointId())
                .name(request.getName().trim())
                .description(request.getDescription() != null ? request.getDescription().trim() : null)
                .sequenceOrder(request.getSequenceOrder())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .address(request.getAddress() != null ? request.getAddress().trim() : null)
                .build();

        return waypointRepository.save(waypoint);
    }

    /**
     * Update a waypoint.
     */
    public Waypoint updateWaypoint(
        Member member,
        Integer waypointId,
        UpdateWaypointRequest request
    ) throws NotFoundException, ValidationException {
        Waypoint waypoint = waypointRepository.findById_WithThrow(waypointId);
        RideLeg rideLeg = rideLegRepository.findById_WithThrow(waypoint.getRideLegId());
        Route route = routeRepository.findById_WithThrow(rideLeg.getRouteId());
        Ride ride = rideRepository.findById_WithThrow(route.getRideId());
        checkOwnership(member, ride);

        if (DataValidator.nonEmpty(request.getName())) {
            waypoint.setName(request.getName().trim());
        }
        if (DataValidator.nonEmpty(request.getDescription())) {
            waypoint.setDescription(request.getDescription().trim());
        }
        if (request.getSequenceOrder() != null) {
            waypoint.setSequenceOrder(request.getSequenceOrder());
        }
        if (request.getBonusPointId() != null) {
            // Validate bonus point exists
            bonusPointRepository.findById(request.getBonusPointId())
                    .orElseThrow(() -> new NotFoundException("Bonus point not found"));
            waypoint.setBonusPointId(request.getBonusPointId());
        }
        if (request.getLatitude() != null) {
            waypoint.setLatitude(request.getLatitude());
        }
        if (request.getLongitude() != null) {
            waypoint.setLongitude(request.getLongitude());
        }
        if (DataValidator.nonEmpty(request.getAddress())) {
            waypoint.setAddress(request.getAddress().trim());
        }

        return waypointRepository.save(waypoint);
    }

    /**
     * Get a waypoint by ID.
     */
    public Waypoint getWaypoint(Member member, Integer waypointId) throws NotFoundException {
        Waypoint waypoint = waypointRepository.findById_WithThrow(waypointId);
        RideLeg rideLeg = rideLegRepository.findById_WithThrow(waypoint.getRideLegId());
        Route route = routeRepository.findById_WithThrow(rideLeg.getRouteId());
        Ride ride = rideRepository.findById_WithThrow(route.getRideId());
        checkOwnership(member, ride);
        return waypoint;
    }

    /**
     * List all waypoints for a ride leg.
     */
    public List<Waypoint> listWaypoints(Member member, Integer rideLegId) throws NotFoundException {
        RideLeg rideLeg = rideLegRepository.findById_WithThrow(rideLegId);
        Route route = routeRepository.findById_WithThrow(rideLeg.getRouteId());
        Ride ride = rideRepository.findById_WithThrow(route.getRideId());
        checkOwnership(member, ride);
        return waypointRepository.findByRideLegId(rideLegId);
    }

    /**
     * Delete a waypoint.
     */
    public void deleteWaypoint(Member member, Integer waypointId) throws NotFoundException, ValidationException {
        Waypoint waypoint = waypointRepository.findById_WithThrow(waypointId);
        RideLeg rideLeg = rideLegRepository.findById_WithThrow(waypoint.getRideLegId());
        Route route = routeRepository.findById_WithThrow(rideLeg.getRouteId());
        Ride ride = rideRepository.findById_WithThrow(route.getRideId());
        checkOwnership(member, ride);

        waypointRepository.delete(waypoint);
    }

    //======================================================================
    // Helper methods
    //======================================================================

    /**
     * Check that the member owns the ride. Throws NotFoundException if not.
     */
    private void checkOwnership(Member member, Ride ride) throws NotFoundException {
        if (!ride.getMemberId().equals(member.getId())) {
            throw new NotFoundException("Ride not found");
        }
    }
}
