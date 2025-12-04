package org.showpage.rallyserver.ui;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.showpage.rallyserver.interfaces.HasId;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Waypoint details for a stop on a ride leg")
public class UiWaypoint implements HasId<UiWaypoint> {
    @Schema(description = "Unique waypoint identifier", example = "1")
    private Integer id;

    @Schema(description = "Ride leg ID this waypoint belongs to", example = "1")
    private Integer rideLegId;

    @Schema(description = "Optional bonus point ID if this waypoint references a rally bonus", example = "1")
    private Integer bonusPointId;

    @Schema(description = "Waypoint name", example = "Aitkin Airport", required = true)
    private String name;

    @Schema(description = "Waypoint description", example = "Stop for photo at airport sign")
    private String description;

    @Schema(description = "Sequence order of this waypoint", example = "1")
    private Integer sequenceOrder;

    @Schema(description = "Waypoint latitude", example = "46.5461")
    private Float latitude;

    @Schema(description = "Waypoint longitude", example = "-93.6772")
    private Float longitude;

    @Schema(description = "Waypoint address", example = "12345 Airport Rd, Aitkin, MN")
    private String address;

    @Schema(description = "Marker color for map display", example = "blue")
    private String markerColor;

    @Schema(description = "Marker icon type for map display", example = "pin")
    private String markerIcon;
}
