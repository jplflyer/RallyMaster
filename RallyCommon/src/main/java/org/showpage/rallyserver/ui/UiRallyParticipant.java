package org.showpage.rallyserver.ui;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.showpage.rallyserver.entity.RallyParticipantType;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Rally participant information including role, odometer readings, and final score")
public class UiRallyParticipant {
    @Schema(description = "Unique rally participant identifier", example = "1")
    private Integer id;

    @Schema(description = "Rally ID this participant belongs to", example = "1", required = true)
    private Integer rallyId;

    @Schema(description = "Member ID of the participant", example = "1", required = true)
    private Integer memberId;

    @Schema(description = "Participant type (RIDER, ORGANIZER, STAFF)", example = "RIDER", required = true)
    private RallyParticipantType participantType;

    @Schema(description = "Odometer reading at rally start", example = "42500")
    private Integer odometerIn;

    @Schema(description = "Odometer reading at rally finish", example = "53200")
    private Integer odometerOut;

    @Schema(description = "Whether the participant successfully finished the rally", example = "true")
    private Boolean finisher;

    @Schema(description = "Final score for the participant", example = "15000")
    private Integer finalScore;
}
