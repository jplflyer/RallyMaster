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
@Schema(description = "Request to change a member's password")
public class ChangePasswordRequest {
    @Schema(description = "Current password", example = "oldP@ssw0rd", required = true)
    private String oldPassword;

    @Schema(description = "New password", example = "newP@ssw0rd", required = true)
    private String newPassword;
}
