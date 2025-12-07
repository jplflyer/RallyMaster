package org.showpage.rallyserver.ui;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Request to update an existing ride leg")
public class UpdateRideLegRequest {
    @Schema(description = "Leg name", example = "Day 1")
    private String name;

    @Schema(description = "Leg description", example = "Morning segment")
    private String description;

    @Schema(description = "Sequence order", example = "1")
    private Integer sequenceOrder;
}
