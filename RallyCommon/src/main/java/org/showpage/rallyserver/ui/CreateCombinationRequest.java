package org.showpage.rallyserver.ui;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCombinationRequest {
    private String code;
    private String name;
    private String description;
    private Integer points;
    private Boolean requiresAll;
    private Integer numRequired;
    private List<CreateCombinationPointRequest> combinationPoints;
}
