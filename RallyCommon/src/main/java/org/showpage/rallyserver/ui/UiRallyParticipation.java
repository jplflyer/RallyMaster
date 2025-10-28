package org.showpage.rallyserver.ui;

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
public class UiRallyParticipation {
    private UiRally rally;
    private RallyParticipantType participantType;
}
