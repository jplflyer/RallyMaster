package org.showpage.rallyserver.service;

import org.showpage.rallyserver.entity.*;
import org.showpage.rallyserver.ui.*;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Maps between DTOs and Entities.
 */
public class DtoMapper {
    public static UiRally toUiRally(Member member, Rally rally) {
        if (rally == null) {
            return null;
        }

        boolean isOrganizer = isOrganizer(member, rally);

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
}
