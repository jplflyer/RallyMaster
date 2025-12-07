package org.showpage.rallyserver.ui;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.showpage.rallyserver.interfaces.HasId;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Rally event details including dates, location, and associated bonus points")
public class UiRally implements HasId<UiRally> {
    @Schema(description = "Unique rally identifier", example = "1")
    private Integer id;

    @Schema(description = "Rally name", example = "Iron Butt Rally 2024", required = true)
    private String name;

    @Schema(description = "Rally description", example = "The world's toughest motorcycle rally")
    private String description;

    @Schema(description = "Rally start date", example = "2024-08-20")
    private LocalDate startDate;

    @Schema(description = "Rally end date", example = "2024-08-31")
    private LocalDate endDate;

    @Schema(description = "Rally location city", example = "Buffalo")
    private String locationCity;

    @Schema(description = "Rally location state/province", example = "NY")
    private String locationState;

    @Schema(description = "Rally location country (2-digit code)", example = "US")
    private String locationCountry;

    @Schema(description = "Rally latitude coordinate", example = "42.8864")
    private Float latitude;

    @Schema(description = "Rally longitude coordinate", example = "-78.8784")
    private Float longitude;

    @Schema(description = "Whether rally is publicly visible", example = "true")
    private Boolean isPublic;

    @Schema(description = "Whether bonus points are publicly visible", example = "true")
    private Boolean pointsPublic;

    @Schema(description = "Whether rider list is publicly visible", example = "true")
    private Boolean ridersPublic;

    @Schema(description = "Whether organizer list is publicly visible", example = "true")
    private Boolean organizersPublic;

    @Schema(description = "List of rally participants")
    private List<UiRallyParticipant> participants;

    @Schema(description = "List of bonus points available in this rally")
    private List<UiBonusPoint> bonusPoints;

    @Schema(description = "List of bonus point combinations in this rally")
    private List<UiCombination> combinations;
}
