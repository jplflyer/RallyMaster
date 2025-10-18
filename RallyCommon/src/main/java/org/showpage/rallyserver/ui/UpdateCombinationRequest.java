package org.showpage.rallyserver.ui;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCombinationRequest {
    private String code;
    private String name;
    private String description;
    private Integer points;
    private Boolean requiresAll;
    private Integer numRequired;
}
