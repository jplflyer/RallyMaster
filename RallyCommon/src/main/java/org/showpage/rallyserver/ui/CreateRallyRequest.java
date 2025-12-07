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

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Request to create a new rally event")
public class CreateRallyRequest {
    @Schema(description = "Rally name", example = "Iron Butt Rally 2024", required = true)
    private String name;

    @Schema(description = "Rally description", example = "The world's toughest motorcycle rally", required = true)
    private String description;

    @Schema(description = "Rally start date (defaults to end date if not provided)", example = "2024-08-20")
    private LocalDate startDate;

    @Schema(description = "Rally end date (defaults to start date if not provided)", example = "2024-08-31")
    private LocalDate endDate;

    @Schema(description = "Rally headquarters latitude", example = "42.8864")
    private Float latitude;

    @Schema(description = "Rally headquarters longitude", example = "-78.8784")
    private Float longitude;

    @Schema(description = "Rally location city", example = "Buffalo", required = true)
    private String locationCity;

    @Schema(description = "Rally location state/province", example = "NY", required = true)
    private String locationState;

    @Schema(description = "Rally location country (2-digit code)", example = "US")
    private String locationCountry;

    @Schema(description = "Whether rally is publicly visible", example = "true")
    private Boolean isPublic;

    @Schema(description = "Whether bonus points are publicly visible", example = "true")
    private Boolean pointsPublic;

    @Schema(description = "Whether rider list is publicly visible", example = "true")
    private Boolean ridersPublic;

    @Schema(description = "Whether organizer list is publicly visible", example = "true")
    private Boolean organizersPublic;

    /**
     * We must have a name, description. start/end date, and location info.
     * @return
     */
    public void checkValid() throws ValidationException {
        if (startDate == null) {
            startDate = endDate;
        }
        if (endDate == null) {
            endDate = startDate;
        }

        DataValidator.validate(
                name, "Name",
                description, "Description",
                startDate, "Start Date",
                locationCity, "Location City",
                locationState, "Location State"
        );
    }
}
