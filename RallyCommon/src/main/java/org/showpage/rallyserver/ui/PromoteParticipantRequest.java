package org.showpage.rallyserver.ui;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.showpage.rallyserver.entity.RallyParticipantType;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromoteParticipantRequest {
    private Integer targetMemberId;
    private RallyParticipantType newType;
}
