package org.showpage.rallyserver.ui;

import com.fasterxml.jackson.annotation.JsonInclude;
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
public class UiRallyParticipant {
    private Integer id;
    private Integer rallyId;
    private Integer memberId;
    private RallyParticipantType participantType;
    private Integer odometerIn;
    private Integer odometerOut;
    private Boolean finisher;
    private Integer finalScore;
}
