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
@Schema(description = "Request to update an existing route")
public class UpdateRouteRequest {
    @Schema(description = "Route name", example = "Primary Route")
    private String name;

    @Schema(description = "Route description", example = "Scenic highway route")
    private String description;

    @Schema(description = "Whether this is the primary route", example = "true")
    private Boolean isPrimary;
}
