package org.showpage.rallyserver.service;

import org.showpage.rallyserver.entity.*;
import org.showpage.rallyserver.repository.BonusPointRepository;
import org.showpage.rallyserver.repository.CombinationPointRepository;
import org.showpage.rallyserver.repository.CombinationRepository;
import org.showpage.rallyserver.ui.*;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Maps between DTOs and Entities.
 */
public class DtoMapper {
    public static UiRally toUiRally(
            Member member,
            Rally rally
    ) {
        if (rally == null) {
            return null;
        }

        boolean isOrganizer = isOrganizer(member, rally);

        // Conditionally populate arrays based on organizer status or public flags
        List<UiRallyParticipant> participants = null;
        if (isOrganizer || (rally.getRidersPublic() != null && rally.getRidersPublic())) {
            if (rally.getParticipants() != null) {
                participants = rally.getParticipants() != null
                        ? rally.getParticipants().stream()
                        .map(DtoMapper::toUiRallyParticipant)
                        .collect(Collectors.toList())
                        : null;
            }
        }

        List<UiBonusPoint> bonusPoints = null;
        if (isOrganizer || (rally.getPointsPublic() != null && rally.getPointsPublic())) {
            if (rally.getBonusPoints() != null) {
                bonusPoints = rally.getBonusPoints().stream()
                        .map(DtoMapper::toUiBonusPoint)
                        .collect(Collectors.toList());
            }
        }

        List<UiCombination> combinations = null;
        if (isOrganizer || (rally.getPointsPublic() != null && rally.getPointsPublic())) {
            if (rally.getCombinations() != null) {
                combinations = rally.getCombinations().stream()
                        .map(c -> toUiCombination(c))
                        .collect(Collectors.toList());
            }
        }

        return UiRally
                .builder()
                .id(rally.getId())
                .name(rally.getName())
                .description(rally.getDescription())
                .startDate(rally.getStartDate())
                .endDate(rally.getEndDate())
                .locationCity(rally.getLocationCity())
                .locationState(rally.getLocationState())
                .locationCountry(rally.getLocationCountry())
                .pointsPublic(isOrganizer ? rally.getPointsPublic() : null)
                .ridersPublic(isOrganizer ? rally.getRidersPublic() : null)
                .organizersPublic(isOrganizer ? rally.getOrganizersPublic() : null)
                .participants(participants)
                .bonusPoints(bonusPoints)
                .combinations(combinations)
                .build();
    }

    public static UiRallyParticipant toUiRallyParticipant(RallyParticipant participant) {
        if (participant == null) {
            return null;
        }

        return UiRallyParticipant
                .builder()
                .id(participant.getId())
                .rallyId(participant.getRallyId())
                .memberId(participant.getMemberId())
                .participantType(participant.getParticipantType())
                .odometerIn(participant.getOdometerIn())
                .odometerOut(participant.getOdometerOut())
                .finisher(participant.getFinisher())
                .finalScore(participant.getFinalScore())
                .build();
    }

    public static UiBonusPoint toUiBonusPoint(BonusPoint bonusPoint) {
        if (bonusPoint == null) {
            return null;
        }

        return UiBonusPoint
                .builder()
                .id(bonusPoint.getId())
                .rallyId(bonusPoint.getRallyId())
                .code(bonusPoint.getCode())
                .name(bonusPoint.getName())
                .description(bonusPoint.getDescription())
                .latitude(bonusPoint.getLatitude())
                .longitude(bonusPoint.getLongitude())
                .address(bonusPoint.getAddress())
                .points(bonusPoint.getPoints())
                .required(bonusPoint.getRequired())
                .repeatable(bonusPoint.getRepeatable())
                .build();
    }

    public static UiCombinationPoint toUiCombinationPoint(CombinationPoint combinationPoint) {
        if (combinationPoint == null) {
            return null;
        }

        return UiCombinationPoint
                .builder()
                .id(combinationPoint.getId())
                .combinationId(combinationPoint.getCombinationId())
                .bonusPointId(combinationPoint.getBonusPointId())
                .required(combinationPoint.getRequired())
                .build();
    }

    public static UiCombination toUiCombination(Combination combination) {
        if (combination == null) {
            return null;
        }

        List<UiCombinationPoint> combinationPoints = combination.getCombinationPoints() != null
            ? combination.getCombinationPoints()
                .stream()
                .map(DtoMapper::toUiCombinationPoint)
                .collect(Collectors.toList())
            : null;

        return UiCombination
                .builder()
                .id(combination.getId())
                .rallyId(combination.getRallyId())
                .code(combination.getCode())
                .name(combination.getName())
                .description(combination.getDescription())
                .points(combination.getPoints())
                .requiresAll(combination.getRequiresAll())
                .numRequired(combination.getNumRequired())
                .combinationPoints(combinationPoints)
                .build();
    }

    /**
     * Determines if the given member is an organizer for the given rally.
     */
    private static boolean isOrganizer(Member member, Rally rally) {
        if (member == null || rally == null || rally.getParticipants() == null) {
            return false;
        }

        return rally.getParticipants().stream()
                .anyMatch(p -> p.getMemberId().equals(member.getId())
                        && RallyParticipantType.ORGANIZER.equals(p.getParticipantType()));
    }

    public static UiMember toUiMember(Member member) {
        return toUiMember(member, null);
    }

    public static UiMember toUiMember(Member member, List<RallyParticipant> rallyParticipations) {
        if (member == null) {
            return null;
        }

        List<Motorcycle> motorcycles = member.getMotorcycles();
        List<UiMotorcycle> uiMotorcycles = null;

        if (motorcycles != null && !motorcycles.isEmpty()) {
            uiMotorcycles = new ArrayList<>(motorcycles.size());
            for (Motorcycle motorcycle : motorcycles) {
                uiMotorcycles.add(toUiMotorcycle(motorcycle));
            }
        }

        List<UiRallyParticipation> uiParticipations = null;
        if (rallyParticipations != null && !rallyParticipations.isEmpty()) {
            uiParticipations = rallyParticipations.stream()
                    .map(DtoMapper::toUiRallyParticipation)
                    .collect(Collectors.toList());
        }

        return UiMember
                .builder()
                .id(member.getId())
                .email(member.getEmail())
                .spotwallaUsername(member.getSpotwallaUsername())
                .motorcycles(uiMotorcycles)
                .rallyParticipations(uiParticipations)
                .build();
    }

    public static UiRallyParticipation toUiRallyParticipation(RallyParticipant participation) {
        if (participation == null) {
            return null;
        }

        return UiRallyParticipation
                .builder()
                .rally(toUiRally(participation.getMember(), participation.getRally()))
                .participantType(participation.getParticipantType())
                .build();
    }

    public static UiMotorcycle toUiMotorcycle(Motorcycle motorcycle) {
        if (motorcycle == null) {
            return null;
        }
        return UiMotorcycle
                .builder()
                .id(motorcycle.getId())
                .memberId(motorcycle.getMemberId())
                .make(motorcycle.getMake())
                .model(motorcycle.getModel())
                .year(motorcycle.getYear())
                .color(motorcycle.getColor())
                .status(motorcycle.getStatus())
                .active(motorcycle.getActive())
                .build();
    }

    public static UiRide toUiRide(Ride ride) {
        if (ride == null) {
            return null;
        }

        List<UiRoute> routes = null;
        if (ride.getRoutes() != null) {
            routes = ride.getRoutes().stream()
                    .map(DtoMapper::toUiRoute)
                    .collect(Collectors.toList());
        }

        return UiRide
                .builder()
                .id(ride.getId())
                .memberId(ride.getMemberId())
                .rallyId(ride.getRallyId())
                .name(ride.getName())
                .description(ride.getDescription())
                .expectedStart(ride.getExpectedStart())
                .expectedEnd(ride.getExpectedEnd())
                .routes(routes)
                .actualStart(ride.getActualStart())
                .actualEnd(ride.getActualEnd())
                .spotwallaLink(ride.getSpotwallaLink())
                .stopDuration(ride.getStopDuration())
                .expectedStart(ride.getExpectedStart())
                .odometerEnd(ride.getOdometerEnd())
                .build();
    }

    public static UiRoute toUiRoute(Route route) {
        if (route == null) {
            return null;
        }

        List<UiRideLeg> rideLegs = null;
        if (route.getRideLegs() != null) {
            rideLegs = route.getRideLegs().stream()
                    .map(DtoMapper::toUiRideLeg)
                    .collect(Collectors.toList());
        }

        return UiRoute
                .builder()
                .id(route.getId())
                .rideId(route.getRideId())
                .name(route.getName())
                .description(route.getDescription())
                .isPrimary(route.getIsPrimary())
                .rideLegs(rideLegs)
                .build();
    }

    public static UiRideLeg toUiRideLeg(RideLeg rideLeg) {
        if (rideLeg == null) {
            return null;
        }

        List<UiWaypoint> waypoints = null;
        if (rideLeg.getWaypoints() != null) {
            waypoints = rideLeg.getWaypoints().stream()
                    .map(DtoMapper::toUiWaypoint)
                    .collect(Collectors.toList());
        }

        return UiRideLeg
                .builder()
                .id(rideLeg.getId())
                .routeId(rideLeg.getRouteId())
                .name(rideLeg.getName())
                .description(rideLeg.getDescription())
                .sequenceOrder(rideLeg.getSequenceOrder())
                .waypoints(waypoints)
                .build();
    }

    public static UiWaypoint toUiWaypoint(Waypoint waypoint) {
        if (waypoint == null) {
            return null;
        }

        return UiWaypoint
                .builder()
                .id(waypoint.getId())
                .rideLegId(waypoint.getRideLegId())
                .bonusPointId(waypoint.getBonusPointId())
                .name(waypoint.getName())
                .description(waypoint.getDescription())
                .sequenceOrder(waypoint.getSequenceOrder())
                .latitude(waypoint.getLatitude())
                .longitude(waypoint.getLongitude())
                .address(waypoint.getAddress())
                .build();
    }
}
