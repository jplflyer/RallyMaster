package org.showpage.rallyserver.ui;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Request to update an existing rally event (all fields optional)")
public class UpdateRallyRequest {
    @Schema(description = "Rally name", example = "Iron Butt Rally 2024")
    private String name;

    @Schema(description = "Rally description", example = "The world's toughest motorcycle rally")
    private String description;

    @Schema(description = "Rally start date", example = "2024-08-20")
    private LocalDate startDate;

    @Schema(description = "Rally end date", example = "2024-08-31")
    private LocalDate endDate;

    @Schema(description = "Rally headquarters latitude", example = "42.8864")
    private Float latitude;

    @Schema(description = "Rally headquarters longitude", example = "-78.8784")
    private Float longitude;

    @Schema(description = "Rally location city", example = "Buffalo")
    private String locationCity;

    @Schema(description = "Rally location state/province", example = "NY")
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
}
