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
@Schema(description = "Request to create a new ride leg")
public class CreateRideLegRequest {
    @Schema(description = "Leg name", example = "Day 1", required = true)
    private String name;

    @Schema(description = "Leg description", example = "Morning segment")
    private String description;

    @Schema(description = "Sequence order", example = "1")
    private Integer sequenceOrder;

    public void checkValid() throws ValidationException {
        DataValidator.validate(name, "Name");
    }
}
