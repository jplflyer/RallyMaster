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
@Schema(description = "Request to update an existing bonus point combination")
public class UpdateCombinationRequest {
    @Schema(description = "Unique code for this combination", example = "COMBO1")
    private String code;

    @Schema(description = "Combination name", example = "Great Lakes Tour")
    private String name;

    @Schema(description = "Combination description", example = "Visit all Great Lakes states")
    private String description;

    @Schema(description = "Bonus points awarded for completing this combination", example = "5000")
    private Integer points;

    @Schema(description = "Whether all points in the combination are required", example = "true")
    private Boolean requiresAll;

    @Schema(description = "Number of points required if not all (when requiresAll is false)", example = "3")
    private Integer numRequired;
}
