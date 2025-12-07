package org.showpage.rallyserver.ui;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.showpage.rallyserver.entity.OwnershipStatus;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to update an existing motorcycle")
public class UpdateMotorcycleRequest {
    @Schema(description = "Motorcycle make/manufacturer", example = "BMW")
    private String make;

    @Schema(description = "Motorcycle model", example = "R1250GS")
    private String model;

    @Schema(description = "Motorcycle year", example = "2023")
    private Integer year;

    @Schema(description = "Motorcycle color", example = "Black")
    private String color;

    @Schema(description = "Ownership status (OWNED, LEASED, SOLD)", example = "OWNED")
    private OwnershipStatus status;

    @Schema(description = "Whether this motorcycle is currently active", example = "true")
    private Boolean active;
}
