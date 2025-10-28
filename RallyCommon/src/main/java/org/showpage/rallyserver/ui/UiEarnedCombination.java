package org.showpage.rallyserver.ui;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Earned combination record for a rally participant")
public class UiEarnedCombination {
    @Schema(description = "Unique earned combination identifier", example = "1")
    private Integer id;

    @Schema(description = "Rally participant ID who earned this combination", example = "5")
    private Integer rallyParticipantId;

    @Schema(description = "Combination ID that was earned", example = "3")
    private Integer combinationId;

    @Schema(description = "Whether this earned combination has been confirmed by organizers", example = "true")
    private Boolean confirmed;
}
