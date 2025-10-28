package org.showpage.rallyserver.ui;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Member account information")
public class UiMember {
    @Schema(description = "Unique member identifier", example = "1")
    private Integer id;

    @Schema(description = "Member email address", example = "rider@example.com", required = true)
    private String email;

    @Schema(description = "Spotwalla tracking username", example = "johndoe")
    private String spotwallaUsername;

    @Schema(description = "List of motorcycles owned by this member")
    private List<UiMotorcycle> motorcycles;

    @Schema(description = "List of rallies this member is participating in")
    private List<UiRallyParticipation> rallyParticipations;
}
