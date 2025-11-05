package org.showpage.rallyserver.ui;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.showpage.rallyserver.interfaces.HasId;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Ride leg details for a route segment")
public class UiRideLeg implements HasId<UiRideLeg> {
    @Schema(description = "Unique ride leg identifier", example = "1")
    private Integer id;

    @Schema(description = "Route ID this leg belongs to", example = "1")
    private Integer routeId;

    @Schema(description = "Leg name", example = "Day 1", required = true)
    private String name;

    @Schema(description = "Leg description", example = "Morning route through Twin Cities")
    private String description;

    @Schema(description = "Sequence order of this leg", example = "1")
    private Integer sequenceOrder;

    @Schema(description = "List of waypoints for this leg")
    private List<UiWaypoint> waypoints;
}
