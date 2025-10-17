package org.showpage.rallyserver.service;

import org.showpage.rallyserver.entity.Rally;
import org.showpage.rallyserver.ui.UiRally;
import org.springframework.stereotype.Service;

/**
 * Maps between DTOs and Entities.
 */
public class DtoMapper {
    public static UiRally toUiRally(Rally rally) {
        if (rally == null) {
            return null;
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
                .build();
    }
}
