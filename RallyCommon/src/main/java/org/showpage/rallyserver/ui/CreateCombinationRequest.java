package org.showpage.rallyserver.ui;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to create a new bonus point combination")
public class CreateCombinationRequest {
    @Schema(description = "Unique code for this combination", example = "COMBO1", required = true)
    private String code;

    @Schema(description = "Combination name", example = "Great Lakes Tour", required = true)
    private String name;

    @Schema(description = "Combination description", example = "Visit all Great Lakes states")
    private String description;

    @Schema(description = "Bonus points awarded for completing this combination", example = "5000")
    private Integer points;

    @Schema(description = "Whether all points in the combination are required", example = "true")
    private Boolean requiresAll;

    @Schema(description = "Number of points required if not all (when requiresAll is false)", example = "3")
    private Integer numRequired;

    @Schema(description = "List of bonus points to include in this combination")
    private List<CreateCombinationPointRequest> combinationPoints;
}
