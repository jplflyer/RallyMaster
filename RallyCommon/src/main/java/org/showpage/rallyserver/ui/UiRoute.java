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
@Schema(description = "Route details for a ride")
public class UiRoute implements HasId<UiRoute> {
    @Schema(description = "Unique route identifier", example = "1")
    private Integer id;

    @Schema(description = "Ride ID this route belongs to", example = "1")
    private Integer rideId;

    @Schema(description = "Route name", example = "Primary Route", required = true)
    private String name;

    @Schema(description = "Route description", example = "Planned route via scenic highways")
    private String description;

    @Schema(description = "Whether this is the primary/selected route", example = "true")
    private Boolean isPrimary;

    @Schema(description = "List of ride legs for this route")
    private List<UiRideLeg> rideLegs;
}
