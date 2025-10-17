package org.showpage.rallyserver.ui;

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
public class CreateRallyRequest {
    private String name;
    private String description;

    private LocalDate startDate;
    private LocalDate endDate;
    private String locationCity;
    private String locationState;
    private String locationCountry; /** 2-digit code. */
    private Boolean isPublic;

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
