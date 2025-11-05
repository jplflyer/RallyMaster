package org.showpage.rallyserver.ui;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.showpage.rallyserver.interfaces.HasId;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Ride details for a rider's personal or rally-associated ride")
public class UiRide implements HasId<UiRide> {
    @Schema(description = "Unique ride identifier", example = "1")
    private Integer id;

    @Schema(description = "Member ID who owns this ride", example = "1")
    private Integer memberId;

    @Schema(description = "Optional rally ID if this ride is associated with a rally", example = "1")
    private Integer rallyId;

    @Schema(description = "Ride name", example = "Saddlesore 1000", required = true)
    private String name;

    @Schema(description = "Ride description", example = "1000 miles in 24 hours")
    private String description;

    @Schema(description = "Ride start date", example = "2024-09-15")
    private LocalDateTime expectedStart;

    @Schema(description = "Ride end date", example = "2024-09-16")
    private LocalDateTime expectedEnd;

    @Schema(description = "List of routes for this ride")
    private List<UiRoute> routes;

    @Schema(description = "Default stop duration in seconds")
    private Integer stopDuration;

    @Schema(description = "Link to Spotwalla")
    private String spotwallaLink;

    @Schema(description = "Actual start date", example = "2024-09-15")
    private LocalDateTime actualStart;
    @Schema(description = "Actual end date", example = "2024-09-15")
    private LocalDateTime actualEnd;

    @Schema(description = "Odometer at start of ride")
    private Integer odometerStart;
    @Schema(description = "Odometer at end of ride")
    private Integer odometerEnd;
}
