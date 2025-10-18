package org.showpage.rallyserver.ui;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateEarnedBonusPointRequest {
    private Integer riderId;  // The member ID of the rider
    private Integer bonusPointId;
    private Integer odometer;
    private Instant earnedAt;
    private Boolean confirmed;
}
