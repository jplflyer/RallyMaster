package org.showpage.rallyserver.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * In Rallies a stop where you earn points is referred to as a Bonus Point, which is confusing,
 * as Point in this case refers to a location. But Bonus Points have Points, which is a number.
 */
@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BonusPoint {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rally_id")
    private Rally rally;

    @Column(name = "rally_id", insertable = false, updatable = false)
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
    private Boolean isStart;
    private Boolean isFinish;
    private String markerColor;
    private String markerIcon;
}
