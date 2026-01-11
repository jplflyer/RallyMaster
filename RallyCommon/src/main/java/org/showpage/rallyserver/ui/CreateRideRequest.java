package org.showpage.rallyserver.ui;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.showpage.rallyserver.exception.ValidationException;
import org.showpage.rallyserver.util.DataValidator;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Request to create a new ride")
public class CreateRideRequest {
    @Schema(description = "Ride name", example = "Saddlesore 1000", required = true)
    private String name;

    @Schema(description = "Ride description", example = "1000 miles in 24 hours")
    private String description;

    @Schema(description = "Ride expected start date", example = "2024-09-15")
    private LocalDateTime expectedStart;

    @Schema(description = "Ride expected end date", example = "2024-09-16")
    private LocalDateTime expectedEnd;

    @Schema(description = "Optional rally ID if associated with a rally", example = "1")
    private Integer rallyId;

    @Schema(description = "Starting bonus point ID (for rally rides)", example = "1")
    private Integer startingBonusPointId;

    @Schema(description = "Ending bonus point ID (for rally rides)", example = "1")
    private Integer endingBonusPointId;

    @Schema(description = "Default duration of stops in seconds", example = "90")
    private Integer stopDuration;

    @Schema(description = "Link to Spotwalla")
    private String spotwallaLink;

    public void checkValid() throws ValidationException {
        DataValidator.validate(name, "Name");

        if (expectedStart != null && expectedEnd != null && expectedEnd.isBefore(expectedStart)) {
            throw new ValidationException("End date cannot be before start date");
        }
    }
}
