package org.showpage.rallyserver.ui;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.showpage.rallyserver.exception.ValidationException;
import org.showpage.rallyserver.util.DataValidator;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Request to create a new route for a ride")
public class CreateRouteRequest {
    @Schema(description = "Route name", example = "Primary Route", required = true)
    private String name;

    @Schema(description = "Route description", example = "Scenic highway route")
    private String description;

    @Schema(description = "Whether this is the primary route", example = "true")
    private Boolean isPrimary;

    public void checkValid() throws ValidationException {
        DataValidator.validate(name, "Name");
    }
}
