package org.showpage.rallyserver.ui;

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
@Schema(description = "Request to promote a participant to a different role in the rally")
public class PromoteParticipantRequest {
    @Schema(description = "Member ID of the participant to promote", example = "5", required = true)
    private Integer targetMemberId;

    @Schema(description = "New participant type (RIDER, ORGANIZER, STAFF)", example = "ORGANIZER", required = true)
    private RallyParticipantType newType;
}
