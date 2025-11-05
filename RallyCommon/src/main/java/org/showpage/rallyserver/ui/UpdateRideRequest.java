package org.showpage.rallyserver.ui;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

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

    @Schema(description = "Ride start date", example = "2024-09-15")
    private LocalDate startDate;

    @Schema(description = "Ride end date", example = "2024-09-16")
    private LocalDate endDate;

    @Schema(description = "Optional rally ID if associated with a rally", example = "1")
    private Integer rallyId;
}
