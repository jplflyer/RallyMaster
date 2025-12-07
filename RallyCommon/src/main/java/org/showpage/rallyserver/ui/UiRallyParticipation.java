package org.showpage.rallyserver.ui;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.showpage.rallyserver.entity.RallyParticipantType;

/**
 * Represents a member's participation in a rally.
 * Contains the rally information and the member's role.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Member's participation in a rally with their role")
public class UiRallyParticipation {
    @Schema(description = "Rally information")
    private UiRally rally;

    @Schema(description = "Participant type/role in the rally (RIDER, ORGANIZER, STAFF)", example = "RIDER")
    private RallyParticipantType participantType;
}
