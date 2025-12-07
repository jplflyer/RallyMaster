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
@Schema(description = "Request to update member profile information")
public class UpdateMemberRequest {
    @Schema(description = "Spotwalla tracking username", example = "johndoe")
    private String spotwallaUsername;
}
