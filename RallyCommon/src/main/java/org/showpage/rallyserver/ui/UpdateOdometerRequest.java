package org.showpage.rallyserver.ui;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to update a rider's odometer reading")
public class UpdateOdometerRequest {
    @Schema(description = "Member ID of the rider", example = "5", required = true)
    private Integer riderId;

    @Schema(description = "Odometer reading value", example = "47500", required = true)
    private Integer odometer;
}
