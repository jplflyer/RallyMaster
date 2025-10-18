package org.showpage.rallyserver.entity;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Combination {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false,  cascade = CascadeType.ALL)
    @JoinColumn(name = "rally_id")
    private Rally rally;

    @Column(name = "rally_id", insertable = false, updatable = false)
    private Integer rallyId;

    private String code;
    private String name;
    private String description;
    private Integer points;
    private Boolean requiresAll;
    private Integer numRequired;

    @OneToMany(
            mappedBy = "combination",                // owning side is RallyParticipant.rally
            cascade = CascadeType.ALL,         // persist/update/remove participants with the rally
            orphanRemoval = true,              // remove rows when detached from collection
            fetch = FetchType.LAZY
    )
    private List<CombinationPoint> combinationPoints;
}
