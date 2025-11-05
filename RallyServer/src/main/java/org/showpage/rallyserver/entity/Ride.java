package org.showpage.rallyserver.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.showpage.rallyserver.interfaces.HasId;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Ride implements HasId<Ride> {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false, insertable = false, updatable = false)
    private Member member;

    @Column(name = "member_id", nullable = false)
    private Integer memberId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rally_id", insertable = false, updatable = false)
    private Rally rally;

    @Column(name = "rally_id")
    private Integer rallyId;

    @Column(nullable = false)
    private String name;

    private String description;

    private LocalDateTime expectedStart;
    private LocalDateTime expectedEnd;

    /** Seconds, default stop duration for all waypoints, can be overridden individually. */
    private Integer stopDuration;

    private String spotwallaLink;

    private LocalDateTime actualStart;
    private LocalDateTime actualEnd;
    private Integer odometerStart;
    private Integer odometerEnd;

    @OneToMany(
            mappedBy = "ride",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    private List<Route> routes;
}
