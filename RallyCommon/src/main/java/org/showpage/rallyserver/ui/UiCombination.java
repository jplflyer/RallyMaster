package org.showpage.rallyserver.ui;

import com.fasterxml.jackson.annotation.JsonInclude;
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
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Bonus point combination offering bonus points for visiting multiple locations")
public class UiCombination {
    @Schema(description = "Unique combination identifier", example = "1")
    private Integer id;

    @Schema(description = "Rally ID this combination belongs to", example = "1", required = true)
    private Integer rallyId;

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

    @Schema(description = "List of bonus points that are part of this combination")
    private List<UiCombinationPoint> combinationPoints;
}
