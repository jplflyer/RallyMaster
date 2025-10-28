package org.showpage.rallyserver.ui;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "A bonus point that is part of a combination")
public class UiCombinationPoint {
    @Schema(description = "Unique combination point identifier", example = "1")
    private Integer id;

    @Schema(description = "Combination ID this point belongs to", example = "1", required = true)
    private Integer combinationId;

    @Schema(description = "Bonus point ID that is part of this combination", example = "5", required = true)
    private Integer bonusPointId;

    @Schema(description = "Whether this bonus point is required for the combination", example = "true")
    private Boolean required;
}
