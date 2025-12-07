package org.showpage.rallyserver.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.showpage.rallyserver.interfaces.HasId;

import java.util.List;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RideLeg implements HasId<RideLeg> {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "route_id", nullable = false, insertable = false, updatable = false)
    private Route route;

    @Column(name = "route_id", nullable = false)
    private Integer routeId;

    @Column(nullable = false)
    private String name;

    private String description;

    /** Sequence order of this leg within the route. */
    private Integer sequenceOrder;

    @OneToMany(
            mappedBy = "rideLeg",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    private List<Waypoint> waypoints;
}
