package org.showpage.rallyserver.ui;

import com.fasterxml.jackson.annotation.JsonInclude;
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
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Earned bonus point record for a rally participant")
public class UiEarnedBonusPoint {
    @Schema(description = "Unique earned bonus point identifier", example = "1")
    private Integer id;

    @Schema(description = "Rally participant ID who earned this bonus point", example = "5")
    private Integer rallyParticipantId;

    @Schema(description = "Bonus point ID that was earned", example = "10")
    private Integer bonusPointId;

    @Schema(description = "Odometer reading when the bonus point was earned", example = "47500")
    private Integer odometer;

    @Schema(description = "Timestamp when the bonus point was earned", example = "2024-08-22T14:30:00Z")
    private Instant earnedAt;

    @Schema(description = "Whether this earned bonus point has been confirmed by organizers", example = "true")
    private Boolean confirmed;
}
