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
@Schema(description = "Request to update an existing waypoint")
public class UpdateWaypointRequest {
    @Schema(description = "Waypoint name", example = "Aitkin Airport")
    private String name;

    @Schema(description = "Waypoint description", example = "Photo at airport sign")
    private String description;

    @Schema(description = "Sequence order", example = "1")
    private Integer sequenceOrder;

    @Schema(description = "Optional bonus point ID", example = "1")
    private Integer bonusPointId;

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
