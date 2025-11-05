package org.showpage.rallyserver.ui;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.showpage.rallyserver.exception.ValidationException;
import org.showpage.rallyserver.util.DataValidator;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Request to create a new waypoint")
public class CreateWaypointRequest {
    @Schema(description = "Waypoint name", example = "Aitkin Airport", required = true)
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

    public void checkValid() throws ValidationException {
        DataValidator.validate(name, "Name");

        // Must have either a bonusPointId OR location information
        boolean hasBonus = bonusPointId != null;
        boolean hasLocation = latitude != null && longitude != null;

        if (!hasBonus && !hasLocation) {
            throw new ValidationException("Waypoint must have either a bonusPointId or location coordinates");
        }
    }
}
