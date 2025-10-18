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
public class UiBonusPoint {
    private Integer id;
    private Integer rallyId;
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
