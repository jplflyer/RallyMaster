package org.showpage.rallyserver.ui;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateBonusPointRequest {
    private String code;
    private String name;
    private String description;
    private Double latitude;
    private Double longitude;
    private String address;
    private Integer points;
    private Boolean required;
    private Boolean repeatable;
}
