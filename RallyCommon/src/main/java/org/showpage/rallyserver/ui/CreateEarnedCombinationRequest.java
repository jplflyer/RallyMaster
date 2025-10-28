package org.showpage.rallyserver.ui;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to record a rider earning a bonus point combination")
public class CreateEarnedCombinationRequest {
    @Schema(description = "Member ID of the rider", example = "5", required = true)
    private Integer riderId;

    @Schema(description = "Combination ID that was earned", example = "3", required = true)
    private Integer combinationId;

    @Schema(description = "Whether this earned combination has been confirmed by organizers", example = "false")
    private Boolean confirmed;
}
