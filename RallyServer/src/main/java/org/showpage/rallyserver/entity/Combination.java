package org.showpage.rallyserver.entity;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Combination {
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
    private Integer points;
    private Boolean requiresAll;
    private Integer numRequired;
    private String markerColor;
    private String markerIcon;

    @OneToMany(
            mappedBy = "combination",                // owning side is RallyParticipant.rally
            cascade = CascadeType.ALL,         // persist/update/remove participants with the rally
            orphanRemoval = true,              // remove rows when detached from collection
            fetch = FetchType.LAZY
    )
    private List<CombinationPoint> combinationPoints;

    public Combination addCombinationPoint(CombinationPoint cp) {
        if (combinationPoints == null) {
            combinationPoints = new ArrayList<>();
        }
        combinationPoints.add(cp);
        cp.setCombination(this);
        return this;
    }

    public Combination removeCombinationPoint(CombinationPoint cp) {
        if (combinationPoints != null) {
            combinationPoints.remove(cp);
        }
        cp.setCombination(null);
        return this;
    }
}
