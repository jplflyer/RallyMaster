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
public class UiMember {
    private Integer id;
    private String email;
    private String spotwallaUsername;
    private List<UiMotorcycle> motorcycles;
    private List<UiRallyParticipation> rallyParticipations;
}
