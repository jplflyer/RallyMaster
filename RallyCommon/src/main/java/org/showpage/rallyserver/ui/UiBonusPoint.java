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
@Schema(description = "Bonus point location in a rally where riders can earn points")
public class UiBonusPoint {
    @Schema(description = "Unique bonus point identifier", example = "1")
    private Integer id;

    @Schema(description = "Rally ID this bonus point belongs to", example = "1")
    private Integer rallyId;

    @Schema(description = "Unique code for this bonus point", example = "BP001", required = true)
    private String code;

    @Schema(description = "Bonus point name", example = "Statue of Liberty", required = true)
    private String name;

    @Schema(description = "Bonus point description", example = "Photo with Liberty Island in background")
    private String description;

    @Schema(description = "Bonus point latitude", example = "40.6892")
    private Double latitude;

    @Schema(description = "Bonus point longitude", example = "-74.0445")
    private Double longitude;

    @Schema(description = "Bonus point address", example = "Liberty Island, New York, NY 10004")
    private String address;

    @Schema(description = "Point value for visiting this location", example = "100")
    private Integer points;

    @Schema(description = "Whether this bonus point is required to complete the rally", example = "false")
    private Boolean required;

    @Schema(description = "Whether this bonus point can be claimed multiple times", example = "false")
    private Boolean repeatable;

    @Schema(description = "Whether this is the rally start location", example = "false")
    private Boolean isStart;

    @Schema(description = "Whether this is a rally finish location", example = "false")
    private Boolean isFinish;

    @Schema(description = "Marker color for map display", example = "red")
    private String markerColor;

    @Schema(description = "Marker icon type for map display", example = "flag")
    private String markerIcon;
}
