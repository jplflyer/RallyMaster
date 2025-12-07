package org.showpage.rallyserver.ui;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Request to update an existing ride")
public class UpdateRideRequest {
    @Schema(description = "Ride name", example = "Saddlesore 1000")
    private String name;

    @Schema(description = "Ride description", example = "1000 miles in 24 hours")
    private String description;

    @Schema(description = "Ride expected start date", example = "2024-09-15")
    private LocalDateTime expectedStart;

    @Schema(description = "Ride expected end date", example = "2024-09-16")
    private LocalDateTime expectedEnd;

    @Schema(description = "Optional rally ID if associated with a rally", example = "1")
    private Integer rallyId;

    /** Seconds, default stop duration for all waypoints, can be overridden individually. */
    private Integer stopDuration;

    @Schema(description = "Ride actual start date", example = "2024-09-15")
    private LocalDateTime actualStart;

    @Schema(description = "Ride actual end date", example = "2024-09-16")
    private LocalDateTime actualEnd;

    @Schema(description = "Link to Spotwalla")
    private String spotwallaLink;

    @Schema(description = "Odometer at start", example = "24010")
    private Integer odometerStart;

    @Schema(description = "Odomteter at end", example = "25022")
    private Integer odometerEnd;
}
