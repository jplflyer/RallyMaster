package org.showpage.rallyserver.ui;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.time.LocalDate;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UpdateRallyRequest {
    private String name;
    private String description;

    private LocalDate startDate;
    private LocalDate endDate;
    private String locationCity;
    private String locationState;
    private String locationCountry; /** 2-digit code. */
    private Boolean isPublic;
    private Boolean pointsPublic;
    private Boolean ridersPublic;
    private Boolean organizersPublic;
}
