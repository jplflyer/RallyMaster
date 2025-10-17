package org.showpage.rallyserver.service;

import org.showpage.rallyserver.entity.Member;
import org.showpage.rallyserver.entity.Rally;
import org.showpage.rallyserver.entity.RallyParticipant;
import org.showpage.rallyserver.entity.RallyParticipantType;
import org.showpage.rallyserver.ui.UiRally;
import org.springframework.stereotype.Service;

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
