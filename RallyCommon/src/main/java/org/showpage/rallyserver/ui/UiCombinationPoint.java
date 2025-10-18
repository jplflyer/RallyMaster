package org.showpage.rallyserver.ui;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UiCombinationPoint {
    private Integer id;
    private Integer combinationId;
    private Integer bonusPointId;
    private Boolean required;
}
