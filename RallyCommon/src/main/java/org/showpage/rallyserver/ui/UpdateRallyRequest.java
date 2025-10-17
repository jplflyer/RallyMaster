package org.showpage.rallyserver.ui;

import lombok.Data;

import java.time.LocalDate;

@Data
public class UpdateRallyRequest {
    private String name;
    private String description;

    private LocalDate startDate;
    private LocalDate endDate;
    private String locationCity;
    private String locationState;
    private String locationCountry; /** 2-digit code. */
    private Boolean isPublic;
}
