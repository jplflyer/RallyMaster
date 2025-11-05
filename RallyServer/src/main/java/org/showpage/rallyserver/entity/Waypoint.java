package org.showpage.rallyserver.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.showpage.rallyserver.interfaces.HasId;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Waypoint implements HasId<Waypoint> {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ride_leg_id", nullable = false, insertable = false, updatable = false)
    private RideLeg rideLeg;

    @Column(name = "ride_leg_id", nullable = false)
    private Integer rideLegId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bonus_point_id", insertable = false, updatable = false)
    private BonusPoint bonusPoint;

    @Column(name = "bonus_point_id")
    private Integer bonusPointId;

    @Column(nullable = false)
    private String name;

    private String description;

    /** Sequence order of this waypoint within the leg. */
    private Integer sequenceOrder;

    /** Location information (can be null if bonusPointId is set). */
    private Float latitude;
    private Float longitude;
    private String address;
}
