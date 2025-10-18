package org.showpage.rallyserver.ui;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateEarnedCombinationRequest {
    private Integer riderId;  // The member ID of the rider
    private Integer combinationId;
    private Boolean confirmed;
}
