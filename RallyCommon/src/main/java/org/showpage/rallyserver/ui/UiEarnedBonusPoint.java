package org.showpage.rallyserver.ui;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UiEarnedBonusPoint {
    private Integer id;
    private Integer rallyParticipantId;
    private Integer bonusPointId;
    private Integer odometer;
    private Instant earnedAt;
    private Boolean confirmed;
}
