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
public class Route implements HasId<Route> {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ride_id", nullable = false, insertable = false, updatable = false)
    private Ride ride;

    @Column(name = "ride_id", nullable = false)
    private Integer rideId;

    @Column(nullable = false)
    private String name;

    private String description;

    /** Whether this is the primary/selected route for the ride. */
    @Builder.Default
    private Boolean isPrimary = Boolean.FALSE;

    @OneToMany(
            mappedBy = "route",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    private List<RideLeg> rideLegs;
}
