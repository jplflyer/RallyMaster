package org.showpage.rallyserver.ui;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to record a rider earning a bonus point")
public class CreateEarnedBonusPointRequest {
    @Schema(description = "Member ID of the rider", example = "5", required = true)
    private Integer riderId;

    @Schema(description = "Bonus point ID that was earned", example = "10", required = true)
    private Integer bonusPointId;

    @Schema(description = "Odometer reading when the bonus point was earned", example = "47500")
    private Integer odometer;

    @Schema(description = "Timestamp when the bonus point was earned", example = "2024-08-22T14:30:00Z")
    private Instant earnedAt;

    @Schema(description = "Whether this earned bonus point has been confirmed by organizers", example = "false")
    private Boolean confirmed;
}
