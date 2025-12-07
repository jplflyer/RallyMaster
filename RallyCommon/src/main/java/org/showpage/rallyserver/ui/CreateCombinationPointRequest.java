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
@Schema(description = "Request to add a bonus point to a combination")
public class CreateCombinationPointRequest {
    @Schema(description = "Bonus point ID to include in the combination", example = "5", required = true)
    private Integer bonusPointId;

    @Schema(description = "Whether this bonus point is required for the combination", example = "true")
    private Boolean required;
}
