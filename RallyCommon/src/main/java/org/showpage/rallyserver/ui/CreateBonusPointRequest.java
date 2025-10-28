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
@Schema(description = "Request to create a new bonus point in a rally")
public class CreateBonusPointRequest {
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
}
