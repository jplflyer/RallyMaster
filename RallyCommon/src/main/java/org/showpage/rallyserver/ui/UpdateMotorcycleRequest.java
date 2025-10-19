package org.showpage.rallyserver.ui;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.showpage.rallyserver.entity.OwnershipStatus;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateMotorcycleRequest {
    private String make;
    private String model;
    private Integer year;
    private String color;
    private OwnershipStatus status;
    private Boolean active;
}
